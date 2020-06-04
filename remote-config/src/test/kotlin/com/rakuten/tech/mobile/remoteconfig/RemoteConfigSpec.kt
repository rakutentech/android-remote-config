package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.junit.Test

class RemoteConfigSpec {

    @Test
    fun `should initialize instance with RealRemoteConfig`() {
        RemoteConfig.init(mock())

        RemoteConfig.instance() shouldBeInstanceOf RealRemoteConfig::class
    }

    @Test
    fun `should return fallback string when not initialized`() {
        RemoteConfig.instance()
            .getString("test_key", "fallback_value") shouldEqual "fallback_value"
    }

    @Test
    fun `should return fallback boolean when not initialized`() {
        RemoteConfig.instance()
            .getBoolean("test_key", true) shouldEqual true
    }

    @Test
    fun `should return fallback number when not initialized`() {
        RemoteConfig.instance()
            .getNumber("test_key", 10) shouldEqual 10
    }

    @Test
    fun `should return empty map when not initialized`() {
        RemoteConfig.instance()
            .getConfig() shouldEqual emptyMap()
    }

    @Test
    fun `should not call listener when not initialized`() = runBlockingTest {
        RemoteConfig.instance().fetchAndApplyConfig().shouldBeEmpty()
    }
}
