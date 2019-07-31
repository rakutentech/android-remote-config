package com.rakuten.tech.mobile.remoteconfig.verification

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import org.amshove.kluent.*
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import java.security.cert.Certificate

class RsaEncryptorSpec : RobolectricBaseSpec() {

    private val mockKeyStore: KeyStore = mock()
    private val mockKeyGenerator: RsaKeyGenerator = mock()
    private val mockKeyEntry: KeyStore.PrivateKeyEntry = mock()

    @Before
    fun setup() {
        val key = generateRsaKey()
        val certificate: Certificate = mock()
        When calling certificate.publicKey itReturns key.public
        When calling mockKeyStore.getEntry(any(), anyOrNull()) itReturns mockKeyEntry
        When calling mockKeyEntry.privateKey itReturns key.private
        When calling mockKeyEntry.certificate itReturns certificate
        When calling mockKeyGenerator.generateKey(any()) itReturns key
    }

    @Test
    fun `it should encrypt the data`() {
        val encryptor = createRsaEncryptor()

        encryptor.encrypt("test data") shouldNotContain "test data"
    }

    @Test
    fun `it should decrypt the data`() {
        val encryptor = createRsaEncryptor()

        val encryptedData = encryptor.encrypt("test data")

        encryptor.decrypt(encryptedData) shouldEqual "test data"
    }

    @Test
    fun `it should generate a new key when one isn't in the key store`() {
        When calling mockKeyStore.getEntry(any(), anyOrNull()) itReturns null
        val encryptor = createRsaEncryptor()

        encryptor.encrypt("test data")

        Verify on mockKeyGenerator that mockKeyGenerator.generateKey(any()) was called
    }

    @Test
    fun `it should not generate a key when one is already in the key store`() {
        When calling mockKeyGenerator.generateKey(any()) itReturns mock()
        val encryptor = createRsaEncryptor()

        val encryptedData = encryptor.encrypt("test data")
        encryptor.decrypt(encryptedData)

        VerifyNotCalled on mockKeyGenerator that mockKeyGenerator.generateKey(any())
    }

    @Test
    fun `it should generate a new key when the one in the store is not an AES key`() {
        val mockAesKeyEntry: KeyStore.SecretKeyEntry = mock()
        When calling mockKeyStore.getEntry(any(), anyOrNull()) itReturns mockAesKeyEntry
        val encryptor = createRsaEncryptor()

        encryptor.encrypt("test data")

        Verify on mockKeyGenerator that mockKeyGenerator.generateKey(any()) was called
    }

    private fun createRsaEncryptor(
        keyStore: KeyStore = mockKeyStore,
        keyGenerator: RsaKeyGenerator = mockKeyGenerator
    ) = RsaEncryptor(mock(), keyStore, keyGenerator)
}
