package com.rakuten.tech.mobile.remoteconfig.verification

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import org.amshove.kluent.*
import org.junit.Before
import org.junit.Test
import java.security.KeyStore

class AesEncryptorSpec : RobolectricBaseSpec() {

    private val mockKeyStore: KeyStore = mock()
    private val mockKeyGenerator: AesKeyGenerator = mock()
    private val mockKeyEntry: KeyStore.SecretKeyEntry = mock()

    @Before
    fun setup() {
        val key = generateAesKey()
        When calling mockKeyGenerator.generateKey() itReturns key
        When calling mockKeyEntry.secretKey itReturns key
        When calling mockKeyStore.getEntry(any(), anyOrNull()) itReturns mockKeyEntry
    }

    @Test
    fun `it should encrypt the data`() {
        val encryptor = createAesEncryptor()

        encryptor.encrypt("test data") shouldNotContain "test data"
    }

    @Test
    fun `it should attach IV`() {
        val encryptor = createAesEncryptor()

        encryptor.encrypt("test data") shouldContain "\"iv\":"
    }

    @Test
    fun `it should decrypt the data`() {
        val encryptor = createAesEncryptor()

        val encryptedData = encryptor.encrypt("test data")

        encryptor.decrypt(encryptedData) shouldEqual "test data"
    }

    @Test
    fun `it should generate a new key when one is not in the key store`() {
        When calling mockKeyStore.getEntry(any(), anyOrNull()) itReturns null
        val encryptor = createAesEncryptor()

        encryptor.encrypt("test data")

        Verify on mockKeyGenerator that mockKeyGenerator.generateKey() was called
    }

    @Test
    fun `it should not generate a key when one is already in the key store`() {
        When calling mockKeyGenerator.generateKey() itReturns mock()
        val encryptor = createAesEncryptor()

        encryptor.encrypt("test data")

        VerifyNotCalled on mockKeyGenerator that mockKeyGenerator.generateKey()
    }

    @Test
    fun `it should generate a new key when the one in the store is not an AES key`() {
        val mockRsaKeyEntry: KeyStore.PrivateKeyEntry = mock()
        When calling mockKeyStore.getEntry(any(), anyOrNull()) itReturns mockRsaKeyEntry
        val encryptor = createAesEncryptor()
        encryptor.encrypt("test data")

        Verify on mockKeyGenerator that mockKeyGenerator.generateKey() was called
    }

    private fun createAesEncryptor(
        keyStore: KeyStore = mockKeyStore,
        keyGenerator: AesKeyGenerator = mockKeyGenerator
    ) = AesEncryptor(keyStore, keyGenerator)
}
