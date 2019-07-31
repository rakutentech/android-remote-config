package com.rakuten.tech.mobile.remoteconfig.verification

internal interface Encryptor {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
}
