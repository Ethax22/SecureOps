# Jenkins Data Diagnostic Script
# Run this script to diagnose why Jenkins data is not showing

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Jenkins Data Diagnostic Tool" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Check 1: Is ngrok running?
Write-Host "[1/8] Checking if ngrok is running..." -ForegroundColor Yellow
$ngrokProcess = Get-Process ngrok -ErrorAction SilentlyContinue
if ($ngrokProcess) {
    Write-Host "  OK ngrok is running (PID: $($ngrokProcess.Id))" -ForegroundColor Green
} else {
    Write-Host "  ERROR ngrok is NOT running!" -ForegroundColor Red
    Write-Host "    Action: Run 'ngrok http 192.168.1.9:8080' in a new PowerShell window" -ForegroundColor Yellow
}
Write-Host ""

# Check 2: Get ngrok URL
Write-Host "[2/8] Getting ngrok URL..." -ForegroundColor Yellow
try {
    $ngrokApi = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -ErrorAction Stop
    $ngrokUrl = $ngrokApi.tunnels[0].public_url
    Write-Host "  OK ngrok URL: $ngrokUrl" -ForegroundColor Green
} catch {
    Write-Host "  ERROR Could not get ngrok URL!" -ForegroundColor Red
    Write-Host "    Error: $_" -ForegroundColor Red
    $ngrokUrl = $null
}
Write-Host ""

# Check 3: Test Jenkins locally
Write-Host "[3/8] Testing Jenkins server (local)..." -ForegroundColor Yellow
try {
    $jenkinsLocal = Invoke-WebRequest -Uri "http://192.168.1.9:8080" -Method HEAD -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
    Write-Host "  OK Jenkins is accessible locally (Status: $($jenkinsLocal.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "  ERROR Jenkins is NOT accessible locally!" -ForegroundColor Red
    Write-Host "    Action: Start Jenkins service or verify it's running" -ForegroundColor Yellow
}
Write-Host ""

# Check 4: Test ngrok URL
if ($ngrokUrl) {
    Write-Host "[4/8] Testing ngrok URL..." -ForegroundColor Yellow
    try {
        $ngrokTest = Invoke-WebRequest -Uri "$ngrokUrl" -Method HEAD -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop
        Write-Host "  OK ngrok URL is accessible (Status: $($ngrokTest.StatusCode))" -ForegroundColor Green
    } catch {
        Write-Host "  ERROR ngrok URL is NOT accessible!" -ForegroundColor Red
        Write-Host "    Error: $_" -ForegroundColor Red
    }
} else {
    Write-Host "[4/8] Skipping ngrok URL test (URL not found)" -ForegroundColor Gray
}
Write-Host ""

# Check 5: Is device connected?
Write-Host "[5/8] Checking device connection..." -ForegroundColor Yellow
$devices = & $adb devices | Select-String -Pattern "\tdevice$"
if ($devices) {
    Write-Host "  OK Device connected: $($devices -replace '\tdevice', '')" -ForegroundColor Green
} else {
    Write-Host "  ERROR No device connected!" -ForegroundColor Red
    Write-Host "    Action: Connect your phone via USB and enable USB debugging" -ForegroundColor Yellow
}
Write-Host ""

# Check 6: Is app installed?
Write-Host "[6/8] Checking if SecureOps app is installed..." -ForegroundColor Yellow
$appInstalled = & $adb shell pm list packages | Select-String "com.secureops.app"
if ($appInstalled) {
    Write-Host "  OK SecureOps app is installed" -ForegroundColor Green
} else {
    Write-Host "  ERROR SecureOps app is NOT installed!" -ForegroundColor Red
    Write-Host "    Action: Install the app first" -ForegroundColor Yellow
}
Write-Host ""

# Check 7: Clear logcat and start monitoring
Write-Host "[7/8] Clearing old logs..." -ForegroundColor Yellow
& $adb logcat -c
Write-Host "  OK Logs cleared" -ForegroundColor Green
Write-Host ""

# Check 8: Instructions for next steps
Write-Host "[8/8] Next Steps:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  IMPORTANT: Check your Jenkins account configuration in the app!" -ForegroundColor Cyan
Write-Host ""
Write-Host "  1. Open SecureOps app > Settings > Manage Accounts" -ForegroundColor White
Write-Host "  2. Check the Jenkins account Base URL:" -ForegroundColor White
if ($ngrokUrl) {
    Write-Host "     Expected: $ngrokUrl" -ForegroundColor Green
} else {
    Write-Host "     Expected: https://YOUR-ID.ngrok-free.app" -ForegroundColor Yellow
}
Write-Host "     If different: Delete account and add new one with correct URL" -ForegroundColor White
Write-Host ""
Write-Host "  3. Pull down to refresh on Dashboard" -ForegroundColor White
Write-Host ""
Write-Host "  4. Watch logs in real-time:" -ForegroundColor White
Write-Host "     (Starting automatically after summary...)" -ForegroundColor Cyan
Write-Host ""

# Summary
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Diagnostic Summary" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan

if ($ngrokProcess -and $ngrokUrl -and $devices -and $appInstalled) {
    Write-Host "All systems ready!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Most likely issue: Account Base URL mismatch OR Jenkins has no jobs" -ForegroundColor Yellow
    Write-Host "Action: Verify account uses this URL: $ngrokUrl" -ForegroundColor Yellow
} else {
    Write-Host "Some issues detected. Review the results above." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Press any key to start real-time log monitoring..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

Write-Host ""
Write-Host "Starting log monitor... (Press Ctrl+C to stop)" -ForegroundColor Green
Write-Host "Now go to the app and refresh the Dashboard!" -ForegroundColor Cyan
Write-Host ""

& $adb logcat -v time *:S Timber:D PipelineRepository:D OkHttp:D JenkinsService:D
