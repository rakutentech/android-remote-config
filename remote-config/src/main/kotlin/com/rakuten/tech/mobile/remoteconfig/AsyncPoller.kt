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

    private val delayInMilliseconds = if (delayInSeconds < MIN_DELAY) {
        TimeUnit.SECONDS.toMillis(MIN_DELAY.toLong())
    } else {
        TimeUnit.SECONDS.toMillis(delayInSeconds.toLong())
    }

    private var job: Job? = null
    private var method: (suspend () -> Unit)? = null

    fun start(method: suspend () -> Unit) {
        this.method = method
        job = scope.launch {
            repeat(Int.MAX_VALUE) {
                method.invoke()

                delay(delayInMilliseconds)
            }
        }
    }

    fun stop() {
        // stop current poller task
        job?.cancel()
    }

    fun restart() {
        // restart stopped job with delay
        if (method != null && job?.isActive == false) {
            scope.launch {
                delay(delayInMilliseconds)
                start(this@AsyncPoller.method!!)
            }
        }
    }

    companion object {
        private const val MIN_DELAY = 60 // in secs.
    }
}
