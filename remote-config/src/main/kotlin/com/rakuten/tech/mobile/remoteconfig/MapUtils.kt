package com.rakuten.tech.mobile.remoteconfig

import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.map

internal fun Map<String, String>.toInputStream() = Json.nonstrict.stringify(
    (StringSerializer to StringSerializer).map,
    this
).byteInputStream()
