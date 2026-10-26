# Location Reminder

A modern, offline-first Android application for location-based and time-based reminders, recurring daily tasks, and historical task tracking. Built with Kotlin, Jetpack Compose, Material 3, Room, MapLibre GL Native, and Google Play Services Location.

---

## Features

### 1. Location-Based Reminders & Map Picker
- **Interactive Map Picker**: Powered by **MapLibre GL Native SDK (11.8.0)** with **OpenFreeMap Bright** vector tiles and an **Esri World Imagery** satellite toggle.
- **Place Search & Geocoding**: Search places and reverse-geocode coordinates using Android platform geocoding with OpenStreetMap Nominatim fallback.
- **Geofencing**: Utilizes Google Play Services Location API to trigger notifications on `GEOFENCE_TRANSITION_ENTER` and `GEOFENCE_TRANSITION_EXIT` with custom trigger radius.
- **Reboot Resilience**: Automatically re-registers active geofences on device boot (`RECEIVE_BOOT_COMPLETED`) and app updates (`MY_PACKAGE_REPLACED`).

### 2. Time-Based Reminders & Alarms
- **Exact Alarms**: Schedules precise alerts using `AlarmManager.setExactAndAllowWhileIdle()` (`SCHEDULE_EXACT_ALARM`).
- **Repetition & Snooze**: Supports customizable repeat intervals (in minutes) and flexible snooze periods.
- **Heads-Up Notifications**: High-priority notification channels with actions to complete, snooze, or dismiss.

### 3. Daily Tasks & 30-Day History
- **Daily Recurring Checklist**: Daily tasks reset automatically every midnight (`00:00`) for the new calendar day without losing history.
- **30-Day Calendar Retention**: Dynamically calculates the last 30 calendar days (Today down to 29 days ago) from device date.
- **Date-Wise Progress**: Collapsible list showing date-specific completion percentages (`0%` to `100%`).
- **Inline History Inspection & Editing**: Tap any historical date to expand and inspect completed (🟢) vs incomplete (🟠) tasks, with an inline edit mode to correct past completion records for that specific date.

### 4. Reminders History & Management
- **History Tracking**: View past and completed reminders with completion timestamps.
- **Reactivation & Clean-up**: Easily reactivate completed reminders or clear history.
- **Bottom Navigation**: Unified bottom bar with a 3-choice menu (`Location Based`, `Time Based`, `History`) and direct access to `Daily Tasks`.

### 5. Modern Architecture & UI
- **Jetpack Compose & Material 3**: Fluid micro-animations (`pressScale`, animated chevrons, card expansions).
- **Adaptive Theme**: Dark, Light, and System theme support with DataStore Preferences.
- **Offline-First Persistence**: Room Database (version 9) with sequential migrations (1 through 9).

---

## Prerequisites & Required Software

| Requirement | Supported Version | Notes |
| :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS, Linux | PowerShell, bash, or zsh |
| **JDK (Java Development Kit)** | **JDK 17** (Temurin 17 / Corretto 17 / Zulu 17) | Gradle daemon is configured for Java 17 |
| **Android Studio** | Ladybug / Koala / Meerkat or later | Recommended IDE |
| **Android SDK Platform** | **Android 15 (API 35)** / **API 37** | `compileSdk = 37`, `targetSdk = 35`, `minSdk = 24` |
| **Android SDK Build-Tools** | **35.0.0** or **36.0.0** | Install via Android Studio SDK Manager |
| **Gradle** | **9.6.0** | Handled automatically via `gradlew` wrapper |
| **Android Gradle Plugin** | **9.4.1** | Configured in `gradle/libs.versions.toml` |
| **Kotlin / Compose** | Kotlin **2.2.10**, Compose Compiler plugin | KSP `2.2.10-2.0.2` |

---

## Migration & Setup on New Laptop

### Step 1: Clone the Repository
Open a terminal (PowerShell on Windows, or bash/zsh on macOS/Linux):

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL> Location_Reminder
cd Location_Reminder
```

### Step 2: Configure Android SDK (`local.properties`)
Copy the template `local.properties.example` to `local.properties`:

**Windows (PowerShell):**
```powershell
Copy-Item local.properties.example local.properties
```

**macOS / Linux:**
```bash
cp local.properties.example local.properties
```

Open `local.properties` and ensure `sdk.dir` points to your Android SDK installation path:

- **Windows**: `sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk`
- **macOS**: `sdk.dir=/Users/<YourUsername>/Library/Android/sdk`
- **Linux**: `sdk.dir=/home/<YourUsername>/Android/Sdk`

> **Note:** When opening the project in Android Studio, it typically generates `local.properties` automatically if the Android SDK is already installed.

### Step 3: Map & Geocoding Secrets
**No proprietary API keys are needed!**
- The app uses **OpenFreeMap Bright** vector styles (`https://tiles.openfreemap.org/styles/bright`) and public **Esri World Imagery** satellite raster tiles over MapLibre Native SDK.
- Geocoding uses the built-in Android platform `Geocoder` with OpenStreetMap Nominatim fallback.
- No Google Maps API key or billing setup is required.

---

## Building & Running

### Run Unit Tests
Execute the unit tests to verify database migrations, geofence reliability, alarm scheduling, and daily task 30-day retention logic:

**Windows PowerShell:**
```powershell
.\gradlew.bat test
```

**macOS / Linux:**
```bash
./gradlew test
```

### Build Debug APK
**Windows PowerShell:**
```powershell
.\gradlew.bat assembleDebug
```

**macOS / Linux:**
```bash
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install & Run on Connected Device or Emulator
Ensure an Android device (with USB debugging enabled) or emulator is connected:

**Check connected devices:**
```bash
adb devices
```

**Install and launch:**
**Windows PowerShell:**
```powershell
.\gradlew.bat installDebug
adb shell am start -n com.example.location_reminder/.MainActivity
```

**macOS / Linux:**
```bash
./gradlew installDebug
adb shell am start -n com.example.location_reminder/.MainActivity
```

---

## Release Signing & AAB Generation

### Release Signing Setup
The release build in `app/build.gradle.kts` looks for signing credentials either in Gradle properties or environment variables:

| Property | Environment Variable | Description |
| :--- | :--- | :--- |
| `RELEASE_STORE_FILE` | `RELEASE_STORE_FILE` | Absolute path to `.jks` or `.keystore` file |
| `RELEASE_STORE_PASSWORD` | `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | `RELEASE_KEY_PASSWORD` | Key password |

> [!CAUTION]
> **Keystore Security & Backup Rules**:
> 1. **NEVER** commit `.jks`, `.keystore`, or password files to GitHub. These patterns are strictly excluded in `.gitignore`.
> 2. Back up your production release keystore file securely (e.g. encrypted storage, 1Password, or offline USB).
> 3. If you have already published an app version signed with a keystore, you **must use that exact same keystore** on your new laptop to publish app updates to Google Play. Losing your keystore prevents updating the app on the Play Store.

### Creating a New Keystore (If none exists)
If you do not yet have a production release keystore, generate one with `keytool`:

```bash
keytool -genkey -v -keystore release-keystore.jks -alias reminder-release-key -keyalg RSA -keysize 2048 -validity 10000
```
Keep `release-keystore.jks` in a secure folder outside the repository or ensure it is ignored by Git.

### Configuring Signing Credentials on New Laptop
Add credentials to your user-level Gradle properties file (e.g., `C:\Users\<YourUsername>\.gradle\gradle.properties` on Windows or `~/.gradle/gradle.properties` on macOS/Linux):

```properties
RELEASE_STORE_FILE=C\:\\Users\\<YourUsername>\\secure_keys\\release-keystore.jks
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=reminder-release-key
RELEASE_KEY_PASSWORD=your_key_password
```

### Generate Release Android App Bundle (AAB)
To generate an AAB for the Google Play Console:

**Windows PowerShell:**
```powershell
.\gradlew.bat bundleRelease
```

**macOS / Linux:**
```bash
./gradlew bundleRelease
```

Output path:
`app/build/outputs/bundle/release/app-release.aab`

*(Note: If no release signing config is provided, Gradle builds an unsigned bundle without failing).*

---

## Project Structure Overview

```
Location_Reminder/
├── app/
│   ├── build.gradle.kts          # App-level dependencies, SDK config, signing
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/location_reminder/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── alarm/          # AlarmScheduler & ReminderAlarmReceiver
│       │   │   │   ├── geofence/       # GeofenceManager & BroadcastReceivers
│       │   │   │   ├── local/          # Room Entities, DAOs, Database & Migrations
│       │   │   │   ├── location/       # Geocoding & Place search
│       │   │   │   ├── notification/   # Notification channels & dispatch
│       │   │   │   ├── preferences/    # DataStore Theme preferences
│       │   │   │   ├── repository/     # ReminderRepository implementation
│       │   │   │   └── util/           # Date formatters, permissions
│       │   │   ├── ui/
│       │   │   │   ├── components/     # Dialogs, cards, MapLibre picker
│       │   │   │   ├── screens/        # Home, DailyTasks, 30-Day History
│       │   │   │   └── theme/          # Material 3 colors, typography, motion
│       │   │   └── viewmodel/          # ReminderViewModel
│       │   └── res/                    # Drawables, mipmaps, strings, styles
│       ├── test/                       # Unit tests (Migrations, DailyTasks, Geofence)
│       └── androidTest/                # On-device instrumentation tests
├── gradle/
│   ├── libs.versions.toml             # Centralized version catalog
│   ├── gradle-daemon-jvm.properties   # Daemon JVM toolchain (JDK 17)
│   └── wrapper/
│       ├── gradle-wrapper.jar         # Gradle wrapper executable JAR
│       └── gradle-wrapper.properties  # Gradle 9.6.0 distribution spec
├── build.gradle.kts                   # Root build script
├── settings.gradle.kts                # Plugin & dependency repositories
├── gradle.properties                  # JVM args & performance flags
├── compose_compiler_config.conf       # Compose stability metrics configuration
├── local.properties.example           # Template for local SDK and signing setup
├── .gitignore                         # Android Studio & secrets exclusion rules
└── README.md                          # Documentation & migration guide
```

---

## Known External Services & Network Access
- **OpenFreeMap**: Fetches vector style JSON and vector pbf tiles from `https://tiles.openfreemap.org/`.
- **Esri World Imagery**: Fetches satellite raster tiles from `https://server.arcgisonline.com/`.
- **OpenStreetMap Nominatim**: Used for fallback reverse-geocoding when system geocoder is unavailable.
- **Internet Permission**: Required for downloading map tiles and geocoding queries. The rest of the app (reminders, alarms, geofencing, daily tasks, history) is 100% offline-first.
