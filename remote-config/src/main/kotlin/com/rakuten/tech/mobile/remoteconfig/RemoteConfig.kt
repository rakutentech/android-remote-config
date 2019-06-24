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

    /**
     * Get a boolean from the Cached config.
     * If the key does not exist, [fallback] will be returned.
     *
     * @param key returns value for this key in the config
     * @param fallback returned when the key does not exist
     * @return Boolean value for the specified key
     */
    abstract fun getBoolean(key: String, fallback: Boolean): Boolean

    /**
     * Get a number from the Cached config.
     * If the key does not exist, [fallback] will be returned.
     * Number will attempt to be converted to type [T]. If the conversion fails
     * (i.e. the value in the cached config is a not a number) then [fallback] will be returned.
     *
     * @param key returns value for this key in the config
     * @param fallback returned when the key does not exist or cached value cannot be converted
     * to number
     * @return [T] value for the specified key. [T] must be a subtype of [Number] such as Int,
     * Long, Short, Float, Double, or Byte
     */
    abstract fun <T : Number> getNumber(key: String, fallback: T): T

    /**
     * Get the cached config.
     *
     * @return [Map] contains all keys/values in the cached config
     */
    abstract fun getConfig(): Map<String, String>

    companion object {
        private var instance: RemoteConfig = NotInitializedRemoteConfig()

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

internal class NotInitializedRemoteConfig : RemoteConfig() {

    override fun getString(key: String, fallback: String) = fallback
    override fun getBoolean(key: String, fallback: Boolean): Boolean = fallback
    override fun <T : Number> getNumber(key: String, fallback: T) = fallback
    override fun getConfig() = emptyMap<String, String>()
}
