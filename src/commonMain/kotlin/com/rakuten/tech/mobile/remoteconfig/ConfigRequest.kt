package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.response.readText
import io.ktor.client.response.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ConfigRequest internal constructor(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val appId: String
) {

    suspend fun fetch(): Config {
        val response = httpClient.get<HttpResponse>("${baseUrl}/app/$appId/config")

        if (!response.status.isSuccess()) {
            throw ResponseException(response)
        }

        val body = response.readText()
            .trim() // OkHttp sometimes adds an extra newline character when caching the response
        val (_body, keyId) = ConfigResponse.fromJsonString(body)
        val signature = response.headers["Signature"] ?: ""

        return Config(body, signature, keyId)
    }
}

@Serializable
internal data class ConfigResponse(
    val body: Map<String, String>,
    val keyId: String
) {

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}

@Serializable
data class Config(
    val rawBody: String,
    val signature: String,
    val keyId: String
) {

    fun toJsonString() = Json.stringify(serializer(), this)

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}
