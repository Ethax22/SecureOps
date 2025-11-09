# Test Jenkins API Directly
# This tests if Jenkins API is returning job data

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Jenkins API Test" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Get ngrok URL
try {
    $ngrokApi = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -ErrorAction Stop
    $ngrokUrl = $ngrokApi.tunnels[0].public_url
    Write-Host "ngrok URL: $ngrokUrl" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Could not get ngrok URL!" -ForegroundColor Red
    Write-Host "Make sure ngrok is running: ngrok http 192.168.1.9:8080" -ForegroundColor Yellow
    exit
}

Write-Host ""
Write-Host "Enter your Jenkins username:" -ForegroundColor Yellow
$username = Read-Host

Write-Host "Enter your Jenkins API token:" -ForegroundColor Yellow
$apiToken = Read-Host -AsSecureString
$apiTokenPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($apiToken))

Write-Host ""
Write-Host "Testing Jenkins API..." -ForegroundColor Yellow
Write-Host ""

# Create auth header
$credentials = "${username}:${apiTokenPlain}"
$credentialsBytes = [System.Text.Encoding]::UTF8.GetBytes($credentials)
$base64Credentials = [Convert]::ToBase64String($credentialsBytes)
$headers = @{
    "Authorization" = "Basic $base64Credentials"
}

# Test 1: Check if Jenkins is accessible
Write-Host "[1/3] Testing basic Jenkins access..." -ForegroundColor Cyan
$jenkinsApiUrl = "$ngrokUrl/api/json"
try {
    $response = Invoke-RestMethod -Uri $jenkinsApiUrl -Headers $headers -Method Get -TimeoutSec 30
    Write-Host "  SUCCESS: Jenkins API is accessible" -ForegroundColor Green
    Write-Host "  Jenkins Version: $($response.version)" -ForegroundColor White
} catch {
    Write-Host "  ERROR: Cannot access Jenkins API!" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "  Reason: 401 Unauthorized - Check your username/token" -ForegroundColor Yellow
    }
    exit
}

Write-Host ""

# Test 2: Get jobs list
Write-Host "[2/3] Fetching jobs list..." -ForegroundColor Cyan
$jobsApiUrl = "$ngrokUrl/api/json?tree=jobs[name,url,color,lastBuild[number,result,timestamp,duration]]"
try {
    $jobsResponse = Invoke-RestMethod -Uri $jobsApiUrl -Headers $headers -Method Get -TimeoutSec 30
    $jobCount = $jobsResponse.jobs.Count
    
    if ($jobCount -eq 0) {
        Write-Host "  WARNING: No jobs found in Jenkins!" -ForegroundColor Yellow
        Write-Host "  Action: Create at least one job in Jenkins" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "To create a test job:" -ForegroundColor Cyan
        Write-Host "1. Open Jenkins: http://192.168.1.9:8080" -ForegroundColor White
        Write-Host "2. Click 'New Item'" -ForegroundColor White
        Write-Host "3. Enter name: test-pipeline" -ForegroundColor White
        Write-Host "4. Select 'Freestyle project'" -ForegroundColor White
        Write-Host "5. Click OK and Save" -ForegroundColor White
        Write-Host "6. Click 'Build Now'" -ForegroundColor White
    } else {
        Write-Host "  SUCCESS: Found $jobCount job(s)" -ForegroundColor Green
        Write-Host ""
        Write-Host "Jobs:" -ForegroundColor White
        foreach ($job in $jobsResponse.jobs) {
            Write-Host "  - $($job.name)" -ForegroundColor Cyan
            Write-Host "    URL: $($job.url)" -ForegroundColor Gray
            Write-Host "    Color: $($job.color)" -ForegroundColor Gray
            if ($job.lastBuild) {
                Write-Host "    Last Build: #$($job.lastBuild.number) - $($job.lastBuild.result)" -ForegroundColor Gray
            } else {
                Write-Host "    Last Build: None" -ForegroundColor Yellow
            }
            Write-Host ""
        }
    }
} catch {
    Write-Host "  ERROR: Cannot fetch jobs!" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Test 3: Show what the app should receive
Write-Host "[3/3] API Response Summary" -ForegroundColor Cyan
Write-Host ""

if ($jobCount -eq 0) {
    Write-Host "RESULT: Jenkins API works but has NO JOBS" -ForegroundColor Yellow
    Write-Host "This is why the app shows 'No pipelines yet'" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "SOLUTION: Create a job in Jenkins and run it at least once" -ForegroundColor Green
} else {
    Write-Host "RESULT: Jenkins API is working correctly!" -ForegroundColor Green
    Write-Host "Found $jobCount job(s) that should appear in the app" -ForegroundColor Green
    Write-Host ""
    Write-Host "If the app still shows 'No pipelines yet', check:" -ForegroundColor Yellow
    Write-Host "1. Account Base URL in app matches: $ngrokUrl" -ForegroundColor White
    Write-Host "2. Username and token are correct" -ForegroundColor White
    Write-Host "3. Run: .\check_jenkins_logs.ps1 and refresh the app" -ForegroundColor White
}

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Test Complete" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
