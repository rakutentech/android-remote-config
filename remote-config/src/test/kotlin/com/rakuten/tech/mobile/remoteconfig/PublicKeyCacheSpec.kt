package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.When
import org.amshove.kluent.calling
import org.amshove.kluent.itReturns
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class PublicKeyCacheSpec : RobolectricBaseSpec() {

    private val stubFetcher: PublicKeyFetcher = mock()
    private val context = RuntimeEnvironment.application

    @Before
    fun setup() {
        When calling stubFetcher.fetch("test_key_id") itReturns "test_public_key"
    }

    @Test
    fun `should fetch the public key by key id`() {
        val cache = PublicKeyCache(stubFetcher, context)

        cache.fetch("test_key_id") shouldEqual "test_public_key"
    }

    @Test
    fun `should cache the public key after it has been fetched`() {
        val cache = PublicKeyCache(stubFetcher, context)

        cache.fetch("test_key_id")

        cache["test_key_id"] shouldEqual "test_public_key"
    }

    @Test
    fun `should return 'null' for a key id that is not cached`() {
        val cache = PublicKeyCache(stubFetcher, context)

        cache["random_key"] shouldEqual null
    }
}
