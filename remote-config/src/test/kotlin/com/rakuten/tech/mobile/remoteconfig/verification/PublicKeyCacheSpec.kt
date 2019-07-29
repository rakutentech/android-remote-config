package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
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

class PublicKeyCacheSpec : RobolectricBaseSpec() {

    private val stubFetcher: PublicKeyFetcher = mock()

    @Before
    fun setup() {
        When calling stubFetcher.fetch("test_key_id") itReturns "test_public_key"
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

    private fun createCache(
        fetcher: PublicKeyFetcher = stubFetcher,
        context: Context = RuntimeEnvironment.application
    ) = PublicKeyCache(fetcher, context)
}
