package com.nikhilpallavur.remotehub.core.transport

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.core.transport.WakeOnLan
import org.junit.Test

class WakeOnLanTest {
    @Test
    fun parsesColonAndDashSeparatedMacs() {
        val expected = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        assertThat(WakeOnLan.parseMac("AA:BB:CC:DD:EE:FF")).isEqualTo(expected)
        assertThat(WakeOnLan.parseMac("AA-BB-CC-DD-EE-FF")).isEqualTo(expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedMac() {
        WakeOnLan.parseMac("AA:BB:CC")
    }

    @Test
    fun magicPacketHasSyncStreamThenSixteenMacRepeats() {
        val packet = WakeOnLan.magicPacket("AA:BB:CC:DD:EE:FF")
        assertThat(packet.size).isEqualTo(102)
        for (i in 0 until 6) {
            assertThat(packet[i]).isEqualTo(0xFF.toByte())
        }
        // first MAC repeat starts right after the 6-byte 0xFF sync stream
        assertThat(packet.copyOfRange(6, 12))
            .isEqualTo(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()))
    }
}
