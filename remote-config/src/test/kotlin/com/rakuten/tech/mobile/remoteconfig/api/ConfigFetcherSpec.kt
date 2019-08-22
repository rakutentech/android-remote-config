package com.rakuten.tech.mobile.remoteconfig.api

import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import junit.framework.TestCase
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonUnknownKeyException
import kotlinx.serialization.map
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.*
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.IOException
import java.util.logging.Level
import java.util.logging.LogManager

@Ignore
open class ConfigFetcherSpec : RobolectricBaseSpec() {

    val server = MockWebServer()
    val context = RuntimeEnvironment.application
    lateinit var baseUrl: String

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

    internal fun enqueueResponse(
        body: Map<String, String> = hashMapOf("foo" to "bar"),
        keyId: String = "key_id_value",
        signature: String = "test_signature",
        etag: String = "etag_value"
    ) {
        val body = Json.nonstrict.stringify(
            (StringSerializer to StringSerializer).map,
            body
        )
        server.enqueue(
            MockResponse()
                .setBody("""{"body":$body,"keyId":"$keyId"}""".trimIndent())
                .setHeader("Signature", signature)
                .setHeader("ETag", etag)
        )
    }

    internal fun createFetcher(
        url: String = baseUrl,
        appId: String = "test_app_id",
        subscriptionKey: String = "test_subscription_key"
    ) = ConfigFetcher(
        appId = appId,
        client = ConfigApiClient(
            baseUrl = url,
            appId = "test_app_id",
            subscriptionKey = "test_subscription_key",
            context = context
        )
    )
}

class ConfigFetcherNormalSpec : ConfigFetcherSpec() {
    @Test
    fun `should fetch the config body`() {
        val fetcher = createFetcher()
        enqueueResponse(
            body = hashMapOf("foo" to "bar"),
            keyId = "test_key_id"
        )

        fetcher.fetch().rawBody shouldEqual """{"body":{"foo":"bar"},"keyId":"test_key_id"}"""
    }

    @Test
    fun `should fetch the config keyId`() {
        val fetcher = createFetcher()
        enqueueResponse(keyId = "test_key_id")

        fetcher.fetch().keyId shouldEqual "test_key_id"
    }

    @Test
    fun `should fetch the config signature`() {
        val fetcher = createFetcher()
        enqueueResponse(signature = "test_signature")

        fetcher.fetch().signature shouldEqual "test_signature"
    }

    @Test
    fun `should fetch the config for the provided App Id`() {
        val fetcher = createFetcher(appId = "test-app-id")
        enqueueResponse()

        fetcher.fetch()

        server.takeRequest().path shouldContain "/app/test-app-id"
    }

    @Test
    fun `should fetch the config from the 'config' endpoint`() {
        val fetcher = createFetcher()
        enqueueResponse()

        fetcher.fetch()

        server.takeRequest().path shouldEndWith "/config"
    }
}

class ConfigFetcherErrorSpec : ConfigFetcherSpec() {

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
            MockResponse().setBody("""{"key_id":"test_key_id"}""")
        )

        fetcher.fetch()
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'keyId' key is missing in response`() {
        val fetcher = createFetcher()
        server.enqueue(
            MockResponse().setBody("""{"body": {}}""")
        )

        fetcher.fetch()
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun `should not throw when there are extra keys in the response`() {
        val fetcher = createFetcher()
        server.enqueue(
            MockResponse().setBody("""{
                    "body": {},
                    "keyId": "test_key_id",
                    "randomKey": "random_value"
                }""".trimIndent())
        )

        try {
            fetcher.fetch()
        } catch (e: JsonUnknownKeyException) {
            TestCase.fail("Should not throw an exception when there are extra keys in response.")
            throw e
        }
    }
}
