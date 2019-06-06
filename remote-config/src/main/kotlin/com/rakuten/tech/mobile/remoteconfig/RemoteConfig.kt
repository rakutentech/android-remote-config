package com.rakuten.tech.mobile.remoteconfig

/**
 * Main entry point for the Remote Config SDK.
 * Should be accessed via [RemoteConfig.instance].
 */
@Suppress("UnnecessaryAbstractClass")
abstract class RemoteConfig internal constructor() {

    /**
     * Get a string from the Cached config.
     * If the key does not exist, [fallback] will be returned.
     *
     * @param key returns value for this key in the config
     * @param fallback returned when the key does not exist
     * @return String value for the specified key
     */
    abstract fun getString(key: String, fallback: String): String

    companion object {
        private var instance: RemoteConfig = NotInitialzedRemoteConfig()

        /**
         * Instance of [RemoteConfig].
         *
         * @return [RemoteConfig] instance
         */
        @JvmStatic
        fun instance(): RemoteConfig = instance

        internal fun init(cache: ConfigCache) {
            instance = RealRemoteConfig(cache)
        }
    }
}

internal class NotInitialzedRemoteConfig : RemoteConfig() {

    override fun getString(key: String, fallback: String) = fallback
}
