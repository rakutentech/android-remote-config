package com.rakuten.tech.mobile.remoteconfig

import junit.framework.TestCase
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEndWith
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldStartWith
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.LogManager

class ConfigFetcherSpec : RobolectricBaseSpec() {

    private val server = MockWebServer()
    private val context = RuntimeEnvironment.application
    private lateinit var baseUrl: String

    init {
        LogManager.getLogManager()
            .getLogger(MockWebServer::class.java.name).level = Level.OFF
    }

    @Before
    fun setup() {
        server.start()
        baseUrl = server.url("config").toString()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `should fetch the config`() {
        enqueueResponseValues(hashMapOf("foo" to "bar"))

        val fetcher = createFetcher()

        fetcher.fetch()["foo"] shouldEqual "bar"
    }

    @Test
    fun `should fetch the config from the provided url`() {
        enqueueResponseValues()

        val fetcher = createFetcher(url = baseUrl)

        fetcher.fetch()

        server.takeRequest().requestUrl.toString() shouldStartWith baseUrl
    }

    @Test
    fun `should fetch the config for the provided App Id`() {
        enqueueResponseValues()

        val fetcher = createFetcher(appId = "test-app-id")

        fetcher.fetch()

        server.takeRequest().path shouldContain "/app/test-app-id"
    }

    @Test
    fun `should fetch the config from the 'config' endpoint`() {
        enqueueResponseValues()

        val fetcher = createFetcher()

        fetcher.fetch()

        server.takeRequest().path shouldEndWith "/config"
    }

    @Test
    fun `should fetch the config using the provided Subscription Key prepended with 'ras-'`() {
        enqueueResponseValues()

        val fetcher = createFetcher(subscriptionKey = "test_subscription_key")

        fetcher.fetch()

        server.takeRequest().headers["apiKey"] shouldEqual "ras-test_subscription_key"
    }

    @Test
    fun `should return cached config for 304 response code`() {
        val fetcher = createFetcher()
        enqueueResponseValues(hashMapOf("foo" to "bar"))
        server.enqueue(MockResponse().setResponseCode(304))

        fetcher.fetch()

        fetcher.fetch()["foo"] shouldEqual "bar"
    }

    @Test
    fun `should cache the config between App launches`() {
        enqueueResponseValues(hashMapOf("foo" to "bar"))
        server.enqueue(MockResponse().setResponseCode(304))

        createFetcher(cacheDirectory = context.cacheDir)
            .fetch()

        createFetcher(cacheDirectory = context.cacheDir)
            .fetch()["foo"] shouldEqual "bar"
    }

    @Test(expected = Exception::class)
    fun `should throw when an invalid base url is provided`() {
        enqueueResponseValues()

        createFetcher(url = "invalid url")
    }

    @Test(expected = IOException::class)
    fun `should throw when the request is unsuccessful`() {
        server.enqueue(
            MockResponse().setResponseCode(400)
        )

        val fetcher = createFetcher()

        fetcher.fetch()
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'body' key is missing in response`() {
        server.enqueue(
            MockResponse().setBody("""{"randomKey":"random_value"}""")
        )

        val fetcher = createFetcher()

        fetcher.fetch()
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun `should not throw when there are extra keys in the response`() {
        server.enqueue(
            MockResponse().setBody("""
                {
                    "body": {},
                    "randomKey": "random_value"
                }
            """.trimIndent())
        )

        val fetcher = createFetcher()

        try {
            fetcher.fetch()
        } catch (e: Exception) {
            TestCase.fail("Should not throw an exception.")
            throw e
        }
    }

    private fun enqueueResponseValues(
        values: Map<String, String> = hashMapOf("foo" to "bar")
    ) {
        server.enqueue(
            MockResponse()
                .setBody("""
                {
                    "body": ${Json.nonstrict.stringify(
                        (StringSerializer to StringSerializer).map, values
                    )}
                }
            """.trimIndent())
                .setHeader("etag", "test")
        )
    }

    private fun createFetcher(
        url: String = baseUrl,
        appId: String = "test_app_id",
        subscriptionKey: String = "test_subscription_key",
        cacheDirectory: File = context.cacheDir
    ) = ConfigFetcher(
        baseUrl = url,
        appId = appId,
        subscriptionKey = subscriptionKey,
        cacheDirectory = cacheDirectory
    )
}
