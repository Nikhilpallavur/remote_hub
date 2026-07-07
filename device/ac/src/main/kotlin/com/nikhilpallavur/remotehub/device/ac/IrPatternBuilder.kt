package com.nikhilpallavur.remotehub.device.ac

/**
 * Accumulates the alternating on/off microsecond durations `ConsumerIrManager.transmit` expects.
 * Encoders are responsible for strict mark/space alternation; this class only concatenates.
 */
internal class IrPatternBuilder {
    private val durations = ArrayList<Int>()

    fun mark(us: Int) = apply { durations.add(us) }

    fun space(us: Int) = apply { durations.add(us) }

    /** One data bit: a fixed mark then a one- or zero-length space. */
    fun bit(one: Boolean, bitMark: Int, oneSpace: Int, zeroSpace: Int) = apply {
        mark(bitMark)
        space(if (one) oneSpace else zeroSpace)
    }

    /** Eight data bits of [byte], least-significant bit first (Samsung, Daikin). */
    fun byteLsbFirst(byte: Int, bitMark: Int, oneSpace: Int, zeroSpace: Int) = apply {
        for (i in 0 until 8) bit((byte shr i) and 1 == 1, bitMark, oneSpace, zeroSpace)
    }

    /** The lowest [bits] bits of [value], most-significant bit first (Coolix, LG). */
    fun bitsMsbFirst(value: Long, bits: Int, bitMark: Int, oneSpace: Int, zeroSpace: Int) = apply {
        for (i in bits - 1 downTo 0) bit((value shr i) and 1L == 1L, bitMark, oneSpace, zeroSpace)
    }

    fun build(): IntArray = durations.toIntArray()
}
