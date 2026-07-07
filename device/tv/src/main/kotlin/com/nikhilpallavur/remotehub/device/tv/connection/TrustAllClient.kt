package com.nikhilpallavur.remotehub.device.tv.connection

import com.nikhilpallavur.remotehub.device.tv.protocol.AndroidTvCrypto
import okhttp3.OkHttpClient

/**
 * Derives an OkHttp client that accepts the self-signed certificates Samsung (wss:8002) and the
 * LG pointer socket (wss:3001) present. As with the Android TV link, trust is established by the
 * on-TV authorization step, not by a certificate chain — so chain validation is intentionally
 * skipped for these LAN-only sockets.
 */
internal fun OkHttpClient.trustingAll(): OkHttpClient {
    val sslContext = AndroidTvCrypto.sslContext(keyManagers = null)
    return newBuilder()
        .sslSocketFactory(sslContext.socketFactory, AndroidTvCrypto.TrustAllManager)
        .hostnameVerifier { _, _ -> true }
        .build()
}
