# Build and Release Guide

## Local Build

## Firebase Prerequisite

This app uses Firebase Analytics and Crashlytics.

- Place `google-services.json` at `app/google-services.json`.
- See [FIREBASE_SETUP.md](../FIREBASE_SETUP.md) for full setup details.

### Debug install

```bash
./gradlew :app:installGithubDebug
```

### Release APKs

```bash
./gradlew :app:assembleGithubRelease
./gradlew :app:assembleGithubNightly
```

## Signing Configuration

Release signing is loaded from:

- Gradle properties
- `local.properties`
- environment variables

Required keys:

- `storePassword`
- `keyAlias`
- `keyPassword`

If unavailable, release APK generation falls back to unsigned output.

## CI Workflow

Workflow file: `.github/workflows/build.yml`

It performs:

1. checkout
2. JDK 17 setup
3. Gradle cache restore
4. keystore decode from secret
5. multi-variant assemble
6. artifact upload
7. tagged release creation

## Release Tagging

Tag format expected by workflow:

```text
v<version>
```

Example:

```text
v1.0.1
```

## Release Checklist

1. update changelog
2. confirm versioning in Gradle config
3. run local smoke tests
4. push changes to default branch
5. create signed tag
6. verify generated release artifacts