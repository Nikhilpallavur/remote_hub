package com.nikhilpallavur.remotehub.device.tv.protocol

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Date
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Crypto for Android TV Remote v2: minting the self-signed client certificate the TV pins
 * during pairing, building the (deliberately permissive — security comes from the pairing
 * secret, not the cert chain) TLS context, and computing the pairing secret.
 *
 * The secret computation is the protocol's crux and is pure, so [pairingSecret] /
 * [unsignedBytes] / [isCodeValid] are unit-tested directly.
 */
// Groups the pure pairing-secret math with the cert/TLS plumbing it feeds; splitting them would
// just scatter one cohesive protocol concern.
@Suppress("TooManyFunctions")
object AndroidTvCrypto {
    private const val KEY_SIZE = 2048
    private const val CERT_VALIDITY_MS = 25L * 365 * 24 * 60 * 60 * 1000
    private const val CLIENT_ID_BYTES = 4
    const val CLIENT_ALIAS = "omnicore-remote"

    /**
     * SHA-256(clientModulus ‖ clientExponent ‖ serverModulus ‖ serverExponent ‖ codeTail), where
     * codeTail is the hex code with its first byte (the checksum nibble-pair) removed. The full
     * digest is the secret sent back to the TV. Matches the Polo/`androidtvremote2` algorithm.
     */
    fun pairingSecret(code: String, clientKey: RSAPublicKey, serverKey: RSAPublicKey): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(unsignedBytes(clientKey.modulus))
        digest.update(unsignedBytes(clientKey.publicExponent))
        digest.update(unsignedBytes(serverKey.modulus))
        digest.update(unsignedBytes(serverKey.publicExponent))
        digest.update(hexToBytes(code.substring(2)))
        return digest.digest()
    }

    fun pairingSecret(code: String, clientCert: X509Certificate, serverCert: X509Certificate): ByteArray =
        pairingSecret(code, clientCert.publicKey as RSAPublicKey, serverCert.publicKey as RSAPublicKey)

    /** The TV's code is valid iff its first byte equals the first byte of our computed digest. */
    fun isCodeValid(code: String, secret: ByteArray): Boolean {
        val codeBytes = hexToBytes(code)
        return codeBytes.isNotEmpty() && secret.isNotEmpty() && codeBytes[0] == secret[0]
    }

    /** Big-endian magnitude bytes of [value] with any leading sign byte stripped (I2OSP). */
    fun unsignedBytes(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        return if (raw.size > 1 && raw[0].toInt() == 0) raw.copyOfRange(1, raw.size) else raw
    }

    fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "hex string must have even length" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    // ---------------- certificate + TLS plumbing (runtime, Android/BouncyCastle) ----------------

    /**
     * Generates a fresh RSA key pair plus a long-lived self-signed certificate around it. The CN
     * carries a random per-install suffix so every phone presents a distinct identity to the TV —
     * with a shared constant name, several phones in one household are indistinguishable in the
     * TV's paired-remotes bookkeeping and can evict each other's session. Existing keystores are
     * never regenerated, so already-paired installs keep their old CN and stay paired.
     */
    fun newClientKeyStore(password: CharArray): KeyStore {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(KEY_SIZE)
        val keyPair = generator.generateKeyPair()

        val now = Date()
        val notAfter = Date(now.time + CERT_VALIDITY_MS)
        val subject = X500Name("CN=RemoteHub-${randomSuffix()}")
        val serial = BigInteger.valueOf(now.time)
        val builder = JcaX509v3CertificateBuilder(subject, serial, now, notAfter, subject, keyPair.public)
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        return KeyStore.getInstance("PKCS12").apply {
            load(null, password)
            setKeyEntry(CLIENT_ALIAS, keyPair.private, password, arrayOf<X509Certificate>(certificate))
        }
    }

    fun loadKeyStore(input: InputStream, password: CharArray): KeyStore =
        KeyStore.getInstance("PKCS12").apply { load(input, password) }

    fun saveKeyStore(keyStore: KeyStore, output: OutputStream, password: CharArray) {
        keyStore.store(output, password)
    }

    fun clientCertificate(keyStore: KeyStore): X509Certificate =
        keyStore.getCertificate(CLIENT_ALIAS) as X509Certificate

    /**
     * Short stable fingerprint of [certificate]'s public key — the per-install id woven into the
     * names the TV sees (pairing dialog, connected-remotes list). Derived, not stored, so it works
     * for pre-existing keystores minted before CNs carried a suffix.
     */
    fun clientId(certificate: X509Certificate): String =
        MessageDigest.getInstance("SHA-256")
            .digest(certificate.publicKey.encoded)
            .take(CLIENT_ID_BYTES)
            .joinToString("") { byte -> Integer.toHexString(byte.toInt() and 0xFF).padStart(2, '0') }

    private fun randomSuffix(): String {
        val bytes = ByteArray(CLIENT_ID_BYTES)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { byte -> Integer.toHexString(byte.toInt() and 0xFF).padStart(2, '0') }
    }

    /** TLS context that presents our client cert and trusts any server cert (LAN + pairing pin). */
    fun sslContext(keyStore: KeyStore, password: CharArray): SSLContext {
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, password)
        return sslContext(kmf.keyManagers)
    }

    fun sslContext(keyManagers: Array<KeyManager>?): SSLContext =
        SSLContext.getInstance("TLS").apply {
            init(keyManagers, arrayOf<TrustManager>(TrustAllManager), SecureRandom())
        }

    fun peerCertificate(session: SSLSession): X509Certificate =
        session.peerCertificates.first() as X509Certificate

    /** Accept-all trust manager. The Android TV link is secured by the pairing secret, not PKI. */
    object TrustAllManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
