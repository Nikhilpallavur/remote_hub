package com.nikhilpallavur.remotehub.device.tv.store

import android.content.Context
import com.nikhilpallavur.remotehub.device.tv.protocol.AndroidTvCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext

/**
 * Owns the one long-lived self-signed client certificate used for every Android TV pairing,
 * persisted as a PKCS12 file in app-private storage so a paired TV keeps trusting us across app
 * restarts. Generated lazily on first use.
 */
@Singleton
class AndroidTvKeyStoreProvider @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val file = File(context.filesDir, "androidtv_remote.p12")
    private val password = "remotehub-remote".toCharArray()
    private val keyStore: KeyStore by lazy { loadOrCreate() }

    fun sslContext(): SSLContext = AndroidTvCrypto.sslContext(keyStore, password)

    fun clientCertificate(): X509Certificate = AndroidTvCrypto.clientCertificate(keyStore)

    /** Short per-install id (public-key fingerprint) so each phone is distinct to the TV. */
    fun clientId(): String = AndroidTvCrypto.clientId(clientCertificate())

    @Synchronized
    private fun loadOrCreate(): KeyStore {
        if (file.exists()) {
            val existing = runCatching {
                file.inputStream().use { AndroidTvCrypto.loadKeyStore(it, password) }
            }.getOrNull()
            if (existing != null) return existing
        }
        val created = AndroidTvCrypto.newClientKeyStore(password)
        runCatching { file.outputStream().use { AndroidTvCrypto.saveKeyStore(created, it, password) } }
        return created
    }
}
