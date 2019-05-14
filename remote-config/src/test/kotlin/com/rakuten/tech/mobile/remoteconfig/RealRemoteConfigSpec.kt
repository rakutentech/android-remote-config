package com.rakuten.tech.mobile.remoteconfig

import org.junit.Test

class RealRemoteConfigSpec {

    @Test
    fun `should be true`() {
        assert(RealRemoteConfig().test())
    }
}
