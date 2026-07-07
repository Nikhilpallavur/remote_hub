package com.nikhilpallavur.remotehub.device.ac

import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed

/**
 * Coolix 24-bit protocol — used by Midea, Voltas, Blue Star, Carrier, Kelvinator and many OEMs.
 *
 * Wire format (per IRremoteESP8266 ir_Coolix): 24 bits MSB-first, each byte followed by its
 * complement (48 bits on the wire), whole message transmitted twice. Bit layout from LSB:
 * [0] unused, [1] zone-follow, [3:2] mode, [7:4] temperature code, [12:8] sensor temp (0b11111 =
 * off), [15:13] fan, [23:16] fixed 0xB2 prefix. Power-off and swing are dedicated constants.
 */
object CoolixEncoder : AcProtocolEncoder {

    override val displayName = "Coolix"
    override val temperatureRange = 17..30

    private const val CARRIER_HZ = 38_000
    private const val HDR_MARK = 4692
    private const val HDR_SPACE = 4416
    private const val BIT_MARK = 552
    private const val ONE_SPACE = 1656
    private const val ZERO_SPACE = 552
    private const val MIN_GAP = 5244

    private const val PREFIX = 0xB2
    private const val SENSOR_TEMP_OFF = 0b11111
    private const val POWER_OFF_STATE = 0xB27BE0L
    private const val SWING_TOGGLE_STATE = 0xB26BE0L
    private const val FAN_MODE_TEMP_CODE = 0b1110

    /** 4-bit Gray-coded temperature values for 17..30 °C. */
    private val TEMP_CODES = intArrayOf(
        0b0000, 0b0001, 0b0011, 0b0010, 0b0110, 0b0111, 0b0101,
        0b0100, 0b1100, 0b1101, 0b1001, 0b1000, 0b1010, 0b1011,
    )

    override fun encode(state: AcState): IrFrame =
        IrFrame(CARRIER_HZ, patternFor(rawFor(state)))

    override fun swingToggleFrame(state: AcState): IrFrame =
        IrFrame(CARRIER_HZ, patternFor(SWING_TOGGLE_STATE))

    /** The 24-bit state word; exposed internally for unit tests. */
    internal fun rawFor(state: AcState): Long {
        if (!state.power) return POWER_OFF_STATE
        val temp = state.temperatureC.coerceIn(temperatureRange)
        // FAN mode rides on the DRY mode bits with a reserved temperature code; AUTO and DRY
        // require the fan field's dedicated auto value 0b000.
        val modeBits = when (state.mode) {
            ClimateMode.COOL -> 0b00
            ClimateMode.DRY, ClimateMode.FAN -> 0b01
            ClimateMode.AUTO -> 0b10
            ClimateMode.HEAT -> 0b11
        }
        val tempCode = if (state.mode == ClimateMode.FAN) {
            FAN_MODE_TEMP_CODE
        } else {
            TEMP_CODES[temp - temperatureRange.first]
        }
        val fanBits = when {
            state.mode == ClimateMode.AUTO || state.mode == ClimateMode.DRY -> 0b000
            else -> when (state.fan) {
                FanSpeed.AUTO -> 0b101
                FanSpeed.LOW -> 0b100
                FanSpeed.MEDIUM -> 0b010
                FanSpeed.HIGH -> 0b001
            }
        }
        return (PREFIX.toLong() shl 16) or
            (fanBits.toLong() shl 13) or
            (SENSOR_TEMP_OFF.toLong() shl 8) or
            (tempCode.toLong() shl 4) or
            (modeBits.toLong() shl 2)
    }

    private fun patternFor(raw: Long): IntArray {
        val builder = IrPatternBuilder()
        repeat(2) {
            builder.mark(HDR_MARK).space(HDR_SPACE)
            for (shift in intArrayOf(16, 8, 0)) {
                val byte = ((raw shr shift) and 0xFF).toInt()
                builder.bitsMsbFirst(byte.toLong(), 8, BIT_MARK, ONE_SPACE, ZERO_SPACE)
                builder.bitsMsbFirst((byte.inv() and 0xFF).toLong(), 8, BIT_MARK, ONE_SPACE, ZERO_SPACE)
            }
            builder.mark(BIT_MARK).space(MIN_GAP)
        }
        return builder.build()
    }
}
