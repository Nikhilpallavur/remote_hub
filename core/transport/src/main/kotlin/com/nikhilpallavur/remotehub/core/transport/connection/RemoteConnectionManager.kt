package com.nikhilpallavur.remotehub.core.transport.connection

import com.nikhilpallavur.remotehub.core.common.IoDispatcher
import com.nikhilpallavur.remotehub.core.common.MainImmediateDispatcher
import com.nikhilpallavur.remotehub.core.drivers.DriverRegistry
import com.nikhilpallavur.remotehub.core.drivers.PairedDeviceStore
import com.nikhilpallavur.remotehub.core.drivers.RemoteConnection
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.transport.WakeOnLan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single owner of the active [RemoteConnection]. It resolves the driver for a device from the
 * [DriverRegistry] (never a hard-coded switch), mirrors that connection's lifecycle into one
 * [state] flow for the UI, and persists credentials the moment a device reports
 * [ConnectionState.Connected].
 */
@Singleton
class RemoteConnectionManager @Inject constructor(
    private val registry: DriverRegistry,
    private val store: PairedDeviceStore,
    @MainImmediateDispatcher private val mainDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private val mutableState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)

    val state: StateFlow<ConnectionState> = mutableState.asStateFlow()

    private var active: RemoteConnection? = null
    private var mirrorJob: Job? = null

    // Commands flow through one queue drained by one worker, so they reach the socket in exactly
    // the order the UI issued them. Launching a coroutine per send let a burst of keyboard-mirror
    // edits race each other onto the IO dispatcher — BACKSPACEs could land after the replacement
    // text, corrupting what the TV shows (and over-deleting acts as BACK on most TVs).
    private val outbound = Channel<RemoteCommand>(Channel.UNLIMITED)

    init {
        scope.launch { for (command in outbound) active?.send(command) }
    }

    fun connect(device: RemoteDevice) {
        disconnect()
        val driver = registry.byId(device.driverId)
        if (driver == null) {
            mutableState.value = ConnectionState.Failed(device, "No driver installed for this device")
            return
        }
        val connection = driver.createConnection()
        active = connection
        mirrorJob = scope.launch {
            connection.state.collect { state ->
                mutableState.value = state
                if (state is ConnectionState.Connected) {
                    store.upsert(state.device.copy(lastConnectedEpochMs = System.currentTimeMillis()))
                }
            }
        }
        scope.launch { connection.connect(device) }
    }

    fun submitPairingCode(code: String) {
        scope.launch { active?.submitPairingCode(code) }
    }

    fun send(command: RemoteCommand) {
        outbound.trySend(command)
    }

    fun activeSupports(command: RemoteCommand): Boolean = active?.supports(command) ?: false

    fun disconnect() {
        mirrorJob?.cancel()
        mirrorJob = null
        active?.close()
        active = null
        // Whatever is still queued was meant for the connection that just died; draining it keeps
        // stale presses from firing at the next device the user connects.
        while (outbound.tryReceive().isSuccess) Unit
        mutableState.value = ConnectionState.Idle
    }

    /** Sends a Wake-on-LAN magic packet to power a standby device on (Samsung/LG mostly). */
    fun wakeOnLan(mac: String) {
        scope.launch(ioDispatcher) {
            runCatching {
                val packet = WakeOnLan.magicPacket(mac)
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val address = InetAddress.getByName(BROADCAST_ADDRESS)
                    socket.send(DatagramPacket(packet, packet.size, address, WakeOnLan.DEFAULT_PORT))
                }
            }
        }
    }

    private companion object {
        const val BROADCAST_ADDRESS = "255.255.255.255"
    }
}
