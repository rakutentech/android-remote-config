package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.api.PublicKeyFetcher
import com.rakuten.tech.mobile.remoteconfig.jsonMapAdapter
import java.io.File

internal class PublicKeyCache @VisibleForTesting constructor(
    private val keyFetcher: PublicKeyFetcher,
    private val file: File,
    private val encryptor: Encryptor
) {

    constructor(
        keyFetcher: PublicKeyFetcher,
        context: Context
    ) : this(
        keyFetcher,
        File(
            context.noBackupFilesDir,
            "com.rakuten.tech.mobile.remoteconfig.publickeys.json"
        ),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            AesEncryptor()
        else
            RsaEncryptor(context)
    )

    private val keys = if (file.exists()) {
        val text = file.readText()

        if (text.isNotBlank()) {
            jsonMapAdapter<String, String>()
                .fromJson(text)!!.toMutableMap()
        } else {
            mutableMapOf()
        }
    } else {
        mutableMapOf()
    }

    operator fun get(keyId: String): String? {
        val encryptedKey = keys[keyId]
            ?: return null

        return encryptor.decrypt(encryptedKey)
    }

    fun remove(keyId: String) {
        keys.remove(keyId)

        file.writeText(keys.toJsonString())
    }

    fun fetch(keyId: String): String {
        val key = keyFetcher.fetch(keyId)

        // Key is encrypted to prevent modification of the cached file (i.e. on a rooted device)
        val encryptedKey = encryptor.encrypt(key)
        keys[keyId] = encryptedKey

        file.writeText(keys.toJsonString())

        return key
    }

    private fun Map<String, String>.toJsonString() = jsonMapAdapter<String, String>().toJson(this)
}
