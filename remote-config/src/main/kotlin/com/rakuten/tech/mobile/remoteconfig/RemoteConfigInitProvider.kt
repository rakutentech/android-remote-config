package com.rakuten.tech.mobile.remoteconfig

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.rakuten.tech.mobile.manifestconfig.annotations.ManifestConfig
import com.rakuten.tech.mobile.manifestconfig.annotations.MetaData

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
        @MetaData(key = "com.rakuten.tech.mobile.relay.AppId")
        fun appId(): String

        /**
         * Subscription Key for the Remote Config API.
         **/
        @MetaData(key = "com.rakuten.tech.mobile.relay.SubscriptionKey")
        fun subscriptionKey(): String
    }

    override fun onCreate(): Boolean {
        val context = context ?: return false

        RemoteConfig.init()

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
