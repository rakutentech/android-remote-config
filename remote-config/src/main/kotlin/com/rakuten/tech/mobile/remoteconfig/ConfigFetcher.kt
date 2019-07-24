package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

import java.io.IOException
import java.lang.IllegalArgumentException

internal class ConfigFetcher constructor(
    baseUrl: String,
    appId: String,
    context: Context,
    private val verifier: SignatureVerifier,
    client: OkHttpClient
) {

    private val client = client.newBuilder()
        .cache(Cache(context.cacheDir, CACHE_SIZE))
        .build()
    private val requestUrl = try {
        HttpUrl.get(baseUrl)
            .newBuilder()
            .addPathSegments("app/$appId/config")
            .build()
    } catch (exception: IllegalArgumentException) {
        throw InvalidRemoteConfigBaseUrlException(exception)
    }

    fun fetch(): Config {
        val response = client.newCall(buildFetchRequest())
            .execute()

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching remote config: $response")
        }

        val (body, keyId) = ConfigResponse.fromJsonString(
            response.body()!!.string() // Body is never null if request is successful
        )
        val signature = response.header("Signature") ?: ""

        val isVerified = verifier.verifyFetched(keyId, body.toInputStream(), signature)
        if (!isVerified) {
            throw IOException("Failed to verify signature of the payload from server.")
        }

        return Config(body, signature, keyId)
    }

    private fun buildFetchRequest() = Request.Builder()
        .url(requestUrl)
        .build()

    companion object {
        private const val CACHE_SIZE = 1024 * 1024 * 2L
    }
}

@Serializable
private data class ConfigResponse(
    val body: Map<String, String>,
    val keyId: String
) {

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}

/**
 * Exception thrown when the value set in `AndroidManifest.xml` for
 * `com.rakuten.tech.mobile.remoteconfig.BaseUrl` is not a valid URL.
 */
class InvalidRemoteConfigBaseUrlException(
    exception: IllegalArgumentException
) : IllegalArgumentException("An invalid URL was provided for the Remote Config base url.", exception)
