# Sleep Tracker (phone + Galaxy Watch)

A personal sleep and energy log. The phone app stores everything in a local Room database and
exports CSV. The Wear OS app is a 3-button remote that sends events to the phone over the
Wear OS Data Layer.

| Module  | Device                                | UI                         | minSdk / targetSdk |
|---------|---------------------------------------|----------------------------|--------------------|
| `app`   | Galaxy S21 FE (Android 12+)           | Jetpack Compose, Material 3 | 31 / 34            |
| `watch` | Galaxy Watch 4 (Wear OS 3+)           | Compose for Wear OS, Wear Material 3 | 30 / 34   |

Both modules use the application ID `com.sleeptracker`. That shared ID, plus the shared debug
signing key you get by installing both from the same computer, is what lets the Data Layer
connect them.

Toolchain: Android Gradle Plugin 9.4.1, Gradle 9.6.1, Kotlin 2.4.20, Compose BOM 2026.09.00,
Wear Compose 1.7.0, Room 2.8.5, compileSdk 37 (current AndroidX libraries require it; the apps
still target SDK 34). Opens in Android Studio Quail 4 or newer (Rabbit 1 | 2026.2.1 is the
current stable release).

## How it works

- **Phone buttons** log the current time as Bedtime or Wake-up. **Rate Energy** saves a 1-5
  rating. There is one rating per calendar day; rating again the same day replaces it.
- **Tap a list entry** to change its time or date, change an energy rating, or delete it.
- **Export CSV** writes `date,type,time,value` rows (oldest first) and opens the share sheet.
- **Watch buttons** send `/bedtime|{timestamp}`, `/wakeup|{timestamp}` or
  `/energy|{rating}|{timestamp}` to the phone. The path is the message path, the rest is the
  payload. The watch shows a checkmark and the logged time.
- **Phone out of range?** The watch saves the event as a Data Layer item instead. It reaches
  the phone automatically when they reconnect, and the watch says "Syncs when phone is near".
  Late energy ratings never overwrite a newer rating. Duplicates are ignored.

Key files:

```
app/src/main/java/com/sleeptracker/
  data/        SleepLog (entity), SleepLogDao, AppDatabase, SleepRepository (logging rules)
  ui/          MainScreen, EnergyPicker, EditLogDialog, MainViewModel, theme/Theme.kt
  export/      CsvExporter
  sync/        WatchListenerService (receives watch events), WatchSync (protocol)
watch/src/main/java/com/sleeptracker/watch/
  ui/          MainScreen (buttons, energy picker, confirmation), theme/Theme.kt
  sync/        PhoneSync (MessageClient, with DataClient fallback)
```

## Setup guide (first time with Android Studio)

### 1. Install Android Studio

1. Download Android Studio from https://developer.android.com/studio and run the installer.
   Keep the default options.
2. Start Android Studio. In the setup wizard choose **Standard**, keep the dark theme if you
   like, accept the licenses, and click **Finish**. It downloads the Android SDK, which takes a
   few minutes.

### 2. Open this project

1. On the Welcome screen click **Open** and pick the `sleep-tracker` folder (the one that
   contains `settings.gradle.kts`).
2. Click **Trust Project** when asked.
3. Wait for **Gradle sync** to finish (progress bar at the bottom right). The first sync
   downloads libraries and can take 5-10 minutes.
4. If a yellow banner says an SDK platform is missing, click the install link in it. If Studio
   offers to upgrade the Android Gradle Plugin, you can skip that.

### 3. Turn on developer mode on the Galaxy S21 FE

1. Open **Settings > About phone > Software information**.
2. Tap **Build number** 7 times and enter your PIN. You'll see "Developer mode has been turned on".
3. Go back to **Settings**, open **Developer options** (at the bottom), and turn on **USB debugging**.
4. If your phone has **Settings > Security and privacy > Auto Blocker**, turn it off. Otherwise it
   blocks app installs over USB.

### 4. Turn on developer mode on the Galaxy Watch 4

1. On the watch open **Settings > About watch > Software information**.
2. Tap **Software version** about 5 times until "Developer mode turned on" appears.
3. Open **Settings > Developer options** (at the bottom of Settings) and turn on **ADB debugging**
   and **Wireless debugging** (called **Debug over Wi-Fi** on some versions).
4. Open **Settings > Connections > Wi-Fi** and connect the watch to the same Wi-Fi network as
   your PC.

### 5. Run the phone app

1. Connect the phone to the PC with a USB cable.
2. On the phone, tap **Allow** on "Allow USB debugging?" and tick **Always allow from this computer**.
3. In Android Studio's top toolbar, pick **app** in the run-configuration dropdown and your phone
   (Samsung SM-G990...) in the device dropdown.
4. Click the green **Run** button. The app installs and opens on the phone.

If the phone doesn't appear, change the USB mode on the phone to **File transfer**, or install
the Samsung USB driver from https://developer.samsung.com/android-usb-driver.

### 6. Run the watch app

1. On the watch, open **Developer options > Wireless debugging** and tap **Pair new device**.
   The watch shows a 6-digit code.
2. In Android Studio, open the device dropdown and choose **Pair Devices Using Wi-Fi**. Select
   the **Pair using pairing code** tab, pick the watch, and type the code.
3. Pick **watch** in the run-configuration dropdown and the watch in the device dropdown.
4. Click **Run**. "Sleep Tracker" opens on the watch.

If Studio can't find the watch, pair from a terminal instead. Use the IP and ports shown on the
watch's Wireless debugging screens:

```
adb pair 192.168.1.50:41234      (pairing port + 6-digit code from "Pair new device")
adb connect 192.168.1.50:38765   (connection port from the main Wireless debugging screen)
```

### 7. Connect the phone and watch apps

1. The watch must already be paired with the phone through the **Galaxy Wearable** app, as in
   normal everyday use. Bluetooth must be on.
2. Install both apps from the same computer (steps 5 and 6). They then share a signing key,
   which the Data Layer requires.
3. Open the phone app once. Then on the watch tap **Bed**. The watch should show a checkmark,
   the time, and "Saved on phone". The entry appears in the phone's list right away.

If the watch says "Syncs when phone is near" while the phone is next to it, check that Bluetooth
is on and the phone app is installed, then open the phone app. Queued entries arrive within a
few seconds of reconnecting.

## Notes

- Debug builds are fine for personal use. Re-running from Android Studio updates the app and
  keeps your data. Uninstalling the phone app deletes the database, so export first.
- Wireless debugging on the watch turns off after a reboot. Repeat step 6 when you need to
  reinstall the watch app.
