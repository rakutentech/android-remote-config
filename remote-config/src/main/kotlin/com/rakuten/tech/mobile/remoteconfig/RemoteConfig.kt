package com.rakuten.tech.mobile.remoteconfig

/**
 * Main entry point for the Remote Config SDK.
 * Should be accessed via [RemoteConfig.instance].
 */
@Suppress("UnnecessaryAbstractClass")
abstract class RemoteConfig internal constructor() {

    companion object {
        private var instance: RemoteConfig = NotInitialzedRemoteConfig()

        /**
         * Instance of [RemoteConfig].
         *
         * @return [RemoteConfig] instance
         */
        @JvmStatic
        fun instance(): RemoteConfig = instance

        internal fun init() {
            instance = RealRemoteConfig()
        }

    }
}

internal class NotInitialzedRemoteConfig: RemoteConfig() {

}
