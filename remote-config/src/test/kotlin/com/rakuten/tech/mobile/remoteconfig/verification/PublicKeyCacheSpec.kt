package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import com.rakuten.tech.mobile.remoteconfig.api.PublicKeyFetcher
import org.amshove.kluent.When
import org.amshove.kluent.calling
import org.amshove.kluent.itReturns
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File

class PublicKeyCacheSpec : RobolectricBaseSpec() {

    private val stubFetcher: PublicKeyFetcher = mock()
    private val stubEncryptor: Encryptor = mock()

    @Before
    fun setup() {
        When calling stubFetcher.fetch("test_key_id") itReturns "test_public_key"
        When calling stubEncryptor.encrypt("test_public_key") itReturns "test_public_key"
        When calling stubEncryptor.decrypt("test_public_key") itReturns "test_public_key"
    }

    @Test
    fun `should fetch the public key by key id`() {
        val cache = createCache()

        cache.fetch("test_key_id") shouldEqual "test_public_key"
    }

    @Test
    fun `should return 'null' for a key id that is not cached`() {
        val cache = createCache()

        cache["random_key"] shouldEqual null
    }

    @Test
    fun `should fetch the public key`() {
        val cache = createCache()

        cache.fetch("test_key_id") shouldEqual "test_public_key"
    }

    @Test
    fun `should cache the public key after it has been fetched`() {
        val cache = createCache()

        cache.fetch("test_key_id")

        cache["test_key_id"] shouldEqual "test_public_key"
    }

    @Test
    fun `should be empty when file is empty`() {
        val context = RuntimeEnvironment.application
        val file = File(context.cacheDir, "keys.json")
        file.writeText(" ")

        val cache = createCache(file = file)

        cache["random_key"] shouldEqual null
    }

    @Test
    fun `should cache the public key between App launches`() {
        val cache = createCache()

        cache.fetch("test_key_id")

        val secondCache = createCache()

        secondCache["test_key_id"] shouldEqual "test_public_key"
    }

    @Test
    fun `should cache the public key in an encrypted form after fetching`() {
        When calling stubEncryptor.encrypt(any()) itReturns "encrypted_publicKey"
        When calling stubEncryptor.decrypt("encrypted_publicKey") itReturns "decrypted_public_key"
        val cache = createCache()

        cache.fetch("test_key_id")

        cache["test_key_id"] shouldEqual "decrypted_public_key"
    }

    @Test
    fun `should remove the public key from the cache`() {
        val cache = createCache()

        cache.fetch("test_key_id")
        cache.remove("test_key_id")

        cache["test_key_id"] shouldEqual null
    }

    private fun createCache(
        fetcher: PublicKeyFetcher = stubFetcher,
        context: Context = RuntimeEnvironment.application,
        file: File = File(context.cacheDir, "keys.json"),
        encryptor: Encryptor = stubEncryptor
    ) = PublicKeyCache(fetcher, context, file, encryptor)
}
