package com.nikhilpallavur.remotehub.device.tv.connection

import com.nikhilpallavur.remotehub.core.drivers.BaseRemoteConnection
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.PairingMode
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.device.tv.protocol.SamsungProtocol
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Samsung Tizen WebSocket connection. The first connection (no token) triggers the on-TV
 * Allow/Deny prompt; accepting it yields a token we persist so future sessions are silent.
 */
class SamsungConnection(
    descriptor: DriverDescriptor,
    client: OkHttpClient,
    private val appName: String,
) : BaseRemoteConnection(descriptor) {
    private val secureClient = client.trustingAll()
    private var webSocket: WebSocket? = null
    private var device: RemoteDevice? = null

    override suspend fun connect(device: RemoteDevice) {
        this.device = device
        mutableState.value = ConnectionState.Connecting(device)
        val host = device.host
        if (host == null) {
            fail(device, "Samsung TV has no host address.")
            return
        }
        val url = SamsungProtocol.socketUrl(host, device.port, appName, device.token)
        webSocket = secureClient.newWebSocket(Request.Builder().url(url).build(), Listener(device))
        if (device.token.isNullOrBlank()) {
            mutableState.value = ConnectionState.AwaitingPairing(device, PairingMode.CONFIRM_ON_DEVICE)
        }
    }

    override suspend fun send(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> {
            val name = SamsungProtocol.keyName(command.key)
            name != null && webSocket?.send(SamsungProtocol.clickCommand(name)) == true
        }
        else -> false
    }

    override fun supports(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> SamsungProtocol.keyName(command.key) != null
        else -> false
    }

    override fun close() {
        webSocket?.close(NORMAL_CLOSURE, null)
        webSocket = null
        mutableState.value = ConnectionState.Idle
    }

    private inner class Listener(private val target: RemoteDevice) : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            when (SamsungProtocol.parseEvent(text)) {
                SamsungProtocol.EVENT_CONNECT -> {
                    val token = SamsungProtocol.parseToken(text) ?: target.token
                    val resolved = target.copy(paired = true, token = token)
                    device = resolved
                    mutableState.value = ConnectionState.Connected(resolved)
                }
                SamsungProtocol.EVENT_UNAUTHORIZED ->
                    fail(target, "The TV denied the connection. Accept the prompt and try again.")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            fail(target, t.message ?: "Samsung connection failed.")
        }
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
