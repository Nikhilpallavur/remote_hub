package com.nikhilpallavur.remotehub.device.tv

import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import com.nikhilpallavur.remotehub.core.drivers.DiscoverySpec
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.drivers.RemoteConnection
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.DeviceCategory
import com.nikhilpallavur.remotehub.core.model.Transport
import com.nikhilpallavur.remotehub.core.transport.Ssdp
import com.nikhilpallavur.remotehub.device.tv.connection.RokuConnection
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/** Drives Roku players/TVs over the External Control Protocol (HTTP, no pairing). */
@Singleton
class RokuDriver @Inject constructor(
    private val client: OkHttpClient,
) : DeviceDriver {

    override val descriptor = DriverDescriptor(
        id = "roku",
        displayName = "Roku",
        manufacturer = "Roku",
        category = DeviceCategory.STREAMING_DEVICE,
        transport = Transport.WIFI,
        capabilities = setOf(
            DeviceCapability.POWER,
            DeviceCapability.DPAD,
            DeviceCapability.TOUCHPAD,
            DeviceCapability.VOLUME,
            DeviceCapability.CHANNEL,
            DeviceCapability.MEDIA,
            DeviceCapability.TEXT_INPUT,
            DeviceCapability.APP_SHORTCUTS,
        ),
        pairingMode = null,
        discovery = DiscoverySpec(
            ssdpSearchTargets = listOf(Ssdp.ST_ROKU),
            ssdpMatchKeywords = listOf("roku"),
        ),
        defaultPort = 8060,
    )

    override fun createConnection(): RemoteConnection = RokuConnection(descriptor, client)
}
