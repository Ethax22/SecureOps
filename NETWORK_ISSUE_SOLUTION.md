# Network Connectivity Issue - Solution Guide

## 🔍 Problem Identified

Your Android device (192.168.1.8) **cannot connect** to Jenkins server (192.168.1.9:8080), but your
computer can access it.

### Test Results:

- ✅ Jenkins is running on 192.168.1.9:8080
- ✅ Jenkins is listening on all interfaces (0.0.0.0:8080)
- ✅ Computer can reach Jenkins
- ❌ Android device cannot reach Jenkins (30 second timeout)

## 🎯 Most Likely Cause: AP Isolation

**AP Isolation** (also called Client Isolation or WiFi Isolation) is a router feature that prevents
WiFi devices from communicating with each other. It's often enabled on guest networks or public
WiFi.

## 🔧 Solutions (Try in Order)

### Solution 1: Disable AP Isolation (RECOMMENDED)

1. **Access your WiFi router settings**
    - Open browser: Usually `http://192.168.1.1` or `http://192.168.0.1`
    - Login with admin credentials

2. **Find AP Isolation setting**
    - Look under: Wireless Settings, Advanced WiFi, or Security
    - Common names: "AP Isolation", "Client Isolation", "Station Isolation"

3. **Disable AP Isolation**
    - Uncheck or disable the option
    - Save settings and restart router if needed

4. **Test again** - Refresh the app and check if pipelines load

---

### Solution 2: Use Same Computer as Proxy

If you can't change router settings, use your computer as a bridge:

1. **Install ngrok** (free tunneling service)
   ```powershell
   # Download from https://ngrok.com/download
   # Or use chocolatey: choco install ngrok
   ```

2. **Create tunnel to Jenkins**
   ```powershell
   ngrok http 192.168.1.9:8080
   ```

3. **Use ngrok URL in app**
    - ngrok will give you a URL like: `https://abc123.ngrok.io`
    - In the app, delete old account
    - Add new Jenkins account with ngrok URL
    - This will use HTTPS and go through internet (slower but works)

---

### Solution 3: Connect Computer and Phone via Hotspot

1. **Enable Mobile Hotspot on your phone**
    - Settings → Network & Internet → Hotspot & tethering
    - Turn on WiFi hotspot

2. **Connect your computer to phone's hotspot**
    - Both devices will be on same network

3. **Find your computer's new IP**
   ```powershell
   ipconfig | Select-String "192.168"
   ```

4. **Update Jenkins account in app**
    - Use the new IP address

---

### Solution 4: Use Jenkins on Same Device (Testing)

For testing purposes, you can run Jenkins on the phone itself:

1. **Use Android Jenkins client** or
2. **Access Jenkins via localhost** if running in emulator
3. **Use IP `10.0.2.2`** if testing on emulator (refers to host machine)

---

## 🧪 Testing Connectivity from Phone

To verify network connectivity from your Android device:

### Method 1: Use Terminal App

1. Install **Termux** from Play Store
2. Run: `ping 192.168.1.9`
3. If ping fails, it's a network issue

### Method 2: Use Browser

1. Open Chrome on phone
2. Navigate to: `http://192.168.1.9:8080`
3. You should see Jenkins login page
4. If it doesn't load, network is blocked

---

## 🔍 Verify Router Settings to Check

Common router settings that can cause this issue:

| Setting | Location | Should Be |
|---------|----------|-----------|
| AP Isolation | Wireless → Advanced | **Disabled** |
| Client Isolation | WiFi → Security | **Disabled** |
| Wireless Isolation | Advanced Settings | **Disabled** |
| Guest Network | WiFi Settings | Make sure phone is on **main network** |
| MAC Filtering | Security | Ensure phone is **allowed** |
| Firewall Rules | Security/Firewall | Allow **LAN to LAN** traffic |

---

## 🎯 Quick Test After Fixing

1. **Test browser access on phone**
   ```
   Open Chrome → http://192.168.1.9:8080
   ```
   Should see Jenkins page

2. **Open SecureOps app**
    - Go to Dashboard
    - Tap Refresh icon
    - Watch for pipelines to load

3. **Monitor logs** (from computer)
   ```powershell
   adb logcat -v time | Select-String "Pipeline|Jenkins|Synced"
   ```

### Success will show:

```
D/PipelineRepository: Jenkins API response: X jobs found
D/PipelineRepository: Synced X pipelines for account: Prakash D (JENKINS)
```

---

## 🆘 Alternative: Use HTTPS Jenkins Cloud

If you have access to a Jenkins server on the internet with HTTPS:

1. **Delete local Jenkins account** in app
2. **Add cloud Jenkins account**
    - URL: `https://your-jenkins.example.com`
    - Use proper SSL certificate
    - No HTTP cleartext issues

This avoids all local network problems.

---

## 📞 Still Not Working?

### Check these:

1. **Verify phone and computer on same WiFi**
   ```
   Phone: Settings → WiFi → Connected network name
   Computer: Look at WiFi icon → Network name
   ```

2. **Check if VPN is active on phone**
    - VPN can block local network access
    - Disable temporarily to test

3. **Windows Firewall (on computer)**
   ```powershell
   # Allow Jenkins through firewall
   netsh advfirewall firewall add rule name="Jenkins" dir=in action=allow protocol=TCP localport=8080
   ```

4. **Try from different device**
    - Test from another phone or tablet on same WiFi
    - If it works, issue is specific to your device

---

## 💡 Recommended Long-term Solution

**Option A: Use HTTPS Jenkins**

- Set up Jenkins with proper SSL certificate
- Use Let's Encrypt for free SSL
- Access via domain name or static IP
- More secure and reliable

**Option B: Use VPN**

- Set up WireGuard or OpenVPN
- Connect phone to home network remotely
- Access Jenkins securely from anywhere

**Option C: Cloud Jenkins**

- Use Jenkins on AWS, DigitalOcean, or CloudBees
- Always accessible with HTTPS
- No local network issues

---

## 🎬 Summary

**Your app is working perfectly!** The only issue is network connectivity between devices.

**Next Step:** Check if you can access `http://192.168.1.9:8080` from your phone's browser.

- If **YES**: There's an app configuration issue (unlikely)
- If **NO**: It's AP Isolation - disable it in router settings

Once you can access Jenkins from phone's browser, the app will work immediately.

---
**Status**: ✅ App is ready | ⚠️ Network configuration needed
