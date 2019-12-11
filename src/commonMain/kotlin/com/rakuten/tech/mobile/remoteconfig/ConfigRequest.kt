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
        val response: HttpResponse = httpClient.get("${baseUrl}/app/$appId/config")

        if (!response.status.isSuccess()) {
            throw ResponseException(response)
        }

        val rawBody = response.readText()
            .trim() // OkHttp sometimes adds an extra newline character when caching the response
        val (body, keyId) = ConfigResponse.fromJsonString(rawBody)
        val signature = response.headers["Signature"] ?: ""

        return Config(
            rawBody = rawBody,
            body = body,
            signature = signature,
            keyId = keyId
        )
    }
}

@Serializable
private data class ConfigResponse(
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
    val body: Map<String, String>,
    val signature: String,
    val keyId: String
) {

    fun toJsonString() = Json.stringify(serializer(), this)

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}
