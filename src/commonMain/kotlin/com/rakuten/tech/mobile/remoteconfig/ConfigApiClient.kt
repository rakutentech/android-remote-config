package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.header
import io.ktor.client.response.HttpResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.Transient

internal expect val ApplicationDispatcher: CoroutineDispatcher

class ConfigApiClient internal constructor(
    private val baseUrl: String,
    private val appId: String,
    private val subscriptionKey: String,
    private val scope: CoroutineScope
) {

    constructor(
        baseUrl: String,
        appId: String,
        subscriptionKey: String
    ) : this (
        baseUrl = baseUrl,
        appId = appId,
        subscriptionKey = subscriptionKey,
        scope = CoroutineScope(ApplicationDispatcher)
    )

    private val url = "${baseUrl}/app/$appId"
    private val client = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        defaultRequest {
            header("apiKey" , "ras-$subscriptionKey")
        }
    }

    fun fetchConfig(response: (Config) -> Unit, error: (Exception) -> Unit) {
        scope.launch {
            try {
                val config = ConfigFetcher(
                    client = client,
                    baseUrl = url
                ).fetch()
                response(config)
            } catch (exception: ResponseException) {
                error(exception)
            }
        }
    }

    companion object {
        private const val CACHE_SIZE = 1024 * 1024 * 2L
        private const val HTTP_STATUS_300 = 300
    }
}

/**
 * Base for default response exceptions.
 * @param response: origin response
 */
open class ResponseException(
    @Transient val response: HttpResponse
) : IllegalStateException("Bad response: ${response.status}")
