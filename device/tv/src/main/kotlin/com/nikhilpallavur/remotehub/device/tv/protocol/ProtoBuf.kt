package com.nikhilpallavur.remotehub.device.tv.protocol

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream

/**
 * A deliberately tiny Protocol Buffers (proto2/proto3 wire-compatible) reader/writer — just
 * enough to speak the Android TV Remote v2 pairing and remote messages without pulling in a
 * code-gen toolchain. Mirrors the "pure byte logic" parser style already used for the firewall
 * DNS/IP packets, so it is fully JVM-unit-testable.
 *
 * Wire types handled: 0 = varint, 1 = 64-bit, 2 = length-delimited, 5 = 32-bit.
 */
object ProtoBuf {
    const val WIRE_VARINT = 0
    const val WIRE_FIXED64 = 1
    const val WIRE_LENGTH = 2
    const val WIRE_FIXED32 = 5

    /** Encodes an unsigned base-128 varint (little-endian, high-bit = continuation). */
    fun encodeVarint(value: Long): ByteArray {
        val out = ByteArrayOutputStream()
        var v = value
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v == 0L) {
                out.write(b)
                break
            }
            out.write(b or 0x80)
        }
        return out.toByteArray()
    }

    /** Reads a varint from [input], or returns null at clean end-of-stream. */
    fun readVarint(input: InputStream): Long? {
        var shift = 0
        var result = 0L
        while (shift < 64) {
            val b = input.read()
            if (b < 0) return if (shift == 0) null else throw EOFException("truncated varint")
            result = result or ((b.toLong() and 0x7F) shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
        }
        throw EOFException("varint too long")
    }

    /** Frames [message] as a length-delimited payload: varint(length) followed by the bytes. */
    fun frame(message: ByteArray): ByteArray =
        encodeVarint(message.size.toLong()) + message

    /** Reads one length-delimited message off [input], or null at end-of-stream. */
    fun readFramed(input: InputStream): ByteArray? {
        val length = readVarint(input) ?: return null
        val buffer = ByteArray(length.toInt())
        var read = 0
        while (read < buffer.size) {
            val n = input.read(buffer, read, buffer.size - read)
            if (n < 0) throw EOFException("truncated message")
            read += n
        }
        return buffer
    }

    /** Parses a flat protobuf message into field-number -> values. Unknown fields are kept. */
    @Suppress("ReturnCount")
    fun parse(bytes: ByteArray): ProtoMessage {
        val fields = LinkedHashMap<Int, MutableList<ProtoField>>()
        var i = 0
        while (i < bytes.size) {
            val (tag, tagLen) = decodeVarintAt(bytes, i)
            i += tagLen
            val field = (tag ushr 3).toInt()
            when ((tag and 0x7).toInt()) {
                WIRE_VARINT -> {
                    val (value, len) = decodeVarintAt(bytes, i)
                    i += len
                    fields.add(field, ProtoField.VarInt(value))
                }
                WIRE_FIXED64 -> {
                    fields.add(field, ProtoField.Raw(bytes.copyOfRange(i, i + 8)))
                    i += 8
                }
                WIRE_LENGTH -> {
                    val (length, len) = decodeVarintAt(bytes, i)
                    i += len
                    val end = i + length.toInt()
                    fields.add(field, ProtoField.Bytes(bytes.copyOfRange(i, end)))
                    i = end
                }
                WIRE_FIXED32 -> {
                    fields.add(field, ProtoField.Raw(bytes.copyOfRange(i, i + 4)))
                    i += 4
                }
                else -> error("unknown wire type in tag $tag")
            }
        }
        return ProtoMessage(fields)
    }

    private fun MutableMap<Int, MutableList<ProtoField>>.add(field: Int, value: ProtoField) {
        getOrPut(field) { mutableListOf() }.add(value)
    }

    private fun decodeVarintAt(bytes: ByteArray, offset: Int): Pair<Long, Int> {
        var shift = 0
        var result = 0L
        var i = offset
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            result = result or ((b.toLong() and 0x7F) shl shift)
            i++
            if (b and 0x80 == 0) return result to (i - offset)
            shift += 7
        }
        throw EOFException("truncated varint at $offset")
    }
}

/** A decoded protobuf field value. */
sealed interface ProtoField {
    data class VarInt(val value: Long) : ProtoField

    data class Bytes(val value: ByteArray) : ProtoField {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Bytes && value.contentEquals(other.value))

        override fun hashCode(): Int = value.contentHashCode()
    }

    /** Fixed32/Fixed64 bytes, retained verbatim (unused by our messages but parsed safely). */
    data class Raw(val value: ByteArray) : ProtoField {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Raw && value.contentEquals(other.value))

        override fun hashCode(): Int = value.contentHashCode()
    }
}

/** Typed read access over a parsed message. */
class ProtoMessage(private val fields: Map<Int, List<ProtoField>>) {
    fun has(field: Int): Boolean = fields.containsKey(field)

    fun varint(field: Int): Long? =
        (fields[field]?.firstOrNull() as? ProtoField.VarInt)?.value

    fun bytes(field: Int): ByteArray? =
        (fields[field]?.firstOrNull() as? ProtoField.Bytes)?.value

    fun string(field: Int): String? = bytes(field)?.toString(Charsets.UTF_8)

    fun message(field: Int): ProtoMessage? = bytes(field)?.let { ProtoBuf.parse(it) }
}

/** Fluent builder for a flat protobuf message. */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun varint(field: Int, value: Long): ProtoWriter = apply {
        out.write(ProtoBuf.encodeVarint((field.toLong() shl 3) or ProtoBuf.WIRE_VARINT.toLong()))
        out.write(ProtoBuf.encodeVarint(value))
    }

    fun bool(field: Int, value: Boolean): ProtoWriter = varint(field, if (value) 1 else 0)

    fun enum(field: Int, value: Int): ProtoWriter = varint(field, value.toLong())

    fun bytes(field: Int, value: ByteArray): ProtoWriter = apply {
        out.write(ProtoBuf.encodeVarint((field.toLong() shl 3) or ProtoBuf.WIRE_LENGTH.toLong()))
        out.write(ProtoBuf.encodeVarint(value.size.toLong()))
        out.write(value)
    }

    fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun message(field: Int, value: ByteArray): ProtoWriter = bytes(field, value)

    fun toByteArray(): ByteArray = out.toByteArray()
}
