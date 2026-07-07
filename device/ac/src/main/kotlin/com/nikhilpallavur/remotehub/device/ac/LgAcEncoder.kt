package com.nikhilpallavur.remotehub.device.ac

import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed

/**
 * LG 28-bit AC protocol (per IRremoteESP8266 ir_LG, GE6711AR2853M-style timing).
 *
 * Bit layout from LSB: [3:0] checksum, [7:4] fan, [11:8] temperature − 15, [14:12] mode,
 * [17:15] unused, [19:18] power (0b00 on, 0b11 off), [27:20] signature 0x88. Checksum is the
 * sum of the six data nibbles masked to 4 bits. Power-off and vertical-swing are dedicated
 * command words. 28 bits transmitted MSB-first.
 */
object LgAcEncoder : AcProtocolEncoder {

    override val displayName = "LG"
    override val temperatureRange = 16..30

    private const val CARRIER_HZ = 38_000
    private const val HDR_MARK = 8500
    private const val HDR_SPACE = 4250
    private const val BIT_MARK = 550
    private const val ONE_SPACE = 1600
    private const val ZERO_SPACE = 550

    private const val BITS = 28
    private const val SIGNATURE = 0x88L
    private const val TEMP_ADJUST = 15
    private const val POWER_OFF_COMMAND = 0x88C0051L
    private const val SWING_TOGGLE_COMMAND = 0x8810001L

    override fun encode(state: AcState): IrFrame =
        IrFrame(CARRIER_HZ, patternFor(rawFor(state)))

    override fun swingToggleFrame(state: AcState): IrFrame =
        IrFrame(CARRIER_HZ, patternFor(SWING_TOGGLE_COMMAND))

    /** The 28-bit command word; exposed internally for unit tests. */
    internal fun rawFor(state: AcState): Long {
        if (!state.power) return POWER_OFF_COMMAND
        val temp = state.temperatureC.coerceIn(temperatureRange)
        val mode = when (state.mode) {
            ClimateMode.COOL -> 0L
            ClimateMode.DRY -> 1L
            ClimateMode.FAN -> 2L
            ClimateMode.AUTO -> 3L
            ClimateMode.HEAT -> 4L
        }
        val fan = when (state.fan) {
            FanSpeed.AUTO -> 5L
            FanSpeed.LOW -> 1L
            FanSpeed.MEDIUM -> 2L
            FanSpeed.HIGH -> 4L
        }
        val unsummed = (SIGNATURE shl 20) or
            (mode shl 12) or
            ((temp - TEMP_ADJUST).toLong() shl 8) or
            (fan shl 4)
        return unsummed or checksum(unsummed)
    }

    /** Sum of the six data nibbles above the checksum nibble, masked to 4 bits. */
    private fun checksum(unsummed: Long): Long {
        var sum = 0L
        for (nibble in 1..6) sum += (unsummed shr (4 * nibble)) and 0xF
        return sum and 0xF
    }

    private fun patternFor(raw: Long): IntArray = IrPatternBuilder()
        .mark(HDR_MARK).space(HDR_SPACE)
        .bitsMsbFirst(raw, BITS, BIT_MARK, ONE_SPACE, ZERO_SPACE)
        .mark(BIT_MARK)
        .build()
}
