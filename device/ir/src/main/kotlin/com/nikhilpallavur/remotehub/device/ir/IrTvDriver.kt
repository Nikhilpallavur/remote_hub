package com.nikhilpallavur.remotehub.device.ir

import android.content.Context
import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.drivers.RemoteConnection
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.DeviceCategory
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.Transport
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic infrared TV driver for phones with an IR blaster. Not discoverable (IR is one-way); it is
 * offered as a manual "IR TV" device so the user can drive legacy sets in line of sight.
 */
@Singleton
class IrTvDriver @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceDriver {

    override val descriptor = DriverDescriptor(
        id = "ir-tv",
        displayName = "Infrared TV",
        category = DeviceCategory.TELEVISION,
        transport = Transport.INFRARED,
        capabilities = setOf(
            DeviceCapability.POWER,
            DeviceCapability.VOLUME,
            DeviceCapability.CHANNEL,
            DeviceCapability.NUMBER_PAD,
        ),
        pairingMode = null,
    )

    override fun createConnection(): RemoteConnection = IrConnection(descriptor, context)

    /** IR is stateless and hostless — a single virtual device represents the blaster itself. */
    override fun manualDevice(host: String): RemoteDevice = RemoteDevice(
        id = RemoteDevice.idFor(descriptor.id, "blaster"),
        name = descriptor.displayName,
        driverId = descriptor.id,
        category = descriptor.category,
        transport = descriptor.transport,
    )
}
