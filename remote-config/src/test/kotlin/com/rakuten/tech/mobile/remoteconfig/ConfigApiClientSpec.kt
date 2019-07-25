package com.rakuten.tech.mobile.remoteconfig

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldStartWith
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.util.logging.Level
import java.util.logging.LogManager

class ConfigApiClientSpec : RobolectricBaseSpec() {
    private val server = MockWebServer()
    private lateinit var baseUrl: String

    private val context = RuntimeEnvironment.application

    init {
        LogManager.getLogManager()
            .getLogger(MockWebServer::class.java.name).level = Level.OFF
    }

    @Before
    fun setup() {
        server.start()
        baseUrl = server.url("client").toString()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `should fetch the response`() {
        val client = createClient()
        enqueueResponse("test_body")

        client.fetchPath("test-path").body()!!.string() shouldEqual "test_body"
    }

    @Test
    fun `should fetch from the base url`() {
        val client = createClient()
        enqueueResponse()

        client.fetchPath("test-path")

        server.takeRequest().requestUrl.toString() shouldStartWith baseUrl
    }

    @Test
    fun `should fetch using the provided path`() {
        val client = createClient()
        enqueueResponse()

        client.fetchPath("test/path/to/fetch")

        server.takeRequest().requestUrl.toString() shouldContain "/test/path/to/fetch"
    }

    @Test
    fun `should attach ETag value to subsequent requests as If-None-Match`() {
        val client = createClient()
        enqueueResponse(etag = "etag_value")
        enqueueResponse()

        client.fetchPath("test-path").body()!!.string()
        server.takeRequest()
        client.fetchPath("test-path").body()!!.string()

        server.takeRequest().headers.get("If-None-Match") shouldEqual "etag_value"
    }

    @Test
    fun `should return cached body for 304 response code`() {
        val client = createClient()
        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))

        client.fetchPath("test-path").body()!!.string()

        client.fetchPath("test-path").body()!!.string() shouldEqual "test-body"
    }

    @Test
    fun `should cache the body between App launches`() {
        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))

        createClient().fetchPath("test-path").body()!!.string()

        createClient().fetchPath("test-path").body()!!.string() shouldEqual "test-body"
    }

    @Test(expected = Exception::class)
    fun `should throw when an invalid base url is provided`() {
        createClient(url = "invalid url")
    }

    private fun enqueueResponse(
        body: String = "test_body",
        etag: String = "etag_value"
    ) {
        server.enqueue(
            MockResponse()
                .setBody(body)
                .setHeader("ETag", etag)
        )
    }

    private fun createClient(
        url: String = baseUrl,
        appId: String = "test_app_id",
        subscriptionKey: String = "test_subscription_key"
    ) = ConfigApiClient(
        baseUrl = url,
        appId = appId,
        subscriptionKey = subscriptionKey,
        context = context
    )
}
