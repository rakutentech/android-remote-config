package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.api.ConfigFetcher
import com.rakuten.tech.mobile.remoteconfig.api.ConfigResponse
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File

@Ignore
open class RealRemoteConfigSpec : RobolectricBaseSpec() {

    private val stubCache: ConfigCache = mock()

    @Suppress("TooGenericExceptionThrown")
    internal fun createRemoteConfig(
        vararg keyValues: Pair<String, String>
    ): RealRemoteConfig {
        for (entry in keyValues.asList()) {
            When calling stubCache[entry.first] itReturns entry.second
        }

        When calling stubCache.getConfig() itReturns hashMapOf(*keyValues)

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
    fun `should fetch config`() = runBlockingTest {
        val remoteConfig = createRemoteConfigMockedPoller()
        remoteConfig.getConfig().shouldBeEmpty()

        try {
            val config = remoteConfig.fetchAndApplyConfig()
            config["test_key"] shouldEqual "test_value"
            config["another_test_key"] shouldEqual "another_test_value"
            System.out.println("test")
        } catch (ex: Exception) {
            ex.printStackTrace()
            Assert.fail()
        }
    }

    @Test
    fun `should return error when fetching config`() = runBlockingTest {
        val remoteConfig = createRemoteConfigMockedPoller(isError = true)
        remoteConfig.getConfig().shouldBeEmpty() // config is not yet applied.
        try {
            remoteConfig.fetchAndApplyConfig()
            Assert.fail()
        } catch (ex: Exception) {
            ex.message shouldEqual "fetch error"
        }
    }

    @Suppress("TooGenericExceptionThrown", "LongMethod")
    private suspend fun createRemoteConfigMockedPoller(
        configValues: Map<String, String> =
                        hashMapOf("test_key" to "test_value", "another_test_key" to "another_test_value"),
        file: File = File(ApplicationProvider.getApplicationContext<Context>().applicationContext.filesDir,
                "cache.json"),
        isError: Boolean = false
    ): RealRemoteConfig {
        val stubFetcher: ConfigFetcher = mock()
        val stubPoller: AsyncPoller = mock()
        val stubVerifier: ConfigVerifier = mock()

        When calling stubPoller.start(any()) itAnswers {
            GlobalScope.launch { (it.arguments[0] as suspend () -> Unit).invoke() }
        }
        When calling stubVerifier.verify(any()) itReturns true

        if (isError) {
            When calling stubFetcher.fetch() itAnswers { throw Exception("fetch error") }
        } else {
            When calling stubFetcher.fetch() itReturns createConfig(configValues)
        }

        return RealRemoteConfig(ConfigCache(stubFetcher, file, stubPoller, stubVerifier, false,
                TestCoroutineDispatcher()))
    }

    private fun createConfig(values: Map<String, String>): Config {
        val body = jsonAdapter<ConfigResponse>()
                .toJson(ConfigResponse(values, "test_key_id"))

        return Config(
                rawBody = body,
                signature = "test_signature",
                keyId = "test_key_id"
        )
    }
}
