# HerHub
## Personal Safety, Wellness, and Support App

HerHub is an Android application that gives women practical tools for personal safety, emergency response, wellness, trusted contacts, community support, and everyday confidence. It includes local-first features alongside optional Firebase account backup and community data.

Copyright © 2026 mzamon. All rights reserved.

## Features

- Personal Safety & Emergency tools with SOS, siren, emergency calls, and alerts.
- Location sharing and safety tracking.
- Community Safety Map with OpenStreetMap, local reports, and shared safety notices.
- Period and menstrual tracking.
- Pregnancy and fertility tools.
- Mental health and emotional wellness tools, including mood check-ins and breathing.
- Health and wellness diary, water tracking, medical ID, and appointments.
- Digital and personal security checklist.
- Trusted Circle contacts.
- Safe Journey / Travel Mode with trip timing and arrival check-in.
- Women's Information Hub for health, rights, and safety guidance.
- Support and moderated community features.
- Abuse and harassment support with a private log and safety plan.
- Financial safety tools, scam checking, savings goals, and fraud guidance.
- Smart notifications and reminders.
- Emergency Resource Directory with searchable hotlines.
- AI Safety & Wellness Assistant with practical next steps.
- Appearance controls for Light, Dark, and system theme modes.
- Guest use with local device storage and optional signed-in backup.

## Technology

| Area | Technology |
|---|---|
| Application | Native Android |
| Language | Java |
| Build system | Gradle |
| Compile / target SDK | Android 35 |
| Minimum SDK | Android 8.0 (API 26) |
| UI | AndroidX, Material Components, programmatic views |
| Maps | osmdroid with OpenStreetMap |
| Authentication | Firebase Authentication |
| Cloud data | Firebase Firestore and Firebase Storage |
| Local data | Android SharedPreferences and local JSON |
| Testing | JUnit 4 and Android Gradle checks |

## Requirements

- Android Studio with the Android SDK and build tools for API 35.
- JDK 17.
- An Android device or emulator running API 26 or newer.
- Firebase configuration in `app/google-services.json` for authentication and cloud features.
- Internet access for Firebase services, OpenStreetMap tiles, and optional online backups.

## Quick start

1. Open the project directory in Android Studio.
2. Confirm that JDK 17 is selected for Gradle.
3. Add the Firebase project configuration as `app/google-services.json`.
4. Sync the Gradle project and allow dependencies to restore.
5. Select an emulator or connected Android device.
6. Run the `app` configuration.

The app can be used as a guest. Signing in enables Firebase-backed account data and community synchronization where available.

## Command line

From the project directory:

```powershell
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Authentication and data

HerHub supports guest use and Firebase authentication. Guest data remains on the device. Authenticated users can back up supported data securely to their account. Community safety reports are stored in Firestore when the network is available, while reports are also retained locally when offline.

Do not commit Firebase credentials, signing keys, or other secrets. Use the existing ignored local configuration files for environment-specific values.

## Safety and privacy

- Location is requested only when a location feature needs it.
- SMS, notification, and location permissions are requested at the point of use.
- SOS actions provide clear user feedback and emergency contact options.
- Community reports should remain factual and must not include names or private details.
- HerHub is a support tool and does not dispatch emergency services. For emergencies, use the appropriate local emergency number.

## Project structure

```text
HerHub/
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat
└── app/
    ├── build.gradle.kts
    ├── src/main/java/za/co/herhub/   Application screens and services
    ├── src/main/res/                 Drawables, icons, menus, and themes
    ├── src/main/assets/              Bundled assistant knowledge
    └── src/test/                     JVM unit tests
```

## Validation and GitHub Actions

The project includes a GitHub Actions workflow at `.github/workflows/android.yml`. It runs on every push and pull request, restores Gradle dependencies, runs `./gradlew check`, and builds the debug APK with `./gradlew assembleDebug`.

The local smoke test verifies that all 17 catalogue features have complete, unique entries and mapped icons. Add focused unit tests when new business logic or user workflows are introduced.

## Copyright

Copyright © 2026 mzamon. All rights reserved.
