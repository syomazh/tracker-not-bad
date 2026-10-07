# Build a Sleep & Energy Tracker — Android Phone + Wear OS Watch App

## Overview
Build a complete Android project with two modules: a **phone app** and a **Wear OS watch app**. They sync via the Wear OS Data Layer API. The app tracks sleep (bedtime + wake time) and a daily energy rating. Data lives on the phone and can be exported as CSV.

This is sideloaded (not published to Play Store), for personal use only.

## Target Devices
- **Phone:** Samsung Galaxy S21 FE 5G (Android 12+)
- **Watch:** Samsung Galaxy Watch 4 (Wear OS 3, API 30+)

## Tech Stack
- **Language:** Kotlin
- **Build:** Gradle with version catalogs (modern Android Studio default)
- **Phone app:** Jetpack Compose, Material 3, Room database
- **Watch app:** Compose for Wear OS, Wear Material 3
- **Sync:** Wear OS Data Layer API (MessageClient / DataClient)
- **Min SDK:** Phone = 31, Watch = 30
- **Target SDK:** 34

## App Behavior — Phone

### Main Screen (3 big buttons + log list below)
1. **"Going to Bed" button** — taps log the current timestamp as a bedtime entry. Shows a brief confirmation toast/snackbar with the logged time. The entry appears in the log list below.
2. **"I'm Awake" button** — taps log the current timestamp as a wake-up entry. Same confirmation behavior.
3. **"Rate Energy" button** — opens a simple 1–5 selector (big tappable numbers or stars). One rating per calendar day. If the user rates again the same day, it **overwrites** the previous rating for that day. Shows confirmation with the rating.

### Log List (below the buttons)
- Scrollable list of all entries, newest first
- Each entry shows: type (Bedtime / Wake-up / Energy), timestamp, and value (time for sleep events, 1-5 for energy)
- Tap any entry to **edit its time** (shows a time picker dialog) or delete it
- Keep it simple — just a flat chronological list, no grouping needed

### Export
- A small "Export CSV" button (top bar or bottom)
- Exports ALL data as a CSV file with columns: `date, type, time, value`
  - For bedtime/wake-up: date is the calendar date, time is the HH:MM, value is empty
  - For energy: date is the calendar date, time is when it was logged, value is 1-5
- Uses Android's share sheet so the user can save to Files, send to Drive, email, etc.

### Theme
- **Dark theme only** (no light mode toggle needed)
- Clean, minimal, calming colors — think dark navy/charcoal background, muted blue or purple accents
- Large tap targets, nothing tiny

## App Behavior — Watch

### Single Screen (3 big buttons stacked vertically, scrollable)
1. **"Bed" button** — sends a message to the phone app via Data Layer to log bedtime NOW. Shows a brief confirmation on watch (checkmark + time).
2. **"Awake" button** — same, logs wake-up time.
3. **"Energy" button** — shows a quick 1–5 picker (5 big number buttons in a row or grid), sends the rating to the phone.

That's it. No history view on the watch, no settings. Just 3 buttons.

### Watch Design
- Very large buttons that are easy to tap on a small round screen
- Dark background matching the phone app
- Minimal text, clear icons or labels

## Data Sync (Phone ↔ Watch)
- Watch sends messages to phone via `MessageClient`
- Phone receives messages via `WearableListenerService`, parses them, and writes to Room database
- Message format can be simple: `/bedtime|{timestamp}`, `/wakeup|{timestamp}`, `/energy|{rating}|{timestamp}`
- Phone app should work fully standalone too (if someone only uses the phone buttons, no watch needed)

## Database (Room — Phone only)
Single table `sleep_logs`:
- `id` (auto-increment primary key)
- `type` (enum: BEDTIME, WAKEUP, ENERGY)
- `timestamp` (Long — epoch millis)
- `value` (Int, nullable — only used for energy rating 1-5)

## Project Structure
```
sleep-tracker/
├── build.gradle.kts (project-level)
├── settings.gradle.kts
├── gradle/libs.versions.toml
├── app/                          # Phone module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/sleeptracker/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── SleepLog.kt          (Room entity)
│       │   │   ├── SleepLogDao.kt       (Room DAO)
│       │   │   └── AppDatabase.kt       (Room database)
│       │   ├── ui/
│       │   │   ├── theme/Theme.kt
│       │   │   ├── MainScreen.kt
│       │   │   ├── EnergyPicker.kt
│       │   │   └── EditLogDialog.kt
│       │   ├── export/CsvExporter.kt
│       │   └── sync/WatchListenerService.kt
│       └── res/
│           ├── values/strings.xml
│           └── values/colors.xml
├── watch/                        # Wear OS module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/sleeptracker/watch/
│           ├── MainActivity.kt
│           ├── ui/
│           │   ├── theme/Theme.kt
│           │   └── MainScreen.kt
│           └── sync/PhoneSync.kt
```

## Important Notes
- Make sure both modules have the **same applicationId** (`com.sleeptracker`) — this is required for Data Layer communication between phone and watch.
- Watch module needs `com.google.android.wearable` and `com.google.android.gms.wearable` in its manifest.
- Phone module needs the `WearableListenerService` declared in its manifest with the right intent filter.
- Make sure Gradle dependencies are compatible — use BOM for Compose, check Wear Compose version compatibility with Wear OS 3.
- The app should compile and run without errors. Test-build both modules mentally before outputting.

## Deliverable
Give me the **complete, ready-to-open project**. Every file, every line. I will open this folder in Android Studio and hit Run. Do not leave TODOs or placeholders — write the actual implementation.

After the project, give me a **step-by-step Android Studio setup guide** assuming I've never opened it before:
1. How to install Android Studio
2. How to open this project
3. How to enable developer mode on Galaxy S21 FE
4. How to enable developer mode on Galaxy Watch 4
5. How to run the phone app
6. How to run the watch app
7. How to pair them
