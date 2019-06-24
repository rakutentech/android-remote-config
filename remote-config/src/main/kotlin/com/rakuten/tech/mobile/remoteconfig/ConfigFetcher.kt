package com.rakuten.tech.mobile.remoteconfig

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException

internal class ConfigFetcher(
    baseUrl: String,
    appId: String,
    private val subscriptionKey: String,
    cacheDirectory: File
) {

    private val client = OkHttpClient.Builder()
        .cache(Cache(cacheDirectory, CACHE_SIZE))
        .build()
    private val requestUrl = try {
        HttpUrl.get(baseUrl)
            .newBuilder()
            .addPathSegments("app/$appId/config")
            .build()
    } catch (exception: IllegalArgumentException) {
        throw InvalidRemoteConfigBaseUrlException(exception)
    }

    fun fetch(): Map<String, String> {
        val response = client.newCall(buildFetchRequest())
            .execute()

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching remote config: $response")
        }

        val body = response.body()!!.string() // Body is never null if request is successful

        return ConfigResponse.fromJsonString(body).body
    }

    private fun buildFetchRequest() = Request.Builder()
            .url(requestUrl)
            .addHeader("apiKey", "ras-$subscriptionKey")
            .build()

    companion object {
        private const val CACHE_SIZE = 1024 * 1024 * 2L
    }
}

@Serializable
private data class ConfigResponse(val body: Map<String, String>) {

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
