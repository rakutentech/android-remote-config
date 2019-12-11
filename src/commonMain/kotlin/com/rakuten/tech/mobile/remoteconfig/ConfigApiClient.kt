package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.Transient

internal expect val ApplicationDispatcher: CoroutineDispatcher

/**
 * API Client used for fetching config and public key.
 */
class ConfigApiClient(
    platformClient: HttpClient,
    baseUrl: String,
    private val appId: String,
    subscriptionKey: String,
    deviceModel: String,
    osVersion: String,
    appName: String,
    appVersion: String,
    sdkVersion: String
) {

    private val baseUrl = baseUrl.trimEnd { it == '/' }
    private val scope = CoroutineScope(ApplicationDispatcher)
    private val client = createDefaultHttpClient(
        platformClient = platformClient,
        appId = appId,
        subscriptionKey = subscriptionKey,
        deviceModel = deviceModel,
        osVersion = osVersion,
        appName = appName,
        appVersion = appVersion,
        sdkVersion = sdkVersion
    )

    /**
     * Fetch config response.
     *
     * @param success callback when request is successful
     * @param error callback when request fails
     * @returns Config response
     */
    fun fetchConfig(
        success: (Config) -> Unit,
        error: (Exception) -> Unit
    ) = fetchConfig(
        success = success,
        error = error,
        request = ConfigRequest(
            httpClient = client,
            baseUrl = baseUrl,
            appId = appId
        )
    )

    internal fun fetchConfig(
        success: (Config) -> Unit,
        error: (Exception) -> Unit,
        request: ConfigRequest
    ) {
        scope.launch {
            try {
                val config = request.fetch()
                success(config)
            } catch (exception: ResponseException) {
                error(exception)
            }
        }
    }

    /**
     * Fetch public key for key id.
     *
     * @param keyId id of public key that should be fetched
     * @param success callback when request is successful
     * @param error callback when request fails
     * @returns public key
     */
    fun fetchPublicKey(
        keyId: String,
        success: (String) -> Unit,
        error: (Exception) -> Unit
    ) = fetchPublicKey(
        keyId = keyId,
        success = success,
        error = error,
        request = PublicKeyRequest(
            httpClient = client,
            baseUrl = baseUrl
        )
    )

    internal fun fetchPublicKey(
        keyId: String,
        success: (String) -> Unit,
        error: (Exception) -> Unit,
        request: PublicKeyRequest
    ) {
        scope.launch {
            try {
                val key = request.fetch(keyId)
                success(key)
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
