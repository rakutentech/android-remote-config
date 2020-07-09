[remote-config](../../index.md) / [com.rakuten.tech.mobile.remoteconfig](../index.md) / [RemoteConfig](index.md) / [getNumber](./get-number.md)

# getNumber

`abstract fun <T : `[`Number`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)`> getNumber(key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fallback: `[`T`](get-number.md#T)`): `[`T`](get-number.md#T)

Get a number from the Cached config.
If the key does not exist, [fallback](get-number.md#com.rakuten.tech.mobile.remoteconfig.RemoteConfig$getNumber(kotlin.String, com.rakuten.tech.mobile.remoteconfig.RemoteConfig.getNumber.T)/fallback) will be returned.
Number will attempt to be converted to type [T](get-number.md#T). If the conversion fails
(i.e. the value in the cached config is a not a number) then [fallback](get-number.md#com.rakuten.tech.mobile.remoteconfig.RemoteConfig$getNumber(kotlin.String, com.rakuten.tech.mobile.remoteconfig.RemoteConfig.getNumber.T)/fallback) will be returned.

### Parameters

`key` - returns value for this key in the config

`fallback` - returned when the key does not exist or cached value cannot be converted
to number

**Return**
[T](get-number.md#T) value for the specified key. [T](get-number.md#T) must be a subtype of [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) such as Int,
Long, Short, Float, Double, or Byte

