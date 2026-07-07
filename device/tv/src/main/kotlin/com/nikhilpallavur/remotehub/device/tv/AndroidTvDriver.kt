package com.nikhilpallavur.remotehub.device.tv

import android.os.Build
import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import com.nikhilpallavur.remotehub.core.drivers.DiscoverySpec
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.drivers.RemoteConnection
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.DeviceCategory
import com.nikhilpallavur.remotehub.core.model.PairingMode
import com.nikhilpallavur.remotehub.core.model.Transport
import com.nikhilpallavur.remotehub.device.tv.connection.AndroidTvConnection
import com.nikhilpallavur.remotehub.device.tv.store.AndroidTvKeyStoreProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives Android TV / Google TV (and Chromecast/Fire TV devices that speak Android TV Remote v2)
 * over the encrypted v2 protocol with a PIN handshake.
 */
@Singleton
class AndroidTvDriver @Inject constructor(
    private val keyStoreProvider: AndroidTvKeyStoreProvider,
) : DeviceDriver {

    override val descriptor = DriverDescriptor(
        id = "android-tv",
        displayName = "Android TV / Google TV",
        manufacturer = "Google",
        category = DeviceCategory.ANDROID_TV,
        transport = Transport.WIFI,
        capabilities = setOf(
            DeviceCapability.POWER,
            DeviceCapability.DPAD,
            DeviceCapability.TOUCHPAD,
            DeviceCapability.VOLUME,
            DeviceCapability.MEDIA,
            DeviceCapability.NUMBER_PAD,
            DeviceCapability.TEXT_INPUT,
            DeviceCapability.APP_SHORTCUTS,
        ),
        pairingMode = PairingMode.PIN_CODE,
        discovery = DiscoverySpec(
            nsdServiceTypes = listOf("_androidtvremote2._tcp.", "_googlecast._tcp."),
        ),
        defaultPort = 6466,
    )

    override fun createConnection(): RemoteConnection = AndroidTvConnection(
        descriptor = descriptor,
        sslContext = keyStoreProvider.sslContext(),
        clientCert = keyStoreProvider.clientCertificate(),
        deviceName = "RemoteHub (${Build.MODEL})",
    )
}
