package com.nikhilpallavur.remotehub.device.tv.protocol

import com.nikhilpallavur.remotehub.core.model.RemoteKey

/**
 * Builders and parsers for the Android TV Remote v2 wire messages, split across the two TLS
 * channels: the pairing channel (port 6467) and the remote/command channel (port 6466). Field
 * numbers follow the reverse-engineered `pairingmessage.proto` / `remotemessage.proto` used by
 * the widely deployed `androidtv-remote` and Home Assistant `androidtvremote2` clients.
 *
 * Everything here is pure (bytes in, bytes out) so it is unit-tested without a TV.
 */
// One codec object per protocol keeps the message builders/parsers together; the count is high
// by design (pairing + remote channels), like the firewall wire-format parsers.
@Suppress("TooManyFunctions")
object AndroidTvProtocol {
    // --- PairingMessage top-level fields ---
    private const val PAIR_PROTOCOL_VERSION = 1
    private const val PAIR_STATUS = 2
    private const val PAIR_REQUEST = 10
    private const val PAIR_REQUEST_ACK = 11
    private const val PAIR_OPTION = 20
    private const val PAIR_CONFIGURATION = 30
    private const val PAIR_CONFIGURATION_ACK = 31
    private const val PAIR_SECRET = 40
    private const val PAIR_SECRET_ACK = 41

    const val STATUS_OK = 200
    const val PROTOCOL_VERSION = 2

    private const val ROLE_INPUT = 1
    private const val ENCODING_HEXADECIMAL = 3
    private const val PAIRING_SYMBOL_LENGTH = 6

    // --- RemoteMessage top-level fields ---
    private const val REMOTE_CONFIGURE = 1
    private const val REMOTE_SET_ACTIVE = 2
    private const val REMOTE_ERROR = 3
    private const val REMOTE_PING_REQUEST = 8
    private const val REMOTE_PING_RESPONSE = 9
    private const val REMOTE_KEY_INJECT = 10
    private const val REMOTE_START = 20
    private const val REMOTE_APP_LINK = 90

    private const val ACTIVE_MAGIC = 622L

    // RemoteDirection.SHORT = a discrete tap (the value the reference androidtvremote2 client uses).
    const val DIRECTION_SHORT = 3

    // KeyEvent bases used to type text as individual key presses.
    private const val KEYCODE_0 = 7
    private const val KEYCODE_A = 29
    private const val KEYCODE_SPACE = 62

    /**
     * Maps a universal [RemoteKey] to its Android `KeyEvent` key-code, or null when Android TV
     * has no equivalent. Values mirror `android.view.KeyEvent.KEYCODE_*`.
     */
    @Suppress("CyclomaticComplexMethod")
    fun keyCode(key: RemoteKey): Int? = when (key) {
        RemoteKey.POWER -> 26
        RemoteKey.POWER_ON -> 26
        RemoteKey.POWER_OFF -> 26
        RemoteKey.HOME -> 3
        RemoteKey.BACK -> 4
        RemoteKey.MENU -> 82
        RemoteKey.GUIDE -> 172
        RemoteKey.INFO -> 165
        RemoteKey.INPUT_SOURCE -> 178
        RemoteKey.SETTINGS -> 176
        RemoteKey.SEARCH -> 84
        RemoteKey.DPAD_UP -> 19
        RemoteKey.DPAD_DOWN -> 20
        RemoteKey.DPAD_LEFT -> 21
        RemoteKey.DPAD_RIGHT -> 22
        RemoteKey.DPAD_CENTER -> 23
        RemoteKey.VOLUME_UP -> 24
        RemoteKey.VOLUME_DOWN -> 25
        RemoteKey.MUTE -> 164
        RemoteKey.CHANNEL_UP -> 166
        RemoteKey.CHANNEL_DOWN -> 167
        RemoteKey.BACKSPACE -> 67
        RemoteKey.PLAY_PAUSE -> 85
        RemoteKey.PLAY -> 126
        RemoteKey.PAUSE -> 127
        RemoteKey.STOP -> 86
        RemoteKey.REWIND -> 89
        RemoteKey.FAST_FORWARD -> 90
        RemoteKey.NEXT -> 87
        RemoteKey.PREVIOUS -> 88
        RemoteKey.NUM_0 -> 7
        RemoteKey.NUM_1 -> 8
        RemoteKey.NUM_2 -> 9
        RemoteKey.NUM_3 -> 10
        RemoteKey.NUM_4 -> 11
        RemoteKey.NUM_5 -> 12
        RemoteKey.NUM_6 -> 13
        RemoteKey.NUM_7 -> 14
        RemoteKey.NUM_8 -> 15
        RemoteKey.NUM_9 -> 16
        RemoteKey.NETFLIX, RemoteKey.YOUTUBE, RemoteKey.PRIME_VIDEO,
        RemoteKey.DISNEY_PLUS, RemoteKey.HOTSTAR, RemoteKey.SPOTIFY, RemoteKey.APPLE_TV,
        -> null
    }

    /**
     * The `KeyEvent` code that types a single character into a focused field, or null when the
     * character can't be sent as a plain key press. Letters are injected without a shift meta —
     * Android TV search boxes treat the letter keys case-insensitively.
     */
    fun charKeyCode(character: Char): Int? = when (character) {
        in 'a'..'z' -> KEYCODE_A + (character - 'a')
        in 'A'..'Z' -> KEYCODE_A + (character - 'A')
        in '0'..'9' -> KEYCODE_0 + (character - '0')
        ' ' -> KEYCODE_SPACE
        else -> null
    }

    /** Deep-link launch URLs for app shortcuts that have no key-code. */
    fun appLink(key: RemoteKey): String? = when (key) {
        RemoteKey.YOUTUBE -> "https://www.youtube.com"
        RemoteKey.NETFLIX -> "https://www.netflix.com/title"
        RemoteKey.PRIME_VIDEO -> "https://app.primevideo.com"
        RemoteKey.DISNEY_PLUS -> "https://www.disneyplus.com"
        RemoteKey.HOTSTAR -> "https://www.hotstar.com"
        RemoteKey.SPOTIFY -> "https://open.spotify.com"
        RemoteKey.APPLE_TV -> "https://tv.apple.com"
        else -> null
    }

    // ---------------- Pairing channel (6467) ----------------

    fun pairingRequest(serviceName: String, clientName: String): ByteArray {
        val request = ProtoWriter()
            .string(1, serviceName)
            .string(2, clientName)
            .toByteArray()
        return pairingEnvelope { message(PAIR_REQUEST, request) }
    }

    fun pairingOption(): ByteArray {
        val encoding = ProtoWriter()
            .enum(1, ENCODING_HEXADECIMAL)
            .varint(2, PAIRING_SYMBOL_LENGTH.toLong())
            .toByteArray()
        val option = ProtoWriter()
            .message(1, encoding)
            .enum(3, ROLE_INPUT)
            .toByteArray()
        return pairingEnvelope { message(PAIR_OPTION, option) }
    }

    fun pairingConfiguration(): ByteArray {
        val encoding = ProtoWriter()
            .enum(1, ENCODING_HEXADECIMAL)
            .varint(2, PAIRING_SYMBOL_LENGTH.toLong())
            .toByteArray()
        val configuration = ProtoWriter()
            .message(1, encoding)
            .enum(2, ROLE_INPUT)
            .toByteArray()
        return pairingEnvelope { message(PAIR_CONFIGURATION, configuration) }
    }

    fun pairingSecret(secret: ByteArray): ByteArray {
        val payload = ProtoWriter().bytes(1, secret).toByteArray()
        return pairingEnvelope { message(PAIR_SECRET, payload) }
    }

    private inline fun pairingEnvelope(body: ProtoWriter.() -> Unit): ByteArray =
        ProtoWriter()
            .varint(PAIR_PROTOCOL_VERSION, PROTOCOL_VERSION.toLong())
            .enum(PAIR_STATUS, STATUS_OK)
            .apply(body)
            .toByteArray()

    /** Classifies a server pairing message so the state machine can advance. */
    fun classifyPairing(message: ProtoMessage): PairingReply {
        val status = message.varint(PAIR_STATUS)?.toInt() ?: 0
        return when {
            status != STATUS_OK -> PairingReply.Error(status)
            message.has(PAIR_REQUEST_ACK) -> PairingReply.RequestAck
            message.has(PAIR_OPTION) -> PairingReply.OptionAck
            message.has(PAIR_CONFIGURATION_ACK) -> PairingReply.ConfigurationAck
            message.has(PAIR_SECRET_ACK) -> PairingReply.SecretAck
            else -> PairingReply.Unknown
        }
    }

    // ---------------- Remote channel (6466) ----------------

    fun configureResponse(model: String, vendor: String, packageName: String, appVersion: String): ByteArray {
        val deviceInfo = ProtoWriter()
            .string(1, model)
            .string(2, vendor)
            .varint(3, 1)
            .bool(4, true)
            .string(5, packageName)
            .string(6, appVersion)
            .toByteArray()
        val configure = ProtoWriter()
            .varint(1, ACTIVE_MAGIC)
            .message(2, deviceInfo)
            .toByteArray()
        return ProtoWriter().message(REMOTE_CONFIGURE, configure).toByteArray()
    }

    fun setActive(): ByteArray {
        val active = ProtoWriter().varint(1, ACTIVE_MAGIC).toByteArray()
        return ProtoWriter().message(REMOTE_SET_ACTIVE, active).toByteArray()
    }

    fun pingResponse(value: Long): ByteArray {
        val ping = ProtoWriter().varint(1, value).toByteArray()
        return ProtoWriter().message(REMOTE_PING_RESPONSE, ping).toByteArray()
    }

    fun keyInject(keyCode: Int, direction: Int = DIRECTION_SHORT): ByteArray {
        // Field order matters: RemoteKeyInject { RemoteKeyCode key_code = 1; RemoteDirection direction = 2 }.
        // (These were previously swapped, so every key was read by the TV as key_code = SHORT's value.)
        val inject = ProtoWriter()
            .enum(1, keyCode)
            .enum(2, direction)
            .toByteArray()
        return ProtoWriter().message(REMOTE_KEY_INJECT, inject).toByteArray()
    }

    fun appLinkLaunch(url: String): ByteArray {
        val launch = ProtoWriter().string(1, url).toByteArray()
        return ProtoWriter().message(REMOTE_APP_LINK, launch).toByteArray()
    }

    /** Classifies a server remote-channel message and extracts the ping value when present. */
    fun classifyRemote(message: ProtoMessage): RemoteServerMessage = when {
        message.has(REMOTE_CONFIGURE) -> RemoteServerMessage.Configure
        message.has(REMOTE_SET_ACTIVE) -> RemoteServerMessage.SetActive
        message.has(REMOTE_START) -> RemoteServerMessage.Start
        message.has(REMOTE_PING_REQUEST) ->
            RemoteServerMessage.Ping(message.message(REMOTE_PING_REQUEST)?.varint(1) ?: 0)
        message.has(REMOTE_ERROR) -> RemoteServerMessage.Error
        else -> RemoteServerMessage.Other
    }
}

/** Outcome of a server message on the pairing channel. */
sealed interface PairingReply {
    data object RequestAck : PairingReply
    data object OptionAck : PairingReply
    data object ConfigurationAck : PairingReply
    data object SecretAck : PairingReply
    data object Unknown : PairingReply
    data class Error(val status: Int) : PairingReply
}

/** Outcome of a server message on the remote/command channel. */
sealed interface RemoteServerMessage {
    data object Configure : RemoteServerMessage
    data object SetActive : RemoteServerMessage
    data object Start : RemoteServerMessage
    data class Ping(val value: Long) : RemoteServerMessage
    data object Error : RemoteServerMessage
    data object Other : RemoteServerMessage
}
