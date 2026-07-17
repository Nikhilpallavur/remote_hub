package com.nikhilpallavur.remotehub.device.tv.connection

import android.util.Log
import com.nikhilpallavur.remotehub.core.drivers.BaseRemoteConnection
import com.nikhilpallavur.remotehub.core.drivers.DriverDescriptor
import com.nikhilpallavur.remotehub.core.model.ConnectionState
import com.nikhilpallavur.remotehub.core.model.PairingMode
import com.nikhilpallavur.remotehub.core.model.RemoteCommand
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.model.RemoteKey
import com.nikhilpallavur.remotehub.device.tv.protocol.AndroidTvCrypto
import com.nikhilpallavur.remotehub.device.tv.protocol.AndroidTvProtocol
import com.nikhilpallavur.remotehub.device.tv.protocol.PairingReply
import com.nikhilpallavur.remotehub.device.tv.protocol.ProtoBuf
import com.nikhilpallavur.remotehub.device.tv.protocol.RemoteServerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket

/**
 * Android TV Remote v2 — the protocol the stock OnePlus / Google TV remote app speaks. Two TLS
 * channels: pairing (6467) mints trust via a 6-digit code shown on the TV, then the command
 * channel (6466) carries key injections. Our self-signed client cert is what the TV pins, so once
 * paired we connect straight to 6466 and only re-pair if the TV has forgotten us.
 */
// Two TLS channels + pairing handshake + key/text injection make for many small, focused methods.
@Suppress("TooManyFunctions")
class AndroidTvConnection(
    descriptor: DriverDescriptor,
    private val sslContext: SSLContext,
    private val clientCert: X509Certificate,
    private val deviceName: String,
) : BaseRemoteConnection(descriptor) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private var device: RemoteDevice? = null
    private var pairingSocket: SSLSocket? = null
    private var serverCert: X509Certificate? = null
    private var commandSocket: SSLSocket? = null
    private var loopJob: Job? = null

    override suspend fun connect(device: RemoteDevice) {
        this.device = device
        mutableState.value = ConnectionState.Connecting(device)
        if (device.paired) openCommandChannel(device) else startPairing(device)
    }

    private suspend fun startPairing(device: RemoteDevice) {
        withContext(Dispatchers.IO) {
            runCatching {
                val socket = openSocket(requireHost(device), PAIRING_PORT, PAIRING_TIMEOUT_MS)
                pairingSocket = socket
                serverCert = AndroidTvCrypto.peerCertificate(socket.session)
                val out = socket.outputStream
                val input = socket.inputStream

                writeFramed(out, AndroidTvProtocol.pairingRequest(SERVICE_NAME, deviceName))
                requirePairing(input)
                writeFramed(out, AndroidTvProtocol.pairingOption())
                requirePairing(input)
                writeFramed(out, AndroidTvProtocol.pairingConfiguration())
                requirePairing(input)
            }.onSuccess {
                mutableState.value = ConnectionState.AwaitingPairing(device, PairingMode.PIN_CODE)
            }.onFailure { error ->
                closePairing()
                fail(device, pairingFailureMessage(device.host.orEmpty(), error))
            }
        }
    }

    override suspend fun submitPairingCode(code: String) {
        val device = device ?: return
        val socket = pairingSocket ?: return
        val server = serverCert ?: return
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = code.trim().lowercase()
                val secret = AndroidTvCrypto.pairingSecret(normalized, clientCert, server)
                require(AndroidTvCrypto.isCodeValid(normalized, secret)) {
                    "That code didn't match — re-enter the 6 digits on the TV."
                }
                writeFramed(socket.outputStream, AndroidTvProtocol.pairingSecret(secret))
                requirePairing(socket.inputStream)
            }.onSuccess {
                closePairing()
                openCommandChannel(device.copy(paired = true))
            }.onFailure { error ->
                fail(device, error.message ?: "Pairing failed.")
            }
        }
    }

    private suspend fun openCommandChannel(device: RemoteDevice) {
        mutableState.value = ConnectionState.Connecting(device)
        val result = withContext(Dispatchers.IO) { connectCommandSocket(device) }
        when (result) {
            is CommandChannelResult.Open -> {
                commandSocket = result.socket
                loopJob = scope.launch { readCommandLoop(device, result.socket) }
            }
            CommandChannelResult.NotTrusted -> {
                // The TV rejected our certificate during the TLS handshake — it has genuinely
                // forgotten us, so a fresh pairing is the only way back in.
                this.device = device.copy(paired = false)
                startPairing(device.copy(paired = false))
            }
            is CommandChannelResult.Unreachable -> fail(
                device,
                "Couldn't reach ${device.name}. Make sure the TV is on and on the same Wi-Fi, " +
                    "then tap it to reconnect. (${result.reason})",
            )
        }
    }

    /**
     * Opens the command socket with retries. TVs coming out of standby routinely drop or stall the
     * first knock, so transient failures (refused, timed out, reset) are retried with a short
     * backoff instead of being misread as "the TV forgot us" — only a TLS handshake rejection
     * means our certificate is no longer trusted and a re-pair is required.
     */
    private suspend fun connectCommandSocket(device: RemoteDevice): CommandChannelResult {
        var lastError: Throwable? = null
        RETRY_BACKOFF_MS.forEachIndexed { attempt, backoffMs ->
            try {
                return CommandChannelResult.Open(openSocket(requireHost(device), COMMAND_PORT, 0))
            } catch (error: SSLHandshakeException) {
                Log.i(TAG, "command handshake rejected — TV no longer trusts us: ${error.message}")
                return CommandChannelResult.NotTrusted
            } catch (error: IOException) {
                lastError = error
                Log.d(TAG, "command connect attempt ${attempt + 1} failed: ${error.message}")
                if (backoffMs > 0) delay(backoffMs)
            }
        }
        return CommandChannelResult.Unreachable(lastError?.message ?: "no response")
    }

    private sealed interface CommandChannelResult {
        data class Open(val socket: SSLSocket) : CommandChannelResult
        data object NotTrusted : CommandChannelResult
        data class Unreachable(val reason: String) : CommandChannelResult
    }

    private suspend fun readCommandLoop(device: RemoteDevice, socket: SSLSocket) {
        val out = socket.outputStream
        val input = socket.inputStream
        runCatching {
            var running = true
            while (running) {
                val frame = ProtoBuf.readFramed(input)
                running = frame != null &&
                    handleServerMessage(device, out, AndroidTvProtocol.classifyRemote(ProtoBuf.parse(frame)))
            }
        }.onFailure { Log.d(TAG, "command loop ended: ${it.message}") }
        if (mutableState.value is ConnectionState.Connected) mutableState.value = ConnectionState.Idle
    }

    /** Replies to a single server message. Returns false to end the loop (error). */
    private suspend fun handleServerMessage(
        device: RemoteDevice,
        out: OutputStream,
        message: RemoteServerMessage,
    ): Boolean {
        when (message) {
            // deviceName (not a shared constant) as the model: it carries the per-install id, so
            // the TV's connected-remotes bookkeeping can tell multiple phones apart.
            RemoteServerMessage.Configure -> writeLocked(
                out,
                AndroidTvProtocol.configureResponse(deviceName, VENDOR, PACKAGE_NAME, APP_VERSION),
            )
            RemoteServerMessage.SetActive -> writeLocked(out, AndroidTvProtocol.setActive())
            RemoteServerMessage.Start -> mutableState.value = ConnectionState.Connected(device)
            is RemoteServerMessage.Ping -> writeLocked(out, AndroidTvProtocol.pingResponse(message.value))
            RemoteServerMessage.Error -> {
                fail(device, "The TV reported a remote error.")
                return false
            }
            RemoteServerMessage.Other -> Unit
        }
        return true
    }

    override suspend fun send(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> sendKey(command.key)
        is RemoteCommand.TypeText -> sendText(command.text)
        else -> false
    }

    override fun supports(command: RemoteCommand): Boolean = when (command) {
        is RemoteCommand.Press -> supportsKey(command.key)
        is RemoteCommand.TypeText -> true
        else -> false
    }

    private suspend fun sendKey(key: RemoteKey): Boolean {
        val out = commandSocket?.outputStream
        if (out == null) {
            Log.w(TAG, "sendKey($key) ignored — no command channel")
            return false
        }
        val keyCode = AndroidTvProtocol.keyCode(key)
        // Must run off the main thread — these are blocking socket writes (callers dispatch on Main).
        return withContext(Dispatchers.IO) {
            runCatching {
                val message = if (keyCode != null) {
                    AndroidTvProtocol.keyInject(keyCode)
                } else {
                    AndroidTvProtocol.appLink(key)?.let { AndroidTvProtocol.appLinkLaunch(it) }
                        ?: return@runCatching false
                }
                writeLocked(out, message)
                Log.d(TAG, "sent key=$key keyCode=$keyCode")
                true
            }.getOrElse { error ->
                Log.w(TAG, "sendKey($key) failed: ${error.message}")
                false
            }
        }
    }

    private suspend fun sendText(text: String): Boolean {
        val out = commandSocket?.outputStream ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                text.forEach { character ->
                    AndroidTvProtocol.charKeyCode(character)?.let {
                        writeLocked(out, AndroidTvProtocol.keyInject(it))
                    }
                }
                true
            }.getOrElse { error ->
                Log.w(TAG, "sendText failed: ${error.message}")
                false
            }
        }
    }

    private fun supportsKey(key: RemoteKey): Boolean =
        AndroidTvProtocol.keyCode(key) != null || AndroidTvProtocol.appLink(key) != null

    override fun close() {
        loopJob?.cancel()
        closePairing()
        runCatching { commandSocket?.close() }
        commandSocket = null
        scope.coroutineContext[Job]?.cancel()
        mutableState.value = ConnectionState.Idle
    }

    private fun openSocket(host: String, port: Int, timeoutMs: Int): SSLSocket {
        val socket = sslContext.socketFactory.createSocket() as SSLSocket
        socket.connect(InetSocketAddress(resolveHost(host), port), CONNECT_TIMEOUT_MS)
        // A dozing TV can accept TCP and then stall the TLS handshake indefinitely; a bounded
        // handshake read keeps reconnect from hanging forever. The caller's timeout only applies
        // to the established channel (0 = block on the command read loop).
        socket.soTimeout = HANDSHAKE_TIMEOUT_MS
        socket.startHandshake()
        socket.soTimeout = timeoutMs
        return socket
    }

    private fun requireHost(device: RemoteDevice): String =
        device.host ?: throw IOException("Android TV device has no host address")

    private fun requirePairing(input: InputStream): PairingReply {
        val frame = ProtoBuf.readFramed(input) ?: throw IOException("pairing socket closed early")
        val reply = AndroidTvProtocol.classifyPairing(ProtoBuf.parse(frame))
        if (reply is PairingReply.Error) throw IOException("TV rejected pairing (status ${reply.status})")
        return reply
    }

    private fun writeFramed(out: OutputStream, message: ByteArray) {
        out.write(ProtoBuf.frame(message))
        out.flush()
    }

    private suspend fun writeLocked(out: OutputStream, message: ByteArray) =
        writeMutex.withLock { writeFramed(out, message) }

    private fun closePairing() {
        runCatching { pairingSocket?.close() }
        pairingSocket = null
    }

    private companion object {
        const val TAG = "AndroidTvConnection"
        const val PAIRING_PORT = 6467
        const val COMMAND_PORT = 6466
        const val CONNECT_TIMEOUT_MS = 8000
        const val HANDSHAKE_TIMEOUT_MS = 5000
        const val PAIRING_TIMEOUT_MS = 15000
        val RETRY_BACKOFF_MS = listOf(500L, 1000L, 2000L, 0L)
        const val SERVICE_NAME = "RemoteHub"
        const val VENDOR = "RemoteHub"
        const val PACKAGE_NAME = "com.nikhilpallavur.remotehub"
        const val APP_VERSION = "1.0"
    }
}

/**
 * Resolves [host] to an address, auto-attaching the active Wi-Fi interface scope to a bare IPv6
 * link-local (fe80::…) literal so users can paste a TV's link-local address without the `%wlan0`
 * suffix. This is the escape hatch when a router isolates IPv4 between clients but still bridges
 * IPv6 neighbor discovery (common on ISP combo routers).
 */
private fun Inet6Address.needsInterfaceScope(): Boolean = scopeId == 0 || scopedInterface == null

private fun resolveHost(host: String): InetAddress {
    val address = InetAddress.getByName(host)
    if (address is Inet6Address && address.isLinkLocalAddress && address.needsInterfaceScope()) {
        // Link-local IPv6 addresses (fe80::...) require a scope ID (interface name) to be routable.
        // We try to find the active Wi-Fi interface.
        val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
        val wifi = interfaces.firstOrNull { it.name.startsWith("wlan") || it.name.contains("wifi") }
            ?: interfaces.firstOrNull { !it.name.startsWith("lo") }

        if (wifi != null) {
            return Inet6Address.getByAddress(null, address.address, wifi)
        }
    }
    return address
}

private fun pairingFailureMessage(host: String, error: Throwable): String {
    val message = error.message ?: ""
    val routeBlocked = error is ConnectException ||
        message.contains("route to host", ignoreCase = true) ||
        message.contains("EHOSTUNREACH", ignoreCase = true)
    return if (routeBlocked) {
        "Can't reach $host. Phone and TV may be on different Wi-Fi bands/SSIDs, or the router has " +
            "client/AP isolation on. Use the same Wi-Fi, turn isolation off, or add the TV by its " +
            "IPv6 address."
    } else {
        "Couldn't start pairing with $host: $message"
    }
}
