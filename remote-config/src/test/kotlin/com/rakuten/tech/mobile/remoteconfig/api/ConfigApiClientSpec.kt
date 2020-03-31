package com.rakuten.tech.mobile.remoteconfig.api

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
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
import java.util.*
import java.util.logging.Level
import java.util.logging.LogManager

class ConfigApiClientSpec : RobolectricBaseSpec() {

    private val server = MockWebServer()
    private val mockRasHeaders: RasSdkHeaders = mock()
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

        client.fetchPath("test-path", true).body()!!.string() shouldEqual "test_body"
    }

    @Test
    fun `should fetch from the base url`() {
        val client = createClient()
        enqueueResponse()

        client.fetchPath("test-path", true)

        server.takeRequest().requestUrl.toString() shouldStartWith baseUrl
    }

    @Test
    fun `should fetch using the provided path`() {
        val client = createClient()
        enqueueResponse()

        client.fetchPath("test/path/to/fetch", true)

        server.takeRequest().requestUrl.toString() shouldContain "/test/path/to/fetch"
    }

    @Test
    fun `should attach the RAS headers to requests`() {
        enqueueResponse()
        When calling mockRasHeaders.asArray() itReturns
            arrayOf("ras_header_name" to "ras_header_value")

        createClient(headers = mockRasHeaders)
            .fetchPath("/", true)

        server.takeRequest().getHeader("ras_header_name") shouldEqual "ras_header_value"
    }

    @Test
    fun `should attach ETag value to subsequent requests as If-None-Match`() {
        val client = createClient()
        enqueueResponse(etag = "etag_value")
        enqueueResponse()

        client.fetchPath("test-path", true).body()!!.string()
        server.takeRequest()
        client.fetchPath("test-path", true).body()!!.string()

        server.takeRequest().headers.get("If-None-Match") shouldEqual "etag_value"
    }

    @Test
    fun `should return cached body for 304 response code`() {
        val client = createClient()
        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))

        client.fetchPath("test-path", true).body()!!.string()

        client.fetchPath("test-path", true).body()!!.string() shouldEqual "test-body"
    }

    @Test
    fun `should cache the body between App launches`() {
        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))

        createClient().fetchPath("test-path", true).body()!!.string()

        createClient().fetchPath("test-path", true).body()!!.string() shouldEqual "test-body"
    }

    @Test
    fun `should not add app version in request`() {
        val validContext: Context = ApplicationProvider.getApplicationContext()
        val context: Context = mock()
        When calling context.packageManager itReturns validContext.packageManager
        When calling context.packageName itReturns "invalid.package"
        When calling context.resources itReturns validContext.resources
        val client = ConfigApiClient(baseUrl, context, mockRasHeaders)

        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))
        val response = client.fetchPath("test-path", true)

        response.request().url().queryParameter("appVersion").shouldBeNull()
    }

    @Test
    fun `should not add country in request`() {
        val validContext: Context = ApplicationProvider.getApplicationContext()
        val context: Context = mock()
        val resource: Resources = mock()
        val config: Configuration = mock()
        When calling context.packageManager itReturns validContext.packageManager
        When calling context.packageName itReturns validContext.packageName
        When calling context.resources itReturns resource
        When calling resource.configuration itReturns config
        When calling config.locales itReturns LocaleList(Locale("invalid", "invalid"))
        val client = ConfigApiClient(baseUrl, context, mockRasHeaders)

        enqueueResponse("test-body")
        server.enqueue(MockResponse().setResponseCode(304))
        val response = client.fetchPath("test-path", true)

        response.request().url().queryParameter("country").shouldBeNull()
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
        headers: RasSdkHeaders = mockRasHeaders
    ) = ConfigApiClient(
        baseUrl = url,
        context = ApplicationProvider.getApplicationContext(),
        headers = headers
    )
}
