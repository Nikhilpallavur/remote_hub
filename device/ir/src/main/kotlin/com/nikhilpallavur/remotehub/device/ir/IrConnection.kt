package com.nikhilpallavur.remotehub.device.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import com.nikhilpallavur.remotehub.core.drivers.BaseRemoteConnection
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.RemoteKey

/**
 * Infrared connection for phones that have an IR blaster. Unlike the Wi-Fi drivers this is
 * line-of-sight and one-way (no discovery, no acknowledgements), kept for legacy devices.
 */
class IrConnection(
    descriptor: DriverDescriptor,
    context: Context,
) : BaseRemoteConnection(descriptor) {
    private val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    private val hasEmitter = irManager?.hasIrEmitter() == true

    override suspend fun connect(device: RemoteDevice) {
        mutableState.value = if (hasEmitter) {
            ConnectionState.Connected(device.copy(paired = true))
        } else {
            ConnectionState.Failed(device, "This phone has no infrared blaster.")
        }
    }

    override suspend fun send(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> sendKey(command.key)
        else -> false
    }

    override fun supports(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> hasEmitter && NecEncoder.commandFor(command.key) != null
        else -> false
    }

    private fun sendKey(key: RemoteKey): Boolean {
        if (!hasEmitter) return false
        val command = NecEncoder.commandFor(key) ?: return false
        return runCatching {
            irManager?.transmit(NecEncoder.CARRIER_HZ, NecEncoder.encode(0x00, command))
            true
        }.getOrDefault(false)
    }

    override fun close() {
        mutableState.value = ConnectionState.Idle
    }
}
