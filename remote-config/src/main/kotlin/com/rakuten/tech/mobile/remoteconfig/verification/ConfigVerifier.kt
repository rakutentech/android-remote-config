package com.rakuten.tech.mobile.remoteconfig.verification

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.Config

internal class ConfigVerifier @VisibleForTesting constructor(
    private val cache: PublicKeyCache,
    private val signatureVerifier: SignatureVerifier
) {

    constructor(cache: PublicKeyCache) : this(cache, SignatureVerifier())

    @Suppress("TooGenericExceptionCaught")
    fun verify(config: Config): Boolean {
        return try {
            val publicKey = cache[config.keyId] ?: return false
            val isVerified = signatureVerifier.verify(
                publicKey,
                config.rawBody.toByteArray(Charsets.UTF_8).inputStream(),
                config.signature
            )

            if (isVerified) true else throw InvalidSignatureException()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to verify signature of config.", exception)
            cache.remove(config.keyId)
            false
        }
    }

    fun ensureFetchedKey(keyId: String) = cache[keyId] ?: cache.fetch(keyId)

    companion object {
        private const val TAG = "RC_ConfigVerifier"
    }
}

internal class InvalidSignatureException : Exception("Signature was invalid.")
