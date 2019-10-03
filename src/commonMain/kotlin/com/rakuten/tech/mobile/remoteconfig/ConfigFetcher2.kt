package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.response.readText
import io.ktor.client.response.HttpResponse
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.jvm.Transient

class ConfigFetcher2 constructor(
    baseUrl: String,
    appId: String,
    private val subscriptionKey: String
) {

    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }
    var address = Url("${baseUrl}app/$appId/config")

    suspend fun fetch(): Config {
        val response = client.get<HttpResponse>(address) {
            header("apiKey", "ras-$subscriptionKey")
        }

        if (response.status.value >= HTTP_STATUS_300) {
            throw ResponseException(response)
        }

        val body = response.readText()
            .trim() // OkHttp sometimes adds an extra newline character when caching the response
        val (_body, keyId) = ConfigResponse.fromJsonString(body)
        val signature = response.headers["Signature"] ?: ""

        return Config(body, signature, keyId)
    }

    companion object {
        private const val CACHE_SIZE = 1024 * 1024 * 2L
        private const val HTTP_STATUS_300 = 300
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

/**
 * Base for default response exceptions.
 * @param response: origin response
 */
open class ResponseException(
    @Transient val response: HttpResponse
) : IllegalStateException("Bad response: ${response.status}")
