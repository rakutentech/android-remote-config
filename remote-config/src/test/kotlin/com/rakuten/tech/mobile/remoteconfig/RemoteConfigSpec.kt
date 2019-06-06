package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.mock
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
}