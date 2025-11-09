# Simple Jenkins Log Checker
# This will show you exactly what's happening when you refresh the dashboard

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Jenkins Data Log Monitor" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Get ngrok URL
try {
    $ngrokApi = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -ErrorAction Stop
    $ngrokUrl = $ngrokApi.tunnels[0].public_url
    Write-Host "Your ngrok URL: $ngrokUrl" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "Could not get ngrok URL (ngrok might not be running)" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "INSTRUCTIONS:" -ForegroundColor Yellow
Write-Host "1. Make sure your Jenkins account in the app uses the ngrok URL above" -ForegroundColor White
Write-Host "2. Open the app and go to Dashboard" -ForegroundColor White
Write-Host "3. Pull down to refresh" -ForegroundColor White
Write-Host "4. Watch the logs below" -ForegroundColor White
Write-Host ""
Write-Host "Looking for:" -ForegroundColor Yellow
Write-Host "  - 'Synced X pipelines' = SUCCESS" -ForegroundColor Green
Write-Host "  - '401 Unauthorized' = Bad token" -ForegroundColor Red
Write-Host "  - 'timeout' or 'failed to connect' = Network issue" -ForegroundColor Red
Write-Host "  - 'No jobs found' or '0 pipelines' = Jenkins has no jobs" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Clear old logs
& $adb logcat -c

# Start monitoring
& $adb logcat -v time *:S Timber:D PipelineRepository:D OkHttp:D AddAccountViewModel:D DashboardViewModel:D
