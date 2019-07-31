package com.rakuten.tech.mobile.remoteconfig.verification

import android.util.Base64
import java.io.InputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

internal class SignatureVerifier {

    fun verify(publicKey: String, message: InputStream, signature: String): Boolean {
        return Signature.getInstance("SHA256withECDSA")
            .apply {
                initVerify(rawToEncodedECPublicKey(publicKey))

                val buffer = ByteArray(SIXTEEN_KILOBYTES)
                var read = message.read(buffer)
                while (read != -1) {
                    update(buffer, 0, read)

                    read = message.read(buffer)
                }
            }
            .verify(Base64.decode(signature, Base64.DEFAULT))
    }

    private fun rawToEncodedECPublicKey(key: String): ECPublicKey {
        val parameters = ecParameterSpecForCurve("secp256r1")
        val keySizeBytes = parameters.order.bitLength() / java.lang.Byte.SIZE
        val pubKey = Base64.decode(key, Base64.DEFAULT)

        // First Byte represents compressed/uncompressed status
        // We're expecting it to always be uncompressed (04)
        var offset = UNCOMPRESSED_OFFSET
        val x = BigInteger(POSITIVE_BIG_INTEGER, pubKey.copyOfRange(offset, offset + keySizeBytes))

        offset += keySizeBytes
        val y = BigInteger(POSITIVE_BIG_INTEGER, pubKey.copyOfRange(offset, offset + keySizeBytes))

        val keySpec = ECPublicKeySpec(ECPoint(x, y), parameters)
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory
            .generatePublic(keySpec) as ECPublicKey
    }

    private fun ecParameterSpecForCurve(curveName: String): ECParameterSpec {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(curveName))

        return (kpg.generateKeyPair().public as ECPublicKey).params
    }

    companion object {
        private const val SIXTEEN_KILOBYTES = 16 * 1024

        private const val UNCOMPRESSED_OFFSET = 1
        private const val POSITIVE_BIG_INTEGER = 1
    }
}
