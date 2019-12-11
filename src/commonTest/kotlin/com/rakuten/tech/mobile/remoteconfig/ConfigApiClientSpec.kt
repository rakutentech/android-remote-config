package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldEqual
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test

class ConfigApiClientSpec {

    private val mockConfigRequest: ConfigRequest = mockk()
    private val mockPublicKeyRequest: PublicKeyRequest = mockk()
    private val mockConfig: Config = mockk()

    @Test
    fun `should fetch the config response`() = runBlockingTest {
        every { mockConfigRequest["fetch"]() } returns mockConfig

        val client = createClient()

        val config = suspendCoroutine<Config> { continuation ->
            client.fetchConfig(
                { continuation.resume(it) },
                { continuation.resumeWithException(it) },
                mockConfigRequest
            )
        }

        config shouldEqual mockConfig
    }

    @Test
    fun `should fetch the public key response`() = runBlockingTest {
        every { mockPublicKeyRequest["fetch"]("test_key_id") } returns "test_public_key"

        val client = createClient()

        val config = suspendCoroutine<String> { continuation ->
            client.fetchPublicKey(
                "test_key_id",
                { continuation.resume(it) },
                { continuation.resumeWithException(it) },
                mockPublicKeyRequest
            )
        }

        config shouldEqual "test_public_key"
    }


    private fun createHttpClient(
        responseBody: String = "",
        responseHandler: suspend (request: HttpRequestData) -> HttpResponseData =
            { respond(responseBody) }
    ) = HttpClient(MockEngine) {
        engine {
            addHandler(responseHandler)
        }
    }

    private fun createClient(
        platformClient: HttpClient = createHttpClient(),
        baseUrl: String = "https://www.example.com",
        appId: String = "test-app-id",
        subscriptionKey: String = "test-subscription-key",
        deviceModel: String = "test-device-model",
        osVersion: String = "test-os-version",
        appName: String = "test-app-name",
        appVersion: String = "test-app-version",
        sdkVersion: String = "test-sdk-version"
    ) = ConfigApiClient(
        platformClient = platformClient,
        baseUrl = baseUrl,
        appId = appId,
        subscriptionKey = subscriptionKey,
        deviceModel = deviceModel,
        osVersion = osVersion,
        appName = appName,
        appVersion = appVersion,
        sdkVersion = sdkVersion
    )
}
