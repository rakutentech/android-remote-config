package com.rakuten.tech.mobile.remoteconfig

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    actual fun fetch(response: (Config) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val config = fetcher.fetch()

            withContext(Dispatchers.Main) {
                response(config)
            }
        }
    }
}