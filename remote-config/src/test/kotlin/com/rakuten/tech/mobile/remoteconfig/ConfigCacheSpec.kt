package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.api.ConfigFetcher
import com.rakuten.tech.mobile.remoteconfig.api.ConfigResponse
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.io.IOException

@Ignore
open class ConfigCacheSpec : RobolectricBaseSpec() {

    internal val context = ApplicationProvider.getApplicationContext<Context>().applicationContext
    internal val stubFetcher: ConfigFetcher = mock()
    internal val stubPoller: AsyncPoller = mock()
    internal val stubVerifier: ConfigVerifier = mock()

    internal fun `create cache with fetched config`(
        configValues: Map<String, String> = hashMapOf("testKey" to "test_value"),
        file: File = createFile("cache.json")
    ): ConfigCache {
        runBlockingTest {
            When calling stubFetcher.fetch() itReturns createConfig(configValues)
        }
        When calling stubPoller.start(any()) itAnswers {
            runBlockingTest { (it.arguments[0] as suspend () -> Unit).invoke() }
        }
        When calling stubVerifier.verify(any()) itReturns true

        return ConfigCache(stubFetcher, file, stubPoller, stubVerifier, false)
    }

    internal fun createConfig(values: Map<String, String>): Config {
        val body = jsonAdapter<ConfigResponse>()
                .toJson(ConfigResponse(values, "test_key_id"))

        return Config(
                rawBody = body,
                signature = "test_signature",
                keyId = "test_key_id"
        )
    }

    internal fun createFile(name: String) = File(context.filesDir, name)
}

class ConfigCacheGetConfigSpec : ConfigCacheSpec() {

    @Test
    fun `should be empty by default`() {
        val cache = ConfigCache(context, stubFetcher, stubVerifier, stubPoller, false)

        cache.getConfig() shouldEqual emptyMap()
    }

    @Test
    fun `should not apply the cached config if it is blank`() {
        val file = createFile("cache.json")
        file.writeText("")
        val cache = `create cache with fetched config`(
                file = file,
                configValues = hashMapOf("foo" to "bar")
        )

        cache.getConfig() shouldEqual emptyMap()
    }

    @Test
    fun `should not apply the cached config if it is invalid`() {
        val file = createFile("cache.json")
        file.writeText("foo: bar")
        val cache = `create cache with fetched config`(
                file = file,
                configValues = hashMapOf("foo" to "bar")
        )

        cache.getConfig() shouldEqual emptyMap()
    }

    @Test
    fun `should not apply the cached config if it has missing keys`() {
        val file = createFile("cache.json")
        file.writeText("{foo: bar}")
        val cache = `create cache with fetched config`(
                file = file,
                configValues = hashMapOf("foo" to "bar")
        )

        cache.getConfig() shouldEqual emptyMap()
    }
}

class ConfigCacheApplyConfigSpec : ConfigCacheSpec() {

    @Test
    fun `should apply the fetched config the next time App is launched`() {
        `create cache with fetched config`(
                configValues = hashMapOf("foo" to "bar")
        )

        val cache = `create cache with fetched config`()

        cache["foo"] shouldEqual "bar"
    }

    @Test
    fun `should not apply the fetched config while the App is running`() {
        `create cache with fetched config`(
                configValues = hashMapOf("foo" to "bar")
        )

        val cache = `create cache with fetched config`(
                configValues = hashMapOf("foo" to "new_bar")
        )

        cache["foo"] shouldNotEqual "new_bar"
    }

    @Test
    fun `should apply the cached config when fetching fails`() {
        val fileName = "cache.json"
        `create cache with fetched config`(
                file = createFile(fileName),
                configValues = hashMapOf("foo" to "bar")
        )

        runBlockingTest {
            When calling stubFetcher.fetch() doAnswer {
                throw IOException("Failed.")
            }
        }

        val cache = ConfigCache(stubFetcher, createFile(fileName), stubPoller, stubVerifier, false)

        cache["foo"] shouldEqual "bar"
    }

    @Test
    fun `should not cache the fetched config when verification fails`() {
        val fileName = "cache.json"
        runBlockingTest {
            When calling stubFetcher.fetch() itReturns createConfig(hashMapOf("foo" to "bar"))
        }
        When calling stubVerifier.verify(any()) itReturns false
        ConfigCache(stubFetcher, createFile(fileName), stubPoller, stubVerifier, false)

        val cache = ConfigCache(stubFetcher, createFile(fileName), stubPoller, stubVerifier, false)

        cache["foo"] shouldEqual null
    }

    @Test
    fun `should not apply the cached config when verification fails`() {
        val filename = "cache.json"
        `create cache with fetched config`(
                configValues = hashMapOf("foo" to "bar"),
                file = createFile(filename)
        )

        When calling stubVerifier.verify(any()) itReturns false

        val cache = ConfigCache(stubFetcher, createFile(filename), stubPoller, stubVerifier, false)

        cache["foo"] shouldEqual null
    }

    @Test
    fun `should not apply after fetching`() = runBlockingTest {
        val filename = "cache.json"

        When calling stubFetcher.fetch() itReturns createConfig(hashMapOf("foo" to "bar"))
        When calling stubVerifier.verify(any()) itReturns true
        When calling stubPoller.start(any()) itAnswers {
            GlobalScope.launch { (it.arguments[0] as suspend () -> Unit).invoke() }
        }

        val cache = ConfigCache(stubFetcher, createFile(filename), stubPoller, stubVerifier, false)
        advanceTimeBy(1000)
        cache.getConfig().shouldBeEmpty()
    }

    @Test
    fun `should apply after fetching`() {
        val filename = "cache.json"

        runBlockingTest {
            When calling stubFetcher.fetch() itReturns createConfig(hashMapOf("foo" to "bar"))
        }
        When calling stubVerifier.verify(any()) itReturns true
        When calling stubPoller.start(any()) itAnswers {
            runBlockingTest { (it.arguments[0] as suspend () -> Unit).invoke() }
        }

        val cache = ConfigCache(stubFetcher, createFile(filename), stubPoller, stubVerifier, true)
        cache.getConfig().shouldNotBeEmpty()
        cache["foo"] shouldEqual "bar"
    }
}

class ConfigCacheFetchConfigSpec : ConfigCacheSpec() {

    @Test
    fun `should fetch and apply directly with manual trigger`() = runBlockingTest {
        When calling stubFetcher.fetch() itReturns createConfig(hashMapOf("foo" to "bar"))
        When calling stubVerifier.verify(any()) itReturns true
        When calling stubPoller.start(any()) itAnswers { GlobalScope.launch {
                delay(1000)
                (it.arguments[0] as () -> Unit).invoke() } }

        val cache = ConfigCache(stubFetcher, createFile("cache.json"), stubPoller, stubVerifier, true,
                TestCoroutineDispatcher())
        // should not yet be applied because of the delay
        cache.getConfig().shouldBeEmpty()

        try {
            val config = cache.fetchAndApplyConfig()
            cache.getConfig().shouldNotBeEmpty()
            cache["foo"] shouldEqual "bar"
        } catch (ex: Exception) {
            ex.printStackTrace()
            Assert.fail()
        }
    }

    @Test
    @Suppress("TooGenericExceptionThrown")
    fun `should throw exception with manual trigger`() = runBlockingTest {
        When calling stubFetcher.fetch() itAnswers { throw Exception("fetch error") }
        When calling stubVerifier.verify(any()) itReturns true
        When calling stubPoller.start(any()) itAnswers { GlobalScope.launch {
                delay(1000)
                (it.arguments[0] as () -> Unit).invoke() } }

        val cache = ConfigCache(stubFetcher, createFile("cache.json"), stubPoller, stubVerifier, true,
                TestCoroutineDispatcher())
        // should not yet be applied because of the delay
        cache.getConfig().shouldBeEmpty()

        try {
            cache.fetchAndApplyConfig()
            Assert.fail()
        } catch (ex: Exception) {
            ex.printStackTrace()
            cache.getConfig().shouldBeEmpty()
        }
    }
}
