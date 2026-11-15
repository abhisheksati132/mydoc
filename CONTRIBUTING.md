# Contributing to MyDoc

MyDoc is a solo-maintained, offline-first Android app. Contributions are welcome
when they fit that shape: on-device by default, no telemetry, and no new network
dependencies without a clear reason.

## Before you start

- **Bugs**: use the bug report form. Include the app version (Settings → About)
  and, if the app crashed, the report from **Last crash report**.
- **Security issues**: do not open a public issue. Follow [SECURITY.md](SECURITY.md).
- **Feature ideas**: open a feature request first; small, focused changes are much
  easier to review than broad rewrites.

## Build and test

Requirements: JDK 21 and the Android SDK with platform 37.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug        # must stay at 0 errors
```

Instrumented tests need a device or emulator:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Keep UI text in `app/src/main/res/values/strings.xml`; the project's custom lint
rules fail composables that read string resources through `Context` directly.

If you add or upgrade a dependency, pin its checksums so Gradle dependency
verification accepts it:

```sh
./gradlew --write-verification-metadata sha256 help
```

## Pull requests

- One topic per pull request, with a short note on what changed and why.
- Run the three commands above before opening it; CI runs the same gates.
- Match the surrounding style; the codebase favours small, readable Compose
  composables and tested data-layer changes.
- By contributing you agree that your work is licensed under the project's
  [AGPL-3.0](LICENSE), the same licence MyDoc ships under.

## Releases

Releases are cut from `main` by the maintainer: bump `APP_VERSION_NAME` and
`APP_VERSION_CODE` in `app/build.gradle.kts`, update [CHANGELOG.md](CHANGELOG.md),
and tag `vX.Y.Z`. The release workflow builds, signs, verifies, and publishes the
APKs and their SHA-256 files.
