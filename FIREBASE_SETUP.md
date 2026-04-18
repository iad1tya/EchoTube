# Firebase Setup (Analytics + Crashlytics)

This project uses Firebase for:

- Analytics
- Crashlytics

## Current project configuration

The repository includes a sanitized template at `app/google-services.json`.

Before building your own production app, replace it with your real Firebase config file.

Your real file must include clients for:

- `com.echotube.iad1tya.debug`
- `com.echotube.iad1tya.nightly`

## If you are building your own fork

1. Create/select a Firebase project in the Firebase Console.
2. Add an Android app with package name `com.echotube.iad1tya` (or your own package).
3. If you keep `applicationIdSuffix` values, also add Firebase Android apps for your debug/nightly package names.
4. Download your `google-services.json`.
5. Replace `app/google-services.json` with your file.
6. Sync Gradle and build.

## Gradle wiring already included

Top-level `build.gradle.kts` includes:

- `com.google.gms.google-services`
- `com.google.firebase.crashlytics`

App module `app/build.gradle.kts` includes:

- `id("com.google.gms.google-services")`
- `id("com.google.firebase.crashlytics")`
- Firebase BOM
- `firebase-analytics-ktx`
- `firebase-crashlytics-ktx`

## Notes

- Crashlytics data appears after the app has a crash event in a build where Crashlytics collection is enabled.
- For local debug testing, you can force a test crash from code if needed.
- Keep real Firebase credentials out of Git. The provided file in this repo is a placeholder.
