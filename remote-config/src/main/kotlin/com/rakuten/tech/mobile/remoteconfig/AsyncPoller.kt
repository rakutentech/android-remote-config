package com.rakuten.tech.mobile.remoteconfig

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

internal class AsyncPoller @VisibleForTesting constructor(
    delayInSeconds: Int,
    private val scope: CoroutineScope
) {

    constructor(delayInSeconds: Int) : this(delayInSeconds, GlobalScope)

    private val delayInMilliseconds = TimeUnit.SECONDS.toMillis(delayInSeconds.toLong())
    private var job: Job? = null
    private var method: (() -> Unit)? = null

    fun start(method: () -> Unit) {
        this.method = method
        job = scope.launch {
            repeat(Int.MAX_VALUE) {
                method.invoke()

                delay(delayInMilliseconds)
            }
        }
    }

    fun reset() {
        // stop current poller task
        job?.cancel()

        // start with delay
        if (method != null) {
            scope.launch {
                delay(delayInMilliseconds)
                start(this@AsyncPoller.method!!)
            }
        }
    }
}
