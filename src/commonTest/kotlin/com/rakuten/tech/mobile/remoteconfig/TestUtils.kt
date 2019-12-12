package com.rakuten.tech.mobile.remoteconfig

import kotlinx.coroutines.CoroutineScope

internal expect fun runBlockingTest(block: suspend (scope: CoroutineScope) -> Unit)
