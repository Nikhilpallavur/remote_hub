package com.nikhilpallavur.remotehub.core.transport

import java.net.URI

/**
 * SSDP (the discovery half of UPnP) helpers: build the multicast `M-SEARCH` datagram and parse
 * the unicast responses devices send back. Used to find Roku (ECP), DIAL, Samsung and LG endpoints
 * on the LAN. Pure string logic → unit-tested.
 */
object Ssdp {
    const val MULTICAST_ADDRESS = "239.255.255.250"
    const val PORT = 1900

    // Useful search targets.
    const val ST_ROKU = "roku:ecp"
    const val ST_DIAL = "urn:dial-multiscreen-org:service:dial:1"
    const val ST_SAMSUNG = "urn:samsung.com:device:RemoteControlReceiver:1"
    const val ST_LG = "urn:lge-com:service:webos-second-screen:1"
    const val ST_ALL = "ssdp:all"

    fun mSearch(searchTarget: String, mxSeconds: Int = 2): ByteArray =
        (
            "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: $MULTICAST_ADDRESS:$PORT\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: $mxSeconds\r\n" +
                "ST: $searchTarget\r\n" +
                "\r\n"
            ).toByteArray(Charsets.US_ASCII)

    /** Parses an SSDP response into upper-cased header keys -> values. */
    fun parseHeaders(response: String): Map<String, String> =
        response.split("\r\n", "\n")
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) {
                    null
                } else {
                    line.substring(0, idx).trim().uppercase() to line.substring(idx + 1).trim()
                }
            }
            .toMap()

    /** Extracts the host (IP) from a `LOCATION` header URL such as `http://192.168.1.5:8060/`. */
    fun hostFromLocation(location: String): String? =
        runCatching { URI(location).host }.getOrNull()
}
