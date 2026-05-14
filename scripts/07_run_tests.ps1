#Requires -Version 7
<#
.SYNOPSIS
    Krok 7: Uruchamianie testow (jednostkowe lub UI).
.DESCRIPTION
    Uruchamia testy jednostkowe (JVM) lub instrumentowane (UI/E2E na emulatorze).
    Parametr: unit | ui | all
.EXAMPLE
    .\07_run_tests.ps1 unit
    .\07_run_tests.ps1 ui
    .\07_run_tests.ps1 all
#>

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("unit","ui","all")]
    [string]$Type
)

$ErrorActionPreference = "Stop"

$projectDir = Split-Path $PSScriptRoot -Parent
$androidSdkRoot = "$env:USERPROFILE\Android\Sdk"
$env:ANDROID_SDK_ROOT = $androidSdkRoot
$env:PATH = "$androidSdkRoot\platform-tools;$env:PATH"

Set-Location $projectDir
$gradlew = Join-Path $projectDir "gradlew.bat"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KofeinoTracker - Testy"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($Type -eq "unit" -or $Type -eq "all") {
    Write-Host "[UNIT] Uruchamianie testow jednostkowych..." -ForegroundColor Cyan
    & $gradlew.bat :wear:testDebugUnitTest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "TESTY JEDNOSTKOWE NIE POWIODLY SIE" -ForegroundColor Red
        if ($Type -eq "unit") { exit 1 }
    } else {
        Write-Host "Testy jednostkowe: SUKCES" -ForegroundColor Green
    }
}

if ($Type -eq "ui" -or $Type -eq "all") {
    Write-Host "[UI] Sprawdzanie emulatora..." -NoNewline
    $devices = & adb devices | Select-String "emulator-\d+\s+device"
    if (-not $devices) {
        Write-Host " FAIL" -ForegroundColor Red
        Write-Host "Brak emulatora. Uruchom .\05_start_emulator.ps1" -ForegroundColor Yellow
        exit 1
    }
    Write-Host " OK" -ForegroundColor Green

    Write-Host "[UI] Uruchamianie testow instrumentowanych..." -ForegroundColor Cyan
    & $gradlew.bat :wear:connectedDebugAndroidTest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "TESTY UI NIE POWIODLY SIE" -ForegroundColor Red
        exit 1
    } else {
        Write-Host "Testy UI: SUKCES" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Wszystkie testy zakonczone!" -ForegroundColor Green
