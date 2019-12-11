package com.rakuten.tech.mobile.remoteconfig

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_current_queue
import kotlin.coroutines.CoroutineContext

internal object CurrentQueueDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val queue = dispatch_get_current_queue()

        dispatch_async(queue) {
            block.run()
        }
    }
}

val Dispatchers.Current: CoroutineDispatcher get() = CurrentQueueDispatcher
