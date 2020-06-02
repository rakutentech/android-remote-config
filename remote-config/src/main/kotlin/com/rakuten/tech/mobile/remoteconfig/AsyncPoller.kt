package com.rakuten.tech.mobile.remoteconfig

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

internal class AsyncPoller @VisibleForTesting constructor(
    delayInSeconds: Int,
    private val scope: CoroutineScope
) {

    constructor(delayInSeconds: Int) : this(delayInSeconds, GlobalScope)

    private val delayInMilliseconds = TimeUnit.SECONDS.toMillis(delayInSeconds.toLong())

    fun start(method: () -> Unit) {
        scope.launch {
            repeat(Int.MAX_VALUE) {
                method.invoke()

                delay(delayInMilliseconds)
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}
