package com.nikhilpallavur.remotehub.device.ac

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.core.model.ClimateMode
import com.nikhilpallavur.remotehub.core.model.FanSpeed
import org.junit.Test

class SamsungAcEncoderTest {

    private val on = AcState(power = true, mode = ClimateMode.COOL, temperatureC = 24, fan = FanSpeed.AUTO)

    @Test
    fun buildsFourteenByteStateWithPowerBits() {
        val bytes = SamsungAcEncoder.stateBytes(on)
        assertThat(bytes.size).isEqualTo(14)
        assertThat(bytes[0]).isEqualTo(0x02)
        assertThat((bytes[6] shr 4) and 0b11).isEqualTo(0b11)
        assertThat((bytes[13] shr 4) and 0b11).isEqualTo(0b11)
        val off = SamsungAcEncoder.stateBytes(on.copy(power = false))
        assertThat((off[6] shr 4) and 0b11).isEqualTo(0b00)
        assertThat((off[13] shr 4) and 0b11).isEqualTo(0b00)
    }

    @Test
    fun temperatureModeAndFanLandInDocumentedFields() {
        val bytes = SamsungAcEncoder.stateBytes(
            on.copy(temperatureC = 25, mode = ClimateMode.HEAT, fan = FanSpeed.HIGH),
        )
        assertThat(bytes[11] shr 4).isEqualTo(25 - 16)
        assertThat((bytes[12] shr 4) and 0b111).isEqualTo(4) // heat
        assertThat((bytes[12] shr 1) and 0b111).isEqualTo(5) // fan high
    }

    @Test
    fun swingUsesDocumentedFieldValues() {
        val swinging = SamsungAcEncoder.stateBytes(on.copy(swing = true))
        val still = SamsungAcEncoder.stateBytes(on.copy(swing = false))
        assertThat((swinging[9] shr 4) and 0b111).isEqualTo(0b010)
        assertThat((still[9] shr 4) and 0b111).isEqualTo(0b111)
    }

    @Test
    fun sectionChecksumsAreSelfConsistent() {
        for (state in listOf(on, on.copy(power = false), on.copy(temperatureC = 18, swing = true))) {
            val bytes = SamsungAcEncoder.stateBytes(state)
            for (offset in intArrayOf(0, 7)) {
                var bits = Integer.bitCount(bytes[offset])
                bits += Integer.bitCount(bytes[offset + 1] and 0x0F)
                bits += Integer.bitCount(bytes[offset + 2] shr 4)
                for (i in offset + 3 until offset + 7) bits += Integer.bitCount(bytes[i])
                val expected = bits.inv() and 0xFF
                val stored = ((bytes[offset + 1] shr 4) and 0x0F) or ((bytes[offset + 2] and 0x0F) shl 4)
                assertThat(stored).isEqualTo(expected)
            }
        }
    }

    @Test
    fun framesHeaderAndTwoSections() {
        val frame = SamsungAcEncoder.encode(on)
        // Header 2 + 2 sections * (section header 2 + 56 bits * 2 + footer 2) = 234.
        assertThat(frame.pattern.size).isEqualTo(234)
        assertThat(frame.pattern[0]).isEqualTo(690)
        assertThat(frame.pattern[1]).isEqualTo(17844)
        assertThat(frame.carrierHz).isEqualTo(38_000)
    }

    @Test
    fun clampsTemperatureToProtocolRange() {
        assertThat(SamsungAcEncoder.stateBytes(on.copy(temperatureC = 99)).toList())
            .isEqualTo(SamsungAcEncoder.stateBytes(on.copy(temperatureC = 30)).toList())
    }

    @Test
    fun encodingIsDeterministic() {
        assertThat(SamsungAcEncoder.encode(on).pattern.toList())
            .isEqualTo(SamsungAcEncoder.encode(on).pattern.toList())
    }
}
