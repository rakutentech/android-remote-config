package com.rakuten.tech.mobile.remoteconfig.verification

import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal fun generateAesKey(): SecretKey {
    val kgen = KeyGenerator.getInstance("AES")
    kgen.init(256)
    return kgen.generateKey()
}

internal fun generateRsaKey(): KeyPair {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(512)
    return generator.generateKeyPair()
}
