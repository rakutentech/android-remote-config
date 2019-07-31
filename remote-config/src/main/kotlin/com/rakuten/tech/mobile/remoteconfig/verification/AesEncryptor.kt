package com.rakuten.tech.mobile.remoteconfig.verification

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@RequiresApi(Build.VERSION_CODES.M)
internal class AesEncryptor @VisibleForTesting constructor(
    private val keyStore: KeyStore,
    private val keyGenerator: AesKeyGenerator
) : Encryptor {

    constructor() : this(
        keyStore = KeyStore.getInstance("AndroidKeyStore"),
        keyGenerator = AesKeyGenerator(alias = KEYSTORE_ALIAS, provider = "AndroidKeyStore")
    )

    private val encryptionKey get() = (
        try {
            keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry?
        } catch (exception: ClassCastException) {
            // Key wasn't an AES key, so we need to generate a new one
            Log.d(
                "Remote Config",
                "Error retrieving key from KeyStore. A new key will be generated.",
                exception
            )
            null
        }
    )?.secretKey ?: keyGenerator.generateKey()

    init {
        keyStore.load(null)
    }

    override fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        return AesEncryptedData(
            Base64.encodeToString(cipher.iv, Base64.DEFAULT),
            Base64.encodeToString(encryptedData, Base64.DEFAULT)
        ).toJsonString()
    }

    override fun decrypt(data: String): String {
        val (iv, encryptedData) = AesEncryptedData.fromJsonString(data)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, Base64.decode(iv, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec)

        val decryptedData = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT))

        return String(decryptedData)
    }

    companion object {
        private const val KEYSTORE_ALIAS = "remote-config-public-key-encryption-decryption"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }
}

@RequiresApi(Build.VERSION_CODES.M)
internal class AesKeyGenerator(
    private val alias: String,
    private val provider: String
) {

    fun generateKey(): SecretKey {
        val algorithmSpec = KeyGenParameterSpec.Builder(alias, PURPOSE)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)

        keyGenerator.init(algorithmSpec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val PURPOSE = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    }
}

@Serializable
private data class AesEncryptedData(
    val iv: String,
    val encryptedData: String
) {

    fun toJsonString() = Json.stringify(serializer(), this)

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}
