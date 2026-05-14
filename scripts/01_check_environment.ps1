#Requires -Version 7
<#
.SYNOPSIS
    Krok 1: Weryfikacja srodowiska przed instalacja Android SDK / emulatora Wear OS.
.DESCRIPTION
    Sprawdza:
    - PowerShell 7+
    - Wirtualizacje (Hyper-V / SVM / VT-x)
    - Zainstalowane JDK (17 lub 21)
    - Dostepne miejsce na dysku
    - Uprawnienia administratora (opcjonalnie, tylko dla HAXM)
.EXAMPLE
    .\01_check_environment.ps1
#>

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KofeinoTracker - Weryfikacja srodowiska"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allOk = $true

# --- 1. PowerShell version ---
Write-Host "[1/6] Sprawdzanie wersji PowerShell..." -NoNewline
if ($PSVersionTable.PSVersion.Major -ge 7) {
    Write-Host " OK (v$($PSVersionTable.PSVersion))" -ForegroundColor Green
} else {
    Write-Host " FAIL" -ForegroundColor Red
    Write-Host "      Wymagany PowerShell 7+. Pobierz: https://github.com/PowerShell/PowerShell/releases" -ForegroundColor Yellow
    $allOk = $false
}

# --- 2. Windows version ---
Write-Host "[2/6] Sprawdzanie systemu Windows..." -NoNewline
$os = Get-CimInstance Win32_OperatingSystem
if ($os.Caption -match "Windows 10|Windows 11") {
    Write-Host " OK ($($os.Caption))" -ForegroundColor Green
} else {
    Write-Host " WARN" -ForegroundColor Yellow
    Write-Host "      Zalecany Windows 10/11. Obecny: $($os.Caption)" -ForegroundColor Yellow
}

# --- 3. Virtualization ---
Write-Host "[3/6] Sprawdzanie wirtualizacji (Hyper-V / VT-x / SVM)..." -NoNewline
$hyperv = $null
try { $hyperv = Get-ComputerInfo -Property HyperVRequirementVirtualizationFirmwareEnabled -ErrorAction SilentlyContinue } catch {}
if ($hyperv -and $hyperv.HyperVRequirementVirtualizationFirmwareEnabled -eq $true) {
    Write-Host " OK (Hyper-V dostepne)" -ForegroundColor Green
} else {
    # Try alternative check via systeminfo
    $sysinfo = systeminfo | Select-String "Hyper-V Requirements"
    if ($sysinfo -match "Hyper-V Requirements:\s+A hypervisor has been detected" -or
        $sysinfo -match "Hyper-V Requirements:\s+VM Monitor Mode Extensions: Yes") {
        Write-Host " OK (wirtualizacja wlaczona)" -ForegroundColor Green
    } else {
        Write-Host " WARN" -ForegroundColor Yellow
        Write-Host "      Wirtualizacja moze byc wylaczona w BIOS/UEFI." -ForegroundColor Yellow
        Write-Host "      Wlacz: Intel VT-x / AMD-V / SVM. Emulator bedzie bardzo wolny bez tego." -ForegroundColor Yellow
    }
}

# --- 4. JDK ---
Write-Host "[4/6] Sprawdzanie JDK..." -NoNewline
$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) {
    $version = & java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
    if ($version -match "(1[7-9]|2[0-9])") {
        Write-Host " OK ($version)" -ForegroundColor Green
    } else {
        Write-Host " WARN" -ForegroundColor Yellow
        Write-Host "      Wykryto starsza wersje Javy. Zalecane JDK 17+ (najlepiej 21)." -ForegroundColor Yellow
        Write-Host "      Instalacja: winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Yellow
    }
} else {
    Write-Host " FAIL" -ForegroundColor Red
    Write-Host "      Java nie znaleziona. Zainstaluj JDK 21:" -ForegroundColor Yellow
    Write-Host "      winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Yellow
    $allOk = $false
}

# --- 5. Disk space ---
Write-Host "[5/6] Sprawdzanie miejsca na dysku..." -NoNewline
$disk = Get-CimInstance Win32_LogicalDisk -Filter "DeviceID='$($env:SystemDrive)'"
$freeGB = [math]::Round($disk.FreeSpace / 1GB, 2)
if ($freeGB -gt 15) {
    Write-Host " OK ($freeGB GB wolne)" -ForegroundColor Green
} else {
    Write-Host " WARN" -ForegroundColor Yellow
    Write-Host "      Dostepne tylko $freeGB GB. Zalecane min. 15 GB (SDK + emulator + obrazy)." -ForegroundColor Yellow
}

# --- 6. Admin rights (optional) ---
Write-Host "[6/6] Sprawdzanie uprawnien administratora..." -NoNewline
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if ($isAdmin) {
    Write-Host " OK (uruchomione jako Administrator)" -ForegroundColor Green
} else {
    Write-Host " INFO (zwykly uzytkownik)" -ForegroundColor Cyan
    Write-Host "      Jest OK - administrator nie jest wymagany do codziennej pracy." -ForegroundColor Cyan
    Write-Host "      Przydatny tylko przy jednorazowej instalacji HAXM/Hyper-V." -ForegroundColor Cyan
}

Write-Host ""
if ($allOk) {
    Write-Host "Srodowisko GOTOWE do instalacji Android SDK!" -ForegroundColor Green
    Write-Host "Uruchom kolejny skrypt: .\02_install_sdk.ps1" -ForegroundColor Cyan
} else {
    Write-Host "Srodowisko WYMAGA poprawek przed kontynuacja." -ForegroundColor Red
    Write-Host "Napraw zaznaczone problemy i uruchom ten skrypt ponownie." -ForegroundColor Yellow
}
