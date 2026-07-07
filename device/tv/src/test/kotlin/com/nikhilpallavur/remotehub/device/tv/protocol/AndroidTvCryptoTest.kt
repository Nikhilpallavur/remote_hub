package com.nikhilpallavur.remotehub.device.tv.protocol

import com.google.common.truth.Truth.assertThat
import com.nikhilpallavur.remotehub.device.tv.protocol.AndroidTvCrypto
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

class AndroidTvCryptoTest {
    @Test
    fun stripsLeadingSignByteFromMagnitude() {
        // 128 -> BigInteger.toByteArray() is [0x00, 0x80]; the unsigned magnitude is just [0x80].
        assertThat(AndroidTvCrypto.unsignedBytes(BigInteger.valueOf(128)))
            .isEqualTo(byteArrayOf(0x80.toByte()))
        assertThat(AndroidTvCrypto.unsignedBytes(BigInteger.valueOf(255)))
            .isEqualTo(byteArrayOf(0xFF.toByte()))
    }

    @Test
    fun parsesHexPairs() {
        assertThat(AndroidTvCrypto.hexToBytes("0a1bff")).isEqualTo(byteArrayOf(0x0a, 0x1b, 0xFF.toByte()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOddLengthHex() {
        AndroidTvCrypto.hexToBytes("abc")
    }

    @Test
    fun codeIsValidWhenFirstByteMatchesDigest() {
        // Code "ab…" -> first byte 0xAB must equal the digest's first byte.
        assertThat(AndroidTvCrypto.isCodeValid("abcdef", byteArrayOf(0xAB.toByte(), 1, 2))).isTrue()
        assertThat(AndroidTvCrypto.isCodeValid("abcdef", byteArrayOf(0x00, 1, 2))).isFalse()
    }

    @Test
    fun pairingSecretIsDeterministicAnd32Bytes() {
        val generator = KeyPairGenerator.getInstance("RSA").apply { initialize(1024) }
        val client = generator.generateKeyPair().public as RSAPublicKey
        val server = generator.generateKeyPair().public as RSAPublicKey

        val first = AndroidTvCrypto.pairingSecret("12abcd", client, server)
        val second = AndroidTvCrypto.pairingSecret("12abcd", client, server)

        assertThat(first).isEqualTo(second)
        assertThat(first.size).isEqualTo(32)
    }
}
