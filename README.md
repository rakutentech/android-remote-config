# Remote Config SDK Multi-Platform Module

Provides remote configuration for Android applications. This is a Multi-platform (Android and iOS) module that can be added to an existing Android or iOS project.

## How to add to your project

Add the submodule to your project and build:

```bash
$ git sumodule add {repo-url} shared
$ git submodule update
$ ./shared/gradlew assemble
```

After you have commited the submodule, it can be initialized on a fresh repo clone using the following:

```bash
$ git sumodule update --init
```

### Android

Add the `shared` module to your `settings.gradle` file:

```
include ':shared'
```

### iOS

Add one of the following framework path to your XCode project:

- `shared/build/bin/ios/SharedCodeDebugFramework`
- `shared/build/bin/ios/SharedCodeReleaseFramework`
