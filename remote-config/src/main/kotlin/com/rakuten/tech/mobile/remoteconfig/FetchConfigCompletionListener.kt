package com.rakuten.tech.mobile.remoteconfig

/**
 * Fetch config completion listener interface for manual trigger.
 */
interface FetchConfigCompletionListener {
    /**
     * This callback is called when an error is encountered during config fetch.
     *
     * @param ex contains the encountered error details
     */
    fun onFetchError(ex: Exception)

    /**
     * This callback is called when config fetch is completed without error.
     *
     * @param config contains all keys/values in the fetched config
     */
    fun onFetchComplete(config: Map<String, String>)
}