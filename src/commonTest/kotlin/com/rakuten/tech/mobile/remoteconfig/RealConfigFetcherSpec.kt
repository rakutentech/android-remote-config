package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.headersOf
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test


@Ignore
open class ConfigFetcherSpec {

    lateinit var baseUrl: String

    val client = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                when (url.fullUrl) {
                    "https://example.org/" -> {
                        val responseHeaders = headersOf("Content-Type" to listOf(ContentType.Text.Plain.toString()))
                        respond("Hello, world", headers = responseHeaders)
                    }
                    else -> error("Unhandled ${request.url.fullUrl}")
                }
            }
        }
    }


    @BeforeTest
    fun setup() {
        server.start()
        baseUrl = server.url("config").toString()
    }

    @AfterTest
    fun teardown() {
        server.shutdown()
    }

    internal fun enqueueResponseValues(
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

    internal fun createFetcher(
        url: String = baseUrl,
        appId: String = "test_app_id",
        subscriptionKey: String = "test_subscription_key"
    ) = ConfigRequest(
        baseUrl = url,
        appId = appId,
        subscriptionKey = subscriptionKey
    )
}

class ConfigFetcherNormalSpec : ConfigFetcherSpec() {
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

        createFetcher()
            .fetch()

        createFetcher()
            .fetch()["foo"] shouldEqual "bar"
    }
}

class ConfigFetcherErrorSpec : ConfigFetcherSpec() {
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
}
