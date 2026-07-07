package com.nikhilpallavur.remotehub.device.tv.connection

import com.nikhilpallavur.remotehub.core.drivers.BaseRemoteConnection
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.PairingMode
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.device.tv.protocol.LgProtocol
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicInteger

/**
 * LG webOS connection. The command socket (ws:3000) registers — prompting on the TV the first
 * time and returning a persistent client-key — and carries SSAP requests. Navigation keys ride a
 * second "pointer input" socket whose URL the TV returns on request.
 */
class LgWebOsConnection(
    descriptor: DriverDescriptor,
    client: OkHttpClient,
) : BaseRemoteConnection(descriptor) {
    private val plainClient = client
    private val pointerClient = client.trustingAll()
    private var commandSocket: WebSocket? = null
    private var pointerSocket: WebSocket? = null
    private var device: RemoteDevice? = null
    private val requestId = AtomicInteger(0)

    override suspend fun connect(device: RemoteDevice) {
        this.device = device
        mutableState.value = ConnectionState.Connecting(device)
        val host = device.host
        if (host == null) {
            fail(device, "LG TV has no host address.")
            return
        }
        val request = Request.Builder().url(LgProtocol.socketUrl(host, device.port)).build()
        commandSocket = plainClient.newWebSocket(request, CommandListener(device))
    }

    override suspend fun send(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> sendKey(command.key)
        else -> false
    }

    override fun supports(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> command.key.let {
            LgProtocol.ssapUri(it) != null || LgProtocol.isCenter(it) || LgProtocol.pointerButton(it) != null
        }
        else -> false
    }

    private fun sendKey(key: RemoteKey): Boolean {
        val ssap = LgProtocol.ssapUri(key)
        return when {
            ssap != null -> {
                val payload = if (key == RemoteKey.MUTE) LgProtocol.mutePayload(muted = true) else null
                commandSocket?.send(LgProtocol.request(nextId(), ssap, payload)) == true
            }
            LgProtocol.isCenter(key) -> pointerSocket?.send(LgProtocol.POINTER_CLICK_FRAME) == true
            else -> LgProtocol.pointerButton(key)
                ?.let { pointerSocket?.send(LgProtocol.pointerButtonFrame(it)) == true } ?: false
        }
    }

    override fun close() {
        pointerSocket?.close(NORMAL_CLOSURE, null)
        commandSocket?.close(NORMAL_CLOSURE, null)
        pointerSocket = null
        commandSocket = null
        mutableState.value = ConnectionState.Idle
    }

    private fun nextId(): String = "req_${requestId.incrementAndGet()}"

    private fun openPointerSocket(path: String) {
        val request = Request.Builder().url(path).build()
        pointerSocket = pointerClient.newWebSocket(request, PointerListener)
    }

    private inner class CommandListener(private val target: RemoteDevice) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send(LgProtocol.registerPayload(target.token))
            if (target.token.isNullOrBlank()) {
                mutableState.value = ConnectionState.AwaitingPairing(target, PairingMode.CONFIRM_ON_DEVICE)
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            LgProtocol.parseClientKey(text)?.let { clientKey ->
                val resolved = target.copy(paired = true, token = clientKey)
                device = resolved
                mutableState.value = ConnectionState.Connected(resolved)
                webSocket.send(LgProtocol.request(nextId(), LgProtocol.POINTER_SOCKET_URI))
                return
            }
            LgProtocol.parsePointerSocketPath(text)?.let { path -> openPointerSocket(path) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            fail(target, t.message ?: "LG connection failed.")
        }
    }

    /** The pointer socket only carries our outbound button frames; inbound frames are ignored. */
    private object PointerListener : WebSocketListener()

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
