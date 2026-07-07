package com.nikhilpallavur.remotehub.device.tv.protocol

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.device.tv.protocol.ProtoBuf
import com.nikhilpallavur.remotehub.device.tv.protocol.ProtoWriter
import org.junit.Test
import java.io.ByteArrayInputStream

class ProtoBufTest {
    @Test
    fun encodesSingleByteVarints() {
        assertThat(ProtoBuf.encodeVarint(0)).isEqualTo(byteArrayOf(0))
        assertThat(ProtoBuf.encodeVarint(1)).isEqualTo(byteArrayOf(1))
        assertThat(ProtoBuf.encodeVarint(127)).isEqualTo(byteArrayOf(127))
    }

    @Test
    fun encodesMultiByteVarint() {
        // 300 = 0b1_0010_1100 -> 0xAC 0x02
        assertThat(ProtoBuf.encodeVarint(300)).isEqualTo(byteArrayOf(0xAC.toByte(), 0x02))
    }

    @Test
    fun readsVarintFromStream() {
        val stream = ByteArrayInputStream(ProtoBuf.encodeVarint(300))
        assertThat(ProtoBuf.readVarint(stream)).isEqualTo(300L)
    }

    @Test
    fun framesAndReadsBackAMessage() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val framed = ProtoBuf.frame(payload)
        val read = ProtoBuf.readFramed(ByteArrayInputStream(framed))
        assertThat(read).isEqualTo(payload)
    }

    @Test
    fun readFramedReturnsNullAtEndOfStream() {
        assertThat(ProtoBuf.readFramed(ByteArrayInputStream(ByteArray(0)))).isNull()
    }

    @Test
    fun roundTripsVarintStringAndNestedMessage() {
        val inner = ProtoWriter().varint(1, 42).toByteArray()
        val bytes = ProtoWriter()
            .varint(1, 7)
            .string(2, "hello")
            .message(3, inner)
            .toByteArray()

        val message = ProtoBuf.parse(bytes)
        assertThat(message.varint(1)).isEqualTo(7L)
        assertThat(message.string(2)).isEqualTo("hello")
        assertThat(message.message(3)?.varint(1)).isEqualTo(42L)
    }

    @Test
    fun boolFieldEncodesAsVarintOne() {
        val message = ProtoBuf.parse(ProtoWriter().bool(1, true).toByteArray())
        assertThat(message.varint(1)).isEqualTo(1L)
    }
}
