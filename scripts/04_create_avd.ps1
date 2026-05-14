#Requires -Version 7
<#
.SYNOPSIS
    Krok 4: Tworzenie urzadzenia wirtualnego Wear OS (AVD).
.DESCRIPTION
    Tworzy AVD o nazwie 'Wear_OS_6_Kofeino' z obrazem Wear OS 6.
    Jesli AVD istnieje, pyta o nadpisanie.
.EXAMPLE
    .\04_create_avd.ps1
#>

$ErrorActionPreference = "Stop"

$androidSdkRoot = "$env:USERPROFILE\Android\Sdk"
$env:ANDROID_SDK_ROOT = $androidSdkRoot
$env:PATH = "$androidSdkRoot\cmdline-tools\latest\bin;$androidSdkRoot\platform-tools;$androidSdkRoot\emulator;$env:PATH"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KofeinoTracker - Tworzenie AVD"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$avdName = "Wear_OS_6_Kofeino"

# --- Sprawdzenie czy AVD istnieje ---
Write-Host "[1/3] Sprawdzanie istniejacych AVD..." -NoNewline
$avdmanager = "$androidSdkRoot\cmdline-tools\latest\bin\avdmanager.bat"
$existing = & $avdmanager list avd -c 2>$null | Select-String $avdName
if ($existing) {
    Write-Host " ZNALEZIONO" -ForegroundColor Yellow
    $overwrite = Read-Host "AVD '$avdName' juz istnieje. Nadpisac? (t/n)"
    if ($overwrite -ne 't') {
        Write-Host "Pominięto tworzenie AVD. Uzyj istniejacego." -ForegroundColor Cyan
        Write-Host "Nastepny krok: .\05_start_emulator.ps1" -ForegroundColor Cyan
        exit 0
    }
    & $avdmanager delete avd --name $avdName
}
Write-Host " OK" -ForegroundColor Green

# --- Tworzenie AVD ---
Write-Host "[2/3] Tworzenie urzadzenia wirtualnego Wear OS..." -NoNewline
& $avdmanager create avd `
    --name $avdName `
    --device "wearos_small_round" `
    --package "system-images;android-36;android-wear-signed;x86_64" `
    --abi "x86_64" `
    --sdcard 512M `
    --force 2>$null
Write-Host " OK" -ForegroundColor Green

# --- Optymalizacja config.ini ---
Write-Host "[3/3] Optymalizacja konfiguracji AVD..." -NoNewline
$avdConfigDir = "$env:USERPROFILE\.android\avd\${avdName}.avd"
$configFile = "$avdConfigDir\config.ini"
if (Test-Path $configFile) {
    $config = Get-Content $configFile
    $config = $config -replace "^hw.ramSize=.*", "hw.ramSize=2048"
    $config = $config -replace "^hw.heapSize=.*", "hw.heapSize=576"
    $config = $config -replace "^disk.dataPartition.size=.*", "disk.dataPartition.size=2048M"
    $config = $config -replace "^hw.keyboard=.*", "hw.keyboard=yes"
    $config | Set-Content $configFile
}
Write-Host " OK" -ForegroundColor Green

Write-Host ""
Write-Host "AVD '$avdName' utworzone i skonfigurowane!" -ForegroundColor Green
Write-Host "Nastepny krok: .\05_start_emulator.ps1" -ForegroundColor Cyan
