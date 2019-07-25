package com.rakuten.tech.mobile.remoteconfig.api

import android.content.Context
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.IllegalArgumentException

internal class ConfigApiClient(
    baseUrl: String,
    appId: String,
    subscriptionKey: String,
    context: Context
) {

    private val client = OkHttpClient.Builder()
        .cache(Cache(context.cacheDir,
            CACHE_SIZE
        ))
        .addNetworkInterceptor(
            HeadersInterceptor(
                appId = appId,
                subscriptionKey = subscriptionKey,
                context = context
            )
        )
        .build()
    private val requestUrl = try {
        HttpUrl.get(baseUrl)
    } catch (exception: IllegalArgumentException) {
        throw InvalidRemoteConfigBaseUrlException(exception)
    }

    fun fetchPath(path: String): Response {
        val url = requestUrl.newBuilder()
            .addPathSegments(path)
            .build()

        return client.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).execute()
    }

    companion object {
        private const val CACHE_SIZE = 1024 * 1024 * 2L
    }
}

/**
 * Exception thrown when the value set in `AndroidManifest.xml` for
 * `com.rakuten.tech.mobile.remoteconfig.BaseUrl` is not a valid URL.
 */
class InvalidRemoteConfigBaseUrlException(
    exception: IllegalArgumentException
) : IllegalArgumentException("An invalid URL was provided for the Remote Config base url.", exception)
