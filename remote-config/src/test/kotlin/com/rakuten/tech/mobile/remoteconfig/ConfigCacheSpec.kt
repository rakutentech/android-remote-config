package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.api.ConfigFetcher
import com.rakuten.tech.mobile.remoteconfig.api.ConfigResponse
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import kotlinx.serialization.json.Json
import org.amshove.kluent.*
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.IOException

class ConfigCacheSpec : RobolectricBaseSpec() {

    private val context = RuntimeEnvironment.application.applicationContext
    private val stubFetcher: ConfigFetcher = mock()
    private val stubPoller: AsyncPoller = mock()
    private val stubVerifier: ConfigVerifier = mock()

    @Test
    fun `should be empty by default`() {
        val cache = ConfigCache(context, stubFetcher, stubVerifier, stubPoller)

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

        When calling stubFetcher.fetch() doAnswer {
            throw IOException("Failed.")
        }

        val cache = ConfigCache(stubFetcher, createFile(fileName), stubPoller, stubVerifier)

        cache["foo"] shouldEqual "bar"
    }

    @Test
    fun `should not cache the fetched config when verification fails`() {
        val fileName = "cache.json"
        When calling stubFetcher.fetch() itReturns createConfig(hashMapOf("foo" to "bar"))
        When calling stubVerifier.verify(any()) itReturns false
        ConfigCache(stubFetcher, createFile(fileName), stubPoller, stubVerifier)

        val cache = ConfigCache(stubFetcher, createFile(fileName), stubPoller, stubVerifier)

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

        val cache = ConfigCache(stubFetcher, createFile(filename), stubPoller, stubVerifier)

        cache["foo"] shouldEqual null
    }

    private fun `create cache with fetched config`(
        configValues: Map<String, String> = hashMapOf("testKey" to "test_value"),
        file: File = createFile("cache.json")
    ): ConfigCache {
        When calling stubFetcher.fetch() itReturns createConfig(configValues)
        When calling stubPoller.start(any()) itAnswers {
            (it.arguments[0] as () -> Unit).invoke()
        }
        When calling stubVerifier.verify(any()) itReturns true

        return ConfigCache(stubFetcher, file, stubPoller, stubVerifier)
    }

    private fun createConfig(values: Map<String, String>) = Config(
        Json.stringify(
            ConfigResponse.serializer(),
            ConfigResponse(values, "test_key_id")
        ),
        "test_signature",
        "test_key_id"
    )

    private fun createFile(name: String) = File(context.filesDir, name)
}
