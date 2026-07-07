package com.nikhilpallavur.remotehub.device.tv.protocol
import com.nikhilpallavur.remotehub.core.model.RemoteKey

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.device.tv.protocol.SamsungProtocol
import org.junit.Test
import java.util.Base64

class SamsungProtocolTest {
    @Test
    fun mapsKeysToTizenStrings() {
        assertThat(SamsungProtocol.keyName(RemoteKey.POWER)).isEqualTo("KEY_POWER")
        assertThat(SamsungProtocol.keyName(RemoteKey.BACK)).isEqualTo("KEY_RETURN")
        assertThat(SamsungProtocol.keyName(RemoteKey.DPAD_CENTER)).isEqualTo("KEY_ENTER")
        assertThat(SamsungProtocol.keyName(RemoteKey.NUM_5)).isEqualTo("KEY_5")
        assertThat(SamsungProtocol.keyName(RemoteKey.NETFLIX)).isNull()
    }

    @Test
    fun encodesNameAsBase64() {
        val decoded = Base64.getDecoder().decode(SamsungProtocol.encodeName("OmniCore"))
        assertThat(String(decoded)).isEqualTo("OmniCore")
    }

    @Test
    fun appendsTokenToSocketUrlWhenPresent() {
        val withoutToken = SamsungProtocol.socketUrl("1.2.3.4", 8002, "OmniCore", null)
        assertThat(withoutToken).doesNotContain("token=")
        val withToken = SamsungProtocol.socketUrl("1.2.3.4", 8002, "OmniCore", "abc123")
        assertThat(withToken).contains("&token=abc123")
    }

    @Test
    fun clickCommandContainsTheKey() {
        assertThat(SamsungProtocol.clickCommand("KEY_POWER")).contains("\"DataOfCmd\":\"KEY_POWER\"")
    }

    @Test
    fun parsesTokenAndEvent() {
        val message = """{"event":"ms.channel.connect","data":{"token":"99887766"}}"""
        assertThat(SamsungProtocol.parseEvent(message)).isEqualTo(SamsungProtocol.EVENT_CONNECT)
        assertThat(SamsungProtocol.parseToken(message)).isEqualTo("99887766")
    }
}
