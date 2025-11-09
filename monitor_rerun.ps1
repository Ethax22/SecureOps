# Monitor Rerun Button Activity
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Monitoring Rerun Button Activity" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Waiting for you to click the Rerun button..." -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Clear logs
& $adb logcat -c

# Monitor all relevant logs
& $adb logcat -v time | Select-String -Pattern "BuildDetails|Remediation|Jenkins|okhttp|Authorization" --Context 0,2
