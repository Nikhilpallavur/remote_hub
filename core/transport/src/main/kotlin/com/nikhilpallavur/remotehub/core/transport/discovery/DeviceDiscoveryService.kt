package com.nikhilpallavur.remotehub.core.transport.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.nikhilpallavur.remotehub.core.drivers.DeviceDriver
import com.nikhilpallavur.remotehub.core.drivers.DriverRegistry
import com.nikhilpallavur.remotehub.core.model.RemoteDevice
import com.nikhilpallavur.remotehub.core.transport.Ssdp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds controllable devices on the current Wi-Fi using two complementary mechanisms: Android NSD
 * (mDNS/Bonjour) and SSDP. It knows nothing about specific vendors — it reads the discovery hints
 * every registered driver contributes and attributes each result back to its driver, so a new
 * discoverable device type participates automatically.
 */
@Singleton
class DeviceDiscoveryService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: DriverRegistry,
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun discover(): Flow<RemoteDevice> = callbackFlow {
        val discoverable = registry.discoverable()
        val nsdTypeToDriver: Map<String, DeviceDriver> = buildMap {
            discoverable.forEach { driver ->
                driver.descriptor.discovery.nsdServiceTypes.forEach { type -> put(type, driver) }
            }
        }
        val ssdpDrivers = discoverable.filter { it.descriptor.discovery.ssdpMatchKeywords.isNotEmpty() }
        val ssdpTargets = (discoverable.flatMap { it.descriptor.discovery.ssdpSearchTargets } + Ssdp.ST_ALL)
            .distinct()

        val seen = ConcurrentHashMap.newKeySet<String>()
        val emit: (RemoteDevice) -> Unit = { device -> if (seen.add(device.id)) trySend(device) }

        val lock = wifiManager.createMulticastLock("remotehub-discovery").apply {
            setReferenceCounted(false)
            runCatching { acquire() }
        }
        val listeners = nsdTypeToDriver.mapNotNull { (type, driver) -> startNsd(type, driver, emit) }
        val ssdpJob = if (ssdpTargets.size > 1) {
            launch(Dispatchers.IO) { runSsdp(ssdpTargets, ssdpDrivers, emit) }
        } else {
            null
        }

        awaitClose {
            ssdpJob?.cancel()
            listeners.forEach { runCatching { nsdManager.stopServiceDiscovery(it) } }
            runCatching { lock.release() }
        }
    }

    @Suppress("DEPRECATION")
    private fun startNsd(
        serviceType: String,
        driver: DeviceDriver,
        emit: (RemoteDevice) -> Unit,
    ): NsdManager.DiscoveryListener? {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) {
                nsdManager.resolveService(info, resolveListener(driver, emit))
            }

            override fun onStartDiscoveryFailed(type: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(type: String, errorCode: Int) = Unit
            override fun onDiscoveryStarted(type: String) = Unit
            override fun onDiscoveryStopped(type: String) = Unit
            override fun onServiceLost(info: NsdServiceInfo) = Unit
        }
        return runCatching {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            listener
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun resolveListener(driver: DeviceDriver, emit: (RemoteDevice) -> Unit) =
        object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceResolved(info: NsdServiceInfo) {
                val host = bestHostAddress(info) ?: return
                val friendly = info.attributes["fn"]?.toString(Charsets.UTF_8) ?: info.serviceName
                emit(deviceFrom(driver, host, friendly))
            }
        }

    /**
     * Prefer an IPv4 address — NsdManager frequently resolves a device's IPv6 address, which phones
     * often cannot route to (EHOSTUNREACH). On API 34+ we can read the full address list; before
     * that we are limited to the single (deprecated) resolved host.
     */
    @Suppress("DEPRECATION")
    private fun bestHostAddress(info: NsdServiceInfo): String? {
        val host = info.host ?: return null
        val preferred = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                info.hostAddresses.firstOrNull { it is Inet4Address } ?: info.hostAddresses.firstOrNull()
            host is Inet6Address ->
                runCatching {
                    InetAddress.getAllByName(host.hostName).firstOrNull { it is Inet4Address }
                }.getOrNull() ?: host
            else -> host
        }
        return preferred?.hostAddress
    }

    private fun runSsdp(
        targets: List<String>,
        ssdpDrivers: List<DeviceDriver>,
        emit: (RemoteDevice) -> Unit,
    ) {
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = SSDP_READ_TIMEOUT_MS
                val target = InetAddress.getByName(Ssdp.MULTICAST_ADDRESS)
                targets.forEach { st ->
                    val datagram = Ssdp.mSearch(st)
                    socket.send(DatagramPacket(datagram, datagram.size, target, Ssdp.PORT))
                }
                readSsdpResponses(socket, ssdpDrivers, emit)
            }
        }
    }

    private fun readSsdpResponses(
        socket: DatagramSocket,
        ssdpDrivers: List<DeviceDriver>,
        emit: (RemoteDevice) -> Unit,
    ) {
        val deadline = System.currentTimeMillis() + SSDP_SCAN_MS
        val buffer = ByteArray(SSDP_BUFFER)
        while (System.currentTimeMillis() < deadline) {
            val packet = DatagramPacket(buffer, buffer.size)
            val received = runCatching { socket.receive(packet); true }.getOrDefault(false)
            if (!received) continue
            val response = String(packet.data, 0, packet.length, Charsets.US_ASCII)
            ssdpDevice(response, ssdpDrivers)?.let(emit)
        }
    }

    private fun ssdpDevice(response: String, ssdpDrivers: List<DeviceDriver>): RemoteDevice? {
        val headers = Ssdp.parseHeaders(response)
        val host = headers["LOCATION"]?.let(Ssdp::hostFromLocation) ?: return null
        val haystack = "${headers["ST"]} ${headers["SERVER"]} ${headers["USN"]}".lowercase()
        val driver = ssdpDrivers.firstOrNull { d ->
            d.descriptor.discovery.ssdpMatchKeywords.any { it in haystack }
        } ?: return null
        return deviceFrom(driver, host, "${driver.descriptor.displayName} · $host")
    }

    private fun deviceFrom(driver: DeviceDriver, host: String, name: String): RemoteDevice {
        val d = driver.descriptor
        return RemoteDevice(
            id = RemoteDevice.idFor(d.id, host),
            name = name.ifBlank { d.displayName },
            driverId = d.id,
            category = d.category,
            transport = d.transport,
            host = host,
            port = d.defaultPort,
        )
    }

    private companion object {
        const val SSDP_SCAN_MS = 5000L
        const val SSDP_READ_TIMEOUT_MS = 1200
        const val SSDP_BUFFER = 2048
    }
}
