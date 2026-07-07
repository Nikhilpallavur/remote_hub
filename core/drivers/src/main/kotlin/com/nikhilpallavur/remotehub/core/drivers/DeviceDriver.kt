package com.nikhilpallavur.remotehub.core.drivers

import com.nikhilpallavur.remotehub.core.model.RemoteDevice

/**
 * A pluggable device family (Android TV, Roku, an AC brand, a light bridge, …). A driver is pure
 * capability + a factory for live connections; it holds no per-connection state. Contribute one
 * with Hilt `@Binds @IntoSet` and the whole app — Home grid, discovery, pairing, remote UI —
 * picks it up with no other changes. This is the Open/Closed seam of the platform.
 */
interface DeviceDriver {

    val descriptor: DriverDescriptor

    /** Creates a fresh, unconnected link for this driver's transport/protocol. */
    fun createConnection(): RemoteConnection

    /**
     * Builds a device from a manually entered Wi-Fi host, or null if this driver cannot be
     * added by IP. Defaults to a sensible Wi-Fi device using the descriptor's port.
     */
    fun manualDevice(host: String): RemoteDevice? {
        val d = descriptor
        return RemoteDevice(
            id = RemoteDevice.idFor(d.id, host),
            name = d.displayName,
            driverId = d.id,
            category = d.category,
            transport = d.transport,
            host = host,
            port = d.defaultPort,
        )
    }
}
