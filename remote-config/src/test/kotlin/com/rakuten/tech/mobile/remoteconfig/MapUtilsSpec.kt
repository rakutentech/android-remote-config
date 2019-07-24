package com.rakuten.tech.mobile.remoteconfig

import org.amshove.kluent.shouldEqual
import org.junit.Test
import java.io.BufferedReader

class MapUtilsSpec {

    @Test
    fun `should correctly serialize map`() {
        val map = hashMapOf("testKey" to "test_value")

        val inputStream = map.toInputStream()

        inputStream.bufferedReader()
            .use(BufferedReader::readText) shouldEqual """{"testKey":"test_value"}"""
    }
}
