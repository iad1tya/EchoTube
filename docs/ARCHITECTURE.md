# Architecture Overview

## Stack

- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Dependency Injection: Hilt
- Persistence: Room + DataStore
- Async: Coroutines + Flow
- Paging: Paging 3
- Media: AndroidX Media3
- Network: OkHttp, Ktor, NewPipe extractor integration

## High-Level Modules

- `app/src/main/java/.../ui`: composables, navigation, screens
- `app/src/main/java/.../data`: repositories, local storage, recommendation engine
- `app/src/main/java/.../player`: playback, controls, quality, diagnostics
- `app/src/main/java/.../innertube`: extraction and API model layer

## Navigation

Navigation is centralized in the app graph (`EchoTubeNavigation.kt`) and launched from `EchoTubeApp.kt`.

Onboarding, settings, content screens, and player flows are connected through a Compose NavHost.

## State Management

- Screen state primarily uses ViewModels and Flows.
- App-level preferences are handled in `PlayerPreferences` with DataStore.
- Player state is coordinated through `EnhancedPlayerManager` and global state flows.

## Build Variants

EchoTube is documented and released as a single public app version.

Gradle task names may still include `github` in variant naming.

Build types: `debug`, `nightly`, `release`

## CI/CD

GitHub Actions workflow builds and uploads APK artifacts for key variants and can create release assets from tags.