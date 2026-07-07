package com.nikhilpallavur.remotehub.device.ir

import com.nikhilpallavur.remotehub.core.model.RemoteKey

/**
 * NEC infrared encoding for devices that do have an IR blaster (kept as a fallback alongside the
 * Wi-Fi protocols). Produces the on/off microsecond pattern `ConsumerIrManager.transmit` expects.
 * IR command bytes are inherently device-specific; the defaults target common TV code-sets.
 */
object NecEncoder {
    const val CARRIER_HZ = 38_000
    private const val LEAD_MARK = 9000
    private const val LEAD_SPACE = 4500
    private const val PULSE = 560
    private const val ONE_SPACE = 1690
    private const val BYTE_BITS = 8

    /** A best-effort default NEC command byte for [key] (address 0x00). */
    fun commandFor(key: RemoteKey): Int? = when (key) {
        RemoteKey.POWER -> 0x45
        RemoteKey.MENU -> 0x47
        RemoteKey.VOLUME_UP -> 0x15
        RemoteKey.VOLUME_DOWN -> 0x07
        RemoteKey.CHANNEL_UP -> 0x09
        RemoteKey.CHANNEL_DOWN -> 0x1C
        RemoteKey.MUTE -> 0x0D
        RemoteKey.DPAD_CENTER -> 0x18
        RemoteKey.BACK -> 0x08
        else -> null
    }

    /**
     * Encodes an [address]/[command] byte pair into the NEC pattern: a 9 ms lead-in, 32 data bits
     * (address, ~address, command, ~command), then a final stop pulse.
     */
    fun encode(address: Int, command: Int): IntArray {
        val pattern = ArrayList<Int>(BYTE_BITS * 4 * 2 + 3)
        pattern.add(LEAD_MARK)
        pattern.add(LEAD_SPACE)
        val payload = intArrayOf(address, address.inv() and 0xFF, command, command.inv() and 0xFF)
        for (byte in payload) {
            for (bit in 0 until BYTE_BITS) {
                pattern.add(PULSE)
                pattern.add(if ((byte shr bit) and 1 == 1) ONE_SPACE else PULSE)
            }
        }
        pattern.add(PULSE)
        return pattern.toIntArray()
    }
}
