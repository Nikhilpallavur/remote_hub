package com.nikhilpallavur.remotehub.device.ac

import android.content.Context
import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.drivers.RemoteConnection
import com.nikhilpallavur.remotehub.core.model.DeviceCapability
import com.nikhilpallavur.remotehub.core.model.DeviceCategory
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.Transport

/**
 * Base infrared AC driver: one thin subclass per protocol family binds a brand label to its
 * [AcProtocolEncoder]. IR is one-way and hostless, so drivers are not discoverable — like the
 * IR TV driver, each is offered as a single virtual device representing the phone's blaster.
 */
abstract class IrAcDriver(
    private val context: Context,
    private val encoder: AcProtocolEncoder,
    driverId: String,
    brandDisplayName: String,
    manufacturer: String,
) : DeviceDriver {

    final override val descriptor = DriverDescriptor(
        id = driverId,
        displayName = brandDisplayName,
        category = DeviceCategory.AIR_CONDITIONER,
        transport = Transport.INFRARED,
        capabilities = setOf(
            DeviceCapability.POWER,
            DeviceCapability.TEMPERATURE,
            DeviceCapability.MODE,
            DeviceCapability.FAN_SPEED,
            DeviceCapability.SWING,
        ),
        manufacturer = manufacturer,
        pairingMode = null,
        temperatureRangeC = encoder.temperatureRange,
    )

    final override fun createConnection(): RemoteConnection = IrAcConnection(descriptor, context, encoder)

    /** IR is stateless and hostless — a single virtual device represents the blaster itself. */
    final override fun manualDevice(host: String): RemoteDevice = RemoteDevice(
        id = RemoteDevice.idFor(descriptor.id, "blaster"),
        name = descriptor.displayName,
        driverId = descriptor.id,
        category = descriptor.category,
        transport = descriptor.transport,
    )
}
