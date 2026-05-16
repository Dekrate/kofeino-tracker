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
$avdName = "Wear_OS_6_API_36"
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

$avdName = "Wear_OS_6_API_36"

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
avdmanager.bat delete avd --name "Wear_OS_6_API_36"

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
| Emulator bardzo wolny | Zwieksz RAM w AVD: `avdmanager.bat create avd ... --ram 2048` lub edytuj `config.ini` w `%USERPROFILE%\.android\avd\Wear_OS_6_API_36.avd\` |

---

## Podsumowanie komend

```powershell
# Wszystko w jednym podejsciu:
# 1. Zainstaluj SDK + emulator (raz)
# 2. Utworz AVD (raz)
# 3. Uruchom emulator: emulator.bat -avd Wear_OS_6_API_36
# 4. Zbuduj i zainstaluj: .\gradlew.bat :wear:installDebug
# 5. Testy: .\gradlew.bat :wear:testDebugUnitTest
```

**Bezpieczenstwo:** Wszystkie pobierane pliki pochodza bezposrednio z `dl.google.com` i `maven.google.com`. Zadne narzedzie nie wymaga uprawnien administratora do codziennej pracy (poza jednorazowa instalacja HAXM/Hyper-V).
