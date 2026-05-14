#Requires -Version 7
<#
.SYNOPSIS
    Krok 2: Instalacja Android SDK command line tools.
.DESCRIPTION
    Pobiera i rozpakowuje oficjalne command line tools do %USERPROFILE%\Android\Sdk.
    Nie nadpisuje istniejacej instalacji (bezpieczne).
.EXAMPLE
    .\02_install_sdk.ps1
#>

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KofeinoTracker - Instalacja Android SDK"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$androidSdkRoot = "$env:USERPROFILE\Android\Sdk"
$cmdlineDir = "$androidSdkRoot\cmdline-tools"

# --- Utworzenie katalogow ---
Write-Host "[1/4] Przygotowanie katalogow SDK..." -NoNewline
if (-not (Test-Path $androidSdkRoot)) {
    New-Item -ItemType Directory -Path $androidSdkRoot -Force | Out-Null
}
Write-Host " OK" -ForegroundColor Green
Write-Host "      Sciezka SDK: $androidSdkRoot" -ForegroundColor Cyan

# --- Pobranie command line tools ---
Write-Host "[2/4] Pobieranie command line tools..." -NoNewline
$zipUrl = "https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip"
$zipPath = "$env:TEMP\cmdline-tools-win.zip"

if (Test-Path "$cmdlineDir\latest\bin\sdkmanager.bat") {
    Write-Host " POMINIETO (juz zainstalowane)" -ForegroundColor Green
} else {
    try {
        Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath -MaximumRedirection 5 -TimeoutSec 120
        Write-Host " OK" -ForegroundColor Green
    } catch {
        Write-Host " FAIL" -ForegroundColor Red
        Write-Host "      Blad pobierania: $_" -ForegroundColor Red
        exit 1
    }
}

# --- Rozpakowanie ---
Write-Host "[3/4] Rozpakowywanie..." -NoNewline
if (Test-Path $zipPath) {
    if (Test-Path "$cmdlineDir\cmdline-tools") {
        Remove-Item -Recurse -Force "$cmdlineDir\cmdline-tools" -ErrorAction SilentlyContinue
    }
    Expand-Archive -Path $zipPath -DestinationPath $cmdlineDir -Force
    Remove-Item $zipPath -Force
    Write-Host " OK" -ForegroundColor Green
} else {
    Write-Host " POMINIETO" -ForegroundColor Green
}

# --- Ustawienie poprawnej struktury ---
Write-Host "[4/4] Konfiguracja struktury katalogow..." -NoNewline
if (Test-Path "$cmdlineDir\cmdline-tools") {
    if (Test-Path "$cmdlineDir\latest") {
        Remove-Item -Recurse -Force "$cmdlineDir\latest" -ErrorAction SilentlyContinue
    }
    Rename-Item -Path "$cmdlineDir\cmdline-tools" -NewName "latest" -Force
}
Write-Host " OK" -ForegroundColor Green

# --- Wskazowki dla uzytkownika ---
Write-Host ""
Write-Host "Android SDK command line tools zainstalowane!" -ForegroundColor Green
Write-Host ""
Write-Host "Dodaj do zmiennych srodowiskowych (Systemowe lub w profilu PowerShell):" -ForegroundColor Cyan
Write-Host "  [Environment]::SetEnvironmentVariable('ANDROID_SDK_ROOT', '$androidSdkRoot', 'User')" -ForegroundColor White
Write-Host "  `$oldPath = [Environment]::GetEnvironmentVariable('Path', 'User')" -ForegroundColor White
Write-Host "  [Environment]::SetEnvironmentVariable('Path', `"`$oldPath;$cmdlineDir\latest\bin;$androidSdkRoot\platform-tools;$androidSdkRoot\emulator`", 'User')" -ForegroundColor White
Write-Host ""
Write-Host "Nastepny krok: .\03_install_packages.ps1" -ForegroundColor Cyan
