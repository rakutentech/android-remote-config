package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.response.readText
import io.ktor.client.response.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PublicKeyRequest internal constructor(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {

    suspend fun fetch(keyId: String): String {
        val response = httpClient.get<HttpResponse>("${baseUrl}/keys/$keyId")

        if (!response.status.isSuccess()) {
            throw ResponseException(response)
        }

        val body = response.readText()

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
