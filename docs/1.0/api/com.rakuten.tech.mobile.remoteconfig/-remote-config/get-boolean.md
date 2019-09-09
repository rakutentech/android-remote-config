[remote-config](../../index.md) / [com.rakuten.tech.mobile.remoteconfig](../index.md) / [RemoteConfig](index.md) / [getBoolean](./get-boolean.md)

# getBoolean

`abstract fun getBoolean(key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fallback: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)

Get a boolean from the Cached config.
If the key does not exist, [fallback](get-boolean.md#com.rakuten.tech.mobile.remoteconfig.RemoteConfig$getBoolean(kotlin.String, kotlin.Boolean)/fallback) will be returned.

### Parameters

`key` - returns value for this key in the config

`fallback` - returned when the key does not exist

**Return**
Boolean value for the specified key

