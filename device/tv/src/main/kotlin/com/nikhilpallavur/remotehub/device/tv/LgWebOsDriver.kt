package com.nikhilpallavur.remotehub.device.tv

import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import com.nikhilpallavur.remotehub.core.drivers.DiscoverySpec
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.drivers.RemoteConnection
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.DeviceCategory
import com.nikhilpallavur.remotehub.core.model.PairingMode
import com.nikhilpallavur.remotehub.core.model.Transport
import com.nikhilpallavur.remotehub.core.transport.Ssdp
import com.nikhilpallavur.remotehub.device.tv.connection.LgWebOsConnection
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/** Drives LG webOS TVs over the SSAP WebSocket + pointer socket with a confirm-on-TV handshake. */
@Singleton
class LgWebOsDriver @Inject constructor(
    private val client: OkHttpClient,
) : DeviceDriver {

    override val descriptor = DriverDescriptor(
        id = "lg-webos",
        displayName = "LG (webOS)",
        manufacturer = "LG",
        category = DeviceCategory.TELEVISION,
        transport = Transport.WIFI,
        capabilities = setOf(
            DeviceCapability.POWER,
            DeviceCapability.DPAD,
            DeviceCapability.TOUCHPAD,
            DeviceCapability.VOLUME,
            DeviceCapability.CHANNEL,
            DeviceCapability.MEDIA,
        ),
        pairingMode = PairingMode.CONFIRM_ON_DEVICE,
        discovery = DiscoverySpec(
            ssdpSearchTargets = listOf(Ssdp.ST_LG),
            ssdpMatchKeywords = listOf("webos", "lg"),
        ),
        defaultPort = 3000,
    )

    override fun createConnection(): RemoteConnection = LgWebOsConnection(descriptor, client)
}
