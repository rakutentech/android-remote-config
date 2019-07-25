package com.rakuten.tech.mobile.remoteconfig.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

internal class PublicKeyFetcher(private val client: ConfigApiClient) {

    fun fetch(keyId: String): String {
        val response = client.fetchPath("keys/$keyId")

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching public key: $response")
        }

        val body = response.body()!!.string()

        return PublicKeyResponse.fromJsonString(
            body
        ).key
    }
}

@Serializable
private data class PublicKeyResponse(
    val id: String,
    val key: String,
    val createdAt: String
) {

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}
