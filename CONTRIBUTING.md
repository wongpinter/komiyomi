# Contributing to Komiyomi

Komiyomi focuses on managing and reading an existing offline CBZ/CBR collection. Bug reports and feature requests belong in [GitHub Issues](https://github.com/wongpinter/komiyomi/issues).

## Bug reports

Include the Komiyomi version, Android version, device model, archive type and size, and the steps that reproduce the problem. Do not upload copyrighted comic files; a small synthetic archive or a description of its structure is usually enough.

## Development

- Android Studio with Android SDK 36
- JDK 21

Build a debug APK with `./gradlew assembleDebug`. Before opening a pull request, format and check the source with `./gradlew spotlessApply` and `./gradlew spotlessCheck`.

Keep changes focused on local library management, archive handling, and offline reading. Please preserve upstream copyright and license notices when modifying code derived from Mihon, TachiyomiSY, Komikku, or other projects.

## Forks and redistribution

This project is itself a fork. If you redistribute a modified build, give it a distinct application ID and visible app name, configure or disable its update checker, and retain all applicable license notices and attributions. See [LICENSE](./LICENSE).
