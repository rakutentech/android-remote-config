package com.rakuten.tech.mobile.remoteconfig.api

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.rakuten.tech.mobile.remoteconfig.Config
import com.rakuten.tech.mobile.remoteconfig.jsonAdapter
import com.squareup.moshi.JsonClass
import java.io.IOException
import java.util.Locale
import kotlin.collections.HashMap

internal class ConfigFetcher constructor(
    private val client: ConfigApiClient,
    private val appId: String,
    private val context: Context
) {

    fun fetch(): Config {
        val response = client.fetchPath("app/$appId/config", createParamMap())

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching remote config: $response")
        }

        val body = response.body()!!.string()
                .trim() // OkHttp sometimes adds an extra newline character when caching the response

        val (_body, keyId) = ConfigResponse.fromJsonString(body)!!
        val signature = response.header("Signature") ?: ""

        return Config(body, signature, keyId)
    }

    private fun createParamMap(): Map<String, String> {
        val paramMap = HashMap<String, String>()

        try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            paramMap[PARAM_KEY_APP_VERSION] = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(SDK_NAME, e.localizedMessage.toString())
        }

        paramMap[PARAM_KEY_OS_VERSION] = Build.VERSION.RELEASE

        val (language, country) = getLanguageDetails()
        paramMap[PARAM_KEY_LANGUAGE] = language
        paramMap[PARAM_KEY_COUNTRY] = country

        return paramMap
    }

    private fun getLanguageDetails(): Pair<String, String> {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            context.resources.configuration.locale
        }

        return Pair(locale.language.toLowerCase(Locale.getDefault()), locale.country.toLowerCase(Locale.getDefault()))
    }

    companion object {
        private const val PARAM_KEY_APP_VERSION = "appVersion"
        private const val PARAM_KEY_OS_VERSION = "osVersion"
        private const val PARAM_KEY_LANGUAGE = "language"
        private const val PARAM_KEY_COUNTRY = "country"
        private const val SDK_NAME = "Remote Config"
    }
}

@JsonClass(generateAdapter = true)
internal data class ConfigResponse(
    val body: Map<String, String>,
    val keyId: String
) {
    companion object {
        fun fromJsonString(body: String) = jsonAdapter<ConfigResponse>().fromJson(body)!!
    }
}
