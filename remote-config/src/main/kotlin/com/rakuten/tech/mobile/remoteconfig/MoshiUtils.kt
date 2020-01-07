package com.rakuten.tech.mobile.remoteconfig

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

private val moshi = Moshi.Builder().build()

internal inline fun <reified K, reified V> jsonMapAdapter() =
    moshi.adapter<Map<K, V>>(
            Types.newParameterizedType(
                Map::class.java,
                K::class.java,
                V::class.java
            ))
        .lenient()

internal inline fun <reified T> jsonAdapter() =
    moshi.adapter<T>(T::class.java)
        .nonNull()
        .lenient()
