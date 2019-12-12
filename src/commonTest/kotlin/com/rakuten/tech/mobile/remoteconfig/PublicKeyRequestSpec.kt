package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldEqual
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail

@Ignore
open class PublicKeyFetcherSpec {

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
        id: String = "test_id",
        key: String = "test_key",
        createdAt: String = "2019-07-23T07:24:57+00:00"
    ) = """
        {
            "id": "$id",
            "key": "$key",
            "createdAt": "$createdAt"
        }
    """.trimIndent()

    internal fun createKeyRequest(
        client: HttpClient,
        url: String = baseUrl
    ) = PublicKeyRequest(
        httpClient = client,
        baseUrl = url
    )
}

class PublicKeyFetcherNormalSpec : PublicKeyFetcherSpec() {

    @Test
    fun `should fetch the public key`() = runBlockingTest {
        val request = createKeyRequest(
            createClient(createResponseBody(key = "test_key"))
        )

        request.fetch("test_key_id") shouldEqual "test_key"
    }

    @Test
    fun `should fetch the public key for the provided key id`() = runBlockingTest {
        var path: String = ""
        val client = createClient(
            responseHandler = { request ->
                path = request.url.fullPath
                respond(createResponseBody())
            }
        )

        createKeyRequest(client).fetch("test_key_id")

        path shouldContain "/keys/test_key_id"
    }
}

class PublicKeyFetcherErrorSpec : PublicKeyFetcherSpec() {

    @Test
    fun `should throw when the request is unsuccessful`() = runBlockingTest {
        val request = createKeyRequest(
            createClient(responseHandler = {
                respond(
                    content = "",
                    status = HttpStatusCode.NotFound
                )
            })
        )

        assertFailsWith(ResponseException::class) {
            request.fetch("key_id")
        }
    }

    @Test
    fun `should throw when the 'body' key is missing in response`() = runBlockingTest {
        val request = createKeyRequest(
            createClient(responseHandler = {
                respond(
                    content = """{
                        "key": "test_key",
                        "createdAt": "test_created"
                    }""".trimIndent()
                )
            })
        )

        assertFailsWith(Exception::class) {
            request.fetch("key_id")
        }
    }

    @Test
    fun `should throw when the 'key' key is missing in response`() = runBlockingTest {
        val request = createKeyRequest(
            createClient(responseHandler = {
                respond(
                    content = """{
                    "id": "test_key_id",
                    "createdAt": "test_created"
                    }""".trimIndent()
                )
            })
        )

        assertFailsWith(Exception::class) {
            request.fetch("key_id")
        }
    }

    @Test
    fun `should throw when the 'createdAt' key is missing in response`() = runBlockingTest {
        val request = createKeyRequest(
            createClient(responseHandler = {
                respond(
                    content = """{
                    "id": "test_key_id",
                    "key": "test_key"
                    }""".trimIndent()
                )
            })
        )

        assertFailsWith(Exception::class) {
            request.fetch("key_id")
        }
    }

    @Test
    @Suppress("TooGenericExceptionCaught", "LongMethod")
    fun `should not throw when there are extra keys in the response`() = runBlockingTest {
        val request = createKeyRequest(
            createClient(responseHandler = {
                respond(
                    content = """{
                        "id": "test_id",
                        "key": "test_key",
                        "createdAt": "test_created",
                        "randomKey": "random_value"
                    }""".trimIndent()
                )
            })
        )

        try {
            request.fetch("key_id")
        } catch (e: Exception) {
            fail("Should not throw an exception when there are extra keys in response.")
            throw e
        }
    }
}
