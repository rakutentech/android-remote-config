package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Suppress("TooGenericExceptionCaught")
internal class ConfigCache @VisibleForTesting constructor(
    fetcher: ConfigFetcher,
    file: File,
    poller: AsyncPoller
) {

    constructor(context: Context, fetcher: ConfigFetcher) : this(
        fetcher,
        File(
            context.filesDir,
            "com.rakuten.tech.mobile.remoteconfig.configcache.json"
        ),
        AsyncPoller(DELAY_IN_MINUTES)
    )

    private val config = if (file.exists()) {
        Config.fromJsonString(file.readText())
    } else {
        Config(emptyMap())
    }

    init {
        poller.start {
            try {
                val fetchedConfig = fetcher.fetch()
                val configJson = Config(fetchedConfig).toJsonString()

                file.writeText(configJson)
            } catch (error: Exception) {
                Log.e("RemoteConfig", "Error while fetching config from server", error)
            }
        }
    }

    operator fun get(key: String) = config.values[key]

    fun getConfig(): Map<String, String> = config.values

    companion object {
        const val DELAY_IN_MINUTES: Int = 60
    }
}

@Serializable
private data class Config(val values: Map<String, String>) {

    fun toJsonString() = Json.stringify(serializer(), this)

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}
