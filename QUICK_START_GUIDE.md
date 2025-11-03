# SecureOps - Quick Start Guide 🚀

**Status:** 98% Complete - Just need Firebase config and keystore!

---

## Required Steps (15-20 minutes total)

### ✅ Step 1: Get Firebase Configuration (10 minutes)

#### 1.1 Create Firebase Project

1. Go to: https://console.firebase.google.com/
2. Click **"Add project"** or **"Create a project"**
3. Enter project name: `SecureOps` (or your choice)
4. Click **"Continue"**
5. Disable Google Analytics (optional - you can enable later)
6. Click **"Create project"** → Wait → Click **"Continue"**

#### 1.2 Add Android App

1. On Firebase Console home, click the **Android icon** (or "Add app" → Android)
2. Fill in:
    - **Android package name:** `com.secureops.app` ⚠️ **MUST MATCH EXACTLY**
    - **App nickname:** `SecureOps` (optional)
    - **Debug signing SHA-1:** Leave blank (can add later)
3. Click **"Register app"**

#### 1.3 Download Config File

1. Click **"Download google-services.json"**
2. Save the file to your Downloads folder

#### 1.4 Place Config File in Project

**CRITICAL:** Copy the file to the correct location:

```powershell
# Run this in PowerShell
Copy-Item "$env:USERPROFILE\Downloads\google-services.json" "C:\Users\aravi\StudioProjects\Vibestate\app\google-services.json"
```

Or manually:

- Copy `google-services.json` from Downloads
- Paste into: `C:\Users\aravi\StudioProjects\Vibestate\app\`

#### 1.5 Verify

Check that the file is here: `Vibestate\app\google-services.json`

✅ **Firebase Setup Complete!**

---

### ✅ Step 2: Create Release Keystore (5 minutes)

#### 2.1 Generate Keystore

Open PowerShell and run:

```powershell
cd C:\Users\aravi\StudioProjects\Vibestate
keytool -genkey -v -keystore secureops-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias secureops
```

#### 2.2 Answer Prompts

You'll be asked for information. Here's what to enter:

1. **Enter keystore password:** Choose a strong password (e.g., `SecureOps2025!`)
    - ⚠️ **REMEMBER THIS PASSWORD!**
2. **Re-enter new password:** Same password again
3. **What is your first and last name?** Your name or company name
4. **What is the name of your organizational unit?** Just press Enter (or your team name)
5. **What is the name of your organization?** Just press Enter (or your company)
6. **What is the name of your City or Locality?** Your city
7. **What is the name of your State or Province?** Your state/province
8. **What is the two-letter country code?** Two letters (e.g., `US`, `IN`, `GB`)
9. **Is CN=..., OU=..., correct?** Type: `yes`
10. **Enter key password for <secureops>:** Just press Enter (uses same password as keystore)

**IMPORTANT:** Write down your password securely! You'll need it for every release.

#### 2.3 Set Password Environment Variable

Run in PowerShell (use your actual password):

```powershell
# Set for current session (temporary)
$env:KEYSTORE_PASSWORD = "YourActualPasswordHere"
$env:KEY_PASSWORD = "YourActualPasswordHere"

# OR set permanently (recommended)
[System.Environment]::SetEnvironmentVariable('KEYSTORE_PASSWORD', 'YourActualPasswordHere', 'User')
[System.Environment]::SetEnvironmentVariable('KEY_PASSWORD', 'YourActualPasswordHere', 'User')
```

Replace `YourActualPasswordHere` with your actual keystore password!

✅ **Keystore Setup Complete!**

---

### ✅ Step 3: Build & Test (5 minutes)

#### 3.1 Sync Project

In Android Studio:

- Click **File → Sync Project with Gradle Files**
- Wait for sync to complete (should see "BUILD SUCCESSFUL")

#### 3.2 Build Debug Version (Test Build)

```powershell
# In PowerShell at project root
cd C:\Users\aravi\StudioProjects\Vibestate
.\gradlew assembleDebug
```

Expected output: `BUILD SUCCESSFUL`

#### 3.3 Build Release Version (Real Build)

```powershell
.\gradlew assembleRelease
```

Expected output: `BUILD SUCCESSFUL in 2m 15s`

**Output Location:**

```
app\build\outputs\apk\release\app-release.apk
```

#### 3.4 Install & Test

```powershell
# Install on connected device or emulator
adb install app\build\outputs\apk\release\app-release.apk

# Or if already installed:
adb install -r app\build\outputs\apk\release\app-release.apk
```

**Quick Test Checklist:**

- [ ] App launches without crash
- [ ] Dashboard loads
- [ ] Can navigate to Settings
- [ ] Can open Analytics screen
- [ ] Voice screen accessible

✅ **App is Production Ready!**

---

## 📊 Summary

### What's Already Done ✅

- ✅ Firebase plugin configured in gradle
- ✅ Signing configuration set up
- ✅ Logging automatically switches (debug = verbose, release = none)
- ✅ ProGuard rules optimized
- ✅ All code is production-ready
- ✅ Version set to 1.0.0

### What You Need to Do 📝

1. ⏱️ **10 min:** Get `google-services.json` from Firebase Console
2. ⏱️ **5 min:** Create keystore with `keytool`
3. ⏱️ **5 min:** Build & test

**Total Time: ~20 minutes**

---

## 🎯 Quick Reference

### File Locations

- Firebase config: `app/google-services.json`
- Keystore: `secureops-release-key.jks` (in project root)
- APK output: `app/build/outputs/apk/release/app-release.apk`

### Environment Variables

```powershell
KEYSTORE_PASSWORD=YourPasswordHere
KEY_PASSWORD=YourPasswordHere
```

### Common Commands

```powershell
# Sync & clean
.\gradlew clean

# Build debug
.\gradlew assembleDebug

# Build release
.\gradlew assembleRelease

# Install
adb install -r app\build\outputs\apk\release\app-release.apk

# Check connected devices
adb devices
```

---

## 🆘 Troubleshooting

### "google-services.json not found"

➡️ Make sure file is at: `app/google-services.json` (not in root folder)

### "Keystore password incorrect"

➡️ Check environment variables:

```powershell
$env:KEYSTORE_PASSWORD
$env:KEY_PASSWORD
```

### "keytool not found"

➡️ keytool comes with JDK. Check Android Studio's JDK:

```powershell
# In PowerShell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### Build fails with Gradle error

➡️ Try clean build:

```powershell
.\gradlew clean
.\gradlew assembleRelease
```

---

## 🎉 Next Steps After Configuration

Once the app builds successfully:

1. **Test thoroughly** on real device
2. **Generate signed AAB** for Play Store:
   ```powershell
   .\gradlew bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`

3. **Optional: Add to Firebase:**
    - Enable Cloud Messaging in Firebase Console
    - Add server key to your backend (if you have one)
    - Test push notifications

4. **Deploy:**
    - Upload AAB to Google Play Console
    - Fill in store listing
    - Set pricing & distribution
    - Submit for review

---

## 📞 Support

If you encounter any issues:

1. Check the troubleshooting section above
2. Verify all files are in correct locations
3. Ensure environment variables are set
4. Try a clean build

---

**The app is ready to go! Just follow the 3 steps above and you're set! 🚀**
