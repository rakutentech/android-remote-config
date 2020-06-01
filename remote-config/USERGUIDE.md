---
layout: userguide
---

# Remote Config SDK for Android

Provides remote configuration for Android applications.

## Requirements

### Supported Android Versions

This SDK supports Android API level 21 (Lollipop) and above.

## Getting Started

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

### #2 Set your App Id, Subscription Key, Base URL, and Configuration Application Settings

We don't currently host a public API, so you will need to provide your own Base URL for API requests.

Configuration Application Settings is an optional setting ("com.rakuten.tech.mobile.remoteconfig.ApplyDirectly") and is `false` by default.
If set to `true`, configurations are applied directly when fetched. Otherwise, fetched configuration are applied on next app launch from terminated state.

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
        android:value="true or false" />

    </application>
</manifest>
```

### #3 Retrieve Config Values

Config values are fetched from the API at launch time and then periodically fetched again every 60 minutes.
However, newly fetched config values will not be applied until the next App launch.
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

## Changelog

### v1.1.0 (in-progress)
- SDKCF-1991: Add build settings for applying configuration directly after fetching.

### v1.0.0 (2019-09-09)

- Initial release.
