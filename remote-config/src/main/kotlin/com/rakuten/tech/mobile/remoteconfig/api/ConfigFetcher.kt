package com.rakuten.tech.mobile.remoteconfig.api

import com.rakuten.tech.mobile.remoteconfig.Config
import com.rakuten.tech.mobile.remoteconfig.jsonAdapter
import com.squareup.moshi.JsonClass
import java.io.IOException

internal class ConfigFetcher constructor(
    private val client: ConfigApiClient,
    private val appId: String
) {

    fun fetch(): Config {
        val response = client.fetchPath("app/$appId/config", true)

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching remote config: $response")
        }

        val body = response.body()!!.string()
                .trim() // OkHttp sometimes adds an extra newline character when caching the response

        val (_body, keyId) = ConfigResponse.fromJsonString(body)!!
        val signature = response.header("Signature") ?: ""

        return Config(body, signature, keyId)
    }
}

@JsonClass(generateAdapter = true)
internal data class ConfigResponse(
    val body: Map<String, String>,
    val keyId: String
) {
    companion object {
        fun fromJsonString(body: String) = jsonAdapter<ConfigResponse>().fromJson(body)!!
    }
}
