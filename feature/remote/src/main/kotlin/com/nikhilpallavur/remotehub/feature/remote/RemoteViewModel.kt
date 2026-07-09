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
    }

    /**
     * Lands a returning user straight on the remote: the most recently used saved device connects
     * automatically at launch. Guarded on Idle so it never stomps a connect the user started first.
     */
    private fun autoConnectLastDevice() {
        viewModelScope.launch {
            val last = store.devices.first()
                .filter { it.paired && it.lastConnectedEpochMs > 0L }
                .maxByOrNull { it.lastConnectedEpochMs }
                ?: return@launch
            if (manager.state.value is ConnectionState.Idle) connect(last)
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
        climate.value = ClimateSettings()
        // Best-effort wake first: a standby TV won't answer the connect, but it will hear WoL.
        device.macAddress?.let(manager::wakeOnLan)
        manager.connect(device)
    }

    fun connectManual(host: String, driverId: String) {
        if (host.isBlank()) return
        climate.value = ClimateSettings()
        registry.byId(driverId)?.manualDevice(host.trim())?.let(manager::connect)
    }

    /** Connects a hostless (infrared) driver's virtual device — no address to type. */
    fun connectDriver(driverId: String) {
        climate.value = ClimateSettings()
        registry.byId(driverId)?.manualDevice("")?.let(manager::connect)
    }

    fun submitPairingCode(code: String) = manager.submitPairingCode(code)

    fun press(key: RemoteKey) = manager.send(RemoteCommand.Press(key))

    fun sendText(text: String) = manager.send(RemoteCommand.TypeText(text))

    fun disconnect() = manager.disconnect()

    fun forget(device: RemoteDevice) {
        if (uiState.value.connectedDevice?.id == device.id) manager.disconnect()
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
        manager.disconnect()
    }

    private companion object {
        const val SCAN_DURATION_MS = 8000L
        const val STOP_TIMEOUT_MS = 5000L
    }
}
