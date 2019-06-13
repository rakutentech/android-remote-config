package com.rakuten.tech.mobile.remoteconfig

import kotlinx.coroutines.test.TestCoroutineScope
import org.amshove.kluent.shouldEqual
import org.junit.Test
import java.util.concurrent.TimeUnit

class AsyncPollerSpec {

    private val testScope = TestCoroutineScope()

    @Test
    fun `should invoke the provided function`() {
        var numberOfInvocations = 0
        val poller = AsyncPoller(1, testScope)

        poller.start { numberOfInvocations++ }

        numberOfInvocations shouldEqual 1
    }

    @Test
    fun `should poll at the specified interval`() {
        var numberOfInvocations = 0
        val poller = AsyncPoller(1, testScope)

        poller.start { numberOfInvocations++ }

        testScope.advanceTimeBy(TimeUnit.MINUTES.toMillis(5))

        numberOfInvocations shouldEqual 6
    }
}
