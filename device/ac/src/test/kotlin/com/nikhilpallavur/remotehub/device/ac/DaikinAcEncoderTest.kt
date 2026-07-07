package com.nikhilpallavur.remotehub.device.ac

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed
import org.junit.Test

class DaikinAcEncoderTest {

    private val on = AcState(power = true, mode = ClimateMode.COOL, temperatureC = 24, fan = FanSpeed.AUTO)

    @Test
    fun buildsThirtyFiveBytesWithSectionHeaders() {
        val bytes = DaikinAcEncoder.stateBytes(on)
        assertThat(bytes.size).isEqualTo(35)
        for (offset in intArrayOf(0, 8, 16)) {
            assertThat(bytes[offset]).isEqualTo(0x11)
            assertThat(bytes[offset + 1]).isEqualTo(0xDA)
            assertThat(bytes[offset + 2]).isEqualTo(0x27)
        }
    }

    @Test
    fun sectionChecksumsAreByteSums() {
        val bytes = DaikinAcEncoder.stateBytes(on.copy(temperatureC = 28, swing = true))
        assertThat(bytes[7]).isEqualTo((0..6).sumOf { bytes[it] } and 0xFF)
        assertThat(bytes[15]).isEqualTo((8..14).sumOf { bytes[it] } and 0xFF)
        assertThat(bytes[34]).isEqualTo((16..33).sumOf { bytes[it] } and 0xFF)
    }

    @Test
    fun encodesPowerModeTempFanAndSwingFields() {
        val bytes = DaikinAcEncoder.stateBytes(
            on.copy(mode = ClimateMode.HEAT, temperatureC = 25, fan = FanSpeed.AUTO, swing = true),
        )
        assertThat(bytes[21] and 1).isEqualTo(1) // power on
        assertThat((bytes[21] shr 4) and 0b111).isEqualTo(0b100) // heat
        assertThat(bytes[22]).isEqualTo(50) // 25°C in half-degree units
        assertThat(bytes[24] shr 4).isEqualTo(0b1010) // fan auto
        assertThat(bytes[24] and 0xF).isEqualTo(0b1111) // swing on
        val off = DaikinAcEncoder.stateBytes(on.copy(power = false))
        assertThat(off[21] and 1).isEqualTo(0)
    }

    @Test
    fun framesPreambleAndThreeSections() {
        val frame = DaikinAcEncoder.encode(on)
        // Preamble 5 bits * 2 + stop 2, sections of 8/8/19 bytes: (2 + n*16 + 2) each.
        assertThat(frame.pattern.size).isEqualTo(12 + 132 + 132 + 308)
        assertThat(frame.pattern[12]).isEqualTo(3650) // first section header mark
        assertThat(frame.pattern[13]).isEqualTo(1623)
        assertThat(frame.carrierHz).isEqualTo(38_000)
    }

    @Test
    fun clampsTemperatureToProtocolRange() {
        assertThat(DaikinAcEncoder.stateBytes(on.copy(temperatureC = 50)).toList())
            .isEqualTo(DaikinAcEncoder.stateBytes(on.copy(temperatureC = 32)).toList())
    }

    @Test
    fun encodingIsDeterministic() {
        assertThat(DaikinAcEncoder.encode(on).pattern.toList())
            .isEqualTo(DaikinAcEncoder.encode(on).pattern.toList())
    }
}
