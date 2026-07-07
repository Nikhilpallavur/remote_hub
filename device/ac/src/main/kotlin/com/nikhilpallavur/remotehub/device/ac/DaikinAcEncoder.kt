package com.nikhilpallavur.remotehub.device.ac

import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed

/**
 * Daikin 35-byte (280-bit) protocol, the original ARC-remote variant (per IRremoteESP8266
 * ir_Daikin / IRDaikinESP).
 *
 * Three sections of 8 + 8 + 19 bytes, each starting 0x11 0xDA 0x27 and ending in a byte-sum
 * checksum, sent LSB-first with a 5-zero-bit preamble before the first section. Temperature is
 * stored in half-degree units (degrees × 2).
 */
object DaikinAcEncoder : AcProtocolEncoder {

    override val displayName = "Daikin"
    override val temperatureRange = 10..32

    private const val CARRIER_HZ = 38_000
    private const val HDR_MARK = 3650
    private const val HDR_SPACE = 1623
    private const val BIT_MARK = 428
    private const val ONE_SPACE = 1280
    private const val ZERO_SPACE = 428
    private const val GAP = 29000
    private const val PREAMBLE_BITS = 5

    private const val STATE_LENGTH = 35
    private val SECTION_OFFSETS = intArrayOf(0, 8, 16)
    private val SECTION_LENGTHS = intArrayOf(8, 8, 19)

    override fun encode(state: AcState): IrFrame =
        IrFrame(CARRIER_HZ, patternFor(stateBytes(state)))

    /** The 35 state bytes with checksums applied; exposed internally for unit tests. */
    internal fun stateBytes(state: AcState): IntArray {
        val temp = state.temperatureC.coerceIn(temperatureRange)
        val bytes = IntArray(STATE_LENGTH)
        // Fixed section headers and constants from IRDaikinESP::stateReset().
        for (offset in SECTION_OFFSETS) {
            bytes[offset] = 0x11
            bytes[offset + 1] = 0xDA
            bytes[offset + 2] = 0x27
        }
        bytes[4] = 0xC5
        bytes[12] = 0x42
        bytes[27] = 0x06
        bytes[28] = 0x60
        bytes[31] = 0xC0
        val mode = when (state.mode) {
            ClimateMode.AUTO -> 0b000
            ClimateMode.DRY -> 0b010
            ClimateMode.COOL -> 0b011
            ClimateMode.HEAT -> 0b100
            ClimateMode.FAN -> 0b110
        }
        bytes[21] = (mode shl 4) or 0b1000 or (if (state.power) 1 else 0)
        bytes[22] = temp * 2
        val fan = when (state.fan) {
            FanSpeed.AUTO -> 0b1010
            FanSpeed.LOW -> 2 + 1
            FanSpeed.MEDIUM -> 2 + 3
            FanSpeed.HIGH -> 2 + 5
        }
        val swing = if (state.swing) 0b1111 else 0b0000
        bytes[24] = (fan shl 4) or swing
        for (i in SECTION_OFFSETS.indices) {
            val offset = SECTION_OFFSETS[i]
            val checksumIndex = offset + SECTION_LENGTHS[i] - 1
            bytes[checksumIndex] = (offset until checksumIndex).sumOf { bytes[it] } and 0xFF
        }
        return bytes
    }

    private fun patternFor(bytes: IntArray): IntArray {
        val builder = IrPatternBuilder()
        // Preamble: five zero bits, then a stop mark and a long quiet gap.
        repeat(PREAMBLE_BITS) { builder.bit(false, BIT_MARK, ONE_SPACE, ZERO_SPACE) }
        builder.mark(BIT_MARK).space(ZERO_SPACE + GAP)
        for (i in SECTION_OFFSETS.indices) {
            builder.mark(HDR_MARK).space(HDR_SPACE)
            val offset = SECTION_OFFSETS[i]
            for (b in offset until offset + SECTION_LENGTHS[i]) {
                builder.byteLsbFirst(bytes[b], BIT_MARK, ONE_SPACE, ZERO_SPACE)
            }
            builder.mark(BIT_MARK).space(ZERO_SPACE + GAP)
        }
        return builder.build()
    }
}
