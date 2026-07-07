package com.nikhilpallavur.remotehub.device.ir
import com.nikhilpallavur.remotehub.core.model.RemoteKey

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.device.ir.NecEncoder
import org.junit.Test

class NecEncoderTest {
    @Test
    fun encodesLeadInThenThirtyTwoDataBitsPlusStop() {
        val pattern = NecEncoder.encode(0x00, 0x45)
        // 2 (lead) + 32 bits * 2 entries + 1 stop = 67
        assertThat(pattern.size).isEqualTo(67)
        assertThat(pattern[0]).isEqualTo(9000)
        assertThat(pattern[1]).isEqualTo(4500)
    }

    @Test
    fun providesDefaultCommandsForCommonKeysOnly() {
        assertThat(NecEncoder.commandFor(RemoteKey.POWER)).isEqualTo(0x45)
        assertThat(NecEncoder.commandFor(RemoteKey.MUTE)).isEqualTo(0x0D)
        assertThat(NecEncoder.commandFor(RemoteKey.NETFLIX)).isNull()
    }
}
