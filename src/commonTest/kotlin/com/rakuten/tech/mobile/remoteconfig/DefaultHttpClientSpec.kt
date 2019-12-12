package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.Headers
import org.amshove.kluent.shouldEqual
import kotlin.test.Test

class DefaultHttpClientSpec {

    private val expectedHeaders = mapOf(
        "ras-app-id" to "test-app-id",
        "apiKey" to "ras-test-subscription-key",
        "ras-sdk-name" to "Remote Config",
        "ras-app-name" to "test.app.name",
        "ras-app-version" to "1.0.0",
        "ras-device-model" to "test model name",
        "ras-os-version" to "2.0.0",
        "ras-sdk-version" to "3.0.0"
    )

    @Test
    @Suppress("LongMethod")
    fun `should attach ras headers`() = runBlockingTest {
        var headers: Headers = Headers.Empty
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    headers = request.headers
                    respond("")
                }
            }
        }
        val defaultClient = createDefaultHttpClient(
            platformClient = client,
            appId = "test-app-id",
            subscriptionKey = "test-subscription-key",
            deviceModel = "test model name",
            osVersion = "2.0.0",
            appName = "test.app.name",
            appVersion = "1.0.0",
            sdkVersion = "3.0.0"
        )

        defaultClient.get<String>("test")

        expectedHeaders.forEach { header ->
            headers[header.key] shouldEqual header.value
        }
    }
}
