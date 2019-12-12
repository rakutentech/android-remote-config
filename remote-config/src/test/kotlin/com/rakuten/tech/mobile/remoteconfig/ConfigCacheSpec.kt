package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
import org.amshove.kluent.*
import org.junit.Test
import java.io.File

class ConfigCacheSpec : RobolectricBaseSpec() {

    private val context = ApplicationProvider.getApplicationContext<Context>().applicationContext
    private val stubApiClient: ConfigApiClient = mock()
    private val stubPoller: AsyncPoller = mock()
    private val stubVerifier: ConfigVerifier = mock()

    @Test
    fun `should be empty by default`() {
        val cache = ConfigCache(context, stubApiClient, stubVerifier, stubPoller)

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

        When calling stubApiClient.fetchConfig(any(), any()) doAnswer {
            val error = it.arguments[1] as (Exception) -> Unit
            error(Exception(""))
        }

        val cache = ConfigCache(stubApiClient, createFile(fileName), stubPoller, stubVerifier)

        cache["foo"] shouldEqual "bar"
    }

    @Test
    fun `should not cache the fetched config when verification fails`() {
        val fileName = "cache.json"
        When calling stubApiClient.fetchConfig(any(), any()) doAnswer {
            val success = it.arguments[0] as (Config) -> Unit
            success(createConfig(hashMapOf("foo" to "bar")))

        }
        When calling stubVerifier.verify(any()) itReturns false
        ConfigCache(stubApiClient, createFile(fileName), stubPoller, stubVerifier)

        val cache = ConfigCache(stubApiClient, createFile(fileName), stubPoller, stubVerifier)

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

        val cache = ConfigCache(stubApiClient, createFile(filename), stubPoller, stubVerifier)

        cache["foo"] shouldEqual null
    }

    private fun `create cache with fetched config`(
        configValues: Map<String, String> = hashMapOf("testKey" to "test_value"),
        file: File = createFile("cache.json")
    ): ConfigCache {
        When calling stubApiClient.fetchConfig(any(), any()) doAnswer {
            val success = it.arguments[0] as (Config) -> Unit
            success(createConfig(configValues))

        }
        When calling stubPoller.start(any()) itAnswers {
            val poll = it.arguments[0] as suspend () -> Unit
            runBlocking {
                poll()
            }
        }
        When calling stubVerifier.verify(any()) itReturns true

        return ConfigCache(stubApiClient, file, stubPoller, stubVerifier)
    }

    private fun createConfig(values: Map<String, String>) = Config(
        rawBody = """
            {
                body:${ Json.stringify(
                    (StringSerializer to StringSerializer).map,
                    values
                ) },
                keyId:"test_key_id"
            }
        """.trimIndent(),
        body = values,
        signature = "test_signature",
        keyId = "test_key_id"
    )

    private fun createFile(name: String) = File(context.filesDir, name)
}
