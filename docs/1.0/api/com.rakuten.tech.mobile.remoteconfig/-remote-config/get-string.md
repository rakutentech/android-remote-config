[remote-config](../../index.md) / [com.rakuten.tech.mobile.remoteconfig](../index.md) / [RemoteConfig](index.md) / [getString](./get-string.md)

# getString

`abstract fun getString(key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fallback: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Get a string from the Cached config.
If the key does not exist, [fallback](get-string.md#com.rakuten.tech.mobile.remoteconfig.RemoteConfig$getString(kotlin.String, kotlin.String)/fallback) will be returned.

### Parameters

`key` - returns value for this key in the config

`fallback` - returned when the key does not exist

**Return**
String value for the specified key

