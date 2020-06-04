package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.api.ConfigFetcher
import com.rakuten.tech.mobile.remoteconfig.api.ConfigResponse
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

@Suppress("TooGenericExceptionCaught")
internal class ConfigCache @VisibleForTesting constructor(
    private val fetcher: ConfigFetcher,
    private val file: File,
    private val poller: AsyncPoller,
    private val verifier: ConfigVerifier,
    private val applyDirectly: Boolean,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
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
        poller.start {
            try {
                fetchConfig()

                if (applyDirectly) {
                    configBody = applyConfig()
                }
            } catch (error: Exception) {
                Log.e(TAG, "Error while fetching config from server", error)
            }
        }
    }

    operator fun get(key: String) = configBody[key]

    fun getConfig(): Map<String, String> = configBody

    suspend fun fetchAndApplyConfig() = withContext(dispatcher) {
        fetchConfig()
        configBody = applyConfig()
        poller.reset() // stop current polled fetch, then restart after interval.
        configBody
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

    private fun fetchConfig() {
        val fetchedConfig = fetcher.fetch()

        verifier.ensureFetchedKey(fetchedConfig.keyId)

        if (verifier.verify(fetchedConfig)) {
            file.writeText(fetchedConfig.toJsonString())
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
