#Requires -Version 7
<#
.SYNOPSIS
    Krok 6: Budowanie i instalacja aplikacji KofeinoTracker na emulator.
.DESCRIPTION
    Uzywa Gradle wrapper do zbudowania APK modulu 'wear' i instalacji na emulatorze.
    Wymaga uruchomionego emulatora (krok 5).
.EXAMPLE
    .\06_build_and_install.ps1
#>

$ErrorActionPreference = "Stop"

$projectDir = Split-Path $PSScriptRoot -Parent
$androidSdkRoot = "$env:USERPROFILE\Android\Sdk"
$env:ANDROID_SDK_ROOT = $androidSdkRoot
$env:PATH = "$androidSdkRoot\platform-tools;$env:PATH"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KofeinoTracker - Build & Install"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# --- Sprawdzenie emulatora ---
Write-Host "[1/4] Sprawdzanie emulatora..." -NoNewline
$devices = & adb devices | Select-String "emulator-\d+\s+device"
if (-not $devices) {
    Write-Host " FAIL" -ForegroundColor Red
    Write-Host "      Brak podlaczonego emulatora. Uruchom najpierw .\05_start_emulator.ps1" -ForegroundColor Yellow
    exit 1
}
Write-Host " OK ($devices)" -ForegroundColor Green

# --- Sprawdzenie Gradle wrapper ---
Write-Host "[2/4] Sprawdzanie Gradle wrapper..." -NoNewline
$gradlew = Join-Path $projectDir "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host " BRAK" -ForegroundColor Yellow
    Write-Host "      Generowanie wrappera..." -ForegroundColor Cyan
    # Jesli masz zainstalowany Gradle systemowy:
    # gradle wrapper --gradle-version 8.11
    Write-Host "      Pobierz gradle wrapper lub zainstaluj Android Studio raz aby go wygenerowac." -ForegroundColor Yellow
    Write-Host "      Alternatywnie: https://services.gradle.org/distributions/gradle-8.11-bin.zip" -ForegroundColor Yellow
    exit 1
}
Write-Host " OK" -ForegroundColor Green

# --- Budowanie ---
Write-Host "[3/4] Budowanie Wear OS APK (debug)..." -ForegroundColor Cyan
Set-Location $projectDir
& $gradlew.bat :wear:assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD FAILED" -ForegroundColor Red
    exit 1
}
Write-Host "Build zakonczony sukcesem!" -ForegroundColor Green

# --- Instalacja ---
Write-Host "[4/4] Instalacja na emulatorze..." -NoNewline
& $gradlew.bat :wear:installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host " INSTALL FAILED" -ForegroundColor Red
    exit 1
}
Write-Host " OK" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Aplikacja zainstalowana na emulatorze!"
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Kolejne kroki:" -ForegroundColor Cyan
Write-Host "  - Sprawdz logi:  adb logcat -s KofeinoTracker:D" -ForegroundColor White
Write-Host "  - Zrzut ekranu:  adb shell screencap /sdcard/screen.png && adb pull /sdcard/screen.png" -ForegroundColor White
Write-Host "  - Testy jednostkowe: .\07_run_tests.ps1 unit" -ForegroundColor White
Write-Host "  - Testy UI (wymagaja emulatora): .\07_run_tests.ps1 ui" -ForegroundColor White
Write-Host ""
