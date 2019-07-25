package com.rakuten.tech.mobile.remoteconfig.api

import com.rakuten.tech.mobile.remoteconfig.RobolectricBaseSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldEqual
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.util.logging.Level
import java.util.logging.LogManager

@RunWith(ParameterizedRobolectricTestRunner::class)
class HeadersInterceptorSpec(
    private val description: String,
    private val name: String,
    private val value: String
) : RobolectricBaseSpec() {

    private val server = MockWebServer()

    init {
        LogManager.getLogManager()
            .getLogger(MockWebServer::class.java.name).level = Level.OFF
    }

    @Before
    fun setup() {
        server.start()
        server.enqueue(MockResponse())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `should attach the header`() {
        val client = OkHttpClient.Builder().addNetworkInterceptor(
            HeadersInterceptor(
                appId = "test-app-id",
                subscriptionKey = "test-subscription-key",
                appName = "test.app.name",
                appVersion = "1.0.0",
                deviceModel = "test model name",
                deviceOsVersion = "2.0.0",
                sdkVersion = "3.0.0"
            )
        ).build()

        client.newCall(
            Request.Builder().url(server.url("")).build()
        ).execute()

        server.takeRequest().getHeader(name) shouldEqual value
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "\"{0}\" Header"
        )
        fun data(): Collection<Array<String>> {
            return listOf(
                arrayOf("App ID", "ras-app-id", "test-app-id"),
                arrayOf("Subscription Key prefixed with 'ras-'", "apiKey", "ras-test-subscription-key"),
                arrayOf("SDK Name", "ras-sdk-name", "Remote Config"),
                arrayOf("App Name", "ras-app-name", "test.app.name"),
                arrayOf("App Version", "ras-app-version", "1.0.0"),
                arrayOf("Device Model", "ras-device-model", "test model name"),
                arrayOf("Device Version", "ras-device-version", "2.0.0"),
                arrayOf("SDK Version", "ras-sdk-version", "3.0.0")
            )
        }
    }
}
