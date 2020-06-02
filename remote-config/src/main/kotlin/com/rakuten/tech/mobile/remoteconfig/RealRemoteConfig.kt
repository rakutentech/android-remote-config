package com.rakuten.tech.mobile.remoteconfig

import android.util.Log

internal class RealRemoteConfig(
    private val cache: ConfigCache
) : RemoteConfig() {

    override fun getString(key: String, fallback: String) = cache[key] ?: fallback

    override fun getBoolean(key: String, fallback: Boolean) = cache[key]?.toBoolean() ?: fallback

    @Suppress("UNCHECKED_CAST")
    override fun <T : Number> getNumber(key: String, fallback: T): T {
        val value = cache[key] ?: return fallback

        return try {
            when (fallback) {
                is Double -> value.toDouble() as T
                is Long -> value.toLong() as T
                is Short -> value.toShort() as T
                is Byte -> value.toByte() as T
                is Float -> value.toFloat() as T
                else -> value.toInt() as T
            }
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Error parsing number from config", e)

            fallback
        }
    }

    override fun getConfig() = cache.getConfig()
    override fun fetchAndApplyConfig(listener: FetchConfigCompletionListener) {
        cache.fetchConfig(listener)
    }

    companion object {
        private const val TAG = "RC_RealRemote"
    }
}
