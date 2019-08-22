package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.api.ConfigFetcher
import com.rakuten.tech.mobile.remoteconfig.api.ConfigResponse
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Suppress("TooGenericExceptionCaught")
internal class ConfigCache @VisibleForTesting constructor(
    fetcher: ConfigFetcher,
    file: File,
    poller: AsyncPoller,
    private val verifier: ConfigVerifier
) {

    constructor(
        context: Context,
        configFetcher: ConfigFetcher,
        verifier: ConfigVerifier,
        poller: AsyncPoller
    ) : this(
        configFetcher,
        File(
            context.filesDir,
            "com.rakuten.tech.mobile.remoteconfig.configcache.json"
        ),
        poller,
        verifier
    )

    private val configBody = if (file.exists()) {
        val config = Config.fromJsonString(file.readText())

        if (verifier.verify(config)) {
            ConfigResponse.fromJsonString(config.rawBody).body
        } else {
            emptyMap()
        }
    } else {
        emptyMap()
    }

    init {
        poller.start {
            try {
                val fetchedConfig = fetcher.fetch()

                verifier.ensureFetchedKey(fetchedConfig.keyId)

                if (verifier.verify(fetchedConfig)) {
                    file.writeText(fetchedConfig.toJsonString())
                }
            } catch (error: Exception) {
                Log.e("RemoteConfig", "Error while fetching config from server", error)
            }
        }
    }

    operator fun get(key: String) = configBody[key]

    fun getConfig(): Map<String, String> = configBody
}

@Serializable
internal data class Config(
    val rawBody: String,
    val signature: String,
    val keyId: String
) {

    fun toJsonString() = Json.stringify(serializer(), this)

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}
