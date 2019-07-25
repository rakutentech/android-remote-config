package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import com.rakuten.tech.mobile.remoteconfig.api.PublicKeyFetcher

internal class PublicKeyCache(
    private val keyFetcher: PublicKeyFetcher,
    context: Context
) {

    private val prefs = context.getSharedPreferences(
        "com.rakuten.tech.mobile.remoteconfig.publickeys",
        Context.MODE_PRIVATE
    )

    operator fun get(keyId: String): String? = prefs.getString(keyId, null)

    fun fetch(keyId: String): String {
        val key = keyFetcher.fetch(keyId)

        prefs.edit()
            .putString(keyId, key)
            .apply()

        return key
    }
}
