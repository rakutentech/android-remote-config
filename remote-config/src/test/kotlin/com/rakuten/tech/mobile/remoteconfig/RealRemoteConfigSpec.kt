package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.When
import org.amshove.kluent.calling
import org.amshove.kluent.itReturns
import org.amshove.kluent.shouldEqual
import org.junit.Test

class RealRemoteConfigSpec {

    private val stubCache: ConfigCache = mock()

    @Test
    fun `should get string value for key`() {
        When calling stubCache["test_key"] itReturns "test_value"

        val remoteConfig = RealRemoteConfig(stubCache)

        remoteConfig.getString("test_key", "fallback_value") shouldEqual "test_value"
    }

    @Test
    fun `should return the fallback value for key that's not in cache`() {
        val remoteConfig = RealRemoteConfig(stubCache)

        remoteConfig.getString("random_key", "fallback_value") shouldEqual "fallback_value"
    }
}
