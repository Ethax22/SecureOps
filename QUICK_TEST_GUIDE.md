# Quick Test Guide - Jenkins Integration

## ✅ What's Been Fixed

1. Network security policy configured to allow HTTP
2. Jenkins authentication properly handles tokens
3. Enhanced error logging for debugging
4. Better Jenkins status mapping

## 🔧 Current Issue

**Network Connectivity**: Cannot reach Jenkins server at `http://192.168.1.9:8080`

## 🚀 Quick Fix Steps

### Step 1: Verify Jenkins is Running

Open browser and navigate to:

```
http://192.168.1.9:8080
```

You should see the Jenkins dashboard.

### Step 2: Test Jenkins API

In browser or using curl:

```bash
# Replace username:token with your credentials
curl -u username:token http://192.168.1.9:8080/api/json
```

### Step 3: Check Network

From your computer (not the phone):

```bash
ping 192.168.1.9
```

### Step 4: Ensure Device is on Same Network

- Check WiFi settings on phone
- Ensure connected to same network as Jenkins server
- Device should be able to ping `192.168.1.9`

### Step 5: Check Firewall

- Ensure port 8080 is not blocked
- Windows Firewall or antivirus may be blocking
- Try temporarily disabling to test

## 📱 Testing in the App

Once network is working:

1. **Open SecureOps app** on device
2. **Go to Dashboard** (bottom navigation)
3. **Tap Refresh icon** (top right corner)
4. **Watch for pipelines** to appear

## 📊 Monitoring Logs

Open PowerShell and run:

```powershell
adb logcat -v time | Select-String -Pattern "Pipeline|Jenkins|Synced"
```

### Success Looks Like:

```
D/PipelineRepository: Jenkins API response: 5 jobs found
D/PipelineRepository: Fetched 5 Jenkins pipelines from http://192.168.1.9:8080/
D/PipelineRepository: Synced 5 pipelines for account: Prakash D (JENKINS)
```

### Error Looks Like:

```
E/PipelineRepository: Failed to fetch Jenkins pipelines for account Prakash D
E/PipelineRepository: java.net.SocketTimeoutException: failed to connect
```

## 🔍 Troubleshooting Checklist

- [ ] Jenkins server is running
- [ ] Can access Jenkins in browser from computer
- [ ] Phone is on same WiFi network
- [ ] Port 8080 is not blocked by firewall
- [ ] Jenkins API token is correct
- [ ] Jenkins has at least one job with builds

## 🎯 Alternative: Use HTTPS Jenkins

If you have Jenkins with HTTPS, update the account:

1. Delete current Jenkins account in app
2. Add new account with HTTPS URL: `https://jenkins.example.com`
3. This avoids cleartext HTTP issues entirely

## 📞 Need Help?

Check the full documentation: `JENKINS_PIPELINE_FIX_SUMMARY.md`

---
**Last Updated**: Build successful, network testing required
