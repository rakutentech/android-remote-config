package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.response.readText
import io.ktor.client.response.HttpResponse
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.jvm.Transient


class ConfigFetcher internal constructor(
    private val client: HttpClient,
    baseUrl: String
) {
    private val address = Url("${baseUrl}/config")

    suspend fun fetch(): Config {
        val response = client.get<HttpResponse>(address)

        response.status.isSuccess()
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
