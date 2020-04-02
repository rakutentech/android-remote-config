package com.rakuten.tech.mobile.remoteconfig.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import com.rakuten.tech.mobile.sdkutils.RasSdkHeaders
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.logging.Level
import java.util.logging.LogManager

class ConfigApiClientSpec : RobolectricBaseSpec() {

    private val server = MockWebServer()
    private val mockRasHeaders: RasSdkHeaders = mock()
    private val mockContext: Context = mock()
    private lateinit var baseUrl: String

    init {
        LogManager.getLogManager()
            .getLogger(MockWebServer::class.java.name).level = Level.OFF
    }

    @Before
    fun setup() {
        server.start()
        baseUrl = server.url("client").toString()

        When calling mockRasHeaders.asArray() itReturns emptyArray()
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `should fetch the response`() {
        val client = createClient()
        enqueueResponse("test_body")

        client.fetchPath("test-path", null).body()!!.string() shouldEqual "test_body"
    }

    @Test
    fun `should fetch from the base url`() {
        val client = createClient()
        enqueueResponse()

        client.fetchPath("test-path", null)

        server.takeRequest().requestUrl.toString() shouldStartWith baseUrl
    }

    @Test
    fun `should fetch using the provided path`() {
        val client = createClient()
        enqueueResponse()

        client.fetchPath("test/path/to/fetch", null)

        server.takeRequest().requestUrl.toString() shouldContain "/test/path/to/fetch"
    }

    @Test
    fun `should attach the RAS headers to requests`() {
        enqueueResponse()
        When calling mockRasHeaders.asArray() itReturns arrayOf("ras_header_name" to "ras_header_value")

        createClient(headers = mockRasHeaders).fetchPath("/", null)

        server.takeRequest().getHeader("ras_header_name") shouldEqual "ras_header_value"
    }

    @Test
    fun `should attach ETag value to subsequent requests as If-None-Match`() {
        val client = createClient()
        enqueueResponse(etag = "etag_value")
        enqueueResponse()

        client.fetchPath("test-path", null).body()!!.string()
        server.takeRequest()
        client.fetchPath("test-path", null).body()!!.string()

        server.takeRequest().headers.get("If-None-Match") shouldEqual "etag_value"
    }

    @Test
    fun `should return cached body for 304 response code`() {
        val client = createClient()
        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))

        client.fetchPath("test-path", null).body()!!.string()

        client.fetchPath("test-path", null).body()!!.string() shouldEqual "test-body"
    }

    @Test
    fun `should cache the body between App launches`() {
        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))

        createClient().fetchPath("test-path", null).body()!!.string()

        createClient().fetchPath("test-path", null).body()!!.string() shouldEqual "test-body"
    }

    @Test
    fun `should include all query parameter`() {
        val paramMap = HashMap<String, String>()
        paramMap["key1"] = "value1"
        paramMap["key2"] = "value2"
        val client = ConfigApiClient(baseUrl, mockContext, mockRasHeaders)

        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))
        val response = client.fetchPath("test-path", paramMap)

        for ((k, v) in paramMap) response.request().url().queryParameter(k) shouldEqual v
    }

    @Test
    fun `should include have empty value query param`() {
        val paramMap = HashMap<String, String>()
        paramMap["key1"] = "value1"
        paramMap["key2"] = ""
        val client = ConfigApiClient(baseUrl, mockContext, mockRasHeaders)

        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))
        val response = client.fetchPath("test-path", paramMap)

        response.request().url().queryParameterNames().size shouldEqual 1
        response.request().url().queryParameter("key1") shouldEqual "value1"
        response.request().url().queryParameter("key2").shouldBeNull()
    }

    @Test
    fun `should include have empty key query param`() {
        val paramMap = HashMap<String, String>()
        paramMap["key1"] = "value1"
        paramMap[""] = "value2"
        val client = ConfigApiClient(baseUrl, mockContext, mockRasHeaders)

        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))
        val response = client.fetchPath("test-path", paramMap)

        response.request().url().queryParameterNames().size shouldEqual 1
        response.request().url().queryParameter("key1") shouldEqual "value1"
        response.request().url().queryParameter("").shouldBeNull()
    }

    @Test
    fun `should include have empty key value query param`() {
        val paramMap = HashMap<String, String>()
        paramMap["key1"] = "value1"
        paramMap[""] = ""
        val client = ConfigApiClient(baseUrl, mockContext, mockRasHeaders)

        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))
        val response = client.fetchPath("test-path", paramMap)

        response.request().url().queryParameterNames().size shouldEqual 1
        response.request().url().queryParameter("key1") shouldEqual "value1"
        response.request().url().queryParameter("").shouldBeNull()
    }

    @Test
    fun `should not include empty value`() {
        val paramMap = HashMap<String, String>()
        val client = ConfigApiClient(baseUrl, mockContext, mockRasHeaders)

        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))
        val response = client.fetchPath("test-path", paramMap)

        response.request().url().queryParameterNames().shouldBeEmpty()
    }

    @Test(expected = Exception::class)
    fun `should throw when an invalid base url is provided`() {
        createClient(url = "invalid url")
    }

    private fun enqueueResponse(body: String = "test_body", etag: String = "etag_value") {
        server.enqueue(
            MockResponse()
                .setBody(body)
                .setHeader("ETag", etag)
        )
    }

    private fun createClient(url: String = baseUrl, headers: RasSdkHeaders = mockRasHeaders) =
            ConfigApiClient(
                    baseUrl = url,
                    context = ApplicationProvider.getApplicationContext(),
                    headers = headers
            )
}
