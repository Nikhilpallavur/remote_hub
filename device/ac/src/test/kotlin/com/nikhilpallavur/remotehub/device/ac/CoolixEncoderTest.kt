package com.nikhilpallavur.remotehub.device.ac

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed
import org.junit.Test

class CoolixEncoderTest {

    private val on = AcState(power = true, mode = ClimateMode.COOL, temperatureC = 24, fan = FanSpeed.AUTO)

    @Test
    fun matchesReferenceDefaultState() {
        // IRremoteESP8266 kCoolixDefaultState: on, 25°C, auto mode (auto mode forces fan 0b000).
        val raw = CoolixEncoder.rawFor(on.copy(mode = ClimateMode.AUTO, temperatureC = 25))
        assertThat(raw).isEqualTo(0xB21FC8L)
    }

    @Test
    fun powerOffSendsReferenceOffConstant() {
        assertThat(CoolixEncoder.rawFor(on.copy(power = false))).isEqualTo(0xB27BE0L)
    }

    @Test
    fun powerOnAndOffFramesDiffer() {
        assertThat(CoolixEncoder.encode(on).pattern.toList())
            .isNotEqualTo(CoolixEncoder.encode(on.copy(power = false)).pattern.toList())
    }

    @Test
    fun encodesTwoRepeatsOfFortyEightBitsEach() {
        val frame = CoolixEncoder.encode(on)
        // Per repeat: header 2 + 48 bits * 2 + footer mark/gap 2 = 100; sent twice.
        assertThat(frame.pattern.size).isEqualTo(200)
        assertThat(frame.pattern[0]).isEqualTo(4692)
        assertThat(frame.pattern[1]).isEqualTo(4416)
        assertThat(frame.carrierHz).isEqualTo(38_000)
    }

    @Test
    fun clampsTemperatureToProtocolRange() {
        val tooHot = CoolixEncoder.rawFor(on.copy(temperatureC = 45))
        val max = CoolixEncoder.rawFor(on.copy(temperatureC = 30))
        assertThat(tooHot).isEqualTo(max)
    }

    @Test
    fun fanModeUsesReservedTemperatureCode() {
        val raw = CoolixEncoder.rawFor(on.copy(mode = ClimateMode.FAN))
        assertThat((raw shr 4) and 0xF).isEqualTo(0b1110L)
    }

    @Test
    fun autoAndDryModesForceAutoZeroFan() {
        val auto = CoolixEncoder.rawFor(on.copy(mode = ClimateMode.AUTO, fan = FanSpeed.HIGH))
        val dry = CoolixEncoder.rawFor(on.copy(mode = ClimateMode.DRY, fan = FanSpeed.HIGH))
        assertThat((auto shr 13) and 0b111).isEqualTo(0L)
        assertThat((dry shr 13) and 0b111).isEqualTo(0L)
    }

    @Test
    fun swingIsADedicatedToggleFrame() {
        val toggle = CoolixEncoder.swingToggleFrame(on)
        assertThat(toggle.pattern).isNotEmpty()
        assertThat(toggle.pattern.toList()).isNotEqualTo(CoolixEncoder.encode(on).pattern.toList())
    }

    @Test
    fun encodingIsDeterministic() {
        assertThat(CoolixEncoder.encode(on).pattern.toList())
            .isEqualTo(CoolixEncoder.encode(on).pattern.toList())
    }
}
