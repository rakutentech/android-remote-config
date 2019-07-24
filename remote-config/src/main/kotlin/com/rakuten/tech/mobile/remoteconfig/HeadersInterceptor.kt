package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import okhttp3.Interceptor
import okhttp3.Response

internal class HeadersInterceptor @VisibleForTesting constructor(
    private val appId: String,
    private val subscriptionKey: String,
    private val appName: String,
    private val appVersion: String,
    private val deviceModel: String,
    private val deviceOsVersion: String,
    private val sdkVersion: String
) : Interceptor {

    constructor(
        appId: String,
        subscriptionKey: String,
        context: Context
    ) : this(
        appId = appId,
        subscriptionKey = subscriptionKey,
        appName = context.packageName,
        appVersion = context.packageManager
            .getPackageInfo(context.packageName, 0)?.versionName ?: "NONE",
        deviceModel = Build.MODEL,
        deviceOsVersion = Build.VERSION.RELEASE,
        sdkVersion = BuildConfig.VERSION_NAME
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request().newBuilder()
                .addHeader("apiKey", "ras-$subscriptionKey")
                .addHeader("ras-app-id", appId)
                .addHeader("ras-device-model", deviceModel)
                .addHeader("ras-device-version", deviceOsVersion)
                .addHeader("ras-sdk-name", "Remote Config")
                .addHeader("ras-sdk-version", sdkVersion)
                .addHeader("ras-app-name", appName)
                .addHeader("ras-app-version", appVersion)
                .build()
        )
    }
}
