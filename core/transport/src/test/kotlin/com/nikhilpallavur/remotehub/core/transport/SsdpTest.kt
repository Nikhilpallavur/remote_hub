package com.nikhilpallavur.remotehub.core.transport

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.core.transport.Ssdp
import org.junit.Test

class SsdpTest {
    @Test
    fun mSearchTargetsTheRequestedService() {
        val datagram = String(Ssdp.mSearch(Ssdp.ST_ROKU))
        assertThat(datagram).contains("M-SEARCH * HTTP/1.1")
        assertThat(datagram).contains("ST: roku:ecp")
        assertThat(datagram).contains("HOST: 239.255.255.250:1900")
    }

    @Test
    fun parsesResponseHeadersCaseInsensitively() {
        val response = "HTTP/1.1 200 OK\r\nLOCATION: http://192.168.1.5:8060/\r\nST: roku:ecp\r\n\r\n"
        val headers = Ssdp.parseHeaders(response)
        assertThat(headers["LOCATION"]).isEqualTo("http://192.168.1.5:8060/")
        assertThat(headers["ST"]).isEqualTo("roku:ecp")
    }

    @Test
    fun extractsHostFromLocation() {
        assertThat(Ssdp.hostFromLocation("http://192.168.1.5:8060/")).isEqualTo("192.168.1.5")
    }
}
