package com.nikhilpallavur.remotehub.device.tv.protocol

import com.nikhilpallavur.remotehub.core.model.RemoteKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * LG webOS "SSAP" WebSocket protocol. Registration triggers an on-TV prompt and returns a
 * persistent client-key; afterwards SSAP requests drive audio/channel/media/power, while D-pad
 * navigation rides a secondary "pointer input" socket whose URL the TV hands back on request.
 * Pure builders + response parsing, unit-tested without a TV.
 */
object LgProtocol {
    const val POINTER_SOCKET_URI = "ssap://com.webos.service.networkinput/getPointerInputSocket"

    fun socketUrl(host: String, port: Int): String = "ws://$host:$port"

    /** The pairing handshake. When [clientKey] is known the TV re-authorizes without prompting. */
    fun registerPayload(clientKey: String?): String = buildJsonObject {
        put("type", "register")
        put("id", "register_0")
        put(
            "payload",
            buildJsonObject {
                put("forcePairing", false)
                put("pairingType", "PROMPT")
                if (!clientKey.isNullOrBlank()) put("client-key", clientKey)
                put("manifest", Json.parseToJsonElement(MANIFEST))
            },
        )
    }.toString()

    fun request(id: String, uri: String, payload: JsonObject? = null): String = buildJsonObject {
        put("type", "request")
        put("id", id)
        put("uri", uri)
        if (payload != null) put("payload", payload)
    }.toString()

    /** SSAP endpoint for [key], or null when the key is handled via the pointer socket instead. */
    @Suppress("CyclomaticComplexMethod")
    fun ssapUri(key: RemoteKey): String? = when (key) {
        RemoteKey.POWER, RemoteKey.POWER_OFF -> "ssap://system/turnOff"
        RemoteKey.VOLUME_UP -> "ssap://audio/volumeUp"
        RemoteKey.VOLUME_DOWN -> "ssap://audio/volumeDown"
        RemoteKey.MUTE -> "ssap://audio/setMute"
        RemoteKey.CHANNEL_UP -> "ssap://tv/channelUp"
        RemoteKey.CHANNEL_DOWN -> "ssap://tv/channelDown"
        RemoteKey.PLAY, RemoteKey.PLAY_PAUSE -> "ssap://media.controls/play"
        RemoteKey.PAUSE -> "ssap://media.controls/pause"
        RemoteKey.STOP -> "ssap://media.controls/stop"
        RemoteKey.REWIND -> "ssap://media.controls/rewind"
        RemoteKey.FAST_FORWARD -> "ssap://media.controls/fastForward"
        else -> null
    }

    /** Mute is the one SSAP call needing a body; we set an explicit state rather than toggling. */
    fun mutePayload(muted: Boolean): JsonObject = buildJsonObject { put("mute", muted) }

    /** Pointer-socket button name for navigation keys, or null if not a pointer button. */
    fun pointerButton(key: RemoteKey): String? = when (key) {
        RemoteKey.DPAD_UP -> "UP"
        RemoteKey.DPAD_DOWN -> "DOWN"
        RemoteKey.DPAD_LEFT -> "LEFT"
        RemoteKey.DPAD_RIGHT -> "RIGHT"
        RemoteKey.BACK -> "BACK"
        RemoteKey.HOME -> "HOME"
        RemoteKey.MENU -> "MENU"
        RemoteKey.INFO -> "INFO"
        else -> null
    }

    fun pointerButtonFrame(name: String): String = "type:button\nname:$name\n\n"

    /** OK/select is a pointer "click" rather than a named button. */
    const val POINTER_CLICK_FRAME = "type:click\n\n"

    fun isCenter(key: RemoteKey): Boolean = key == RemoteKey.DPAD_CENTER

    fun parseClientKey(message: String): String? = runCatching {
        Json.parseToJsonElement(message).jsonObject["payload"]
            ?.jsonObject?.get("client-key")?.jsonPrimitive?.content
    }.getOrNull()

    fun parsePointerSocketPath(message: String): String? = runCatching {
        Json.parseToJsonElement(message).jsonObject["payload"]
            ?.jsonObject?.get("socketPath")?.jsonPrimitive?.content
    }.getOrNull()

    // Standard LG remote app manifest used by the established webOS client libraries; the TV
    // matches against these permissions when showing the pairing prompt.
    private const val MANIFEST = """
        {
          "manifestVersion": 1,
          "appVersion": "1.1",
          "signed": {
            "created": "20140509",
            "appId": "com.lge.test",
            "vendorId": "com.lge",
            "localizedAppNames": {"": "LG Remote App", "ko-KR": "리모컨 앱"},
            "localizedVendorNames": {"": "LG Electronics"},
            "permissions": ["TEST_SECURE", "CONTROL_INPUT_TEXT", "CONTROL_MOUSE_AND_KEYBOARD",
              "READ_INSTALLED_APPS", "READ_LGE_SDX", "READ_NOTIFICATIONS", "SEARCH",
              "WRITE_SETTINGS", "WRITE_NOTIFICATION_ALERT", "CONTROL_POWER",
              "READ_CURRENT_CHANNEL", "READ_RUNNING_APPS", "READ_UPDATE_INFO",
              "UPDATE_FROM_REMOTE_APP", "READ_LGE_TV_INPUT_EVENTS", "READ_TV_CURRENT_TIME"],
            "serial": "2f930e2d2cfe083771f68e4fe7bb07"
          },
          "permissions": ["LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CLOSE", "TEST_OPEN",
            "TEST_PROTECTED", "CONTROL_AUDIO", "CONTROL_DISPLAY", "CONTROL_INPUT_JOYSTICK",
            "CONTROL_INPUT_MEDIA_RECORDING", "CONTROL_INPUT_MEDIA_PLAYBACK", "CONTROL_INPUT_TV",
            "CONTROL_POWER", "READ_APP_STATUS", "READ_CURRENT_CHANNEL", "READ_INPUT_DEVICE_LIST",
            "READ_NETWORK_STATE", "READ_RUNNING_APPS", "READ_TV_CHANNEL_LIST",
            "WRITE_NOTIFICATION_TOAST", "READ_POWER_STATE", "READ_COUNTRY_INFO"],
          "signatures": [{"signatureVersion": 1, "signature": "eyJhbGdvcml0aG0iOiJSU0EtU0hBMjU2In0"}]
        }
    """
}
