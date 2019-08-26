package com.rakuten.tech.mobile.remoteconfig.api

import com.rakuten.tech.mobile.remoteconfig.Config
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import java.io.IOException

internal class ConfigFetcher constructor(
    private val client: ConfigApiClient,
    private val appId: String
) {

    fun fetch(): Config {
        val response = client.fetchPath("app/$appId/config")

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching remote config: $response")
        }

        val body = response.body()!!.string()
        val (_body, keyId) = ConfigResponse.fromJsonString(body)
        val signature = response.header("Signature") ?: ""

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
