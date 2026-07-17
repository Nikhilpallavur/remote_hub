package com.nikhilpallavur.remotehub.feature.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.drivers.DriverRegistry
import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.FanSpeed
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.core.transport.connection.RemoteConnectionManager
import com.nikhilpallavur.remotehub.core.transport.discovery.DeviceDiscoveryService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val manager: RemoteConnectionManager,
    private val discovery: DeviceDiscoveryService,
    private val store: com.nikhilpallavur.remotehub.core.drivers.PairedDeviceStore,
    private val registry: DriverRegistry,
) : ViewModel() {

    private val discovered = MutableStateFlow<List<RemoteDevice>>(emptyList())
    private val scanning = MutableStateFlow(false)
    private val climate = MutableStateFlow(ClimateSettings())
    private var scanJob: Job? = null
    private var reconnectJob: Job? = null

    /** True while Idle is what the user asked for (disconnect/forget), so drops aren't "rescued". */
    private var suppressReconnect = false

    val uiState: StateFlow<RemoteUiState> = combine(
        manager.state,
        store.devices,
        discovered,
        scanning,
        climate,
    ) { connection, paired, found, isScanning, climateSettings ->
        val descriptor = descriptorFor(connection)
        RemoteUiState(
            connection = connection,
            discovered = found,
            paired = paired,
            isScanning = isScanning,
            capabilities = descriptor?.capabilities ?: emptySet(),
            climate = climateSettings,
            temperatureRangeC = descriptor?.temperatureRangeC ?: DEFAULT_TEMPERATURE_RANGE_C,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), RemoteUiState())

    /** Driver descriptors offered when the user adds a device manually by IP. */
    val manualDrivers: List<DriverDescriptor> = registry.descriptors()

    init {
        startScan()
        autoConnectLastDevice()
        reconnectOnDrop()
    }

    /**
     * Lands a returning user straight on the remote: the most recently used saved device connects
     * automatically at launch. Guarded on Idle so it never stomps a connect the user started first.
     * When the attempt fails outright, the saved address is often just stale (the router leased the
     * TV a new IP since last time), so the rescue path re-discovers the TV by identity, refreshes
     * the stored host, and tries once more.
     */
    private fun autoConnectLastDevice() {
        viewModelScope.launch {
            val last = store.devices.first()
                .filter { it.paired && it.lastConnectedEpochMs > 0L }
                .maxByOrNull { it.lastConnectedEpochMs }
                ?: return@launch
            if (manager.state.value !is ConnectionState.Idle) return@launch
            connect(last)
            val settled = withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
                manager.state.first { it is ConnectionState.Connected || it is ConnectionState.Failed }
            }
            if (settled is ConnectionState.Failed) retryWithFreshAddress(last)
        }
    }

    /** Re-discovers [last] by driver + name, updates its stored host if it moved, reconnects. */
    private suspend fun retryWithFreshAddress(last: RemoteDevice) {
        val fresh = withTimeoutOrNull(SCAN_DURATION_MS) {
            discovery.discover().first {
                it.driverId == last.driverId && it.name == last.name && !it.host.isNullOrBlank()
            }
        } ?: return
        if (fresh.host == last.host) return // Same address — the TV is genuinely off or unreachable.
        val refreshed = last.copy(host = fresh.host, port = if (fresh.port != 0) fresh.port else last.port)
        store.upsert(refreshed)
        val current = manager.state.value
        if (current is ConnectionState.Failed || current is ConnectionState.Idle) connect(refreshed)
    }

    /**
     * A session that dies without the user asking (TV rebooted, Wi-Fi blip, the TV briefly bumping
     * this client while another phone joins) used to strand the app on Idle until a manual tap.
     * Watching for the Connected → Idle edge and quietly redialing keeps the remote usable through
     * those hiccups — key for households where several phones share one TV.
     */
    private fun reconnectOnDrop() {
        viewModelScope.launch {
            var lastConnected: RemoteDevice? = null
            manager.state.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> lastConnected = state.device
                    is ConnectionState.Idle -> {
                        val dropped = lastConnected
                        lastConnected = null
                        if (dropped != null && !suppressReconnect) scheduleReconnect(dropped)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun scheduleReconnect(device: RemoteDevice) {
        reconnectJob?.cancel()
        reconnectJob = viewModelScope.launch {
            for (backoffMs in RECONNECT_BACKOFF_MS) {
                delay(backoffMs)
                val current = manager.state.value
                val takenOver = current is ConnectionState.Connected ||
                    current is ConnectionState.Connecting ||
                    current is ConnectionState.AwaitingPairing
                if (takenOver) return@launch
                // Straight to the manager: the public connect() cancels reconnectJob (user intent
                // wins over background retries), which would cancel this very loop.
                device.macAddress?.let(manager::wakeOnLan)
                manager.connect(device)
                val settled = withTimeoutOrNull(SETTLE_TIMEOUT_MS) {
                    manager.state.first { it is ConnectionState.Connected || it is ConnectionState.Failed }
                }
                if (settled is ConnectionState.Connected) return@launch
            }
        }
    }

    fun startScan() {
        scanJob?.cancel()
        discovered.value = emptyList()
        scanning.value = true
        scanJob = viewModelScope.launch {
            val collector = launch {
                discovery.discover().collect { device ->
                    discovered.update { (it + device).distinctBy(RemoteDevice::id) }
                }
            }
            delay(SCAN_DURATION_MS)
            collector.cancel()
            scanning.value = false
        }
    }

    fun connect(device: RemoteDevice) {
        beginUserConnect()
        // Best-effort wake first: a standby TV won't answer the connect, but it will hear WoL.
        device.macAddress?.let(manager::wakeOnLan)
        manager.connect(device)
    }

    fun connectManual(host: String, driverId: String) {
        if (host.isBlank()) return
        beginUserConnect()
        registry.byId(driverId)?.manualDevice(host.trim())?.let(manager::connect)
    }

    /** Connects a hostless (infrared) driver's virtual device — no address to type. */
    fun connectDriver(driverId: String) {
        beginUserConnect()
        registry.byId(driverId)?.manualDevice("")?.let(manager::connect)
    }

    /** A deliberate connect supersedes any background reconnect and re-arms drop rescue. */
    private fun beginUserConnect() {
        suppressReconnect = false
        reconnectJob?.cancel()
        climate.value = ClimateSettings()
    }

    fun submitPairingCode(code: String) = manager.submitPairingCode(code)

    fun press(key: RemoteKey) = manager.send(RemoteCommand.Press(key))

    fun sendText(text: String) = manager.send(RemoteCommand.TypeText(text))

    fun disconnect() {
        suppressReconnect = true
        reconnectJob?.cancel()
        manager.disconnect()
    }

    fun forget(device: RemoteDevice) {
        if (uiState.value.connectedDevice?.id == device.id) disconnect()
        viewModelScope.launch { store.remove(device.id) }
    }

    fun toggleFavorite(device: RemoteDevice) {
        viewModelScope.launch { store.setFavorite(device.id, !device.favorite) }
    }

    fun wakeOnLan(mac: String) = manager.wakeOnLan(mac)

    /**
     * Climate controls update the optimistic mirror first, then send the command; one-way IR
     * has no acknowledgement, so the UI reflects what was requested.
     */
    fun toggleAcPower() {
        climate.update { it.copy(power = !it.power) }
        manager.send(RemoteCommand.Press(RemoteKey.POWER))
    }

    fun setTemperature(celsius: Int) {
        val clamped = celsius.coerceIn(uiState.value.temperatureRangeC)
        climate.update { it.copy(temperatureC = clamped) }
        manager.send(RemoteCommand.SetTemperature(clamped))
    }

    fun setClimateMode(mode: ClimateMode) {
        climate.update { it.copy(mode = mode) }
        manager.send(RemoteCommand.SetMode(mode))
    }

    fun setFanSpeed(speed: FanSpeed) {
        climate.update { it.copy(fan = speed) }
        manager.send(RemoteCommand.SetFanSpeed(speed))
    }

    fun setSwing(enabled: Boolean) {
        climate.update { it.copy(swing = enabled) }
        manager.send(RemoteCommand.SetSwing(enabled))
    }

    private fun descriptorFor(connection: ConnectionState): DriverDescriptor? {
        val device = (connection as? ConnectionState.Connected)?.device ?: return null
        return registry.byId(device.driverId)?.descriptor
    }

    override fun onCleared() {
        super.onCleared()
        suppressReconnect = true
        manager.disconnect()
    }

    private companion object {
        const val SCAN_DURATION_MS = 8000L
        const val STOP_TIMEOUT_MS = 5000L

        /** How long a connect attempt may take to settle before rescue logic moves on. */
        const val SETTLE_TIMEOUT_MS = 15_000L
        val RECONNECT_BACKOFF_MS = listOf(1000L, 2000L, 4000L)
    }
}
