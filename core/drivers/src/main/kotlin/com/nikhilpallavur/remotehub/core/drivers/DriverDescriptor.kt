package com.nikhilpallavur.remotehub.core.drivers

import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.DeviceCategory
import com.nikhilpallavur.remotehub.core.model.PairingMode
import com.nikhilpallavur.remotehub.core.model.Transport

/**
 * The static, declarative identity of a driver. Everything the app needs to list, discover,
 * pair, and render a device is described here — so the UI, discovery, and connection layers are
 * driven entirely by data a driver contributes, never by conditionals that know about it.
 */
data class DriverDescriptor(
    /** Stable unique key persisted on every [com.nikhilpallavur.remotehub.core.model.RemoteDevice]. */
    val id: String,
    val displayName: String,
    val category: DeviceCategory,
    val transport: Transport,
    val capabilities: Set<DeviceCapability>,
    val manufacturer: String? = null,
    /** null when the device needs no authorization step (Roku, most IR). */
    val pairingMode: PairingMode? = null,
    val discovery: DiscoverySpec = DiscoverySpec.None,
    val defaultPort: Int = 0,
    /** Valid target-temperature bounds for TEMPERATURE-capable drivers; null when not applicable. */
    val temperatureRangeC: IntRange? = null,
) {
    val requiresPairing: Boolean get() = pairingMode != null
}

/**
 * How a driver's devices announce themselves on the network. The generic discovery service reads
 * these hints from every registered driver and runs one shared mDNS + SSDP sweep for all of them.
 */
data class DiscoverySpec(
    /** mDNS/NSD service types, e.g. "_androidtvremote2._tcp.". */
    val nsdServiceTypes: List<String> = emptyList(),
    /** SSDP search targets to probe for, e.g. "roku:ecp". */
    val ssdpSearchTargets: List<String> = emptyList(),
    /**
     * Lower-cased tokens that identify this driver's devices in an SSDP response's
     * ST/SERVER/USN headers, e.g. ["roku"] or ["samsung", "tizen"]. The generic discovery
     * service attributes a response to the first driver whose keyword it contains.
     */
    val ssdpMatchKeywords: List<String> = emptyList(),
) {
    val isDiscoverable: Boolean
        get() = nsdServiceTypes.isNotEmpty() || ssdpSearchTargets.isNotEmpty()

    companion object {
        val None = DiscoverySpec()
    }
}
