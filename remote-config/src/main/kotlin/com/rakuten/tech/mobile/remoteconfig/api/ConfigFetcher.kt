package com.rakuten.tech.mobile.remoteconfig.api

import com.rakuten.tech.mobile.remoteconfig.Config
import com.rakuten.tech.mobile.remoteconfig.SignatureVerifier
import com.rakuten.tech.mobile.remoteconfig.toInputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import java.io.IOException

internal class ConfigFetcher constructor(
    private val client: ConfigApiClient,
    private val appId: String,
    private val verifier: SignatureVerifier
) {

    fun fetch(): Config {
        val response = client.fetchPath("app/$appId/config")

        if (!response.isSuccessful) {
            throw IOException("Unexpected response when fetching remote config: $response")
        }

        val (body, keyId) = ConfigResponse.fromJsonString(
            response.body()!!.string() // Body is never null if request is successful
        )
        val signature = response.header("Signature") ?: ""

        val isVerified = verifier.verifyFetched(keyId, body.toInputStream(), signature)
        if (!isVerified) {
            throw IOException("Failed to verify signature of the payload from server.")
        }

        return Config(body, signature, keyId)
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
