package com.nikhilpallavur.remotehub.device.tv.protocol
import com.nikhilpallavur.remotehub.core.model.RemoteKey

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.device.tv.protocol.AndroidTvProtocol
import com.nikhilpallavur.remotehub.device.tv.protocol.PairingReply
import com.nikhilpallavur.remotehub.device.tv.protocol.ProtoBuf
import com.nikhilpallavur.remotehub.device.tv.protocol.ProtoWriter
import com.nikhilpallavur.remotehub.device.tv.protocol.RemoteServerMessage
import org.junit.Test

class AndroidTvProtocolTest {
    @Test
    fun mapsCoreKeysToAndroidKeyCodes() {
        assertThat(AndroidTvProtocol.keyCode(RemoteKey.POWER)).isEqualTo(26)
        assertThat(AndroidTvProtocol.keyCode(RemoteKey.DPAD_UP)).isEqualTo(19)
        assertThat(AndroidTvProtocol.keyCode(RemoteKey.DPAD_CENTER)).isEqualTo(23)
        assertThat(AndroidTvProtocol.keyCode(RemoteKey.MUTE)).isEqualTo(164)
        assertThat(AndroidTvProtocol.keyCode(RemoteKey.NUM_0)).isEqualTo(7)
    }

    @Test
    fun appShortcutsHaveNoKeyCodeButHaveAppLink() {
        assertThat(AndroidTvProtocol.keyCode(RemoteKey.NETFLIX)).isNull()
        assertThat(AndroidTvProtocol.appLink(RemoteKey.YOUTUBE)).isNotEmpty()
        assertThat(AndroidTvProtocol.appLink(RemoteKey.POWER)).isNull()
    }

    @Test
    fun pairingRequestCarriesVersionStatusAndNames() {
        val message = ProtoBuf.parse(AndroidTvProtocol.pairingRequest("svc", "cli"))
        assertThat(message.varint(1)).isEqualTo(AndroidTvProtocol.PROTOCOL_VERSION.toLong())
        assertThat(message.varint(2)).isEqualTo(AndroidTvProtocol.STATUS_OK.toLong())
        val request = message.message(10)
        assertThat(request?.string(1)).isEqualTo("svc")
        assertThat(request?.string(2)).isEqualTo("cli")
    }

    @Test
    fun keyInjectEncodesKeyCodeThenDirection() {
        // Order is TV-critical: RemoteKeyInject { key_code = 1; direction = 2 }. If these are
        // swapped the TV reads the direction as the key code and every button acts like HOME.
        val inject = ProtoBuf.parse(AndroidTvProtocol.keyInject(26)).message(10)
        assertThat(inject?.varint(1)).isEqualTo(26L)
        assertThat(inject?.varint(2)).isEqualTo(AndroidTvProtocol.DIRECTION_SHORT.toLong())
    }

    @Test
    fun mapsSearchKeyAndTypesCharactersToKeyCodes() {
        assertThat(AndroidTvProtocol.keyCode(RemoteKey.SEARCH)).isEqualTo(84)
        assertThat(AndroidTvProtocol.charKeyCode('a')).isEqualTo(29)
        assertThat(AndroidTvProtocol.charKeyCode('z')).isEqualTo(54)
        assertThat(AndroidTvProtocol.charKeyCode('A')).isEqualTo(29)
        assertThat(AndroidTvProtocol.charKeyCode('0')).isEqualTo(7)
        assertThat(AndroidTvProtocol.charKeyCode('9')).isEqualTo(16)
        assertThat(AndroidTvProtocol.charKeyCode(' ')).isEqualTo(62)
        assertThat(AndroidTvProtocol.charKeyCode('!')).isNull()
    }

    @Test
    fun setActiveAndPingResponseCarryExpectedValues() {
        assertThat(ProtoBuf.parse(AndroidTvProtocol.setActive()).message(2)?.varint(1)).isEqualTo(622L)
        assertThat(ProtoBuf.parse(AndroidTvProtocol.pingResponse(99)).message(9)?.varint(1)).isEqualTo(99L)
    }

    @Test
    fun classifiesServerPairingReplies() {
        val ack = ProtoWriter().varint(2, 200).message(11, ByteArray(0)).toByteArray()
        assertThat(AndroidTvProtocol.classifyPairing(ProtoBuf.parse(ack))).isEqualTo(PairingReply.RequestAck)

        val error = ProtoWriter().varint(2, 400).toByteArray()
        assertThat(AndroidTvProtocol.classifyPairing(ProtoBuf.parse(error)))
            .isInstanceOf(PairingReply.Error::class.java)
    }

    @Test
    fun classifiesServerRemoteMessages() {
        val start = ProtoWriter().message(20, ByteArray(0)).toByteArray()
        assertThat(AndroidTvProtocol.classifyRemote(ProtoBuf.parse(start)))
            .isEqualTo(RemoteServerMessage.Start)

        val pingBody = ProtoWriter().varint(1, 5).toByteArray()
        val ping = ProtoWriter().message(8, pingBody).toByteArray()
        val classified = AndroidTvProtocol.classifyRemote(ProtoBuf.parse(ping))
        assertThat(classified).isEqualTo(RemoteServerMessage.Ping(5))
    }
}
