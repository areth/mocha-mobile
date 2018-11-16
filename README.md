# mocha-mobile
Test Node.js applications on mobile devices.

Mocha-mobile runs mocha-compatible tests within mobile device.

*Supports only Android for now.*

## Prerequisites
### Android

You'll need a macOS or Linux development machine, and a physical Android device.

You'll need [Android Studio](https://developer.android.com/studio/install.html) installed on your development Machine, alongside `Java`, `Gradle`, the `Android 8.0 (Android SDK Platform 26)`, `Android SDK Build Tools v26.0.1`, `Android SDK Tools`, `Android SDK Platform-Tools`, `NDK version >=17` and `CMake`.

You should set the Environment variables required to build Android Applications from the command line: the `ANDROID_HOME` variable pointing to your `android-sdk` path and the `ANDROID_NDK_HOME` variable pointing to the installed NDK path (it usually is `$ANDROID_HOME/ndk-bundle`) . You should also add `$ANDROID_HOME/platform-tools`, `$ANDROID_HOME/tools` and `$ANDROID_HOME/tools/bin` to the `PATH` environment variable.

### iOS prerequisites

You'll need a macOS development machine, an iOS arm64 physical device running iOS 11.0 or greater and a valid iOS Development certificate installed.

iOS tests depend on the [`ios-deploy` tool](https://github.com/phonegap/ios-deploy) for installing and running the test app. You can install it by using npm:
```sh
npm install -g ios-deploy@1.9.2
```

## Installation
mocha-mobile have to be installed locally
```sh
npm i --save-dev mocha-mobile
```
## Run tests
Quick example
```sh
mocha-mobile -a android --timeout 10000
```
Mocha-mobile CLI inherits Mocha CLI with additional parameters:
```
-a, --arch <architecture> - Required. Architecture to run test (android|ios).
--ignore <pattern> - Files pattern to ignore during mobile app assembling. Like `**/build/*`.
--onlyprep - Only prepare test, don't run it
--onlyrun - Only run test, don't prepare it (test has to be prepared)
```
Additional file exceptions may be specified in `.mmignore`

## How it works
It uses precompiled libraries for mobile devices to run node.js code. Mocha-mobile command proceeds the following steps:
* deploys native app with precompiled library to device with Node.js application as a zipped asset
* unzips Node.js application within device and executes Mocha over it
* displays tests results in a host terminal

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details