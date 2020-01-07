package com.rakuten.tech.mobile.remoteconfig.api

import com.nhaarman.mockitokotlin2.argForWhich
import com.rakuten.tech.mobile.remoteconfig.jsonMapAdapter
import junit.framework.TestCase
import okhttp3.*
import org.amshove.kluent.*
import org.junit.Test
import java.io.IOException

open class ConfigFetcherSpec {

    internal val mockApiClient: ConfigApiClient = mock()

    internal fun createBody(
        body: Map<String, String> = hashMapOf("foo" to "bar"),
        keyId: String = "key_id_value"
    ): String {
        val bodyValue = jsonMapAdapter<String, String>().toJson(body)!!

        return """{"body":$bodyValue,"keyId":"$keyId"}"""
    }

    internal fun enqueueResponse(
        body: String,
        signature: String,
        code: Int
    ) {
        When calling mockApiClient.fetchPath(any()) itReturns Response.Builder()
            .request(Request.Builder().url("https://www.example.com").build())
            .protocol(Protocol.HTTP_2)
            .message("")
            .code(code)
            .addHeader("Signature", signature)
            .body(ResponseBody.create(MediaType.get("text/plain; charset=utf-8"), body))
            .build()
    }

    internal fun createFetcher(
        appId: String = "test_app_id"
    ) = ConfigFetcher(
        appId = appId,
        client = mockApiClient
    )
}

class ConfigFetcherNormalSpec : ConfigFetcherSpec() {

    private fun enqueueSuccessResponse(
        body: Map<String, String> = hashMapOf("foo" to "bar"),
        keyId: String = "key_id_value",
        signature: String = "test_signature"
    ) = enqueueSuccessResponse(createBody(body, keyId), signature)

    private fun enqueueSuccessResponse(
        body: String = createBody(),
        signature: String = "test_signature"
    ) {
        enqueueResponse(
            body = body,
            signature = signature,
            code = 200
        )
    }

    @Test
    fun `should fetch the config body`() {
        val fetcher = createFetcher()
        enqueueSuccessResponse(
            body = hashMapOf("foo" to "bar"),
            keyId = "test_key_id"
        )

        fetcher.fetch().rawBody shouldEqual """{"body":{"foo":"bar"},"keyId":"test_key_id"}"""
    }

    @Test
    fun `should trim the body`() {
        val fetcher = createFetcher()
        val body = "\n${createBody(
            body = hashMapOf("foo" to "bar"),
            keyId = "test_key_id"
        )}\n"
        enqueueSuccessResponse(body = body)

        fetcher.fetch().rawBody shouldEqual """{"body":{"foo":"bar"},"keyId":"test_key_id"}"""
    }

    @Test
    fun `should fetch the config keyId`() {
        val fetcher = createFetcher()
        enqueueSuccessResponse(keyId = "test_key_id")

        fetcher.fetch().keyId shouldEqual "test_key_id"
    }

    @Test
    fun `should fetch the config signature`() {
        val fetcher = createFetcher()
        enqueueSuccessResponse(signature = "test_signature")

        fetcher.fetch().signature shouldEqual "test_signature"
    }

    @Test
    fun `should fetch the config for the provided App Id`() {
        val fetcher = createFetcher(appId = "test-app-id")
        enqueueSuccessResponse()

        fetcher.fetch()

        Verify on mockApiClient that mockApiClient.fetchPath(argForWhich {
            contains("app/test-app-id")
        })
    }

    @Test
    fun `should fetch the config from the 'config' endpoint`() {
        val fetcher = createFetcher()
        enqueueSuccessResponse()

        fetcher.fetch()

        Verify on mockApiClient that mockApiClient.fetchPath(argForWhich {
            endsWith("/config")
        })
    }
}

class ConfigFetcherErrorSpec : ConfigFetcherSpec() {

    private fun enqueueErrorResponse(
        body: String = "",
        code: Int = 200
    ) {
        enqueueResponse(
            body = body,
            code = code,
            signature = "test_signature"
        )
    }

    @Test(expected = IOException::class)
    fun `should throw when the request is unsuccessful`() {
        val fetcher = createFetcher()
        enqueueErrorResponse(code = 400)

        fetcher.fetch()
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'body' key is missing in response`() {
        val fetcher = createFetcher()
        enqueueErrorResponse(
            body = """{"key_id":"test_key_id"}"""
        )

        fetcher.fetch()
    }

    @Test(expected = Exception::class)
    fun `should throw when the 'keyId' key is missing in response`() {
        val fetcher = createFetcher()
        enqueueErrorResponse(
            body = """{"body": {}}"""
        )

        fetcher.fetch()
    }

    @Test
    @Suppress("TooGenericExceptionCaught")
    fun `should not throw when there are extra keys in the response`() {
        val fetcher = createFetcher()
        enqueueErrorResponse(
            body = """{
                    "body": {},
                    "keyId": "test_key_id",
                    "randomKey": "random_value"
                }""".trimIndent()
        )

        try {
            fetcher.fetch()
        } catch (e: Exception) {
            TestCase.fail("Should not throw an exception when there are extra keys in response.")
            throw e
        }
    }
}
