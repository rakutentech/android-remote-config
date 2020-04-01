package com.rakuten.tech.mobile.remoteconfig.api

import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.argForWhich
import com.nhaarman.mockitokotlin2.eq
import com.rakuten.tech.mobile.remoteconfig.jsonMapAdapter
import junit.framework.TestCase
import okhttp3.*
import org.amshove.kluent.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

open class ConfigFetcherSpec {

    internal val mockApiClient: ConfigApiClient = mock()
    internal val mockContext: Context = mock()
    internal val resource: Resources = mock()
    internal val config: Configuration = mock()

    internal fun createBody(
        body: Map<String, String> = hashMapOf("foo" to "bar"),
        keyId: String = "key_id_value"
    ): String {
        val bodyValue = jsonMapAdapter<String, String>().toJson(body)!!

        return """{"body":$bodyValue,"keyId":"$keyId"}"""
    }

    internal fun enqueueResponse(body: String, signature: String, code: Int, param: Map<String, String>) {
        val url = HttpUrl.get("https://www.example.com")
        val builder = url.newBuilder()
        for ((k, v) in param) builder.addQueryParameter(k, v)

        When calling mockApiClient.fetchPath(any(), eq(param)) itReturns Response.Builder()
            .request(Request.Builder().url(url).build())
            .protocol(Protocol.HTTP_2)
            .message("")
            .code(code)
            .addHeader("Signature", signature)
            .body(ResponseBody.create(MediaType.get("text/plain; charset=utf-8"), body))
            .build()
    }

    internal fun setMockValidQueryParams(context: Context) {
        When calling mockContext.packageManager itReturns context.packageManager
        When calling mockContext.packageName itReturns context.packageName
        When calling mockContext.resources itReturns resource
        When calling resource.configuration itReturns config
        When calling config.locales itReturns LocaleList(Locale("valid", "valid"))
    }

    internal fun createFetcher(
        appId: String = "test_app_id",
        context: Context = mockContext
    ) = ConfigFetcher(
        appId = appId,
        client = mockApiClient,
        context = context
    )

    internal fun createDefaultParam(context: Context): Map<String, String> {
        val param = HashMap<String, String>()
        val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        param[PARAM_KEY_APP_VERSION] = pInfo.versionName
        param[PARAM_KEY_OS_VERSION] = Build.VERSION.RELEASE
        param[PARAM_KEY_LANGUAGE] = "valid"
        param[PARAM_KEY_COUNTRY] = "valid"

        return param
    }

    companion object {
        internal const val PARAM_KEY_APP_VERSION = "appVersion"
        internal const val PARAM_KEY_OS_VERSION = "osVersion"
        internal const val PARAM_KEY_LANGUAGE = "language"
        internal const val PARAM_KEY_COUNTRY = "country"
    }
}
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConfigFetcherNormalSpec : ConfigFetcherSpec() {
    private val validContext: Context = ApplicationProvider.getApplicationContext()

    private fun enqueueSuccessResponse(
        body: Map<String, String> = hashMapOf("foo" to "bar"),
        keyId: String = "key_id_value",
        signature: String = "test_signature",
        param: Map<String, String> = createDefaultParam(validContext)
    ) = enqueueSuccessResponse(createBody(body, keyId), signature, param)

    private fun enqueueSuccessResponse(
        body: String = createBody(),
        signature: String = "test_signature",
        param: Map<String, String>
    ) {
        enqueueResponse(
            body = body,
            signature = signature,
            code = 200,
            param = param
        )
    }

    @Before
    fun setup() {
        setMockValidQueryParams(validContext)
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
        enqueueSuccessResponse(body = body, param = createDefaultParam(validContext))

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
        }, eq(createDefaultParam(validContext)))
    }

    @Test
    fun `should fetch the config from the 'config' endpoint`() {
        val fetcher = createFetcher()
        enqueueSuccessResponse()

        fetcher.fetch()

        Verify on mockApiClient that mockApiClient.fetchPath(argForWhich {
            endsWith("/config")
        }, eq(createDefaultParam(validContext)))
    }

    @Test
    fun `should not add app version in request`() {
        When calling mockContext.packageManager itReturns validContext.packageManager
        When calling mockContext.packageName itReturns "invalid.package.name"
        When calling mockContext.resources itReturns resource
        When calling resource.configuration itReturns config
        When calling config.locales itReturns LocaleList(Locale("valid", "valid"))

        val param = HashMap<String, String>()
        param[PARAM_KEY_OS_VERSION] = Build.VERSION.RELEASE
        param[PARAM_KEY_LANGUAGE] = "valid"
        param[PARAM_KEY_COUNTRY] = "valid"

        enqueueSuccessResponse(param = param)
        createFetcher(context = mockContext).fetch()

        Verify on mockApiClient that mockApiClient.fetchPath(argForWhich { endsWith("/config") },
                argForWhich { size == 3 && !containsKey(PARAM_KEY_APP_VERSION) && containsKey(PARAM_KEY_OS_VERSION) &&
                    containsKey(PARAM_KEY_LANGUAGE) && containsKey(PARAM_KEY_COUNTRY) })
    }

    @Test
    fun `should contain all params in request`() {
        enqueueSuccessResponse()
        createFetcher(context = mockContext).fetch()

        Verify on mockApiClient that mockApiClient.fetchPath(argForWhich { endsWith("/config") },
                argForWhich { size == 4 && containsKey(PARAM_KEY_APP_VERSION) && containsKey(PARAM_KEY_OS_VERSION) &&
                    containsKey(PARAM_KEY_LANGUAGE) && containsKey(PARAM_KEY_COUNTRY) })
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConfigFetcherErrorSpec : ConfigFetcherSpec() {
    private val validContext: Context = ApplicationProvider.getApplicationContext()

    private fun enqueueErrorResponse(
        body: String = "",
        code: Int = 200
    ) {
        enqueueResponse(
            body = body,
            code = code,
            signature = "test_signature",
            param = createDefaultParam(validContext)
        )
    }

    @Before
    fun setup() {
        setMockValidQueryParams(validContext)
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
