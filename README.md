[![CircleCI](https://circleci.com/gh/rakutentech/android-remote-config.svg?style=svg)](https://circleci.com/gh/rakutentech/android-remote-config)
[![codecov](https://codecov.io/gh/rakutentech/android-remote-config/branch/master/graph/badge.svg)](https://codecov.io/gh/rakutentech/android-remote-config)

# Remote Config SDK for Android

Provides remote configuration for Android applications. See the [User Guide](./remote-config/USERGUIDE.md) for instructions on implementing in an android application.

## How it works

## How to build

This repository uses submodules for some configuration, so they must be initialized first.

```bash
$ git submodule init
$ git submodule update
$ ./gradlew assemble
```

If you wish to publish an artifact to JCenter, you must set the Bintray credentials as environment variables.

```bash
$ export BINTRAY_USER=user_name
$ export BINTRAY_KEY=key
$ export BINTRY_REPO=repo_name
```

## How to test the Sample app

You must first define your API base url, App Id, and subscription key as either environment variables or as gradle properties (such as in your global `~/.gradle/gradle.properties` file).

```
REMOTE_CONFIG_APP_ID=your_app_id
REMOTE_CONFIG_SUBSCRIPTION_KEY=your_subscription_key
REMOTE_CONFIG_BASE_URL=https://www.example.com/
```

## How to use it

Currently we do not host any public APIs but you can create your own APIs and configure the SDK to use those.

## Contributing

See [Contribution guidelines](./CONTRIBUTING.md)
