package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Suppress("TooGenericExceptionCaught", "UndocumentedPublicFunction")
internal class ConfigCache @VisibleForTesting constructor(
    fetcher: ConfigFetcher,
    file: File
) {

    constructor(context: Context, fetcher: ConfigFetcher) : this(
        fetcher,
        File(
            context.filesDir,
            "com.rakuten.tech.mobile.remoteconfig.configcache.json"
        )
    )

    private val json = if (file.exists()) file.readText() else ""
    private val config = if (json.isNotBlank())
        Config.fromJsonString(json)
    else Config(hashMapOf())

    init {
        GlobalScope.launch {
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
}

@Serializable
private data class Config(val values: Map<String, String>) {

    fun toJsonString() = Json.stringify(serializer(), this)

    companion object {
        fun fromJsonString(body: String) = Json.nonstrict.parse(serializer(), body)
    }
}
