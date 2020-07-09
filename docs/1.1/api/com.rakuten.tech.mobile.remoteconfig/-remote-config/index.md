[remote-config](../../index.md) / [com.rakuten.tech.mobile.remoteconfig](../index.md) / [RemoteConfig](./index.md)

# RemoteConfig

`abstract class RemoteConfig`

Main entry point for the Remote Config SDK.
Should be accessed via [RemoteConfig.instance](instance.md).

### Functions

| [fetchAndApplyConfig](fetch-and-apply-config.md) | `abstract suspend fun fetchAndApplyConfig(): `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>`<br>This method is the manual trigger for fetching config values. Config values are applied directly after fetch. |
| [getBoolean](get-boolean.md) | `abstract fun getBoolean(key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fallback: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Get a boolean from the Cached config. If the key does not exist, [fallback](get-boolean.md#com.rakuten.tech.mobile.remoteconfig.RemoteConfig$getBoolean(kotlin.String, kotlin.Boolean)/fallback) will be returned. |
| [getConfig](get-config.md) | `abstract fun getConfig(): `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>`<br>Get the cached config. |
| [getNumber](get-number.md) | `abstract fun <T : `[`Number`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html)`> getNumber(key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fallback: `[`T`](get-number.md#T)`): `[`T`](get-number.md#T)<br>Get a number from the Cached config. If the key does not exist, [fallback](get-number.md#com.rakuten.tech.mobile.remoteconfig.RemoteConfig$getNumber(kotlin.String, com.rakuten.tech.mobile.remoteconfig.RemoteConfig.getNumber.T)/fallback) will be returned. Number will attempt to be converted to type [T](get-number.md#T). If the conversion fails (i.e. the value in the cached config is a not a number) then [fallback](get-number.md#com.rakuten.tech.mobile.remoteconfig.RemoteConfig$getNumber(kotlin.String, com.rakuten.tech.mobile.remoteconfig.RemoteConfig.getNumber.T)/fallback) will be returned. |
| [getString](get-string.md) | `abstract fun getString(key: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, fallback: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Get a string from the Cached config. If the key does not exist, [fallback](get-string.md#com.rakuten.tech.mobile.remoteconfig.RemoteConfig$getString(kotlin.String, kotlin.String)/fallback) will be returned. |

### Companion Object Functions

| [instance](instance.md) | `fun instance(): `[`RemoteConfig`](./index.md)<br>Instance of [RemoteConfig](./index.md). |

