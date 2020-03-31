package com.rakuten.tech.mobile.remoteconfig.api

import com.rakuten.tech.mobile.remoteconfig.jsonAdapter
import com.squareup.moshi.JsonClass
import java.io.IOException

internal class PublicKeyFetcher(private val client: ConfigApiClient) {

    fun fetch(keyId: String): String {
        val response = client.fetchPath("keys/$keyId", false)

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching public key: $response")
        }

        val body = response.body()!!.string() // Body is never null if request is successful

        return PublicKeyResponse.fromJsonString(body).key
    }
}

@JsonClass(generateAdapter = true)
internal data class PublicKeyResponse(
    val id: String,
    val key: String,
    val createdAt: String
) {
    companion object {
        fun fromJsonString(body: String) = jsonAdapter<PublicKeyResponse>().fromJson(body)!!
    }
}
