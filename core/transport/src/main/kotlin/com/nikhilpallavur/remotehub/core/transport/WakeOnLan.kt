package com.nikhilpallavur.remotehub.core.transport

/**
 * Wake-on-LAN magic-packet builder. Many Samsung/LG TVs that do not respond to a network "power
 * on" can still be woken from standby by a magic packet to their MAC. Pure byte logic → tested.
 */
object WakeOnLan {
    const val DEFAULT_PORT = 9
    private const val MAC_BYTES = 6
    private const val REPEAT = 16

    /** Parses "AA:BB:CC:DD:EE:FF" (or '-' separated) into 6 bytes. */
    fun parseMac(mac: String): ByteArray {
        val parts = mac.split(':', '-')
        require(parts.size == MAC_BYTES) { "invalid MAC address: $mac" }
        return ByteArray(MAC_BYTES) { parts[it].toInt(16).toByte() }
    }

    /** 6 bytes of 0xFF followed by the target MAC repeated 16 times. */
    fun magicPacket(mac: String): ByteArray {
        val macBytes = parseMac(mac)
        val packet = ByteArray(MAC_BYTES + REPEAT * MAC_BYTES)
        for (i in 0 until MAC_BYTES) {
            packet[i] = 0xFF.toByte()
        }
        for (i in 0 until REPEAT) {
            System.arraycopy(macBytes, 0, packet, MAC_BYTES + i * MAC_BYTES, MAC_BYTES)
        }
        return packet
    }
}
