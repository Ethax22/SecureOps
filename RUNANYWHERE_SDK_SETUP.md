# 🚀 RunAnywhere SDK Integration Guide

**Version:** 0.1.2-alpha  
**App:** SecureOps  
**Date:** November 2025

---

## 📋 Overview

This guide will help you integrate the **RunAnywhere SDK** into SecureOps to enable on-device AI
capabilities for:

- ✅ Build failure analysis
- ✅ AI-powered playbook generation
- ✅ Root cause determination
- ✅ Intelligent recommendations

---

## 📥 Step 1: Download SDK Files

Download both required AAR files from GitHub releases:

### Option A: Direct Download

1. **RunAnywhereKotlinSDK-release.aar** (4.0MB)
   ```
   https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/RunAnywhereKotlinSDK-release-clean.aar
   ```

2. **runanywhere-llm-llamacpp-release.aar** (2.1MB)
   ```
   https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/runanywhere-llm-llamacpp-release.aar
   ```

### Option B: Command Line

```powershell
# Navigate to your project
cd C:\Users\aravi\StudioProjects\Vibestate

# Create libs directory if it doesn't exist
New-Item -ItemType Directory -Force -Path app\libs

# Download SDK files
cd app\libs

curl -L -o RunAnywhereKotlinSDK-release.aar https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/RunAnywhereKotlinSDK-release-clean.aar

curl -L -o runanywhere-llm-llamacpp-release.aar https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/runanywhere-llm-llamacpp-release.aar
```

---

## 📂 Step 2: Place AAR Files

Place both AAR files in the `app/libs/` directory:

```
Vibestate/
└── app/
    └── libs/
        ├── RunAnywhereKotlinSDK-release.aar    ← Place here
        └── runanywhere-llm-llamacpp-release.aar ← Place here
```

**Verify files exist:**

```powershell
ls app\libs\*.aar
```

You should see:

```
RunAnywhereKotlinSDK-release.aar
runanywhere-llm-llamacpp-release.aar
```

---

## ⚙️ Step 3: Update Gradle Files

The gradle files have already been updated with:

### ✅ `app/build.gradle.kts`

```kotlin
dependencies {
    // RunAnywhere SDK - Local AAR files
    implementation(files("libs/RunAnywhereKotlinSDK-release.aar"))
    implementation(files("libs/runanywhere-llm-llamacpp-release.aar"))

    // Required SDK dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("io.ktor:ktor-client-core:3.0.3")
    // ... other Ktor dependencies
}
```

---

## 🔧 Step 4: Update AndroidManifest.xml

The manifest already has the required permissions. Verify these are present:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission 
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />

<application
    android:largeHeap="true"  ← REQUIRED for AI models
    ... >
</application>
```

---

## 🎯 Step 5: Initialize SDK

The SDK initialization code is already in `RunAnywhereManager.kt`. It will automatically initialize
when the app starts.

**Current Implementation:**

```kotlin
// In SecureOpsApplication.kt (if you have one)
class SecureOpsApplication : Application() {
    @Inject lateinit var runAnywhereManager: RunAnywhereManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize RunAnywhere SDK
        lifecycleScope.launch {
            runAnywhereManager.initialize(apiKey = "dev")
        }
    }
}
```

---

## 🏗️ Step 6: Build Project

Now build the project with the SDK:

```powershell
cd C:\Users\aravi\StudioProjects\Vibestate

# Clean build
.\gradlew clean

# Build debug APK
.\gradlew assembleDebug
```

**Expected Build Time:** 3-5 minutes (first build compiles SDK)

---

## 🎨 Step 7: Add Model Management UI (Optional)

Create a screen for users to download and manage AI models:

### Model Manager Screen

```kotlin
// ModelManagerScreen.kt
@Composable
fun ModelManagerScreen(
    viewModel: ModelManagerViewModel = hiltViewModel()
) {
    val models by viewModel.models.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    
    LazyColumn {
        items(models) { model ->
            ModelCard(
                model = model,
                onDownload = { viewModel.downloadModel(model.id) },
                onLoad = { viewModel.loadModel(model.id) },
                downloadProgress = downloadProgress[model.id]
            )
        }
    }
}
```

### Model Manager ViewModel

```kotlin
class ModelManagerViewModel @Inject constructor(
    private val runAnywhereManager: RunAnywhereManager
) : ViewModel() {
    
    private val _models = MutableStateFlow<List<ModelInfo>>(emptyList())
    val models: StateFlow<List<ModelInfo>> = _models
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress
    
    init {
        loadModels()
    }
    
    private fun loadModels() {
        viewModelScope.launch {
            _models.value = runAnywhereManager.getAvailableModels()
        }
    }
    
    fun downloadModel(modelId: String) {
        viewModelScope.launch {
            runAnywhereManager.downloadModel(modelId).collect { progress ->
                _downloadProgress.value = _downloadProgress.value + (modelId to progress)
            }
            // Refresh model list
            loadModels()
        }
    }
    
    fun loadModel(modelId: String) {
        viewModelScope.launch {
            runAnywhereManager.loadModel(modelId)
        }
    }
}
```

---

## 📱 Step 8: Test the Integration

### Test 1: Verify SDK Initialization

```powershell
# Install and run app
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.secureops.app/.MainActivity

# Check logs
adb logcat | Select-String "RunAnywhere"
```

**Expected Log:**

```
D/RunAnywhereManager: RunAnywhere SDK initialized successfully
D/RunAnywhereManager: Models registered successfully
```

### Test 2: Test Text Generation

Add this test in your app:

```kotlin
// In Settings or Debug screen
Button(onClick = {
    viewModel.testAI()
}) {
    Text("Test AI")
}

// In ViewModel
fun testAI() {
    viewModelScope.launch {
        val result = runAnywhereManager.generateText(
            "Explain why a CI/CD build might fail"
        )
        result.onSuccess { response ->
            Log.d("Test", "AI Response: $response")
            // Show in UI
        }
    }
}
```

### Test 3: Test Model Download

```kotlin
fun testModelDownload() {
    viewModelScope.launch {
        val models = runAnywhereManager.getAvailableModels()
        val firstModel = models.firstOrNull()
        
        if (firstModel != null && !firstModel.isDownloaded) {
            runAnywhereManager.downloadModel(firstModel.id).collect { progress ->
                Log.d("Download", "Progress: ${(progress * 100).toInt()}%")
            }
        }
    }
}
```

---

## 🎯 Recommended Models

The SDK automatically registers these models:

| Model | Size | Speed | Quality | Use Case |
|-------|------|-------|---------|----------|
| SmolLM2 360M | 119 MB | ⚡⚡⚡ | ⭐⭐ | Testing, quick responses |
| Qwen 2.5 0.5B | 374 MB | ⚡⚡ | ⭐⭐⭐ | General use, good balance |
| Llama 3.2 1B | 815 MB | ⚡ | ⭐⭐⭐⭐ | Best quality, detailed analysis |

**Recommendation:** Start with **SmolLM2 360M** for testing, then upgrade to **Qwen 2.5 0.5B** for
production.

---

## 🔧 Troubleshooting

### Issue 1: "Unresolved reference: RunAnywhere"

**Cause:** AAR files not in `app/libs/` directory

**Solution:**

```powershell
# Verify files exist
ls app\libs\*.aar

# If missing, download them:
cd app\libs
curl -L -o RunAnywhereKotlinSDK-release.aar https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/RunAnywhereKotlinSDK-release-clean.aar
curl -L -o runanywhere-llm-llamacpp-release.aar https://github.com/RunanywhereAI/runanywhere-sdks/releases/download/android/v0.1.2-alpha/runanywhere-llm-llamacpp-release.aar

# Then sync project
```

### Issue 2: Build Fails with Dependency Errors

**Solution:**

```powershell
# Clean and rebuild
.\gradlew clean
.\gradlew assembleDebug
```

### Issue 3: "Failed to initialize RunAnywhere SDK"

**Check:**

1. ✅ AAR files in correct location
2. ✅ `largeHeap="true"` in manifest
3. ✅ INTERNET permission granted
4. ✅ Sufficient device storage

### Issue 4: Model Download Fails

**Check:**

1. ✅ Internet connectivity
2. ✅ Storage space (need 100MB - 1GB per model)
3. ✅ HuggingFace URLs accessible
4. ✅ INTERNET permission

---

## 📊 Usage Examples

### Example 1: Analyze Build Failure

```kotlin
suspend fun analyzeBuildFailure(logs: String): String {
    val prompt = """
        Analyze this CI/CD build failure and suggest solutions:
        
        Logs:
        $logs
        
        Analysis:
    """.trimIndent()
    
    val result = runAnywhereManager.generateText(prompt)
    return result.getOrDefault("Unable to analyze")
}
```

### Example 2: Generate Incident Playbook

```kotlin
suspend fun generatePlaybook(error: String): String {
    val prompt = """
        Create a 5-step incident response playbook for this error:
        
        Error: $error
        
        Playbook:
    """.trimIndent()
    
    val result = runAnywhereManager.generateText(prompt)
    return result.getOrDefault("Unable to generate playbook")
}
```

### Example 3: Root Cause Analysis

```kotlin
suspend fun findRootCause(
    commits: List<Commit>,
    errorMessage: String
): String {
    val prompt = """
        Based on these recent commits and error, determine the root cause:
        
        Commits:
        ${commits.joinToString("\n") { "- ${it.message} by ${it.author}" }}
        
        Error: $errorMessage
        
        Root Cause:
    """.trimIndent()
    
    val result = runAnywhereManager.generateText(prompt)
    return result.getOrDefault("Unable to determine root cause")
}
```

---

## 🎉 Benefits of Integration

### Before (Simulation Mode):

- ❌ Generic responses
- ❌ No context awareness
- ❌ Limited accuracy
- ❌ No real AI

### After (Real SDK):

- ✅ **Real AI-powered analysis**
- ✅ **Context-aware responses**
- ✅ **High accuracy**
- ✅ **On-device processing** (privacy-first)
- ✅ **Fast inference** (<80ms TTFT)
- ✅ **Works offline** (after model download)

---

## 📝 Checklist

- [ ] Downloaded both AAR files
- [ ] Placed AARs in `app/libs/`
- [ ] Verified gradle dependencies
- [ ] Confirmed `largeHeap="true"` in manifest
- [ ] Ran `.\gradlew clean`
- [ ] Built project successfully
- [ ] Installed on emulator/device
- [ ] Checked logs for initialization
- [ ] Downloaded at least one model
- [ ] Tested text generation
- [ ] Updated UI for model management (optional)

---

## 🚀 Next Steps

### Immediate:

1. **Download AAR files** and place in `app/libs/`
2. **Build project** with `.\gradlew assembleDebug`
3. **Test initialization** by checking logs

### Short-term:

1. **Download SmolLM2 360M** model for testing
2. **Test AI responses** in your existing features
3. **Monitor performance** on target devices

### Long-term:

1. **Add Model Manager UI** for users
2. **Optimize prompts** for your use cases
3. **Test with different models**
4. **Collect user feedback**

---

## 📚 Resources

- **SDK Releases:** https://github.com/RunanywhereAI/runanywhere-sdks/releases
- **Documentation:** Full guide in repository
- **Issues:** https://github.com/RunanywhereAI/runanywhere-sdks/issues
- **Models:** https://huggingface.co/models?library=gguf

---

## 💡 Tips

1. **Start Small:** Use SmolLM2 360M for testing
2. **Monitor Memory:** Check RAM usage with large models
3. **Test Offline:** Verify offline functionality
4. **User Choice:** Let users select model size
5. **Cache Models:** Models are cached automatically
6. **Graceful Fallback:** Current simulation mode works as fallback

---

**Status:** Ready for integration  
**Action Required:** Download AAR files and build project  
**Estimated Time:** 15-30 minutes

🎊 **Once integrated, SecureOps will have real AI-powered features!**
