package com.rakuten.tech.mobile.remoteconfig

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.*
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.IOException

class ConfigCacheSpec : RobolectricBaseSpec() {

    private val context = RuntimeEnvironment.application.applicationContext
    private val stubFetcher: ConfigFetcher = mock()
    private val stubPoller: AsyncPoller = mock()

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

        When calling stubFetcher.fetch() doAnswer {
            throw IOException("Failed.")
        }

        val cache = ConfigCache(stubFetcher, createFile(fileName), stubPoller)

        cache["foo"] shouldEqual "bar"
    }

    private fun `create cache with fetched config`(
        configValues: Map<String, String> = hashMapOf("testKey" to "test_value"),
        fileName: String = "cache.json"
    ): ConfigCache {
        When calling stubFetcher.fetch() itReturns configValues
        When calling stubPoller.start(any()) itAnswers {
            (it.arguments[0] as () -> Any).invoke()
        }

        return ConfigCache(stubFetcher, createFile(fileName), stubPoller)
    }

    private fun createFile(name: String) = File(context.filesDir, name)
}
