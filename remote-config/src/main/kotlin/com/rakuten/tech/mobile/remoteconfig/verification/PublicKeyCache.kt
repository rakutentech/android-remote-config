package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.api.PublicKeyFetcher

internal class PublicKeyCache @VisibleForTesting constructor(
    private val keyFetcher: PublicKeyFetcher,
    context: Context,
    private val encryptor: Encryptor
) {

    constructor(
        keyFetcher: PublicKeyFetcher,
        context: Context
    ) : this(
        keyFetcher,
        context,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            AesEncryptor()
        else
            RsaEncryptor(context)
    )

    private val prefs = context.getSharedPreferences(
        "com.rakuten.tech.mobile.remoteconfig.publickeys",
        Context.MODE_PRIVATE
    )

    operator fun get(keyId: String): String? {
        val encryptedKey = prefs.getString(keyId, null)
            ?: return null

        return encryptor.decrypt(encryptedKey)
    }

    fun remove(keyId: String) {
        prefs.edit()
            .remove(keyId)
            .apply()
    }

    fun fetch(keyId: String): String {
        val key = keyFetcher.fetch(keyId)

        // Key is encrypted to prevent modification in SharedPreferences (i.e. on a rooted device)
        val encryptedKey = encryptor.encrypt(key)

        prefs.edit()
            .putString(keyId, encryptedKey)
            .apply()

        return key
    }
}
