package com.nikhilpallavur.remotehub.device.ac

import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed

/**
 * Samsung 14-byte AC protocol (per IRremoteESP8266 ir_Samsung).
 *
 * Two 7-byte sections sent LSB-first, each preceded by a section mark/space; the whole message
 * is preceded by one header mark/space. Each section carries a bit-count checksum: the number of
 * set bits in the section (excluding the two checksum nibbles) inverted, with the low nibble
 * stored in byte1's high nibble and the high nibble in byte2's low nibble of the section.
 *
 * Known limitation: some Samsung units expect a longer 21-byte "extended" message for power
 * transitions; the plain 14-byte state (which also carries both power bit-pairs) is what most
 * units accept and is what we transmit.
 */
object SamsungAcEncoder : AcProtocolEncoder {

    override val displayName = "Samsung"
    override val temperatureRange = 16..30

    private const val CARRIER_HZ = 38_000
    private const val HDR_MARK = 690
    private const val HDR_SPACE = 17844
    private const val SECTION_MARK = 3086
    private const val SECTION_SPACE = 8864
    private const val BIT_MARK = 586
    private const val ONE_SPACE = 1432
    private const val ZERO_SPACE = 436
    private const val SECTION_GAP = 2886

    private const val SECTION_LENGTH = 7
    private const val MIN_TEMP = 16
    private const val SWING_V_ON = 0b010
    private const val SWING_OFF = 0b111

    /** Known-good baseline state (IRremoteESP8266 kReset); checksums recomputed after edits. */
    private val RESET = intArrayOf(
        0x02, 0x92, 0x0F, 0x00, 0x00, 0x00, 0xF0,
        0x01, 0x02, 0xAE, 0x71, 0x00, 0x15, 0xF0,
    )

    override fun encode(state: AcState): IrFrame =
        IrFrame(CARRIER_HZ, patternFor(stateBytes(state)))

    /** The 14 state bytes with checksums applied; exposed internally for unit tests. */
    internal fun stateBytes(state: AcState): IntArray {
        val temp = state.temperatureC.coerceIn(temperatureRange)
        val bytes = RESET.copyOf()
        val power = if (state.power) 0b11 else 0b00
        bytes[6] = (bytes[6] and 0b1100_1111) or (power shl 4) // Power1
        bytes[13] = (bytes[13] and 0b1100_1111) or (power shl 4) // Power2
        val swing = if (state.swing) SWING_V_ON else SWING_OFF
        bytes[9] = (bytes[9] and 0b1000_1111) or (swing shl 4)
        bytes[11] = (bytes[11] and 0x0F) or ((temp - MIN_TEMP) shl 4)
        val mode = when (state.mode) {
            ClimateMode.AUTO -> 0
            ClimateMode.COOL -> 1
            ClimateMode.DRY -> 2
            ClimateMode.FAN -> 3
            ClimateMode.HEAT -> 4
        }
        val fan = when (state.fan) {
            FanSpeed.AUTO -> 0
            FanSpeed.LOW -> 2
            FanSpeed.MEDIUM -> 4
            FanSpeed.HIGH -> 5
        }
        bytes[12] = 0b0000_0001 or (fan shl 1) or (mode shl 4)
        applyChecksum(bytes, 0)
        applyChecksum(bytes, SECTION_LENGTH)
        return bytes
    }

    /**
     * Count of set bits in the section — the full first byte, the low nibble of the second, the
     * high nibble of the third, and the remaining four bytes — inverted, then stored split
     * across the two checksum nibbles.
     */
    private fun applyChecksum(bytes: IntArray, offset: Int) {
        var bits = Integer.bitCount(bytes[offset])
        bits += Integer.bitCount(bytes[offset + 1] and 0x0F)
        bits += Integer.bitCount(bytes[offset + 2] shr 4)
        for (i in offset + 3 until offset + SECTION_LENGTH) bits += Integer.bitCount(bytes[i])
        val sum = bits.inv() and 0xFF
        bytes[offset + 1] = (bytes[offset + 1] and 0x0F) or ((sum and 0x0F) shl 4)
        bytes[offset + 2] = (bytes[offset + 2] and 0xF0) or (sum shr 4)
    }

    private fun patternFor(bytes: IntArray): IntArray {
        val builder = IrPatternBuilder()
        builder.mark(HDR_MARK).space(HDR_SPACE)
        for (section in 0 until bytes.size / SECTION_LENGTH) {
            builder.mark(SECTION_MARK).space(SECTION_SPACE)
            for (i in section * SECTION_LENGTH until (section + 1) * SECTION_LENGTH) {
                builder.byteLsbFirst(bytes[i], BIT_MARK, ONE_SPACE, ZERO_SPACE)
            }
            builder.mark(BIT_MARK).space(SECTION_GAP)
        }
        return builder.build()
    }
}
