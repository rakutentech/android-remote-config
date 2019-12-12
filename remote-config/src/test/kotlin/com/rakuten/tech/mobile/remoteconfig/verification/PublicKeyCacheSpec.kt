package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.ConfigApiClient
import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.When
import org.amshove.kluent.calling
import org.amshove.kluent.itReturns
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import java.io.File

class PublicKeyCacheSpec : RobolectricBaseSpec() {

    private val stubApiClient: ConfigApiClient = mock()
    private val stubEncryptor: Encryptor = mock()

    @Before
    fun setup() {
        When calling stubApiClient.fetchPublicKey(any(), any(), any()) doAnswer {
            val success = it.arguments[1] as (String) -> Unit
            success("test_public_key")
        }
        When calling stubEncryptor.encrypt("test_public_key") itReturns "test_public_key"
        When calling stubEncryptor.decrypt("test_public_key") itReturns "test_public_key"
    }

    @Test
    fun `should fetch the public key by key id`() = runBlockingTest {
        val cache = createCache()

        cache.fetch("test_key_id") shouldEqual "test_public_key"
    }

    @Test
    fun `should return 'null' for a key id that is not cached`() {
        val cache = createCache()

        cache["random_key"] shouldEqual null
    }

    @Test
    fun `should fetch the public key`() = runBlockingTest {
        val cache = createCache()

        cache.fetch("test_key_id") shouldEqual "test_public_key"
    }

    @Test
    fun `should cache the public key after it has been fetched`() = runBlockingTest {
        val cache = createCache()

        cache.fetch("test_key_id")

        cache["test_key_id"] shouldEqual "test_public_key"
    }

    @Test
    fun `should be empty when file is empty`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val file = File(context.cacheDir, "keys.json")
        file.writeText(" ")

        val cache = createCache(file = file)

        cache["random_key"] shouldEqual null
    }

    @Test
    fun `should cache the public key between App launches`() = runBlockingTest{
        val cache = createCache()

        cache.fetch("test_key_id")

        val secondCache = createCache()

        secondCache["test_key_id"] shouldEqual "test_public_key"
    }

    @Test
    fun `should cache the public key in an encrypted form after fetching`() = runBlockingTest {
        When calling stubEncryptor.encrypt(any()) itReturns "encrypted_publicKey"
        When calling stubEncryptor.decrypt("encrypted_publicKey") itReturns "decrypted_public_key"
        val cache = createCache()

        cache.fetch("test_key_id")

        cache["test_key_id"] shouldEqual "decrypted_public_key"
    }

    @Test
    fun `should remove the public key from the cache`() = runBlockingTest {
        val cache = createCache()

        cache.fetch("test_key_id")
        cache.remove("test_key_id")

        cache["test_key_id"] shouldEqual null
    }

    private fun createCache(
        client: ConfigApiClient = stubApiClient,
        context: Context = ApplicationProvider.getApplicationContext(),
        file: File = File(context.cacheDir, "keys.json"),
        encryptor: Encryptor = stubEncryptor
    ) = PublicKeyCache(client, file, encryptor)
}
