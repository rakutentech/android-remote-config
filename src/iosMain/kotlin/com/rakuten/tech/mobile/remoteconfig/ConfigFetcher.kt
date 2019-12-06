package com.rakuten.tech.mobile.remoteconfig

import kotlinx.coroutines.*

actual class ConfigFetcher constructor(
    baseUrl: String,
    appId: String,
    subscriptionKey: String
) {

    private val fetcher = RealConfigFetcher(
        baseUrl = baseUrl,
        appId = appId,
        subscriptionKey = subscriptionKey
    )

    /**
     * Must be called from main thread or DispatchQueue.main
     */
    actual fun fetch(response: (Config) -> Unit) {
        CoroutineScope(Dispatchers.Current).launch {
            val config = fetcher.fetch()
            response(config)
        }
    }
}
