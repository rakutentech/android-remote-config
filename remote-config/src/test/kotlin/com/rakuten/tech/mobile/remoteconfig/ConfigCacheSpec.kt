package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.*
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.IOException

class ConfigCacheSpec : RobolectricBaseSpec() {

    private val context = RuntimeEnvironment.application.applicationContext
    private val stubFetcher: ConfigFetcher = mock()

    @Test
    fun `should be empty by default`() {
        val cache = ConfigCache(context, stubFetcher)

        cache["foo"] shouldBe null
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
    fun `should use the cached config when fetching fails`() {
        val fileName = "cache.json"
        `create cache with fetched config`(
            fileName = fileName,
            configValues = hashMapOf("foo" to "bar")
        )

        var exceptionThrown = false
        When calling stubFetcher.fetch() doAnswer {
            exceptionThrown = true
            throw IOException("Failed.")
        }

        ConfigCache(stubFetcher, createFile(fileName))
        await until { exceptionThrown }

        val cache = ConfigCache(stubFetcher, createFile(fileName))

        cache["foo"] shouldEqual "bar"
    }

    private fun `create cache with fetched config`(
        configValues: Map<String, String> = hashMapOf("testKey" to "test_value"),
        fileName: String = "cache.json"
    ): ConfigCache {
        val file = File(context.filesDir, fileName)
        When calling stubFetcher.fetch() itReturns configValues

        val cache = ConfigCache(stubFetcher, createFile(fileName))

        await until { file.exists() && file.readText().isNotBlank() }

        return cache
    }

    private fun createFile(name: String) = File(context.filesDir, name)
}
