package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.api.PublicKeyFetcher

internal class PublicKeyCache @VisibleForTesting constructor(
    private val keyFetcher: PublicKeyFetcher,
    context: Context
) {

    private val prefs = context.getSharedPreferences(
        "com.rakuten.tech.mobile.remoteconfig.publickeys",
        Context.MODE_PRIVATE
    )

    operator fun get(keyId: String): String? {
        return prefs.getString(keyId, null)
            ?: return null
    }

    fun remove(keyId: String) {
        prefs.edit()
            .remove(keyId)
            .apply()
    }

    fun fetch(keyId: String): String {
        val key = keyFetcher.fetch(keyId)

        prefs.edit()
            .putString(keyId, key)
            .apply()

        return key
    }
}
