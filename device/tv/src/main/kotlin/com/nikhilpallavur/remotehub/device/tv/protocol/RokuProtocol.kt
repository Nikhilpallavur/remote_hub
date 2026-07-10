package com.nikhilpallavur.remotehub.device.tv.protocol

import com.nikhilpallavur.remotehub.core.model.RemoteKey
import java.net.URLEncoder

/**
 * Roku External Control Protocol (ECP): plain HTTP on port 8060, no pairing. Key presses are
 * `POST /keypress/<Key>`, apps launch via `POST /launch/<id>`, and device facts come back as XML
 * from `GET /query/device-info`. All pure URL/string building, so fully unit-tested.
 */
object RokuProtocol {
    fun baseUrl(host: String, port: Int): String = "http://$host:$port"

    /** Roku ECP key name for [key], or null when Roku has no matching key. */
    @Suppress("CyclomaticComplexMethod")
    fun keyName(key: RemoteKey): String? = when (key) {
        RemoteKey.POWER -> "Power"
        RemoteKey.POWER_ON -> "PowerOn"
        RemoteKey.POWER_OFF -> "PowerOff"
        RemoteKey.HOME -> "Home"
        RemoteKey.BACK -> "Back"
        RemoteKey.INFO -> "Info"
        RemoteKey.INPUT_SOURCE -> "InputTuner"
        RemoteKey.DPAD_UP -> "Up"
        RemoteKey.DPAD_DOWN -> "Down"
        RemoteKey.DPAD_LEFT -> "Left"
        RemoteKey.DPAD_RIGHT -> "Right"
        RemoteKey.DPAD_CENTER -> "Select"
        RemoteKey.VOLUME_UP -> "VolumeUp"
        RemoteKey.VOLUME_DOWN -> "VolumeDown"
        RemoteKey.MUTE -> "VolumeMute"
        RemoteKey.CHANNEL_UP -> "ChannelUp"
        RemoteKey.CHANNEL_DOWN -> "ChannelDown"
        RemoteKey.BACKSPACE -> "Backspace"
        RemoteKey.PLAY_PAUSE, RemoteKey.PLAY, RemoteKey.PAUSE -> "Play"
        RemoteKey.REWIND -> "Rev"
        RemoteKey.FAST_FORWARD -> "Fwd"
        RemoteKey.NUM_0 -> "Lit_0"
        RemoteKey.NUM_1 -> "Lit_1"
        RemoteKey.NUM_2 -> "Lit_2"
        RemoteKey.NUM_3 -> "Lit_3"
        RemoteKey.NUM_4 -> "Lit_4"
        RemoteKey.NUM_5 -> "Lit_5"
        RemoteKey.NUM_6 -> "Lit_6"
        RemoteKey.NUM_7 -> "Lit_7"
        RemoteKey.NUM_8 -> "Lit_8"
        RemoteKey.NUM_9 -> "Lit_9"
        else -> null
    }

    /** App-store id for shortcut keys that launch apps rather than press a button. */
    fun appId(key: RemoteKey): String? = when (key) {
        RemoteKey.NETFLIX -> "12"
        RemoteKey.YOUTUBE -> "837"
        RemoteKey.PRIME_VIDEO -> "13"
        else -> null
    }

    fun keypressUrl(host: String, port: Int, keyName: String): String =
        "${baseUrl(host, port)}/keypress/$keyName"

    fun launchUrl(host: String, port: Int, appId: String): String =
        "${baseUrl(host, port)}/launch/$appId"

    fun deviceInfoUrl(host: String, port: Int): String =
        "${baseUrl(host, port)}/query/device-info"

    /** Per-character "Lit_" keypress so the user can type into Roku search/text fields. */
    fun literalUrl(host: String, port: Int, character: Char): String {
        val encoded = URLEncoder.encode(character.toString(), "UTF-8")
        return "${baseUrl(host, port)}/keypress/Lit_$encoded"
    }

    /** Pulls a friendly name + model out of the device-info XML (no XML parser needed). */
    fun parseDeviceInfo(xml: String): RokuInfo {
        val name = tag(xml, "user-device-name")
            ?: tag(xml, "friendly-device-name")
            ?: tag(xml, "default-device-name")
            ?: "Roku"
        return RokuInfo(name = name, model = tag(xml, "model-name"))
    }

    private fun tag(xml: String, name: String): String? {
        val open = "<$name>"
        val close = "</$name>"
        val start = xml.indexOf(open)
        if (start < 0) return null
        val end = xml.indexOf(close, start + open.length)
        if (end < 0) return null
        return xml.substring(start + open.length, end).trim().ifEmpty { null }
    }
}

data class RokuInfo(val name: String, val model: String?)
