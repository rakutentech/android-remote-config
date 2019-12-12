package com.rakuten.tech.mobile.remoteconfig

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

internal actual fun runBlockingTest(block: suspend (scope: CoroutineScope) -> Unit) = runBlocking { block(this) }
