package com.nikhilpallavur.remotehub.core.model

import kotlinx.serialization.Serializable

/**
 * A controllable endpoint — freshly discovered, manually added, or remembered. The same shape
 * serves all three; [paired]/[token] distinguish a remembered device, and [driverId] records
 * which driver knows how to talk to it so the connection layer never has to guess.
 */
@Serializable
data class RemoteDevice(
    val id: String,
    val name: String,
    val driverId: String,
    val category: DeviceCategory,
    val transport: Transport,
    /** Host/IP for Wi-Fi devices; null for IR (stateless) and unset Bluetooth devices. */
    val host: String? = null,
    val port: Int = 0,
    val model: String? = null,
    val macAddress: String? = null,
    val bluetoothAddress: String? = null,
    val paired: Boolean = false,
    /** Opaque per-driver credential: Samsung token / LG client-key / Android TV cert alias. */
    val token: String? = null,
    val favorite: Boolean = false,
    val lastConnectedEpochMs: Long = 0L,
) {
    companion object {
        /** Stable identity so re-discovery and persistence converge on one entry. */
        fun idFor(driverId: String, endpoint: String): String = "$driverId@$endpoint"
    }
}
