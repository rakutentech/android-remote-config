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
    private val platformClient: HttpClient,
    private val baseUrl: String,
    private val appId: String,
    private val subscriptionKey: String,
    private val scope: CoroutineScope
) {

    constructor(
        platformClient: HttpClient,
        baseUrl: String,
        appId: String,
        subscriptionKey: String
    ) : this (
        platformClient = platformClient,
        baseUrl = baseUrl,
        appId = appId,
        subscriptionKey = subscriptionKey,
        scope = CoroutineScope(ApplicationDispatcher)
    )

    private val url = "${baseUrl}/app/$appId"
    private val client = platformClient.config {
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
}

/**
 * Base for default response exceptions.
 * @param response: origin response
 */
open class ResponseException(
    @Transient val response: HttpResponse
) : IllegalStateException("Bad response: ${response.status}")
