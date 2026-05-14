#Requires -Version 7
<#
.SYNOPSIS
    Krok 5: Uruchomienie emulatora Wear OS.
.DESCRIPTION
    Uruchamia wczesniej utworzony AVD. W osobnym oknie.
    Pierwszy rozruch moze trwac 2-5 minut.
.EXAMPLE
    .\05_start_emulator.ps1
#>

$ErrorActionPreference = "Stop"

$androidSdkRoot = "$env:USERPROFILE\Android\Sdk"
$env:ANDROID_SDK_ROOT = $androidSdkRoot
$env:PATH = "$androidSdkRoot\emulator;$androidSdkRoot\platform-tools;$env:PATH"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KofeinoTracker - Start emulatora"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$avdName = "Wear_OS_6_Kofeino"
$emulatorExe = "$androidSdkRoot\emulator\emulator.exe"

if (-not (Test-Path $emulatorExe)) {
    Write-Host "ERROR: emulator.exe nie znaleziony. Uruchom najpierw skrypty 02-03." -ForegroundColor Red
    exit 1
}

Write-Host "[1/2] Sprawdzanie AVD..." -NoNewline
$avdList = & "$androidSdkRoot\cmdline-tools\latest\bin\avdmanager.bat" list avd -c 2>$null
if (-not ($avdList | Select-String $avdName)) {
    Write-Host " FAIL" -ForegroundColor Red
    Write-Host "      AVD '$avdName' nie istnieje. Uruchom .\04_create_avd.ps1" -ForegroundColor Yellow
    exit 1
}
Write-Host " OK" -ForegroundColor Green

Write-Host "[2/2] Uruchamianie emulatora Wear OS..." -ForegroundColor Cyan
Write-Host "      To zajmie kilka minut przy pierwszym uruchomieniu." -ForegroundColor Yellow
Write-Host "      Emulator otworzy sie w nowym oknie." -ForegroundColor Yellow
Write-Host ""

# Uruchom w tle (separate window)
Start-Process -FilePath $emulatorExe -ArgumentList @(
    "-avd", $avdName,
    "-no-snapshot-load",
    "-no-boot-anim",
    "-gpu", "swiftshader_indirect"
)

Write-Host "Emulator uruchomiony! Czekam na pelny boot..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

# Czekaj az adb widzi urzadzenie
$timeout = 300
$elapsed = 0
while ($elapsed -lt $timeout) {
    $devices = & adb devices | Select-String "emulator-\d+\s+device"
    if ($devices) {
        Write-Host "Emulator gotowy! ($devices)" -ForegroundColor Green
        Write-Host ""
        Write-Host "Nastepny krok: .\06_build_and_install.ps1" -ForegroundColor Cyan
        exit 0
    }
    Start-Sleep -Seconds 5
    $elapsed += 5
    Write-Host "  Oczekiwanie... ($elapsed s / $timeout s)" -ForegroundColor Yellow
}

Write-Host "Timeout oczekiwania na emulator. Sprawdz czy okno emulatora sie otworzylo." -ForegroundColor Red
