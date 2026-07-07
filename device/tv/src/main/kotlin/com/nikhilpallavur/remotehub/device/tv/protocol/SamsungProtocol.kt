package com.nikhilpallavur.remotehub.device.tv.protocol

import com.nikhilpallavur.remotehub.core.model.RemoteKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Samsung Tizen "ms.remote.control" WebSocket protocol (2016+ TVs). The phone opens a secure
 * WebSocket, the TV shows a one-time Allow/Deny prompt and returns a token; reconnecting with the
 * token skips the prompt. Pure URL/JSON building + token parsing, so unit-tested without a TV.
 */
object SamsungProtocol {
    /** Samsung sends the app name base64-encoded in the connect URL. */
    @OptIn(ExperimentalEncodingApi::class)
    fun encodeName(name: String): String =
        Base64.encode(name.toByteArray(Charsets.UTF_8))

    fun socketUrl(host: String, port: Int, appName: String, token: String? = null): String {
        val name = encodeName(appName)
        val base = "wss://$host:$port/api/v2/channels/samsung.remote.control?name=$name"
        return if (token.isNullOrBlank()) base else "$base&token=$token"
    }

    /** Samsung key string for [key], or null when Tizen has no matching key. */
    @Suppress("CyclomaticComplexMethod")
    fun keyName(key: RemoteKey): String? = when (key) {
        RemoteKey.POWER, RemoteKey.POWER_OFF, RemoteKey.POWER_ON -> "KEY_POWER"
        RemoteKey.HOME -> "KEY_HOME"
        RemoteKey.BACK -> "KEY_RETURN"
        RemoteKey.MENU -> "KEY_MENU"
        RemoteKey.GUIDE -> "KEY_GUIDE"
        RemoteKey.INFO -> "KEY_INFO"
        RemoteKey.INPUT_SOURCE -> "KEY_SOURCE"
        RemoteKey.DPAD_UP -> "KEY_UP"
        RemoteKey.DPAD_DOWN -> "KEY_DOWN"
        RemoteKey.DPAD_LEFT -> "KEY_LEFT"
        RemoteKey.DPAD_RIGHT -> "KEY_RIGHT"
        RemoteKey.DPAD_CENTER -> "KEY_ENTER"
        RemoteKey.VOLUME_UP -> "KEY_VOLUP"
        RemoteKey.VOLUME_DOWN -> "KEY_VOLDOWN"
        RemoteKey.MUTE -> "KEY_MUTE"
        RemoteKey.CHANNEL_UP -> "KEY_CHUP"
        RemoteKey.CHANNEL_DOWN -> "KEY_CHDOWN"
        RemoteKey.PLAY_PAUSE -> "KEY_PLAY_BACK"
        RemoteKey.PLAY -> "KEY_PLAY"
        RemoteKey.PAUSE -> "KEY_PAUSE"
        RemoteKey.STOP -> "KEY_STOP"
        RemoteKey.REWIND -> "KEY_REWIND"
        RemoteKey.FAST_FORWARD -> "KEY_FF"
        RemoteKey.NUM_0 -> "KEY_0"
        RemoteKey.NUM_1 -> "KEY_1"
        RemoteKey.NUM_2 -> "KEY_2"
        RemoteKey.NUM_3 -> "KEY_3"
        RemoteKey.NUM_4 -> "KEY_4"
        RemoteKey.NUM_5 -> "KEY_5"
        RemoteKey.NUM_6 -> "KEY_6"
        RemoteKey.NUM_7 -> "KEY_7"
        RemoteKey.NUM_8 -> "KEY_8"
        RemoteKey.NUM_9 -> "KEY_9"
        else -> null
    }

    /** The click command frame the TV expects for a single key press. */
    fun clickCommand(keyName: String): String = buildJsonObject {
        put("method", "ms.remote.control")
        put(
            "params",
            buildJsonObject {
                put("Cmd", "Click")
                put("DataOfCmd", keyName)
                put("Option", "false")
                put("TypeOfRemote", "SendRemoteKey")
            },
        )
    }.toString()

    /** Extracts the auth token from an incoming `ms.channel.connect` frame, if present. */
    fun parseToken(message: String): String? = runCatching {
        Json.parseToJsonElement(message).jsonObject["data"]
            ?.jsonObject?.get("token")?.jsonPrimitive?.content
    }.getOrNull()

    /** The `event` field of an incoming frame, e.g. `ms.channel.connect` / `ms.channel.unauthorized`. */
    fun parseEvent(message: String): String? = runCatching {
        Json.parseToJsonElement(message).jsonObject["event"]?.jsonPrimitive?.content
    }.getOrNull()

    const val EVENT_CONNECT = "ms.channel.connect"
    const val EVENT_UNAUTHORIZED = "ms.channel.unauthorized"
}
