# Jenkins Data Not Showing - Complete Troubleshooting Guide

## Current Situation

- ✅ ngrok link obtained
- ✅ Jenkins account added to app
- ❌ Pipeline data not appearing in dashboard

## Quick Diagnostic Steps

### Step 1: Verify ngrok is Running

```powershell
# Check if ngrok process is running
Get-Process ngrok -ErrorAction SilentlyContinue
```

**Expected:** Should show ngrok process
**If not found:** Run `ngrok http 192.168.1.9:8080` in a PowerShell window

### Step 2: Get Your ngrok URL

In the ngrok PowerShell window, look for:

```
Forwarding   https://YOUR-UNIQUE-ID.ngrok-free.app -> http://192.168.1.9:8080
```

Copy the `https://YOUR-UNIQUE-ID.ngrok-free.app` URL

### Step 3: Test ngrok URL in Browser

Open the ngrok URL in your **phone's browser** (not computer):

```
https://YOUR-UNIQUE-ID.ngrok-free.app
```

**Expected:** Should see Jenkins login page
**If you see ngrok warning page:** Click "Visit Site" button
**If page doesn't load:** ngrok tunnel is not working

### Step 4: Check Jenkins Server Status

On your computer, open:

```
http://192.168.1.9:8080
```

**Expected:** Jenkins dashboard with jobs visible
**If not:** Jenkins is not running or not accessible

### Step 5: Verify Account Configuration in App

Open SecureOps app → Settings → Manage Accounts

Check your Jenkins account:

- **Name:** Should show your account name (e.g., "Prakash D")
- **Provider:** Should say "JENKINS"
- **Base URL:** Should be the ngrok URL (https://something.ngrok-free.app)
- **Last Sync:** Should show a timestamp or "Not synced yet"
- **Status:** Should show "Active"

### Step 6: Check App Logs in Real-Time

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -v time *:S Timber:D PipelineRepository:D JenkinsService:D OkHttp:D
```

Then in the app:

1. Go to Dashboard
2. Pull down to refresh
3. Watch the logs

## Common Issues and Solutions

### Issue 1: Account Uses Local IP Instead of ngrok URL

**Problem:** Account was added with `http://192.168.1.9:8080` instead of ngrok URL

**Solution:**

1. Settings → Manage Accounts
2. Delete the existing Jenkins account
3. Add new account with:
    - Provider: Jenkins
    - Name: Jenkins (ngrok)
    - Base URL: `https://YOUR-ID.ngrok-free.app` (your ngrok URL)
    - Token: (your Jenkins API token)

### Issue 2: ngrok URL Changed

**Problem:** Free ngrok URLs change every time you restart ngrok

**Solution:**

1. Get new ngrok URL from PowerShell window
2. Delete old Jenkins account in app
3. Add new account with updated URL

### Issue 3: Jenkins API Token Issues

**Problem:** Token might be incorrect or expired

**Solution - Generate New Token:**

1. Open Jenkins: `http://192.168.1.9:8080`
2. Click your username (top right) → Configure
3. Scroll to "API Token" section
4. Click "Add new Token"
5. Give it a name (e.g., "SecureOps App")
6. Click "Generate"
7. **COPY THE TOKEN** (you can't see it again)
8. In app: Delete old account, add new one with new token

### Issue 4: No Jobs in Jenkins

**Problem:** Jenkins doesn't have any jobs/pipelines

**Solution - Create a Test Job:**

1. Open Jenkins: `http://192.168.1.9:8080`
2. Click "New Item"
3. Enter name: "test-job"
4. Select "Freestyle project"
5. Click OK
6. Scroll down, click "Save"
7. Click "Build Now" to run it once
8. Go back to app and refresh

### Issue 5: Network Security / Cleartext Traffic

**Problem:** Android blocking HTTP connections

**Status:** This should already be fixed in the code

**Verify:** Check if `app/src/main/res/xml/network_security_config.xml` exists

### Issue 6: Jenkins CSRF Protection

**Problem:** Jenkins blocking API requests due to CSRF

**Check in Jenkins:**

1. Manage Jenkins → Configure Global Security
2. Look for "Prevent Cross Site Request Forgery exploits"
3. If enabled with "Enable proxy compatibility" unchecked, this might block requests

**Solution:**

- Option A: Check "Enable proxy compatibility"
- Option B: The app should handle CSRF tokens (check if crumb is needed)

### Issue 7: Jenkins Authentication Method

**Problem:** Token format might be wrong

**Check Token Format:**

Jenkins API tokens should be used as:

```
Authorization: Basic base64(username:apitoken)
```

**In the app, when adding account:**

- Username: Your Jenkins username
- Token: Just the API token (app will encode it)

OR

- Token: `username:apitoken` (app will encode it)

### Issue 8: App Cache Issue

**Solution:**

1. Settings → Apps → SecureOps
2. Storage → Clear Cache (NOT Clear Data - you'll lose accounts)
3. Force Stop
4. Open app again

### Issue 9: Database Issues

**Check Database:**

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell "su 0 sh -c 'cd /data/data/com.secureops.app/databases && ls -la && sqlite3 secureops.db \"SELECT * FROM accounts; SELECT * FROM pipelines;\"'"
```

**If device is not rooted:**

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" exec-out run-as com.secureops.app cat databases/secureops.db > secureops_backup.db
```

## Step-by-Step Testing Procedure

### Complete Fresh Start Test

1. **Stop ngrok if running** (Ctrl+C in PowerShell)

2. **Start fresh ngrok tunnel:**
   ```powershell
   ngrok http 192.168.1.9:8080
   ```

3. **Copy the HTTPS URL** from ngrok output

4. **Test URL in phone browser** - should see Jenkins

5. **In app - Delete existing Jenkins account:**
    - Settings → Manage Accounts
    - Tap Jenkins account → Delete

6. **Add new account:**
    - Settings → Add Account
    - Provider: Jenkins
    - Name: Jenkins-ngrok
    - Base URL: (paste ngrok HTTPS URL)
    - Token: (your Jenkins API token)
    - Tap Save

7. **Watch logs in PowerShell:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -v time *:S Timber:D PipelineRepository:D
   ```

8. **In app - Trigger sync:**
    - Go to Dashboard
    - Pull down to refresh

9. **Check logs for these messages:**
    - ✅ `D/PipelineRepository: Synced X pipelines for account: Jenkins-ngrok (JENKINS)`
    - ✅ `D/PipelineRepository: Fetched X Jenkins pipelines from https://...`
    - ❌ `W/PipelineRepository: Jenkins API call failed: XXX`
    - ❌ `E/PipelineRepository: Failed to fetch Jenkins pipelines`

## Advanced Debugging

### Enable Detailed OkHttp Logging

The app should already log HTTP requests in debug builds. Look for:

```
D/OkHttp: --> GET https://your-id.ngrok-free.app/api/json?tree=jobs...
D/OkHttp: Authorization: Basic ********
D/OkHttp: <-- 200 OK https://your-id.ngrok-free.app/api/json
D/OkHttp: {"jobs":[...]}
```

### Test Jenkins API Manually

From your phone (using Termux or browser console):

```bash
curl -u username:apitoken https://YOUR-ID.ngrok-free.app/api/json?tree=jobs[name,url,color,lastBuild[number,result,timestamp,duration]]
```

Expected response:

```json
{
  "jobs": [
    {
      "name": "job-name",
      "url": "https://YOUR-ID.ngrok-free.app/job/job-name/",
      "color": "blue",
      "lastBuild": {
        "number": 123,
        "result": "SUCCESS",
        "timestamp": 1699380000000,
        "duration": 45000
      }
    }
  ]
}
```

### Check ngrok Request Inspector

Open in browser on your computer:

```
http://localhost:4040
```

This shows all requests going through ngrok. When you refresh the app:

- You should see GET requests to `/api/json`
- Check response status codes
- View request/response headers and body

## What the Code Does

### When You Refresh Dashboard:

1. `DashboardViewModel.refreshPipelines()` is called
2. For each account, calls `PipelineRepository.syncPipelines(accountId)`
3. Repository fetches account details from database
4. **Creates dynamic Jenkins service** with:
    - Account's base URL (should be ngrok URL)
    - Account's API token (from secure storage)
    - Basic authentication header
5. Makes GET request to: `{baseUrl}/api/json?tree=jobs[...]`
6. Parses JSON response into Pipeline objects
7. Saves to local database
8. UI observes database and updates automatically

### Key Code Sections:

**Dynamic Service Creation** (`PipelineRepository.kt` line ~380):

```kotlin
private fun createJenkinsService(baseUrl: String, token: String): JenkinsService {
    val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    val base64Token = if (token.contains(":")) {
        android.util.Base64.encodeToString(token.toByteArray(), android.util.Base64.NO_WRAP)
    } else {
        token
    }
    // Creates OkHttp client with auth interceptor
    // Creates Retrofit instance with dynamic baseUrl
}
```

**Fetch Jenkins Pipelines** (`PipelineRepository.kt` line ~169):

```kotlin
private suspend fun fetchJenkinsPipelines(account: Account, token: String): List<Pipeline> {
    val jenkinsServiceDynamic = createJenkinsService(account.baseUrl, token)
    val response = jenkinsServiceDynamic.getJobs()
    // Parses response and returns Pipeline list
}
```

## Expected Log Flow (Success Case)

```
D/DashboardViewModel: Refreshing pipelines
D/PipelineRepository: Syncing pipelines for account: abc-123-def
D/PipelineRepository: Creating Jenkins service for: https://abc123.ngrok-free.app/
D/OkHttp: --> GET https://abc123.ngrok-free.app/api/json?tree=jobs[name,url,color,lastBuild[number,result,timestamp,duration]]
D/OkHttp: Authorization: Basic [size=XX]
D/OkHttp: <-- 200 OK https://abc123.ngrok-free.app/api/json (1234ms)
D/PipelineRepository: Jenkins API response: 3 jobs found
D/PipelineRepository: Fetched 3 Jenkins pipelines from https://abc123.ngrok-free.app/
D/PipelineRepository: Synced 3 pipelines for account: Jenkins-ngrok (JENKINS)
D/DashboardViewModel: Pipelines updated: 3 pipelines
```

## Still Not Working?

### Collect Diagnostic Info:

1. **ngrok URL:**
   ```
   Your URL: _____________________
   ```

2. **Jenkins accessible from phone browser?** Yes / No

3. **Account base URL in app:**
   ```
   Base URL: _____________________
   ```

4. **Log output when refreshing:** (paste here)

5. **ngrok inspector shows requests?** Yes / No

6. **Jenkins has jobs?** Yes / No

7. **Error messages in app?** (screenshot or text)

### Share This Info:

Provide the above diagnostic info, and we can identify the exact issue.

## Quick Checklist

- [ ] ngrok is running
- [ ] Jenkins is accessible at `http://192.168.1.9:8080`
- [ ] ngrok URL opens Jenkins in phone browser
- [ ] Account in app uses ngrok HTTPS URL (not local IP)
- [ ] Jenkins API token is valid and recent
- [ ] Jenkins has at least one job
- [ ] App has been refreshed after adding account
- [ ] Logs show API calls being made
- [ ] No error messages in logs

## Need to Rebuild/Reinstall App?

If you suspect code changes are needed:

```powershell
# Clean build
cd C:/Users/aravi/StudioProjects/Vibestate
.\gradlew clean
.\gradlew assembleDebug

# Uninstall old version
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" uninstall com.secureops.app

# Install new version
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk

# Check logs
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -v time *:S Timber:D PipelineRepository:D
```

---

**Next Steps:** Follow the "Step-by-Step Testing Procedure" section above and report back which step
fails.
