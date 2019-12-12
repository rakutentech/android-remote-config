package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.ConfigApiClient
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class PublicKeyCache @VisibleForTesting constructor(
    private val apiClient: ConfigApiClient,
    private val file: File,
    private val encryptor: Encryptor
) {

    constructor(
        apiClient: ConfigApiClient,
        context: Context
    ) : this(
        apiClient,
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

    suspend fun fetch(keyId: String): String {
        val key = suspendCoroutine<String> { continuation ->
            apiClient.fetchPublicKey(
                keyId,
                { continuation.resume(it) },
                { continuation.resumeWithException(it) }
            )
        }

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
