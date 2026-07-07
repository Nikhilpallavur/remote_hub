package com.nikhilpallavur.remotehub.device.tv.connection

import com.nikhilpallavur.remotehub.core.drivers.BaseRemoteConnection
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.device.tv.protocol.RokuProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Roku ECP connection: stateless HTTP, no pairing. "Connecting" just verifies the device answers on
 * port 8060; every key is a fire-and-forget POST.
 */
class RokuConnection(
    descriptor: DriverDescriptor,
    private val client: OkHttpClient,
) : BaseRemoteConnection(descriptor) {
    private var device: RemoteDevice? = null

    override suspend fun connect(device: RemoteDevice) {
        this.device = device
        mutableState.value = ConnectionState.Connecting(device)
        val host = device.host
        val reachable = host != null && withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(RokuProtocol.deviceInfoUrl(host, device.port)).build()
                client.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }
        if (reachable) {
            mutableState.value = ConnectionState.Connected(device.copy(paired = true))
        } else {
            fail(device, "Couldn't reach the Roku at ${device.host}. Check it's on and on this Wi-Fi.")
        }
    }

    override suspend fun send(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> sendKey(command.key)
        is RemoteCommand.TypeText -> sendText(command.text)
        else -> false
    }

    override fun supports(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> RokuProtocol.keyName(command.key) != null ||
            RokuProtocol.appId(command.key) != null
        is RemoteCommand.TypeText -> true
        else -> false
    }

    private suspend fun sendKey(key: RemoteKey): Boolean {
        val host = device?.host ?: return false
        val port = device?.port ?: return false
        val url = RokuProtocol.appId(key)?.let { RokuProtocol.launchUrl(host, port, it) }
            ?: RokuProtocol.keyName(key)?.let { RokuProtocol.keypressUrl(host, port, it) }
            ?: return false
        return post(url)
    }

    private suspend fun sendText(text: String): Boolean {
        val host = device?.host ?: return false
        val port = device?.port ?: return false
        var ok = true
        for (character in text) {
            ok = post(RokuProtocol.literalUrl(host, port, character)) && ok
        }
        return ok
    }

    override fun close() {
        device = null
        mutableState.value = ConnectionState.Idle
    }

    private suspend fun post(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).post(ByteArray(0).toRequestBody()).build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
