package com.nikhilpallavur.remotehub.feature.remote

import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.RemoteDevice

/** Everything the remote screen renders from, derived in the ViewModel. */
data class RemoteUiState(
    val connection: ConnectionState = ConnectionState.Idle,
    val discovered: List<RemoteDevice> = emptyList(),
    val paired: List<RemoteDevice> = emptyList(),
    val isScanning: Boolean = false,
    val capabilities: Set<DeviceCapability> = emptySet(),
    val climate: ClimateSettings = ClimateSettings(),
    val temperatureRangeC: IntRange = DEFAULT_TEMPERATURE_RANGE_C,
) {
    val connectedDevice: RemoteDevice?
        get() = (connection as? ConnectionState.Connected)?.device

    val connectingDevice: RemoteDevice?
        get() = (connection as? ConnectionState.Connecting)?.device

    val awaitingPairing: ConnectionState.AwaitingPairing?
        get() = connection as? ConnectionState.AwaitingPairing

    val failure: ConnectionState.Failed?
        get() = connection as? ConnectionState.Failed

    /** Discovered devices that are not already saved, for a clean "found" section. */
    val newlyDiscovered: List<RemoteDevice>
        get() {
            val savedIds = paired.map { it.id }.toSet()
            return discovered.filter { it.id !in savedIds }
        }
}
