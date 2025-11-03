# Final Steps Checklist ✅

## Your Remaining Action Items

All code configuration is DONE! You just need to do these 3 manual steps:

---

## Step 1: Get Firebase File (5 minutes)

- [ ] Go to https://console.firebase.google.com/
- [ ] Click "Add project" or use existing
- [ ] Add Android app with package: `com.secureops.app`
- [ ] Download `google-services.json`
- [ ] Move file to: `C:\Users\aravi\StudioProjects\Vibestate\app\google-services.json`

**Exact PowerShell command:**

```powershell
Move-Item "$env:USERPROFILE\Downloads\google-services.json" "C:\Users\aravi\StudioProjects\Vibestate\app\google-services.json"
```

---

## Step 2: Create Keystore (5 minutes)

- [ ] Open PowerShell
- [ ] Navigate to project: `cd C:\Users\aravi\StudioProjects\Vibestate`
- [ ] Run command below
- [ ] Enter passwords when prompted (SAVE THEM!)
- [ ] Fill in your info (or press Enter to skip optional fields)

**Command:**

```powershell
keytool -genkey -v -keystore secureops-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias secureops
```

---

## Step 3: Add Your Passwords (2 minutes)

**Option A - Edit keystore.properties (Easier):**

- [ ] Open `keystore.properties` in the project root
- [ ] Replace `YOUR_KEYSTORE_PASSWORD_HERE` with your actual password
- [ ] Replace `YOUR_KEY_PASSWORD_HERE` with your actual password (usually same)
- [ ] Save file

**Option B - Use Environment Variables:**

```powershell
$env:KEYSTORE_PASSWORD = "your_actual_password"
$env:KEY_PASSWORD = "your_actual_password"
```

---

## Step 4: Build Release APK (2 minutes)

- [ ] Open PowerShell in project root
- [ ] Run: `.\gradlew assembleRelease`
- [ ] Wait for "BUILD SUCCESSFUL"
- [ ] APK will be at: `app\build\outputs\apk\release\app-release.apk`

---

## Step 5: Test Release APK (5 minutes)

- [ ] Connect Android device or start emulator
- [ ] Run: `adb install app\build\outputs\apk\release\app-release.apk`
- [ ] Open app on device
- [ ] Test: Dashboard, Settings, Analytics, Voice screens
- [ ] Check for crashes or errors

---

## ✅ That's It!

Once these steps are done:

- ✅ App is production-ready
- ✅ Can upload to Google Play Console
- ✅ Can distribute to users

---

## 🆘 Troubleshooting

**Firebase error during build?**

- Make sure `google-services.json` is in the `app/` folder (not root)

**Keystore error during build?**

- Check passwords in `keystore.properties` or environment variables
- Make sure `secureops-release-key.jks` is in project root

**Build fails?**

- Try: `.\gradlew clean`
- Then: `.\gradlew assembleRelease`

---

**Total Time Required: ~20 minutes**

**Difficulty: Easy** 😊
