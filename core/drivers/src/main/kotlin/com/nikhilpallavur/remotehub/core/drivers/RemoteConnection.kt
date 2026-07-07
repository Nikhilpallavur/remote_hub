package com.nikhilpallavur.remotehub.core.drivers

import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One live link to a device over its driver's transport. Implementations are stateful (they own
 * sockets / WebSockets / IR handles) and drive [state] through the [ConnectionState] lifecycle.
 * The connection manager owns exactly one of these at a time.
 */
interface RemoteConnection {
    val descriptor: DriverDescriptor

    val state: StateFlow<ConnectionState>

    /**
     * Opens the link to [device]. For confirm-on-device drivers this also triggers the on-screen
     * allow prompt; for PIN drivers it advances to [ConnectionState.AwaitingPairing] and waits for
     * [submitPairingCode]. Never throws — failures surface as [ConnectionState.Failed].
     */
    suspend fun connect(device: RemoteDevice)

    /** Completes a [com.nikhilpallavur.remotehub.core.model.PairingMode.PIN_CODE] handshake. */
    suspend fun submitPairingCode(code: String)

    /** Sends a single command. Returns false if the link was not ready or the send failed. */
    suspend fun send(command: RemoteCommand): Boolean

    /** Whether this driver can express [command] at all (used to enable/disable UI controls). */
    fun supports(command: RemoteCommand): Boolean

    /** Tears down resources and returns to [ConnectionState.Idle]. Safe to call repeatedly. */
    fun close()
}

/** Shared [state] plumbing so concrete connections only implement transport logic. */
abstract class BaseRemoteConnection(
    override val descriptor: DriverDescriptor,
) : RemoteConnection {

    protected val mutableState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = mutableState.asStateFlow()

    override suspend fun submitPairingCode(code: String) = Unit

    protected fun fail(device: RemoteDevice?, reason: String) {
        mutableState.value = ConnectionState.Failed(device, reason)
    }
}
