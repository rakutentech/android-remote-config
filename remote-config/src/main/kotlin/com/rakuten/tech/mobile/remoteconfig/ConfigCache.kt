package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.api.ConfigFetcher
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

    constructor(context: Context, configFetcher: ConfigFetcher, verifier: ConfigVerifier) : this(
        configFetcher,
        File(
            context.filesDir,
            "com.rakuten.tech.mobile.remoteconfig.configcache.json"
        ),
        AsyncPoller(DELAY_IN_MINUTES),
        verifier
    )

    private val config = if (file.exists()) {
        val config = Config.fromJsonString(file.readText())

        if (verifier.verify(config)) {
            config
        } else {
            emptyConfig()
        }
    } else {
        emptyConfig()
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

    operator fun get(key: String) = config.values[key]

    fun getConfig(): Map<String, String> = config.values

    private fun emptyConfig() = Config(emptyMap(), "", "")

    companion object {
        const val DELAY_IN_MINUTES: Int = 60
    }
}

@Serializable
internal data class Config(
    val values: Map<String, String>,
    val signature: String,
    val keyId: String
) {

    fun toJsonString() = Json.stringify(serializer(), this)

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}
