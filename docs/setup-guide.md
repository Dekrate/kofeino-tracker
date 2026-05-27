# Uruchomienie Wear OS emulatora i aplikacji z CLI (Windows PowerShell)

Ten przewodnik pokazuje, jak w pelni zainstalować Android SDK, utworzyć emulator Wear OS i zbudować/zainstalować aplikacje **KofeinoTracker** bez otwierania Android Studio (czyste CLI).

---

## 1. Wymagania wstepne

- Windows 10/11 (PowerShell 7+)
- Właczona wirtualizacja w BIOS/UEFI:
  - Intel: **VT-x** / **Intel Virtualization Technology**
  - AMD: **SVM Mode** / **AMD-V**
  - Sprawdzenie w PowerShell: `systeminfo | findstr /i "Hyper-V"`
- Zainstalowany **JDK 21** (np. Eclipse Temurin): `winget install EclipseAdoptium.Temurin.21.JDK`
- Git: `winget install Git.Git`

---

## 2. Instalacja Android SDK z command line

### 2.1 Pobranie i rozpakowanie command line tools

```powershell
# Utworz folder SDK
$env:ANDROID_SDK_ROOT = "$env:USERPROFILE\Android\Sdk"
New-Item -ItemType Directory -Path "$env:ANDROID_SDK_ROOT\cmdline-tools" -Force | Out-Null

# Pobierz najnowsze command line tools
$url = "https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip"
$zip = "$env:TEMP\cmdline-tools.zip"
Invoke-WebRequest -Uri $url -OutFile $zip
Expand-Archive -Path $zip -DestinationPath "$env:ANDROID_SDK_ROOT\cmdline-tools" -Force
Remove-Item $zip

# Przenies do poprawnej struktury katalogow
Move-Item "$env:ANDROID_SDK_ROOT\cmdline-tools\cmdline-tools" "$env:ANDROID_SDK_ROOT\cmdline-tools\latest" -Force

# Dodaj do PATH (dla biezacej sesji)
$env:PATH = "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:PATH"
```

> Aby trwale dodać `ANDROID_SDK_ROOT` i PATH, dodaj je w **Zmienne srodowiskowe systemu** lub w profilu PowerShell (`$PROFILE`).

### 2.2 Akceptacja licencji i instalacja pakietow

```powershell
# Akceptuj wszystkie licencje (wymagane tylko raz)
sdkmanager.bat --licenses

# Zainstaluj podstawowe narzedzia
sdkmanager.bat "platform-tools" "emulator" "build-tools;36.0.0"

# Zainstaluj platforme Wear OS 6 (API 36)
sdkmanager.bat "platforms;android-36"

# Zainstaluj obraz systemu Wear OS (x86_64)
sdkmanager.bat "system-images;android-36;android-wear;x86_64"
```

> W razie potrzeby starszej wersji Wear OS (np. Wear OS 4, API 33):
> ```powershell
> sdkmanager.bat "system-images;android-33;android-wear;x86_64"
> ```

---

## 3. Tworzenie AVD (Wear OS) z command line

```powershell
# Utworz urzadzenie wirtualne Wear OS
$avdName = "Wear_OS_6_Kofeino"
avdmanager.bat create avd `
    --name $avdName `
    --device "wearos_small_round" `
    --package "system-images;android-36;android-wear;x86_64" `
    --abi "x86_64" `
    --sdcard 512M `
    --force
```

> Dostepne urzadzenia Wear OS sprawdzisz poleceniem: `avdmanager.bat list device | Select-String "wear"`
> Dostepne obrazy systemu: `sdkmanager.bat --list | Select-String "android-wear"`

---

## 4. Uruchomienie emulatora

```powershell
# Start emulatora Wear OS
emulator.bat -avd $avdName -no-snapshot-load

# Opcjonalne flagi:
# -no-window        -> bez okna (headless, przydatne do CI)
# -no-boot-anim     -> bez animacji bootowania (szybszy start)
# -gpu swiftshader_indirect -> software rendering (jesli GPU nie dziala)
# -netdelay none -netspeed full -> pelna predkosc sieci
```

> Emulator uruchomi sie jako osobne okno. Pierwszy rozruch trwa 1-3 minuty.

---

## 5. Budowanie i instalacja aplikacji (Gradle CLI)

W katalogu projektu (`D:\projekty edukacyjne\watch_sample`) uruchom:

```powershell
# Upewnij sie, ze wrapper jest wykonywalny (jesli pobrany z repo)
# .\gradlew.bat :wear:assembleDebug

# Buduj debug APK
.\gradlew.bat :wear:assembleDebug

# Instaluj na emulatorze (podlaczonym przez adb)
.\gradlew.bat :wear:installDebug
```

> Gradle wrapper (`gradlew.bat`) zostanie automatycznie pobrany przy pierwszym uzyciu.

---

## 6. Obsluga adb z CLI

```powershell
# Sprawdz podlaczone urzadzenia (emulator powinien byc widoczny)
adb devices

# Instaluj recznie APK (jesli nie przez gradle)
adb install -r .\wear\build\outputs\apk\debug\wear-debug.apk

# Zaloguj sie do emulatora
adb shell

# Pobierz logi z aplikacji
adb logcat -s KofeinoTracker:D

# Zrzut ekranu
adb shell screencap /sdcard/screen.png
adb pull /sdcard/screen.png .\screen.png
```

---

## 7. Uruchamianie testow z CLI

```powershell
# Testy jednostkowe (JVM + Robolectric)
.\gradlew.bat :wear:testDebugUnitTest

# Testy instrumentowane (wymagaja uruchomionego emulatora)
.\gradlew.bat :wear:connectedDebugAndroidTest

# Wszystkie testy naraz
.\gradlew.bat :wear:test
```

---

## 8. Pelny skrypt startowy `start_emulator.ps1`

Zapisz w katalogu projektu i uruchom jako administrator (tylko przy pierwszej konfiguracji HAXM, potem zwykly uzytkownik):

```powershell
# start_emulator.ps1
$ErrorActionPreference = "Stop"

$env:ANDROID_SDK_ROOT = "$env:USERPROFILE\Android\Sdk"
$env:PATH = "$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:ANDROID_SDK_ROOT\emulator;$env:PATH"

$avdName = "Wear_OS_6_Kofeino"

# Sprawdz czy AVD istnieje
$avdList = avdmanager.bat list avd | Out-String
if ($avdList -notmatch $avdName) {
    Write-Host "AVD nie istnieje. Tworzenie..."
    avdmanager.bat create avd --name $avdName --device "wearos_small_round" --package "system-images;android-36;android-wear;x86_64" --abi "x86_64" --force
}

Write-Host "Uruchamianie emulatora Wear OS: $avdName"
emulator.bat -avd $avdName -no-snapshot-load -no-boot-anim
```

---

## 9. Pelny skrypt budowania `build_and_install.ps1`

```powershell
# build_and_install.ps1
$ErrorActionPreference = "Stop"

Write-Host "Budowanie KofeinoTracker (Wear OS)..."
.\gradlew.bat :wear:assembleDebug

Write-Host "Instalacja na emulatorze..."
.\gradlew.bat :wear:installDebug

Write-Host "Gotowe! Sprawdz logi: adb logcat -s KofeinoTracker:D"
```

---

## 10. Czyszczenie i usuwanie emulatora

```powershell
# Wylacz emulator (jesli dziala w tle)
adb emu kill

# Usun AVD
avdmanager.bat delete avd --name "Wear_OS_6_Kofeino"

# Usun obraz systemu (zaoszczedz miejsce)
sdkmanager.bat --uninstall "system-images;android-36;android-wear;x86_64"
```

---

## 11. Rozwiazywanie problemow

| Problem | Rozwiazanie |
|---------|-------------|
| `emulator: ERROR: x86_64 emulation currently requires hardware acceleration` | Wlacz **Hyper-V** lub **Intel HAXM** / **Android Emulator Hypervisor Driver** (`sdkmanager "extras;intel;Hardware_Accelerated_Execution_Manager"`) |
| `adb devices` pokazuje `unauthorized` | Na emulatorze zaakceptuj debugowanie USB (okno dialogowe) |
| Gradle wrapper nie istnieje | `gradle wrapper --gradle-version 8.11` lub `.\gradlew.bat wrapper` |
| `JAVA_HOME` nie ustawione | `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"` |
| Emulator bardzo wolny | Zwieksz RAM w AVD: `avdmanager.bat create avd ... --ram 2048` lub edytuj `config.ini` w `%USERPROFILE%\.android\avd\Wear_OS_6_Kofeino.avd\` |

---

## 12. Ustawienie języka polskiego w emulatorze

Aby aplikacja poprawnie wyświetlała polskie napisy, emulator musi mieć ustawioną **polską lokalizację**:

```powershell
# Tymczasowo (na czas sesji):
adb shell settings put global system_locale pl-PL
# Wymaga restartu emulatora (cold boot):
adb emu kill

# Trwale — dodaj do config.ini AVD przed cold bootem:
$configPath = "$env:USERPROFILE\.android\avd\Wear_OS_6_Kofeino.avd\config.ini"
Add-Content -Path $configPath -Value "hw.locale=pl-PL"

# Uruchom z cold bootem (bez snapshotu):
emulator.bat -avd Wear_OS_6_Kofeino -no-snapshot-load
```

Po ustawieniu locale na `pl-PL` aplikacja wyświetla polskie komunikaty, daty i formatowanie liczb zgodne z regionalnymi ustawieniami.

---

## 13. Development Setup: Running Both Apps with Sync (English)

This section covers how to run both the Wear OS watch app (`:wear`) and the Android phone companion app (`:app`) simultaneously with cross-device synchronization enabled.

### 13.1 Prerequisites

- Both apps must be installed on their respective devices
- Wear OS emulator + phone emulator (or physical devices running both)
- Both devices must be signed into the **same Google account**
- Google Play Services is required on both sides (available on emulators by default)

> **Note:** On the Wear OS emulator, open Settings → Accounts → Add account and sign in with a Google account. Do the same on the phone emulator via Settings → Accounts. Without a Google account, the Wearable Data Layer cannot establish a connection.
- Bluetooth and/or WiFi must be enabled on both devices

### 13.2 Building Both Apps

```bash
# Build the Wear OS watch app
./gradlew :wear:assembleDebug

# Build the phone companion app
./gradlew :app:assembleDebug

# Install both on connected devices
./gradlew :wear:installDebug :app:installDebug
```

If you have both emulators running simultaneously, `adb` routes correctly as long as each APK targets the right device type.

### 13.3 Enabling Sync

1. Install both apps on their respective devices or emulators.
2. On the **watch**, navigate to **Cross-Device Sync** from the app's main menu.
3. Tap **Start Sync** — this launches the foreground `WearableSyncService` that manages the Wearable Data Layer connection.
4. On the **phone**, open the companion app and navigate to **Cross-Device Sync**.
5. Both devices should display **"Synced"** status once the connection is established.

> Sync uses Google's Wearable Data Layer API under the hood. Messages and data items are exchanged over Bluetooth (when paired) or WiFi (when on the same network).

### 13.4 Verifying Sync

1. **Log a drink on the watch** — it should appear on the phone within seconds (typically < 5 s).
2. **Log a drink on the phone** — it should sync to the watch automatically.
3. **Turn off Bluetooth/WiFi** — changes should queue locally in the pending sync queue.
4. **Reconnect** — queued changes should be flushed and synced automatically (exponential backoff up to 16 s).

Expected behavior: both devices converge to the same state within seconds under normal connectivity.

### 13.5 Troubleshooting Sync Issues

| Issue | Solution |
|-------|----------|
| "No paired device" shown | Ensure both devices are on the same Google account and have Bluetooth/WiFi enabled |
| Sync stuck on "Syncing..." | Check device connectivity and restart the sync service |
| Data not appearing on other device | Wait for queue flush (up to 16 s backoff) or restart sync service |
| Conflict resolution warnings | Check device clock sync — devices should be within 60 s of each other |
| Wearable Sync Service not running (watch) | Restart the app or use `adb shell am startservice -n pl.dekrate.kofeino/.data.sync.WearableSyncService -a pl.dekrate.kofeino.action.START_SYNC` |
| Wearable Sync Service not running (phone) | Restart the app or use `adb shell am force-stop pl.dekrate.kofeino.tracker` and re-open the app |

### 13.6 Using adb to Test Sync

```bash
# --- Watch commands ---
# Check sync service is running
adb shell dumpsys activity services pl.dekrate.kofeino/.data.sync.WearableSyncService

# Force-start sync service on watch
adb shell am startservice -n pl.dekrate.kofeino/.data.sync.WearableSyncService -a pl.dekrate.kofeino.action.START_SYNC

# Force-stop sync service on watch
adb shell am startservice -n pl.dekrate.kofeino/.data.sync.WearableSyncService -a pl.dekrate.kofeino.action.STOP_SYNC

# --- Phone commands ---
# The phone sync service starts automatically with the app.
# To restart: force-stop and reopen
adb shell am force-stop pl.dekrate.kofeino.tracker

# --- Logs (from whichever device is connected) ---
adb logcat -s WearableSyncService:D RealTimeSyncService:D FullSyncManager:D
```

### 13.7 Architecture Note on Testing Sync Locally

Since the Wearable Data Layer requires Google Play Services, local testing with two emulators works best when both are signed into the same Google account. For automated testing, the sync layer is covered by extensive unit tests with mocked `MessageClient` and `CapabilityClient` — no physical devices required.

---

## Podsumowanie komend

```powershell
# Wszystko w jednym podejsciu:
# 1. Zainstaluj SDK + emulator (raz)
# 2. Utworz AVD (raz)
# 3. Uruchom emulator: emulator.bat -avd Wear_OS_6_Kofeino
# 4. Zbuduj i zainstaluj: .\gradlew.bat :wear:installDebug
# 5. Testy: .\gradlew.bat :wear:testDebugUnitTest
```

**Bezpieczenstwo:** Wszystkie pobierane pliki pochodza bezposrednio z `dl.google.com` i `maven.google.com`. Zadne narzedzie nie wymaga uprawnien administratora do codziennej pracy (poza jednorazowa instalacja HAXM/Hyper-V).
