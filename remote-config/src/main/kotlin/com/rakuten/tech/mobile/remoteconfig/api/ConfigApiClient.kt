package com.rakuten.tech.mobile.remoteconfig.api

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.sdkutils.BuildConfig
import com.rakuten.tech.mobile.sdkutils.RasSdkHeaders
import com.rakuten.tech.mobile.sdkutils.okhttp.addHeaderInterceptor
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.MissingResourceException

internal class ConfigApiClient @VisibleForTesting constructor(
    baseUrl: String,
    private val context: Context,
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

    fun fetchPath(path: String, isConfig: Boolean): Response {
        val builder = requestUrl.newBuilder().addPathSegments(path)

        if (isConfig) {
            try {
                val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                builder.addQueryParameter(PARAM_KEY_APP_VERSION, pInfo.versionName)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.d(SDK_NAME, e.localizedMessage.toString())
            }

            builder.addQueryParameter(PARAM_KEY_OS_VERSION, Build.VERSION.SDK_INT.toString())
            val (language, country) = getLanguageDetails()
            if (language.isNotEmpty()) builder.addQueryParameter(PARAM_KEY_LANGUAGE, language)
            if (country.isNotEmpty()) builder.addQueryParameter(PARAM_KEY_COUNTRY, country)
        }

        return client.newCall(Request.Builder()
                .url(builder.build())
                .build()
        ).execute()
    }

    private fun getLanguageDetails(): Pair<String, String> {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            context.resources.configuration.locale
        }
        val lang = locale.language.toLowerCase(Locale.getDefault())

        val country = try { locale.isO3Country.toLowerCase(Locale.getDefault())
        } catch (ex: MissingResourceException) {
            Log.d(SDK_NAME, "three-letter country abbreviation is not available for this locale", ex)
            ""
        }
        return Pair(lang, country)
    }

    companion object {
        private const val CACHE_SIZE = 1024 * 1024 * 2L
        private const val PARAM_KEY_APP_VERSION = "appVersion"
        private const val PARAM_KEY_OS_VERSION = "osVersion"
        private const val PARAM_KEY_LANGUAGE = "language"
        private const val PARAM_KEY_COUNTRY = "country"
        private const val SDK_NAME = "Remote Config"
    }
}

/**
 * Exception thrown when the value set in `AndroidManifest.xml` for
 * `com.rakuten.tech.mobile.remoteconfig.BaseUrl` is not a valid URL.
 */
class InvalidRemoteConfigBaseUrlException(
    exception: IllegalArgumentException
) : IllegalArgumentException("An invalid URL was provided for the Remote Config base url.", exception)
