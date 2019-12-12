package com.rakuten.tech.mobile.remoteconfig

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Suppress("TooGenericExceptionCaught")
internal class ConfigCache @VisibleForTesting constructor(
    configApi: ConfigApiClient,
    file: File,
    poller: AsyncPoller,
    private val verifier: ConfigVerifier
) {

    constructor(
        context: Context,
        configApi: ConfigApiClient,
        verifier: ConfigVerifier,
        poller: AsyncPoller
    ) : this(
        configApi,
        File(
            context.noBackupFilesDir,
            "com.rakuten.tech.mobile.remoteconfig.configcache.json"
        ),
        poller,
        verifier
    )

    private val configBody = if (file.exists()) {
        val text = file.readText()

        if (text.isNotBlank()) {
            parseConfigBody(text) ?: emptyMap()
        } else {
            emptyMap()
        }
    } else {
        emptyMap()
    }

    init {
        poller.start {
            try {
                val config = suspendCoroutine<Config> { continuation ->
                    configApi.fetchConfig(
                        { continuation.resume(it) },
                        { continuation.resumeWithException(it) }
                    )
                }

                verifier.ensureFetchedKey(config.keyId)

                if (verifier.verify(config)) {
                    file.writeText(config.toJsonString())
                }
            } catch (error: Exception) {
                Log.e("RemoteConfig", "Error while fetching config from server", error)
            }
        }
    }

    operator fun get(key: String) = configBody[key]

    fun getConfig(): Map<String, String> = configBody

    private fun parseConfigBody(fileText: String) = try {
        val config = Config.fromJsonString(fileText)

        if (verifier.verify(config)) {
            config.body
        } else {
            null
        }
    } catch (exception: Exception) {
        Log.e("RemoteConfig", "Error parsing config from cached file", exception)
        null
    }
}


