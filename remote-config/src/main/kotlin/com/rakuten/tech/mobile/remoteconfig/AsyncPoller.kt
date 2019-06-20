package com.rakuten.tech.mobile.remoteconfig

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

internal class AsyncPoller @VisibleForTesting constructor(
    delayInMinutes: Int,
    private val scope: CoroutineScope
) {

    constructor(delayInMinutes: Int) : this(delayInMinutes, GlobalScope)

    private val delayInMilliseconds = TimeUnit.MINUTES.toMillis(delayInMinutes.toLong())

    fun start(method: () -> Any) {
        scope.launch {
            repeat(Int.MAX_VALUE) {
                method.invoke()

                delay(delayInMilliseconds)
            }
        }
    }
}
