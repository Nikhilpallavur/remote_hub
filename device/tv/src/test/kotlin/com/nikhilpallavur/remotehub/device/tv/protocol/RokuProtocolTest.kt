package com.nikhilpallavur.remotehub.device.tv.protocol
import com.nikhilpallavur.remotehub.core.model.RemoteKey

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.device.tv.protocol.RokuProtocol
import org.junit.Test

class RokuProtocolTest {
    @Test
    fun mapsKeysToEcpNames() {
        assertThat(RokuProtocol.keyName(RemoteKey.POWER)).isEqualTo("Power")
        assertThat(RokuProtocol.keyName(RemoteKey.DPAD_CENTER)).isEqualTo("Select")
        assertThat(RokuProtocol.keyName(RemoteKey.VOLUME_UP)).isEqualTo("VolumeUp")
        assertThat(RokuProtocol.keyName(RemoteKey.NUM_1)).isEqualTo("Lit_1")
    }

    @Test
    fun appShortcutsResolveToAppIdsNotKeys() {
        assertThat(RokuProtocol.keyName(RemoteKey.NETFLIX)).isNull()
        assertThat(RokuProtocol.appId(RemoteKey.NETFLIX)).isEqualTo("12")
        assertThat(RokuProtocol.appId(RemoteKey.YOUTUBE)).isEqualTo("837")
    }

    @Test
    fun buildsKeypressAndLaunchUrls() {
        assertThat(RokuProtocol.keypressUrl("1.2.3.4", 8060, "Home"))
            .isEqualTo("http://1.2.3.4:8060/keypress/Home")
        assertThat(RokuProtocol.launchUrl("1.2.3.4", 8060, "12"))
            .isEqualTo("http://1.2.3.4:8060/launch/12")
    }

    @Test
    fun parsesFriendlyNameAndModelFromDeviceInfoXml() {
        val xml = """
            <device-info>
              <user-device-name>Living Room TV</user-device-name>
              <model-name>Roku Ultra</model-name>
            </device-info>
        """.trimIndent()
        val info = RokuProtocol.parseDeviceInfo(xml)
        assertThat(info.name).isEqualTo("Living Room TV")
        assertThat(info.model).isEqualTo("Roku Ultra")
    }

    @Test
    fun fallsBackToDefaultNameWhenXmlHasNoNames() {
        assertThat(RokuProtocol.parseDeviceInfo("<device-info></device-info>").name).isEqualTo("Roku")
    }
}
