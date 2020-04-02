package com.rakuten.tech.mobile.remoteconfig

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.rakuten.tech.mobile.manifestconfig.annotations.ManifestConfig
import com.rakuten.tech.mobile.manifestconfig.annotations.MetaData
import com.rakuten.tech.mobile.remoteconfig.api.ConfigApiClient
import com.rakuten.tech.mobile.remoteconfig.api.ConfigFetcher
import com.rakuten.tech.mobile.remoteconfig.api.PublicKeyFetcher
import com.rakuten.tech.mobile.remoteconfig.verification.ConfigVerifier
import com.rakuten.tech.mobile.remoteconfig.verification.PublicKeyCache

/**
 * Fake ContentProvider that initializes the Remote Config SDK.
 *
 * @suppress
**/
@Suppress("UndocumentedPublicClass")
class RemoteConfigInitProvider : ContentProvider() {

    @ManifestConfig
    interface App {

        /**
         * Base Url for the Remote Config API.
         **/
        @MetaData(key = "com.rakuten.tech.mobile.remoteconfig.BaseUrl")
        fun baseUrl(): String

        /**
         * App Id assigned to this App.
         **/
        @MetaData(key = "com.rakuten.tech.mobile.ras.AppId")
        fun appId(): String

        /**
         * Subscription Key for the Remote Config API.
         **/
        @MetaData(key = "com.rakuten.tech.mobile.ras.ProjectSubscriptionKey")
        fun subscriptionKey(): String

        /**
         * Delay in minutes between polls to the Config API.
         * Set to 60 minutes by default.
         **/
        @MetaData(
            key = "com.rakuten.tech.mobile.ras.PollingDelay",
            value = "60"
        )
        fun pollingDelay(): Int
    }

    @Suppress("LongMethod")
    override fun onCreate(): Boolean {
        val context = context ?: return false

        val manifestConfig = AppManifestConfig(context)

        val client = ConfigApiClient(
            baseUrl = manifestConfig.baseUrl(),
            appId = manifestConfig.appId(),
            subscriptionKey = manifestConfig.subscriptionKey(),
            context = context
        )
        val verifier = ConfigVerifier(
            PublicKeyCache(
                keyFetcher = PublicKeyFetcher(client),
                context = context
            )
        )
        val configFetcher = ConfigFetcher(
            appId = manifestConfig.appId(),
            client = client,
            context = context
        )
        val cache = ConfigCache(
            context = context,
            configFetcher = configFetcher,
            verifier = verifier,
            poller = AsyncPoller(manifestConfig.pollingDelay())
        )

        RemoteConfig.init(cache)

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
}
