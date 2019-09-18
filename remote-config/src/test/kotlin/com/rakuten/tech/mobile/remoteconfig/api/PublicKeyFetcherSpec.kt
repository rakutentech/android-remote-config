package com.rakuten.tech.mobile.remoteconfig.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import junit.framework.TestCase
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.*
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.IOException
import java.util.logging.Level
import java.util.logging.LogManager

@Ignore
open class PublicKeyFetcherSpec : RobolectricBaseSpec() {
    val server = MockWebServer()
    lateinit var baseUrl: String

    private val context = ApplicationProvider.getApplicationContext<Context>()

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

    internal fun createFetcher(url: String = baseUrl) =
        PublicKeyFetcher(
            ConfigApiClient(
                baseUrl = url,
                appId = "test_app_id",
                subscriptionKey = "test_subscription_key",
                context = context
            )
        )

    internal fun enqueueResponse(
        id: String = "test_id",
        key: String = "test_key",
        createdAt: String = "2019-07-23T07:24:57+00:00"
    ) {
        server.enqueue(
            MockResponse()
                .setBody("""
                {
                    "id": "$id",
                    "key": "$key",
                    "createdAt": "$createdAt"
                }
            """.trimIndent())
        )
    }
}

class PublicKeyFetcherNormalSpec : PublicKeyFetcherSpec() {

    @Test
    fun `should fetch the public key`() {
        val fetcher = createFetcher()
        enqueueResponse(key = "test_key")

        fetcher.fetch("test_key_id") shouldEqual "test_key"
    }

    @Test
    fun `should fetch the public key for the provided key id`() {
        val fetcher = createFetcher()
        enqueueResponse(id = "test_key_id")

        fetcher.fetch("test_key_id")

        server.takeRequest().path shouldContain "/keys/test_key_id"
    }
}

class PublicKeyFetcherErrorSpec : PublicKeyFetcherSpec() {

    @Test(expected = IOException::class)
    fun `should throw when the request is unsuccessful`() {
        val fetcher = createFetcher()
        server.enqueue(MockResponse().setResponseCode(400))

        fetcher.fetch("test_key_id")
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'id' key is missing in response`() {
        val fetcher = createFetcher()
        server.enqueue(
            MockResponse().setBody("""{
                    "key": "test_key",
                    "createdAt": "test_created"
                }""".trimIndent())
        )

        fetcher.fetch("test_key_id")
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'key' key is missing in response`() {
        val fetcher = createFetcher()
        server.enqueue(
            MockResponse().setBody("""{
                    "id": "test_key_id",
                    "createdAt": "test_created"
                }""".trimIndent())
        )

        fetcher.fetch("test_key_id")
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'createdAt' key is missing in response`() {
        val fetcher = createFetcher()
        server.enqueue(
            MockResponse().setBody("""{
                    "id": "test_key_id",
                    "key": "test_key"
                }""".trimIndent())
        )

        fetcher.fetch("test_key_id")
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun `should not throw when there are extra keys in the response`() {
        val fetcher = createFetcher()
        server.enqueue(
            MockResponse().setBody("""{
                    "id": "test_id",
                    "key": "test_key",
                    "createdAt": "test_created",
                    "randomKey": "random_value"
                }""".trimIndent())
        )

        try {
            fetcher.fetch("test_key_id")
        } catch (e: Exception) {
            TestCase.fail("Should not throw an exception.")
            throw e
        }
    }
}
