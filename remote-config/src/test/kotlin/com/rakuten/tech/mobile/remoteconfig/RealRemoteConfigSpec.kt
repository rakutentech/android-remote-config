package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

@Ignore
open class RealRemoteConfigSpec : RobolectricBaseSpec() {

    private val stubCache: ConfigCache = mock()

    internal fun createRemoteConfig(
        vararg keyValues: Pair<String, String>,
        isError: Boolean = false
    ): RealRemoteConfig {
        for (entry in keyValues.asList()) {
            When calling stubCache[entry.first] itReturns entry.second
        }

        When calling stubCache.getConfig() itReturns hashMapOf(*keyValues)
        if (isError) {
            When calling stubCache.fetchConfig(any()) itAnswers {
                (it.arguments[0] as FetchConfigCompletionListener).onFetchError(java.lang.Exception("fetch error"))
            }
        } else {
            When calling stubCache.fetchConfig(any()) itAnswers {
                (it.arguments[0] as FetchConfigCompletionListener).onFetchComplete(hashMapOf(*keyValues))
            }
        }

        return RealRemoteConfig(stubCache)
    }
}

class RealRemoteConfigGetStringSpec : RealRemoteConfigSpec() {
    @Test
    fun `should get value for key`() {
        val remoteConfig = createRemoteConfig("test_key" to "test_value")

        remoteConfig.getString("test_key", "fallback_value") shouldEqual "test_value"
    }

    @Test
    fun `should return the fallback for key that's not in cache`() {
        val remoteConfig = createRemoteConfig()

        remoteConfig.getString("random_key", "fallback_value") shouldEqual "fallback_value"
    }
}

class RealRemoteConfigGetBooleanSpec : RealRemoteConfigSpec() {
    @Test
    fun `should get value for key`() {
        val remoteConfig = createRemoteConfig("test_key" to "true")

        remoteConfig.getBoolean("test_key", false) shouldEqual true
    }

    @Test
    fun `should return the fallback string for key that's not in cache`() {
        val remoteConfig = createRemoteConfig()

        remoteConfig.getBoolean("random_key", true) shouldEqual true
    }
}

class RealRemoteConfigGetNumberSpec : RealRemoteConfigSpec() {
    @Test
    fun `should get Int value for key`() {
        val remoteConfig = createRemoteConfig("test_key" to "123")

        remoteConfig.getNumber("test_key", 10) shouldEqual 123
    }

    @Test
    fun `should get Long value for key`() {
        val remoteConfig = createRemoteConfig("test_key" to "123")

        remoteConfig.getNumber("test_key", 10L) shouldEqual 123L
    }

    @Test
    fun `should get Short value for key`() {
        val remoteConfig = createRemoteConfig("test_key" to "123")

        remoteConfig.getNumber("test_key", 10.toShort()) shouldEqual 123.toShort()
    }

    @Test
    fun `should get Double value for key`() {
        val remoteConfig = createRemoteConfig("test_key" to "123.123")

        remoteConfig.getNumber("test_key", 10.0) shouldEqual 123.123
    }

    @Test
    fun `should get Float value for key`() {
        val remoteConfig = createRemoteConfig("test_key" to "123.123")

        remoteConfig.getNumber("test_key", 10.0f) shouldEqual 123.123f
    }

    @Test
    fun `should get Byte value for key`() {
        val remoteConfig = createRemoteConfig("test_key" to "123")

        remoteConfig.getNumber("test_key", 10.toByte()) shouldEqual 123.toByte()
    }

    @Test
    fun `should return fallback when a number cannot be parsed from string value`() {
        val remoteConfig = createRemoteConfig("test_key" to "not a number")

        remoteConfig.getNumber("test_key", 10) shouldEqual 10
    }

    @Test
    fun `should return fallback when key doesn't exist`() {
        val remoteConfig = createRemoteConfig()

        remoteConfig.getNumber("random_key", 10) shouldEqual 10
    }
}

class RealRemoteConfigGetConfigSpec : RealRemoteConfigSpec() {
    @Test
    fun `should get the config`() {
        val remoteConfig = createRemoteConfig(
            "test_key" to "test_value",
            "another_test_key" to "another_test_value"
        )

        remoteConfig.getConfig()["test_key"] shouldEqual "test_value"
        remoteConfig.getConfig()["another_test_key"] shouldEqual "another_test_value"
    }
}

class RealRemoteConfigFetchConfigSpec : RealRemoteConfigSpec() {
    @Test
    fun `should fetch config`() {
        val remoteConfig = createRemoteConfig(
                "test_key" to "test_value",
                "another_test_key" to "another_test_value"
        )

        remoteConfig.fetchAndApplyConfig(object : FetchConfigCompletionListener {
            override fun onFetchError(ex: Exception) {
                Assert.fail()
            }

            override fun onFetchComplete(config: Map<String, String>) {
                config["test_key"] shouldEqual "test_value"
                config["another_test_key"] shouldEqual "another_test_value"
            }
        })
    }

    @Test
    fun `should return error when fetching config`() {
        val remoteConfig = createRemoteConfig(
                "test_key" to "test_value",
                "another_test_key" to "another_test_value",
                isError = true
        )

        remoteConfig.fetchAndApplyConfig(object : FetchConfigCompletionListener {
            override fun onFetchError(ex: Exception) {
                ex.message shouldEqual "fetch error"
            }

            override fun onFetchComplete(config: Map<String, String>) {
                Assert.fail()
            }
        })
    }
}
