package com.nikhilpallavur.remotehub.device.ac

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed
import org.junit.Test

class LgAcEncoderTest {

    private val on = AcState(power = true, mode = ClimateMode.COOL, temperatureC = 24, fan = FanSpeed.AUTO)

    @Test
    fun powerOffSendsReferenceOffCommand() {
        assertThat(LgAcEncoder.rawFor(on.copy(power = false))).isEqualTo(0x88C0051L)
    }

    @Test
    fun onStatesCarrySignatureAndPowerOnBits() {
        val raw = LgAcEncoder.rawFor(on)
        assertThat(raw shr 20).isEqualTo(0x88L)
        assertThat((raw shr 18) and 0b11).isEqualTo(0L)
    }

    @Test
    fun temperatureFieldIsDegreesMinusFifteen() {
        val raw = LgAcEncoder.rawFor(on.copy(temperatureC = 24))
        assertThat((raw shr 8) and 0xF).isEqualTo(9L)
    }

    @Test
    fun checksumIsNibbleSumOfDataNibbles() {
        for (temp in intArrayOf(16, 22, 30)) {
            for (mode in ClimateMode.entries) {
                val raw = LgAcEncoder.rawFor(on.copy(temperatureC = temp, mode = mode))
                var expected = 0L
                for (nibble in 1..6) expected += (raw shr (4 * nibble)) and 0xF
                assertThat(raw and 0xF).isEqualTo(expected and 0xF)
            }
        }
    }

    @Test
    fun encodesTwentyEightBitsWithHeaderAndStopBit() {
        val frame = LgAcEncoder.encode(on)
        // Header 2 + 28 bits * 2 + stop mark = 59.
        assertThat(frame.pattern.size).isEqualTo(59)
        assertThat(frame.pattern[0]).isEqualTo(8500)
        assertThat(frame.pattern[1]).isEqualTo(4250)
        assertThat(frame.carrierHz).isEqualTo(38_000)
    }

    @Test
    fun clampsTemperatureToProtocolRange() {
        assertThat(LgAcEncoder.rawFor(on.copy(temperatureC = 5)))
            .isEqualTo(LgAcEncoder.rawFor(on.copy(temperatureC = 16)))
    }

    @Test
    fun swingIsADedicatedToggleFrame() {
        val toggle = LgAcEncoder.swingToggleFrame(on)
        assertThat(toggle.pattern).isNotEmpty()
    }

    @Test
    fun encodingIsDeterministic() {
        assertThat(LgAcEncoder.encode(on).pattern.toList())
            .isEqualTo(LgAcEncoder.encode(on).pattern.toList())
    }
}
