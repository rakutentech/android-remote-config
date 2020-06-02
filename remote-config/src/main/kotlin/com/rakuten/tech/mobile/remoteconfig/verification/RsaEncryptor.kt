package com.rakuten.tech.mobile.remoteconfig.verification

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Calendar
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal

internal class RsaEncryptor @VisibleForTesting constructor(
    private val context: Context,
    private val keyStore: KeyStore,
    private val keyGenerator: RsaKeyGenerator
) : Encryptor {

    constructor(context: Context) : this(
        context = context,
        keyStore = KeyStore.getInstance("AndroidKeyStore"),
        keyGenerator = RsaKeyGenerator(KEYSTORE_ALIAS)
    )

    private val encryptionKey get() = (
        try {
            keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.PrivateKeyEntry?
        } catch (exception: ClassCastException) {
            // Key wasn't an RSA key, so we need to generate a new one
            Log.d(TAG, "Error retrieving key from KeyStore. A new key will be generated.", exception)
            null
        }
    )?.certificate?.publicKey ?: keyGenerator.generateKey(context).public

    private val decryptionKey get() = (
        keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.PrivateKeyEntry
    ).privateKey

    init {
        keyStore.load(null)
    }

    override fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

        return Base64.encodeToString(encryptedData, Base64.DEFAULT)
    }

    override fun decrypt(data: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey)
        val decryptedKey = cipher.doFinal(Base64.decode(data, Base64.DEFAULT))

        return String(decryptedKey)
    }

    companion object {
        private const val KEYSTORE_ALIAS = "remote-config-public-key-encryption-decryption"
        private const val CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private const val TAG = "RC_RSA"
    }
}

internal class RsaKeyGenerator(
    private val alias: String
) {

    fun generateKey(context: Context): KeyPair {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        end.add(Calendar.YEAR, CERTIFICATE_DURATION_YEARS)
        val spec = KeyPairGeneratorSpec.Builder(context)
            .setAlias(alias)
            .setSubject(X500Principal("CN=Rakuten Inc"))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()
        val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
        generator.initialize(spec)
        return generator.generateKeyPair()
    }

    companion object {
        private const val CERTIFICATE_DURATION_YEARS = 10
    }
}
