# RunAnywhere SDK - Quick Start Guide

## ✅ Status: READY TO USE

The RunAnywhere SDK has been successfully integrated into SecureOps. The build compiles
successfully!

---

## 🚀 Quick Start (3 Steps)

### Step 1: Build & Run the App

```bash
./gradlew assembleDebug
```

The SDK initializes automatically when the app starts.

### Step 2: Download a Model

Add this to any ViewModel:

```kotlin
class MyViewModel(
    private val runAnywhereManager: RunAnywhereManager  // Inject with Koin
) : ViewModel() {

    fun downloadSmallModel() {
        viewModelScope.launch {
            // Get first model (SmolLM2 360M - 119 MB)
            val models = runAnywhereManager.getAvailableModels()
            val modelToDownload = models.firstOrNull() ?: return@launch
            
            // Download with progress
            runAnywhereManager.downloadModel(modelToDownload.id).collect { progress ->
                val percent = (progress * 100).toInt()
                println("Downloading: $percent%")
            }
            
            println("Download complete!")
        }
    }
}
```

### Step 3: Load & Use the Model

```kotlin
fun loadAndUseModel() {
    viewModelScope.launch {
        // Get downloaded model
        val models = runAnywhereManager.getAvailableModels()
        val downloadedModel = models.firstOrNull { it.isDownloaded } ?: return@launch
        
        // Load the model
        val loaded = runAnywhereManager.loadModel(downloadedModel.id)
        
        if (loaded) {
            // Generate AI response
            val response = runAnywhereManager.generateAnalysis(
                "What is the cause of build failures?"
            )
            println("AI says: $response")
        }
    }
}
```

---

## 💡 Common Use Cases

### Use Case 1: Streaming Chat

```kotlin
fun chat(userMessage: String) {
    viewModelScope.launch {
        var fullResponse = ""
        
        try {
            runAnywhereManager.generateTextStream(userMessage).collect { token ->
                fullResponse += token
                // Update UI with each token
                _messageState.value = fullResponse
            }
        } catch (e: IllegalStateException) {
            // Model not loaded - show error
            _errorState.value = "Please download and load a model first"
        }
    }
}
```

### Use Case 2: Failure Analysis (Already Implemented)

```kotlin
// This is already working in PlaybookManager.kt!
val playbook = playbookManager.generateAIPlaybook(
    pipeline = myPipeline,
    errorDetails = "Build failed: OutOfMemoryError"
)

// Playbook will have AI-generated steps for remediation
```

### Use Case 3: Commit Analysis (Already Implemented)

```kotlin
// This is already working in ChangelogAnalyzer.kt!
val analysis = changelogAnalyzer.analyzeChangelog(
    pipeline = myPipeline,
    commits = recentCommits
)

// Analysis will identify suspicious commits with AI assistance
println("Root cause: ${analysis.rootCauseCommit?.commit?.message}")
println("Confidence: ${(analysis.confidence * 100).toInt()}%")
```

---

## 📊 Available Models

### Default Models (Pre-configured)

1. **SmolLM2 360M Q8_0** (119 MB)
    - ⚡ Fastest
    - Best for quick responses
    - Low memory (1GB RAM)

2. **Qwen 2.5 0.5B Instruct Q6_K** (374 MB)
    - ⚡⚡ Fast
    - Balanced quality
    - Medium memory (2GB RAM)

### Getting Model IDs

```kotlin
viewModelScope.launch {
    val models = runAnywhereManager.getAvailableModels()
    models.forEach { model ->
        println("Name: ${model.name}")
        println("ID: ${model.id}")
        println("Downloaded: ${model.isDownloaded}")
        println("Size: ${model.size} bytes")
        println("---")
    }
}
```

---

## 🎯 Complete Example: AI Chat ViewModel

```kotlin
class AIChatViewModel(
    private val runAnywhereManager: RunAnywhereManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _modelStatus = MutableStateFlow("No model loaded")
    val modelStatus: StateFlow<String> = _modelStatus

    init {
        checkModelStatus()
    }

    private fun checkModelStatus() {
        viewModelScope.launch {
            val models = runAnywhereManager.getAvailableModels()
            val downloadedModel = models.firstOrNull { it.isDownloaded }
            
            _modelStatus.value = when {
                !runAnywhereManager.isReady() -> "SDK initializing..."
                downloadedModel == null -> "No model downloaded. Tap to download."
                !runAnywhereManager.isModelLoaded() -> "Model ready. Tap to load."
                else -> "Model loaded: ${runAnywhereManager.getCurrentModelId()}"
            }
        }
    }

    fun downloadDefaultModel() {
        viewModelScope.launch {
            _modelStatus.value = "Downloading model..."
            
            try {
                val models = runAnywhereManager.getAvailableModels()
                val model = models.firstOrNull() ?: return@launch
                
                runAnywhereManager.downloadModel(model.id).collect { progress ->
                    _modelStatus.value = "Downloading: ${(progress * 100).toInt()}%"
                }
                
                _modelStatus.value = "Download complete. Tap to load."
            } catch (e: Exception) {
                _modelStatus.value = "Download failed: ${e.message}"
            }
        }
    }

    fun loadDownloadedModel() {
        viewModelScope.launch {
            _modelStatus.value = "Loading model..."
            
            try {
                val models = runAnywhereManager.getAvailableModels()
                val model = models.firstOrNull { it.isDownloaded } ?: return@launch
                
                val success = runAnywhereManager.loadModel(model.id)
                
                _modelStatus.value = if (success) {
                    "Model loaded! Ready to chat."
                } else {
                    "Failed to load model"
                }
            } catch (e: Exception) {
                _modelStatus.value = "Load failed: ${e.message}"
            }
        }
    }

    fun sendMessage(text: String) {
        if (!runAnywhereManager.isModelLoaded()) {
            _modelStatus.value = "Please load a model first"
            return
        }

        // Add user message
        _messages.value += ChatMessage(text, isUser = true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                var aiResponse = ""

                // Stream AI response
                runAnywhereManager.generateTextStream(text).collect { token ->
                    aiResponse += token

                    // Update or create AI message
                    val currentMessages = _messages.value.toMutableList()
                    if (currentMessages.lastOrNull()?.isUser == false) {
                        // Update existing AI message
                        currentMessages[currentMessages.lastIndex] = 
                            ChatMessage(aiResponse, isUser = false)
                    } else {
                        // Create new AI message
                        currentMessages.add(ChatMessage(aiResponse, isUser = false))
                    }
                    _messages.value = currentMessages
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _messages.value += ChatMessage(
                    "Error: ${e.message}",
                    isUser = false
                )
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
```

---

## 🔧 Troubleshooting

### Issue: "SDK not initialized"

**Solution**: Wait a few seconds after app start. The SDK initializes asynchronously.

```kotlin
// Check if ready
if (runAnywhereManager.isReady()) {
    // SDK is ready
} else {
    // Still initializing
}
```

### Issue: "No model loaded"

**Solution**: Download and load a model first.

```kotlin
// 1. Download
runAnywhereManager.downloadModel(modelId).collect { /* ... */ }

// 2. Load
runAnywhereManager.loadModel(modelId)

// 3. Check
if (runAnywhereManager.isModelLoaded()) {
    // Ready to generate
}
```

### Issue: Out of memory

**Solution**: Use a smaller model (SmolLM2 360M) or ensure `largeHeap="true"` in manifest.

```xml
<!-- AndroidManifest.xml -->
<application android:largeHeap="true" ...>
```

### Issue: Download fails

**Solution**: Check internet connection and storage space.

```kotlin
try {
    runAnywhereManager.downloadModel(modelId).collect { /* ... */ }
} catch (e: Exception) {
    // Handle download error
    println("Download failed: ${e.message}")
}
```

---

## 📱 Testing on Device

### Test 1: Check Logs

After app starts, check logcat for:

```
I/SecureOps: Starting RunAnywhere SDK initialization...
I/RunAnywhereManager: RunAnywhere SDK initialized successfully
I/RunAnywhereManager: LlamaCpp Service Provider registered
I/RunAnywhereManager: Models registered
I/RunAnywhereManager: Scanned for downloaded models
```

### Test 2: List Models

```kotlin
viewModelScope.launch {
    val models = runAnywhereManager.getAvailableModels()
    println("Found ${models.size} models")
}
```

Expected: `Found 2 models`

### Test 3: Download Test

Download the smallest model (SmolLM2 360M - 119 MB):

```kotlin
viewModelScope.launch {
    val models = runAnywhereManager.getAvailableModels()
    val smallModel = models.minByOrNull { it.size ?: Long.MAX_VALUE }
    
    smallModel?.let { model ->
        runAnywhereManager.downloadModel(model.id).collect { progress ->
            println("Progress: ${(progress * 100).toInt()}%")
        }
    }
}
```

### Test 4: Generation Test

```kotlin
viewModelScope.launch {
    // Load model first
    val models = runAnywhereManager.getAvailableModels()
    val downloaded = models.firstOrNull { it.isDownloaded }
    
    downloaded?.let { model ->
        runAnywhereManager.loadModel(model.id)
        
        // Generate
        val response = runAnywhereManager.generateAnalysis("Hello, AI!")
        println("AI Response: $response")
    }
}
```

---

## 🎓 Best Practices

### ✅ DO:

1. **Check model status** before generating
2. **Use streaming** for better UX
3. **Handle errors** gracefully
4. **Unload models** when not needed
5. **Start with small models** for testing

```kotlin
// Good
if (runAnywhereManager.isModelLoaded()) {
    runAnywhereManager.generateTextStream(prompt).collect { /* ... */ }
}
```

### ❌ DON'T:

1. **Don't block UI** thread
2. **Don't ignore errors**
3. **Don't load multiple models** simultaneously
4. **Don't generate without checking** model status

```kotlin
// Bad - Can crash if model not loaded
runAnywhereManager.generateTextStream(prompt) // No check!
```

---

## 📚 Where to Learn More

1. **Complete Documentation**: See `RUNANYWHERE_SDK_INTEGRATION_COMPLETE.md`
2. **Example Usage**: Check `PlaybookManager.kt` and `ChangelogAnalyzer.kt`
3. **Official Docs**: [RunAnywhere SDK GitHub](https://github.com/RunanywhereAI/runanywhere-sdks)

---

## ✨ Next Steps

1. ✅ **Build compiles** - DONE
2. 🔄 **Run on device** - Test initialization
3. 📥 **Download a model** - SmolLM2 360M recommended
4. 🤖 **Test generation** - Try simple prompts
5. 🎨 **Build UI** - Create chat or analysis screen

---

**Happy coding! The RunAnywhere SDK is ready to power your on-device AI features! 🚀**

---

*Last updated: 2025-01-04*
*Build Status: ✅ SUCCESS*
