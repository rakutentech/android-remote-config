package com.rakuten.tech.mobile.remoteconfig

import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.header

@Suppress("LongParameterList", "LongMethod")
internal fun createDefaultHttpClient(
    platformClient: HttpClient,
    appId: String,
    subscriptionKey: String,
    deviceModel: String,
    osVersion: String,
    appName: String,
    appVersion: String,
    sdkVersion: String
) = platformClient.config {
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }
    defaultRequest {
        header("apiKey", "ras-$subscriptionKey")
        header("ras-app-id", appId)
        header("ras-device-model", deviceModel)
        header("ras-os-version", osVersion)
        header("ras-sdk-name", "Remote Config")
        header("ras-sdk-version", sdkVersion)
        header("ras-app-name", appName)
        header("ras-app-version", appVersion)
    }
}
