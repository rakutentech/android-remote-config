package com.rakuten.tech.mobile.remoteconfig.verification

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.Config
import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
import org.amshove.kluent.*
import org.junit.Before
import org.junit.Test

@Suppress("TooGenericExceptionThrown")
class ConfigVerifierSpec : RobolectricBaseSpec() {

    private val stubSignatureVerifier: SignatureVerifier = mock()
    private val mockCache: PublicKeyCache = mock()

    @Before
    fun setup() {
        When calling mockCache[any()] itReturns "test_public_key"
        When calling stubSignatureVerifier.verify(any(), any(), any()) itReturns true
        runBlocking {
            When calling mockCache.fetch(any()) itReturns "test_fetched_key"
        }
    }

    @Test
    fun `should return true when verification succeeds`() {
        val verifier = createVerifier()
        val config = createConfig()

        verifier.verify(config) shouldEqual true
    }

    @Test
    fun `should verify using the signature from provided Config`() {
        When calling stubSignatureVerifier.verify(any(), any(), eq("test_signature")) itReturns true
        val verifier = createVerifier()
        val config = createConfig(signature = "test_signature")

        verifier.verify(config) shouldEqual true
    }

    @Test
    fun `should verify using the public key from the cache for the provided Key Id`() {
        When calling stubSignatureVerifier.verify(eq("test_public_key"), any(), any()) itReturns true
        When calling mockCache["test_key_id"] itReturns "test_public_key"
        val verifier = createVerifier()
        val config = createConfig(keyId = "test_key_id")

        verifier.verify(config) shouldEqual true
    }

    @Test
    fun `should verify using the values from provided Config`() {
        val data = Json.stringify(
            (StringSerializer to StringSerializer).map,
            hashMapOf("test_key" to "test_value")
        )
        When calling stubSignatureVerifier.verify(
            any(),
            eq(data.toByteArray(Charsets.UTF_8).inputStream()),
            any()
        ) itReturns true
        val verifier = createVerifier()
        val config = createConfig(values = data)

        verifier.verify(config) shouldEqual true
    }

    @Test
    fun `should return false when verification fails`() {
        When calling stubSignatureVerifier.verify(any(), any(), any()) itReturns false
        val verifier = createVerifier()
        val config = createConfig()

        verifier.verify(config) shouldEqual false
    }

    @Test
    fun `should return false when public key is not cached`() {
        When calling mockCache["test_key_id"] itReturns null
        val verifier = createVerifier()
        val config = createConfig(keyId = "test_key_id")

        verifier.verify(config) shouldEqual false
    }

    @Test
    fun `should return false when the verifier throws an exception`() {
        When calling stubSignatureVerifier.verify(any(), any(), any()) doAnswer {
            throw Exception("")
        }
        val verifier = createVerifier()
        val config = createConfig()

        verifier.verify(config) shouldEqual false
    }

    @Test
    fun `should return false when the cache throws an exception`() {
        When calling mockCache[any()] doAnswer {
            throw Exception("")
        }
        val verifier = createVerifier()
        val config = createConfig()

        verifier.verify(config) shouldEqual false
    }

    @Test
    fun `should remove the public key from the cache when verification fails`() {
        When calling stubSignatureVerifier.verify(any(), any(), any()) itReturns false
        val verifier = createVerifier()
        val config = createConfig(keyId = "test_key_id")

        verifier.verify(config)

        Verify on mockCache that mockCache.remove("test_key_id") was called
    }

    @Test
    fun `should fetch the key if it is not cached`() = runBlockingTest {
        When calling mockCache.fetch("test_key_id") itReturns "test_fetched_key"
        When calling mockCache["test_key_id"] itReturns null
        val verifier = createVerifier()

        verifier.ensureFetchedKey("test_key_id") shouldEqual "test_fetched_key"
    }

    @Test
    fun `should return the cached key instead of fetching if it is already cached`() = runBlockingTest {
        When calling mockCache["test_key_id"] itReturns "test_cached_key"
        val verifier = createVerifier()

        verifier.ensureFetchedKey("test_key_id") shouldEqual "test_cached_key"
    }

    private fun createVerifier() = ConfigVerifier(mockCache, stubSignatureVerifier)

    private fun createConfig(
        values: String = "{}",
        body: Map<String, String> = emptyMap(),
        signature: String = "test_signature",
        keyId: String = "test_key_id"
    ) = Config(
        rawBody = values,
        body = body,
        signature = signature,
        keyId = keyId
    )
}
