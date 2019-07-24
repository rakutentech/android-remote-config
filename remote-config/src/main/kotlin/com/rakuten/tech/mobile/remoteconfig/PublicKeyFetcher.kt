package com.rakuten.tech.mobile.remoteconfig

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class PublicKeyFetcher(
    private val baseUrl: String,
    private val client: OkHttpClient
) {

    fun fetch(keyId: String): String {
        val response = client.newCall(buildFetchRequest(keyId))
            .execute()

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching public key: $response")
        }

        val body = response.body()!!.string()

        return PublicKeyResponse.fromJsonString(body).key
    }

    private fun buildFetchRequest(keyId: String) = Request.Builder()
        .url(HttpUrl.get(baseUrl)
            .newBuilder()
            .addPathSegments("keys/$keyId")
            .build()
        )
        .build()
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
