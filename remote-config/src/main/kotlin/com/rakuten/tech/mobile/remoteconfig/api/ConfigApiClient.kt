package com.rakuten.tech.mobile.remoteconfig.api

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.sdkutils.BuildConfig
import com.rakuten.tech.mobile.sdkutils.RasSdkHeaders
import com.rakuten.tech.mobile.sdkutils.okhttp.addHeaderInterceptor
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.IllegalArgumentException

internal class ConfigApiClient @VisibleForTesting constructor(
    baseUrl: String,
    context: Context,
    headers: RasSdkHeaders
) {

    constructor(
        baseUrl: String,
        appId: String,
        subscriptionKey: String,
        context: Context
    ) : this(
        baseUrl = baseUrl,
        context = context,
        headers = RasSdkHeaders(
            appId = appId,
            subscriptionKey = subscriptionKey,
            sdkName = "Remote Config",
            sdkVersion = BuildConfig.VERSION_NAME
            )
    )

    @Suppress("SpreadOperator")
    private val client = OkHttpClient.Builder()
        .cache(Cache(context.cacheDir,
            CACHE_SIZE
        ))
        .addHeaderInterceptor(*headers.asArray())
        .build()
    private val requestUrl = try {
        HttpUrl.get(baseUrl)
    } catch (exception: IllegalArgumentException) {
        throw InvalidRemoteConfigBaseUrlException(exception)
    }

    fun fetchPath(path: String, paramMap: Map<String, String>?): Response {
        val builder = requestUrl.newBuilder().addPathSegments(path)

        if (paramMap != null) {
            for ((k, v) in paramMap) {
                if (v.isNotEmpty() && k.isNotEmpty()) builder.addQueryParameter(k, v)
            }
        }

        return client.newCall(Request.Builder()
                .url(builder.build())
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
