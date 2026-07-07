package com.nikhilpallavur.remotehub.device.ac

import android.content.Context
import android.hardware.ConsumerIrManager
import com.nikhilpallavur.remotehub.core.drivers.BaseRemoteConnection
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.RemoteKey

/**
 * Infrared AC connection. AC protocols are stateful — every keypress retransmits the complete
 * settings frame — so this connection owns the authoritative [AcState] and re-encodes it
 * wholesale per command. IR is one-way: the state is optimistic and can drift if the AC's own
 * remote is used alongside the app (inherent limitation, same as the IR TV driver).
 */
class IrAcConnection(
    descriptor: DriverDescriptor,
    context: Context,
    private val encoder: AcProtocolEncoder,
) : BaseRemoteConnection(descriptor) {
    private val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    private val hasEmitter = irManager?.hasIrEmitter() == true
    private var acState = AcState()

    override suspend fun connect(device: RemoteDevice) {
        acState = AcState()
        mutableState.value = if (hasEmitter) {
            ConnectionState.Connected(device.copy(paired = true))
        } else {
            ConnectionState.Failed(device, "This phone has no infrared blaster.")
        }
    }

    override suspend fun send(command: RemoteCommand): Boolean {
        if (!hasEmitter || !supports(command)) return false
        acState = when (command) {
            is RemoteCommand.Press -> acState.copy(power = !acState.power)
            is RemoteCommand.SetTemperature ->
                acState.copy(temperatureC = command.celsius.coerceIn(encoder.temperatureRange))
            is RemoteCommand.SetMode -> acState.copy(mode = command.mode)
            is RemoteCommand.SetFanSpeed -> acState.copy(fan = command.speed)
            is RemoteCommand.SetSwing -> acState.copy(swing = command.enabled)
            else -> return false
        }
        // Coolix/LG express swing as a dedicated toggle command rather than a state bit.
        val frame = if (command is RemoteCommand.SetSwing) {
            encoder.swingToggleFrame(acState) ?: encoder.encode(acState)
        } else {
            encoder.encode(acState)
        }
        return transmit(frame)
    }

    override fun supports(command: RemoteCommand): Boolean = hasEmitter && when (command) {
        is RemoteCommand.Press -> command.key == RemoteKey.POWER
        is RemoteCommand.SetTemperature,
        is RemoteCommand.SetMode,
        is RemoteCommand.SetFanSpeed,
        is RemoteCommand.SetSwing,
        -> true
        else -> false
    }

    private fun transmit(frame: IrFrame): Boolean = runCatching {
        irManager?.transmit(frame.carrierHz, frame.pattern)
        true
    }.getOrDefault(false)

    override fun close() {
        mutableState.value = ConnectionState.Idle
    }
}
