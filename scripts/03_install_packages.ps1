#Requires -Version 7
<#
.SYNOPSIS
    Krok 3: Instalacja pakietow Android (platform-tools, emulator, obraz Wear OS).
.DESCRIPTION
    Uzywa sdkmanager do zainstalowania wszystkich niezbednych komponentow.
    Automatycznie akceptuje licencje.
.EXAMPLE
    .\03_install_packages.ps1
#>

$ErrorActionPreference = "Stop"

$androidSdkRoot = "$env:USERPROFILE\Android\Sdk"
$env:ANDROID_SDK_ROOT = $androidSdkRoot
$env:PATH = "$androidSdkRoot\cmdline-tools\latest\bin;$androidSdkRoot\platform-tools;$androidSdkRoot\emulator;$env:PATH"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KofeinoTracker - Instalacja pakietow"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# --- Akceptacja licencji ---
Write-Host "[1/5] Akceptacja licencji Android SDK..." -ForegroundColor Cyan
$sdkmanager = "$androidSdkRoot\cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $sdkmanager)) {
    Write-Host "ERROR: sdkmanager.bat nie znaleziony. Uruchom najpierw 02_install_sdk.ps1" -ForegroundColor Red
    exit 1
}
# Automatyczna akceptacja wszystkich licencji
Write-Host "      Akceptowanie licencji automatycznie..." -ForegroundColor Yellow
echo "y`ny`ny`ny`ny`ny`ny`n" | & $sdkmanager --licenses 2>$null

Write-Host "[2/5] Instalacja platform-tools, emulator, build-tools..." -ForegroundColor Cyan
echo "y" | & $sdkmanager "platform-tools" "emulator" "build-tools;36.0.0"

Write-Host "[3/5] Instalacja platformy Android 36 (Wear OS 6)..." -ForegroundColor Cyan
echo "y" | & $sdkmanager "platforms;android-36"

Write-Host "[4/5] Instalacja obrazu systemu Wear OS 6 (x86_64)..." -ForegroundColor Cyan
Write-Host "      To zajmie kilka minut (~1.5 GB)..." -ForegroundColor Yellow
echo "y" | & $sdkmanager "system-images;android-36;android-wear-signed;x86_64"

Write-Host "[5/5] Weryfikacja instalacji..." -NoNewline
$emulator = "$androidSdkRoot\emulator\emulator.exe"
if (Test-Path $emulator) {
    Write-Host " OK" -ForegroundColor Green
} else {
    Write-Host " FAIL" -ForegroundColor Red
    Write-Host "      Emulator nie zostal zainstalowany poprawnie." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Wszystkie pakiety zainstalowane!" -ForegroundColor Green
Write-Host "Nastepny krok: .\04_create_avd.ps1" -ForegroundColor Cyan
