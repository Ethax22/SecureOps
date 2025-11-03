# Production Configuration Guide 🔧

## Overview

This document lists all placeholders, configuration values, and TODOs that need to be addressed
before deploying SecureOps to production.

**Last Updated:** November 2, 2025  
**Status:** Ready for configuration

---

## 🔴 CRITICAL - Must Configure Before Production

### 1. Firebase Cloud Messaging (FCM) ⚠️ REQUIRED

**Status:** ❌ Not configured  
**Impact:** Push notifications will not work

#### What to do:

1. **Create Firebase Project:**
    - Go to [Firebase Console](https://console.firebase.google.com/)
    - Create a new project or use existing
    - Add Android app with package name: `com.secureops.app`

2. **Download Configuration File:**
    - Download `google-services.json` from Firebase Console
    - Place it in: `app/google-services.json`

3. **Add Firebase Plugin:**
   Add to `app/build.gradle.kts` (at the top):
   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       id("com.google.dagger.hilt.android")
       id("com.google.devtools.ksp")
       kotlin("plugin.serialization") version "1.9.22"
       id("com.google.gms.google-services") // ADD THIS LINE
   }
   ```

4. **Add Classpath:**
   Add to root `build.gradle.kts`:
   ```kotlin
   buildscript {
       dependencies {
           classpath("com.google.gms:google-services:4.4.0")
       }
   }
   ```

**Files Affected:**

- `app/src/main/java/com/secureops/app/data/notification/SecureOpsMessagingService.kt` (Line 20)
- `app/build.gradle.kts` (Firebase dependencies present, plugin missing)

---

### 2. RunAnywhere AI SDK ✅ ACTIVE

**Status:** ✅ SDK integration uncommented and active  
**Impact:** Real AI features enabled (requires API key to function)

**Current Implementation:**

```kotlin
// app/src/main/java/com/secureops/app/ml/RunAnywhereManager.kt
suspend fun initialize(apiKey: String = "demo-api-key") {
    // SDK code is now active and will attempt to use RunAnywhere API
}
```

#### What to do to enable real AI:

1. **Get API Key:**
    - Sign up at [RunAnywhere AI](https://runanywhere.ai/)
    - Get your API key from dashboard

2. **Configure API Key:**
   In `SecureOpsApplication.kt`, pass your API key:
   ```kotlin
   runAnywhereManager.initialize(apiKey = "YOUR_ACTUAL_API_KEY")
   ```

3. **Test Integration:**
    - Build and run the app
    - Check logs for "RunAnywhere SDK initialized successfully"
    - Test AI features (changelog analysis, playbook generation)

**Note:** The code will gracefully fall back to simulated responses if:

- SDK initialization fails
- API key is invalid
- Network issues occur
- SDK encounters errors

This ensures the app remains functional even if the RunAnywhere service is unavailable.

---

### 3. Jenkins Base URL 🟡 DYNAMIC (User-Configured)

**Status:** 🟡 Placeholder present (will be overridden)  
**Impact:** None - Jenkins URL is set per account by user

**Current Value:**

```kotlin
// app/src/main/java/com/secureops/app/di/NetworkModule.kt (Line 75)
.baseUrl("http://localhost:8080/") // Default, will be overridden by account baseUrl
```

#### What to do:

**Option 1: Leave as-is (Recommended)**

- This is a fallback default
- Users will provide their Jenkins URL when adding accounts
- No action needed

**Option 2: Change default:**

```kotlin
.baseUrl("https://jenkins.yourcompany.com/")
```

---

## 🟡 MEDIUM Priority - Recommended Before Production

### 4. ProGuard Rules Review

**Status:** ✅ Basic rules present  
**Impact:** Code obfuscation and optimization

#### What to do:

Review `app/proguard-rules.pro` and add any custom rules:

```proguard
# Keep data models for JSON serialization
-keep class com.secureops.app.domain.model.** { *; }
-keep class com.secureops.app.data.remote.dto.** { *; }

# Keep Retrofit service interfaces
-keep interface com.secureops.app.data.remote.api.** { *; }

# Keep RunAnywhere SDK (if using real AI)
-keep class com.runanywhere.** { *; }
```

---

### 5. Application Signing Configuration

**Status:** ❌ Not configured  
**Impact:** Cannot release to Play Store without signing

#### What to do:

1. **Create Keystore:**
   ```bash
   keytool -genkey -v -keystore secureops-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias secureops
   ```

2. **Add to `app/build.gradle.kts`:**
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../secureops-release-key.jks")
               storePassword = System.getenv("KEYSTORE_PASSWORD")
               keyAlias = "secureops"
               keyPassword = System.getenv("KEY_PASSWORD")
           }
       }

       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               isMinifyEnabled = true
               // ... existing config
           }
       }
   }
   ```

3. **Set Environment Variables:**
   ```bash
   export KEYSTORE_PASSWORD="your_store_password"
   export KEY_PASSWORD="your_key_password"
   ```

---

### 6. Version Management

**Status:** ✅ Set to 1.0.0  
**Impact:** Update before each release

**Current Values:**

```kotlin
// app/build.gradle.kts
versionCode = 1
versionName = "1.0.0"
```

#### What to do:

Update versions for each release:

- `versionCode` - Increment by 1 for each release (1, 2, 3...)
- `versionName` - Semantic versioning (1.0.0, 1.0.1, 1.1.0, etc.)

---

### 7. API Request Logging Level

**Status:** 🟡 Set to BODY (verbose)  
**Impact:** Logs all API requests/responses (should be NONE in production)

**Current Configuration:**

```kotlin
// app/src/main/java/com/secureops/app/di/NetworkModule.kt (Line 36)
level = HttpLoggingInterceptor.Level.BODY
```

#### What to do:

Change for production:

```kotlin
level = if (BuildConfig.DEBUG) {
    HttpLoggingInterceptor.Level.BODY
} else {
    HttpLoggingInterceptor.Level.NONE  // Or BASIC for minimal logging
}
```

---

## 🟢 LOW Priority - Optional Enhancements

### 8. Analytics/Crash Reporting

**Status:** ❌ Not implemented  
**Impact:** No crash reporting or usage analytics

#### What to do (Optional):

Add Firebase Crashlytics or similar:

1. **Add Dependencies:**
   ```kotlin
   // app/build.gradle.kts
   implementation("com.google.firebase:firebase-crashlytics-ktx")
   implementation("com.google.firebase:firebase-analytics-ktx")
   ```

2. **Add Plugin:**
   ```kotlin
   plugins {
       // ... existing plugins
       id("com.google.firebase.crashlytics")
   }
   ```

3. **Initialize in Application:**
   ```kotlin
   // SecureOpsApplication.kt
   FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
   ```

---

### 9. Server-Side Token Management

**Status:** 🟡 TODO comment present  
**Impact:** Optional - FCM tokens not sent to backend

**Location:**

```kotlin
// app/src/main/java/com/secureops/app/data/notification/SecureOpsMessagingService.kt (Line 20)
override fun onNewToken(token: String) {
    super.onNewToken(token)
    Timber.d("FCM Token refreshed: $token")
    // TODO: Send token to server if needed
}
```

#### What to do (if you have a backend):

```kotlin
override fun onNewToken(token: String) {
    super.onNewToken(token)
    Timber.d("FCM Token refreshed: $token")
    
    // Send to your backend
    lifecycleScope.launch {
        try {
            yourBackendApi.updateFcmToken(token)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update FCM token")
        }
    }
}
```

---

### 10. Local Properties (SDK Path)

**Status:** ✅ Set to developer machine  
**Impact:** None - automatically configured by Android Studio

**Current Value:**

```properties
# local.properties
sdk.dir=C\:\\Users\\aravi\\AppData\\Local\\Android\\Sdk
```

#### What to do:

**Nothing** - This file is auto-generated and in `.gitignore`. Each developer/CI will have their
own.

---

## 📋 Pre-Launch Checklist

### Critical Configuration:

- [ ] **Firebase Configuration**
    - [ ] `google-services.json` added
    - [ ] Firebase plugin configured
    - [ ] FCM tested and working

- [ ] **Release Signing**
    - [ ] Keystore created
    - [ ] Signing config added to build.gradle
    - [ ] Environment variables set

- [ ] **ProGuard Rules**
    - [ ] Rules reviewed
    - [ ] Release build tested
    - [ ] App functions correctly after obfuscation

- [ ] **Version Numbers**
    - [ ] versionCode set correctly
    - [ ] versionName updated

- [ ] **Logging Level**
    - [ ] Production logging configured
    - [ ] No sensitive data in logs

### Optional Configuration:

- [ ] **RunAnywhere AI**
    - [ ] API key obtained (if using real AI)
    - [ ] SDK code uncommented (if using real AI)
    - [ ] Tested with real API

- [ ] **Analytics**
    - [ ] Crashlytics added (if desired)
    - [ ] Analytics configured (if desired)

- [ ] **Backend Integration**
    - [ ] FCM token sync implemented (if using backend)
    - [ ] API endpoints configured

---

## 🔍 How to Verify Configuration

### Test Firebase:

1. Build and install app
2. Check logcat for: `FCM Token refreshed: ...`
3. Send test notification from Firebase Console
4. Verify notification appears

### Test Release Build:

```bash
./gradlew assembleRelease
```

Should complete without errors and produce APK at:
`app/build/outputs/apk/release/app-release.apk`

### Test ProGuard:

```bash
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

Launch app and verify all features work.

---

## 📝 Summary of Required Actions

### MUST DO (Before Production):

1. ✅ **Add Firebase configuration** (`google-services.json`)
2. ✅ **Configure release signing**
3. ✅ **Set logging level for production**
4. ✅ **Update version numbers**
5. ✅ **Review ProGuard rules**

### OPTIONAL (Can do later):

1. ⏳ Enable real RunAnywhere AI (currently using fallback)
2. ⏳ Add Crashlytics/Analytics
3. ⏳ Implement backend FCM token sync
4. ⏳ Add custom analytics tracking

---

## 🎯 Current Status

| Item | Status | Required | Action Needed |
|------|--------|----------|---------------|
| Firebase Config | ❌ Missing | ✅ Yes | Add google-services.json |
| Release Signing | ❌ Missing | ✅ Yes | Create keystore |
| Logging Level | 🟡 Verbose | ✅ Yes | Change to NONE for release |
| Version Numbers | ✅ Set | ✅ Yes | Update per release |
| ProGuard Rules | ✅ Basic | ✅ Yes | Review and test |
| RunAnywhere AI | 🟡 Fallback | ❌ No | Works with simulation |
| Jenkins URL | 🟡 Placeholder | ❌ No | User-configured |
| Analytics | ❌ None | ❌ No | Optional enhancement |
| Backend Sync | ❌ None | ❌ No | Optional if needed |

---

## 🚀 Quick Start for Production

Minimum steps to get production-ready:

```bash
# 1. Add Firebase
# - Download google-services.json from Firebase Console
# - Place in app/google-services.json
# - Add plugin to build.gradle.kts

# 2. Create release keystore
keytool -genkey -v -keystore secureops-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias secureops

# 3. Build release
export KEYSTORE_PASSWORD="your_password"
export KEY_PASSWORD="your_password"
./gradlew assembleRelease

# 4. Test release APK
adb install app/build/outputs/apk/release/app-release.apk

# 5. Upload to Play Console
# Done!
```

---

## 📞 Support

If you encounter issues during configuration:

1. Check Firebase Console for FCM setup
2. Verify keystore creation was successful
3. Test release build thoroughly before upload
4. Review ProGuard logs if crashes occur

---

**Document Version:** 1.0  
**Last Updated:** November 2, 2025  
**Status:** Ready for Production Configuration
