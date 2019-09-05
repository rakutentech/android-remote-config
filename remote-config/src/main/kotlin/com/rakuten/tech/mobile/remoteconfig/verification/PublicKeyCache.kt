package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.api.PublicKeyFetcher
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
import java.io.File

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

    private val file = File(
        context.noBackupFilesDir,
        "com.rakuten.tech.mobile.remoteconfig.publickeys.json"
    )
    private val keys = if (file.exists()) {
        val text = file.readText()

        if (text.isNotBlank()) {
            parseJson(text).toMutableMap()
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

    private fun parseJson(json: String) = Json.nonstrict.parse(
        (StringSerializer to StringSerializer).map,
        json
    )

    private fun Map<String, String>.toJsonString() = Json.nonstrict.stringify(
        (StringSerializer to StringSerializer).map,
        this
    )
}
