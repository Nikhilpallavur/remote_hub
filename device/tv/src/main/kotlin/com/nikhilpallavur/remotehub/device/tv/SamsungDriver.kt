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
import com.nikhilpallavur.remotehub.device.tv.connection.SamsungConnection
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/** Drives Samsung Tizen TVs over the secure WebSocket API with a confirm-on-TV handshake. */
@Singleton
class SamsungDriver @Inject constructor(
    private val client: OkHttpClient,
) : DeviceDriver {

    override val descriptor = DriverDescriptor(
        id = "samsung-tizen",
        displayName = "Samsung (Tizen)",
        manufacturer = "Samsung",
        category = DeviceCategory.TELEVISION,
        transport = Transport.WIFI,
        capabilities = setOf(
            DeviceCapability.POWER,
            DeviceCapability.DPAD,
            DeviceCapability.TOUCHPAD,
            DeviceCapability.VOLUME,
            DeviceCapability.CHANNEL,
            DeviceCapability.MEDIA,
            DeviceCapability.NUMBER_PAD,
        ),
        pairingMode = PairingMode.CONFIRM_ON_DEVICE,
        discovery = DiscoverySpec(
            ssdpSearchTargets = listOf(Ssdp.ST_SAMSUNG),
            ssdpMatchKeywords = listOf("samsung", "tizen"),
        ),
        defaultPort = 8002,
    )

    override fun createConnection(): RemoteConnection = SamsungConnection(descriptor, client, APP_NAME)

    private companion object {
        const val APP_NAME = "RemoteHub"
    }
}
