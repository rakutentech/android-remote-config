package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.api.ConfigFetcher
import com.rakuten.tech.mobile.remoteconfig.api.ConfigResponse
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import com.squareup.moshi.JsonClass
import java.io.File

@Suppress("TooGenericExceptionCaught")
internal class ConfigCache @VisibleForTesting constructor(
    private val fetcher: ConfigFetcher,
    private val file: File,
    private val poller: AsyncPoller,
    private val verifier: ConfigVerifier,
    private val applyDirectly: Boolean
) {

    constructor(
        context: Context,
        configFetcher: ConfigFetcher,
        verifier: ConfigVerifier,
        poller: AsyncPoller,
        applyDirectly: Boolean
    ) : this(
        configFetcher,
        File(
            context.noBackupFilesDir,
            "com.rakuten.tech.mobile.remoteconfig.configcache.json"
        ),
        poller,
        verifier,
        applyDirectly
    )

    private var configBody = applyConfig()

    init {
        startPoller(null)
    }

    operator fun get(key: String) = configBody[key]

    fun getConfig(): Map<String, String> = configBody

    fun fetchConfig(listener: FetchConfigCompletionListener) {
        // For resetting the polling delay.
        poller.stop()
        startPoller(listener)
    }

    private fun parseConfigBody(fileText: String) = try {
        val config = Config.fromJsonString(fileText)

        if (verifier.verify(config)) {
            ConfigResponse.fromJsonString(config.rawBody).body
        } else {
            null
        }
    } catch (exception: Exception) {
        Log.e(TAG, "Error parsing config from cached file", exception)
        null
    }

    private fun applyConfig() = if (file.exists()) {
        val text = file.readText()

        if (text.isNotBlank()) {
            parseConfigBody(text) ?: emptyMap()
        } else {
            emptyMap()
        }
    } else {
        emptyMap()
    }

    private fun startPoller(listener: FetchConfigCompletionListener?) {
        poller.start {
            try {
                val fetchedConfig = fetcher.fetch()

                verifier.ensureFetchedKey(fetchedConfig.keyId)

                if (verifier.verify(fetchedConfig)) {
                    file.writeText(fetchedConfig.toJsonString())
                }

                if (listener != null) {
                    // if not null, this execution is from manual trigger.
                    configBody = applyConfig()
                    listener.onFetchComplete(configBody)
                } else if (applyDirectly) {
                    configBody = applyConfig()
                }
            } catch (error: Exception) {
                Log.e(TAG, "Error while fetching config from server", error)
                listener?.onFetchError(error)
            }
        }
    }

    companion object {
        private const val TAG = "RC_ConfigCache"
    }
}

@JsonClass(generateAdapter = true)
internal data class Config(
    val rawBody: String,
    val signature: String,
    val keyId: String
) {

    fun toJsonString() = jsonAdapter<Config>().toJson(this)!!

    companion object {
        fun fromJsonString(body: String) = jsonAdapter<Config>().fromJson(body)!!
    }
}
