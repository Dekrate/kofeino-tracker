@echo off
chcp 65001 >nul
echo ========================================
echo   KofeinoTracker - Menu startowe
echo ========================================
echo.
echo Wybierz krok:
echo   1. Sprawdz srodowisko         (01_check_environment.ps1)
echo   2. Zainstaluj Android SDK     (02_install_sdk.ps1)
echo   3. Zainstaluj pakiety         (03_install_packages.ps1)
echo   4. Utworz emulator Wear OS    (04_create_avd.ps1)
echo   5. Uruchom emulator           (05_start_emulator.ps1)
echo   6. Zbuduj i zainstaluj app    (06_build_and_install.ps1)
echo   7. Uruchom testy              (07_run_tests.ps1)
echo   0. Wyjdz
echo.
set /p choice="Wybierz numer: "

if "%choice%"=="1" pwsh -ExecutionPolicy Bypass -File "%~dp0scripts\01_check_environment.ps1"
if "%choice%"=="2" pwsh -ExecutionPolicy Bypass -File "%~dp0scripts\02_install_sdk.ps1"
if "%choice%"=="3" pwsh -ExecutionPolicy Bypass -File "%~dp0scripts\03_install_packages.ps1"
if "%choice%"=="4" pwsh -ExecutionPolicy Bypass -File "%~dp0scripts\04_create_avd.ps1"
if "%choice%"=="5" pwsh -ExecutionPolicy Bypass -File "%~dp0scripts\05_start_emulator.ps1"
if "%choice%"=="6" pwsh -ExecutionPolicy Bypass -File "%~dp0scripts\06_build_and_install.ps1"
if "%choice%"=="7" (
    set /p t="Jakie testy? (unit/ui/all): "
    pwsh -ExecutionPolicy Bypass -File "%~dp0scripts\07_run_tests.ps1" %t%
)
if "%choice%"=="0" exit /b 0

pause
