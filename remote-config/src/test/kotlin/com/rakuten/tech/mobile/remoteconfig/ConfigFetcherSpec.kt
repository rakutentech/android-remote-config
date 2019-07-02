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
        val fetcher = createFetcher()
        enqueueResponseValues(hashMapOf("foo" to "bar"))

        fetcher.fetch()["foo"] shouldEqual "bar"
    }

    @Test
    fun `should fetch the config from the provided url`() {
        val fetcher = createFetcher(url = baseUrl)
        enqueueResponseValues()

        fetcher.fetch()

        server.takeRequest().requestUrl.toString() shouldStartWith baseUrl
    }

    @Test
    fun `should fetch the config for the provided App Id`() {
        val fetcher = createFetcher(appId = "test-app-id")
        enqueueResponseValues()

        fetcher.fetch()

        server.takeRequest().path shouldContain "/app/test-app-id"
    }

    @Test
    fun `should fetch the config from the 'config' endpoint`() {
        val fetcher = createFetcher()
        enqueueResponseValues()

        fetcher.fetch()

        server.takeRequest().path shouldEndWith "/config"
    }

    @Test
    fun `should fetch the config using the provided Subscription Key prepended with 'ras-'`() {
        val fetcher = createFetcher(subscriptionKey = "test_subscription_key")
        enqueueResponseValues()

        fetcher.fetch()

        server.takeRequest().headers["apiKey"] shouldEqual "ras-test_subscription_key"
    }

    @Test
    fun `should attach ETag value to subsequent requests as If-None-Match`() {
        val fetcher = createFetcher()
        enqueueResponseValues(etag = "etag_value")
        enqueueResponseValues()

        fetcher.fetch()
        server.takeRequest()

        fetcher.fetch()

        server.takeRequest().headers.get("If-None-Match") shouldEqual "etag_value"
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
        createFetcher(url = "invalid url")
    }

    @Test(expected = IOException::class)
    fun `should throw when the request is unsuccessful`() {
        val fetcher = createFetcher()
        server.enqueue(MockResponse().setResponseCode(400))

        fetcher.fetch()
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'body' key is missing in response`() {
        val fetcher = createFetcher()
        server.enqueue(
            MockResponse().setBody("""{"randomKey":"random_value"}""")
        )

        fetcher.fetch()
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun `should not throw when there are extra keys in the response`() {
        val fetcher = createFetcher()
        server.enqueue(
            MockResponse().setBody("""
                {
                    "body": {},
                    "randomKey": "random_value"
                }
            """.trimIndent())
        )

        try {
            fetcher.fetch()
        } catch (e: Exception) {
            TestCase.fail("Should not throw an exception.")
            throw e
        }
    }

    private fun enqueueResponseValues(
        values: Map<String, String> = hashMapOf("foo" to "bar"),
        etag: String = "etag_value"
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
                .setHeader("ETag", etag)
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
