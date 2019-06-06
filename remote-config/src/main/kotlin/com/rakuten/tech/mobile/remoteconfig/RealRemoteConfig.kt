package com.rakuten.tech.mobile.remoteconfig

internal class RealRemoteConfig(
    private val cache: ConfigCache
) : RemoteConfig() {

    override fun getString(key: String, fallback: String) = cache[key] ?: fallback
}
