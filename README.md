# Remote Config SDK Multi-Platform Module

Provides remote configuration for applications. This is a Multi-platform (Android and iOS) module that can be added to an existing Android or iOS project.

## How to add to your project

Add the submodule to your project and build:

```bash
$ git submodule add {repo-url} shared
$ git submodule update
$ cd ./shared
$ ./gradlew assemble
```

After you have committed the submodule, it can be initialized on a fresh repo clone using the following:

```bash
$ git submodule update --init --recursive
```

### Android

Add the `shared` module to your `settings.gradle` file:

```
include ':shared'
```

Next you can add the this module dependency to your App's `build.gradle` file:

```groovy
dependencies {
  implementation project(':shared')
}
```

### iOS

#### 1. Link Library

In XCode, go to settings for your target (i.e. Pods > RRemoteConfig), then choose the `Build Phases` tab. Expand the `Link Binary With Libraries` section, then click the `+` button, and then select the framework at `shared/build/bin/ios/RemoteConfigSharedDebugFramework/RemoteConfigShared.framework`.

#### 2. Add build phase

Still on the `Build Phases` tab, click the `+` button at the top and add a `New Run Script Phase`. Move the phase to the top, and add the following script:

```
cd "$SRCROOT/../shared"
./gradlew linkRemoteConfigSharedDebugFrameworkIos
```

#### 3. Add Framework search path

Go to the `Build Settings` tab and find the `Framework Search Paths` setting. Add `$(SRCROOT)/../shared/build/bin/ios/RemoteConfigSharedDebugFramework`.

You can now import the module with `import RemoteConfigShared`.