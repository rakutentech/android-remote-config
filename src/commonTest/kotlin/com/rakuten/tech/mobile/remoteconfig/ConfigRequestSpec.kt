package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEndWith
import org.amshove.kluent.shouldEqual
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

@Ignore
open class ConfigFetcherSpec {

    var baseUrl: String = "test"

    fun createClient(
        responseBody: String = createResponseBody(),
        responseHandler: suspend (request: HttpRequestData) -> HttpResponseData =
            { respond(responseBody) }
    ) = HttpClient(MockEngine) {
        engine {
            addHandler(responseHandler)
        }
    }

    fun createResponseBody(
        body: Map<String, String> = hashMapOf("foo" to "bar"),
        keyId: String = "key_id_value"
    ): String {
        val bodyValue = Json.nonstrict.stringify(
            (StringSerializer to StringSerializer).map,
            body
        )

        return """{"body":$bodyValue,"keyId":"$keyId"}"""
    }

    internal fun createConfigRequest(
        client: HttpClient,
        url: String = baseUrl,
        appId: String = "test_app_id"
    ) = ConfigRequest(
        httpClient = client,
        baseUrl = url,
        appId = appId
    )
}

class ConfigFetcherNormalSpec : ConfigFetcherSpec() {

    @Test
    fun `should fetch the config values`() = runBlockingTest {
        val request = createConfigRequest(
            createClient(createResponseBody(
                body = hashMapOf("foo" to "bar")
            ))
        )

        request.fetch().body["foo"] shouldEqual "bar"
    }

    @Test
    fun `should fetch the raw config`() = runBlockingTest {
        val request = createConfigRequest(
            createClient(createResponseBody(
                body = hashMapOf("foo" to "bar"),
                keyId = "test_key_id"
            ))
        )

        request.fetch().rawBody shouldEqual """{"body":{"foo":"bar"},"keyId":"test_key_id"}"""
    }

    @Test
    fun `should trim the raw config response`() = runBlockingTest {
        val request = createConfigRequest(
            createClient("\n${createResponseBody(
                body = hashMapOf("foo" to "bar"),
                keyId = "test_key_id"
            )}")
        )

        request.fetch().rawBody shouldEqual """{"body":{"foo":"bar"},"keyId":"test_key_id"}"""
    }

    @Test
    fun `should fetch the config keyId`() = runBlockingTest {
        val request = createConfigRequest(
            createClient(createResponseBody(
                keyId = "test_key_id"
            ))
        )

        request.fetch().keyId shouldEqual "test_key_id"
    }

    @Test
    fun `should fetch the config signature`() = runBlockingTest {
        val request = createConfigRequest(
            createClient(responseHandler = {
                respond(
                    content = createResponseBody(),
                    headers = headersOf("Signature", listOf("test_signature"))
                )
            })
        )

        request.fetch().signature shouldEqual "test_signature"
    }

    @Test
    fun `should fetch the config from the provided url`() = runBlockingTest {
        var url: String = ""
        val client = createClient(
            responseHandler = { request ->
                url = "${request.url.protocol.name}://${request.url.host}"
                respond(createResponseBody())
            }
        )

        createConfigRequest(client = client, url = "https://www.example.com")
            .fetch()

        url shouldEqual "https://www.example.com"
    }

    @Test
    fun `should fetch the config for the provided App Id`() = runBlockingTest {
        var path: String = ""
        val client = createClient(
            responseHandler = { request ->
                path = request.url.fullPath
                respond(createResponseBody())
            }
        )

        createConfigRequest(client = client, appId = "test-app-id")
            .fetch()

        path shouldContain "/app/test-app-id"
    }

    @Test
    fun `should fetch the config from the 'config' endpoint`() = runBlockingTest {
        var path: String = ""
        val client = createClient(
            responseHandler = { request ->
                path = request.url.fullPath
                respond(createResponseBody())
            }
        )

        createConfigRequest(client = client)
            .fetch()

        path shouldEndWith "/config"
    }
}

class ConfigFetcherErrorSpec : ConfigFetcherSpec() {
    @Test
    fun `should throw when the request is unsuccessful`() = runBlockingTest {
        val request = createConfigRequest(
            createClient(responseHandler = {
                respond(
                    content = "",
                    status = HttpStatusCode.NotFound
                )
            })
        )

        assertFailsWith(ResponseException::class) {
            request.fetch()
        }
    }

    @Test
    fun `should throw when the 'body' key is missing in response`() = runBlockingTest {
        val request = createConfigRequest(
            createClient(responseHandler = {
                respond(
                    content = """{"key_id":"test_key_id"}"""
                )
            })
        )

        assertFailsWith(Exception::class) {
            request.fetch()
        }
    }

    @Test
    fun `should throw when the 'keyId' key is missing in response`() = runBlockingTest {
        val request = createConfigRequest(
            createClient(responseHandler = {
                respond(
                    content = """{"body": {}}"""
                )
            })
        )

        assertFailsWith(Exception::class) {
            request.fetch()
        }
    }

    @Test
    @Suppress("TooGenericExceptionCaught", "LongMethod")
    fun `should not throw when there are extra keys in the response`() = runBlockingTest {
        val request = createConfigRequest(
            createClient(responseHandler = {
                respond(
                    content = """{
                        "body": {},
                        "keyId": "test_key_id",
                        "randomKey": "random_value"
                    }""".trimIndent()
                )
            })
        )

        try {
            request.fetch()
        } catch (e: Exception) {
            fail("Should not throw an exception when there are extra keys in response.")
            throw e
        }
    }
}
