package com.nikhilpallavur.remotehub.device.tv.protocol
import com.nikhilpallavur.remotehub.core.model.RemoteKey

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.device.tv.protocol.LgProtocol
import org.junit.Test

class LgProtocolTest {
    @Test
    fun mapsMediaAndAudioKeysToSsapUris() {
        assertThat(LgProtocol.ssapUri(RemoteKey.VOLUME_UP)).isEqualTo("ssap://audio/volumeUp")
        assertThat(LgProtocol.ssapUri(RemoteKey.POWER)).isEqualTo("ssap://system/turnOff")
        assertThat(LgProtocol.ssapUri(RemoteKey.PLAY)).isEqualTo("ssap://media.controls/play")
    }

    @Test
    fun navigationKeysAreRoutedToThePointerSocketNotSsap() {
        assertThat(LgProtocol.ssapUri(RemoteKey.DPAD_UP)).isNull()
        assertThat(LgProtocol.pointerButton(RemoteKey.DPAD_UP)).isEqualTo("UP")
        assertThat(LgProtocol.isCenter(RemoteKey.DPAD_CENTER)).isTrue()
        assertThat(LgProtocol.pointerButtonFrame("UP")).isEqualTo("type:button\nname:UP\n\n")
    }

    @Test
    fun registerPayloadIncludesClientKeyOnlyWhenKnown() {
        assertThat(LgProtocol.registerPayload(null)).doesNotContain("client-key")
        val withKey = LgProtocol.registerPayload("KEY-123")
        assertThat(withKey).contains("client-key")
        assertThat(withKey).contains("KEY-123")
        assertThat(withKey).contains("PROMPT")
    }

    @Test
    fun buildsSsapRequestJson() {
        val request = LgProtocol.request("req_1", "ssap://audio/volumeUp")
        assertThat(request).contains("\"type\":\"request\"")
        assertThat(request).contains("\"id\":\"req_1\"")
        assertThat(request).contains("\"uri\":\"ssap://audio/volumeUp\"")
    }

    @Test
    fun parsesClientKeyAndPointerSocketPath() {
        val registered = """{"type":"registered","payload":{"client-key":"deadbeef"}}"""
        assertThat(LgProtocol.parseClientKey(registered)).isEqualTo("deadbeef")
        val pointer = """{"type":"response","payload":{"socketPath":"wss://1.2.3.4:3001/x"}}"""
        assertThat(LgProtocol.parsePointerSocketPath(pointer)).isEqualTo("wss://1.2.3.4:3001/x")
    }
}
