---
layout: userguide
---

# Remote Config SDK for Android

Provides remote configuration for Android applications.

### This page covers:
* [Requirements](#requirements)
* [Getting Started](#getting-started)
* [Advanced Features](#advanced-features)
* [Change Log](#nchangelog)

## <a name="requirements"></a> Requirements

### Supported Android Versions

This SDK supports Android API level 21 (Lollipop) and above.

## <a name="getting-started"></a> Getting Started

### #1 Add dependency to your app's `build.gradle`

```groovy
repositories {
  jcenter()
}

dependency {
  implementation 'com.rakuten.tech.mobile.remoteconfig:remote-config:${latest_version}'
}
```

Note: please use/enable R8 to avoid proguard issue with Moshi. For enabling and more details on R8, please refer to the [Android Developer documentation](https://developer.android.com/studio/build/shrink-code).

### #2 Configure SDK settings in AndroidManifest.xml
The Remote Config SDK is configured via manifest meta-data, the configurable values are:

| Field                        | Datatype| Manifest Key                                         | Optional   | Default   |
|------------------------------|---------|------------------------------------------------------|------------|---------- |
| Base URL                     | String  | `com.rakuten.tech.mobile.remoteconfig.BaseUrl`       | ❌         | 🚫        |
| RAS Application ID           | String  | `com.rakuten.tech.mobile.ras.AppId`                  | ❌         | 🚫        |
| RAS Project Subscription Key | String  | `com.rakuten.tech.mobile.ras.ProjectSubscriptionKey` | ❌         | 🚫        |
| Config Application           | boolean | `com.rakuten.tech.mobile.remoteconfig.ApplyDirectly` | ✅         | `false`   |
| Polling Delay (in secs)      | integer | `com.rakuten.tech.mobile.remoteconfig.PollingDelay`  | ✅         | `3600`    |

Notes:
* We don't currently host a public API, so you will need to provide your own Base URL for API requests.
* If Config Application is set to `true`, config are applied directly when fetched. Otherwise, fetched config are applied on next app launch from terminated state.
* By default, config values are fetched from the API at launch time and then periodically fetched again every 60 minutes. Set the Polling Delay to the desired interval of fetching config values.
* The minimum Polling Delay is 60s. If the value set is less than the minimum, 60s will be used as Polling Delay.

In your `AndroidManifest.xml`:

```xml
<manifest>
    <application>

      <meta-data
        android:name="com.rakuten.tech.mobile.remoteconfig.BaseUrl"
        android:value="https://www.example.com" />

      <meta-data
        android:name="com.rakuten.tech.mobile.ras.AppId"
        android:value="your_app_id" />

      <meta-data
        android:name="com.rakuten.tech.mobile.ras.ProjectSubscriptionKey"
        android:value="your_subscription_key" />

      <meta-data
        android:name="com.rakuten.tech.mobile.remoteconfig.ApplyDirectly"
        android:value="false" />

      <meta-data
        android:name="com.rakuten.tech.mobile.remoteconfig.PollingDelay"
        android:value="3600" />

    </application>
</manifest>
```

### #3 Retrieve Config Values
By default, config values are fetched from the API at launch time and then periodically fetched again every 60 minutes.
The newly fetched config values will not be applied until the next App launch.
This also means that the first time the App is launched, the fallback values will be used instead of fetched values.

```kotlin
val remoteConfig = RemoteConfig.instance()

// Retrieve a String value
val testString = remoteConfig.getString("stringKeyName", "string_fallback_value")

// Retrieve a Boolean value
val testBoolean = remoteConfig.getBoolean("booleanKeyName", false)

// Retrieve a Number value (can be Int, Short, Long, Double, Float, or Byte)
val testNumber = remoteConfig.getNumber("numberKeyName", 1)

// Retrieve the entire config as a Map
val configMap = remoteConfig.getConfig()
```
## <a name="advanced-features"></a> Advanced Features

### #1 Manual Trigger for Fetching Config
Fetching config can be triggered manually by the app. This can be used in cases where updated config is needed when certain events occurred or a specific screen is displayed.

```kotlin
CoroutineScope(Dispatchers.Main).launch {
  try {
      val config = withContext(Dispatchers.Default) {
          // Trigger manual fetch and direct application of config
          remoteConfig.fetchAndApplyConfig()
      }
      // do something with config
  } catch (ex: Exception) {
      // do something with exception
  }
```

## <a name="changelog"></a> Changelog

### v1.1.0 (in-progress)
- SDKCF-1991: Add build settings for applying configuration directly after fetching.
- SDKCF-2457: Add build settings for config fetching frequency, and new API for manual trigger for fetching config.

### v1.0.0 (2019-09-09)

- Initial release.
