# Remaining 5% - Implementation Plan 🔧

**Status:** IN PROGRESS  
**Estimated Total Effort:** 30-40 hours

---

## 📋 Features to Implement

### **1. OAuth2 Authentication** ⏳ **IN PROGRESS**

**Status:** ✅ Core implementation created  
**Remaining:** Dependencies and UI integration

**Files Created:**

- ✅ `OAuth2Manager.kt` (309 lines) - Core OAuth logic

**Dependencies Needed:**

```gradle
// Add to app/build.gradle.kts
implementation("androidx.browser:browser:1.7.0") // Chrome Custom Tabs
```

**UI Changes Needed:**

- Update `AddAccountScreen.kt` - Add "Sign in with OAuth" button
- Create `OAuthCallbackActivity` - Handle redirect URIs
- Update `AndroidManifest.xml` - Add intent filter for oauth callback

**Provider Support:**

- ✅ GitHub Actions
- ✅ GitLab CI
- ✅ Azure DevOps

**Configuration Required:**

1. Register OAuth apps with each provider
2. Get client IDs and secrets
3. Update OAuth2Manager constants
4. Add redirect URI to manifest

---

### **2. WebSocket Live Streaming** ⏳ **50% DONE**

**Status:** ⚠️ Code exists but not integrated

**Existing Files:**

- ✅ `PipelineStreamService.kt` (260 lines) - WebSocket/SSE infrastructure

**What's Working:**

- ✅ WebSocket connection logic
- ✅ SSE (Server-Sent Events) logic
- ✅ Log entry parsing
- ✅ Build progress tracking

**What's Missing:**

- ❌ Not integrated into BuildDetailsScreen
- ❌ No UI for live streaming
- ❌ No connection management

**Implementation Steps:**

#### **Step 1: Update BuildDetailsViewModel** (2 hours)

```kotlin
// Add streaming functionality
private var logStreamJob: Job? = null

fun startLogStreaming() {
    logStreamJob = viewModelScope.launch {
        pipelineStreamService.streamBuildLogs(pipeline, token)
            .collect { logEntry ->
                // Append log entry to state
                _uiState.value = _uiState.value.copy(
                    logs = _uiState.value.logs + "\n" + logEntry.message
                )
            }
    }
}

fun stopLogStreaming() {
    logStreamJob?.cancel()
}
```

#### **Step 2: Update BuildDetailsScreen** (2 hours)

- Add "Stream Live" toggle button
- Show streaming indicator (pulsing dot)
- Auto-scroll to bottom when streaming
- Stop streaming when navigating away

#### **Step 3: Add Progress Tracking** (4 hours)

- Create `BuildProgressIndicator` composable
- Show current step / total steps
- Display step names
- Animate progress transitions

**Files to Modify:**

- `BuildDetailsViewModel.kt` - Add streaming methods
- `BuildDetailsScreen.kt` - Add streaming UI
- Create `BuildProgressIndicator.kt` - Progress UI component

---

### **3. Artifacts Support** ❌ **NOT STARTED**

**Effort:** 8-16 hours

**Requirements:**

1. List artifacts for a build
2. Download artifacts
3. View/preview artifacts (if possible)
4. Cache downloaded artifacts

#### **Step 1: Add Artifact Models** (1 hour)

```kotlin
data class BuildArtifact(
    val id: String,
    val name: String,
    val size: Long,
    val downloadUrl: String,
    val contentType: String
)
```

#### **Step 2: Add API Calls** (4 hours)

Update each service:

- `GitHubService.kt` - Add `getArtifacts()` and `downloadArtifact()`
- `GitLabService.kt` - Add artifact endpoints
- `JenkinsService.kt` - Add artifact endpoints
- `CircleCIService.kt` - Add artifact endpoints
- `AzureDevOpsService.kt` - Add artifact endpoints

#### **Step 3: Add Repository Methods** (2 hours)

```kotlin
// PipelineRepository.kt
suspend fun getArtifacts(pipeline: Pipeline): Result<List<BuildArtifact>>
suspend fun downloadArtifact(artifact: BuildArtifact, destination: File): Result<File>
```

#### **Step 4: Add UI** (6 hours)

- Create `ArtifactsSection` composable for BuildDetailsScreen
- Add download progress indicator
- Add artifact preview (images, text files)
- Add "Open with" functionality
- Cache downloaded artifacts

**Files to Create:**

- `domain/model/BuildArtifact.kt`
- `ui/components/ArtifactsSection.kt`
- `data/artifacts/ArtifactDownloader.kt`

**Files to Modify:**

- All service interfaces
- `PipelineRepository.kt`
- `BuildDetailsScreen.kt`
- `BuildDetailsViewModel.kt`

---

### **4. Slack Notifications** ❌ **STUB EXISTS**

**Effort:** 4-6 hours

**Current State:**

- ⚠️ `RemediationExecutor.notifySlack()` returns fake success
- ⚠️ No actual Slack API integration

#### **Implementation:**

**Step 1: Add Slack Webhook Support** (2 hours)

```kotlin
class SlackNotifier(private val client: OkHttpClient) {
    suspend fun sendNotification(
        webhookUrl: String,
        message: SlackMessage
    ): Result<Unit> {
        val json = buildSlackMessage(message)
        val request = Request.Builder()
            .url(webhookUrl)
            .post(json.toRequestBody())
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Slack notification failed"))
        }
    }
    
    private fun buildSlackMessage(message: SlackMessage): String {
        return JSONObject().apply {
            put("text", message.text)
            put("blocks", JSONArray().apply {
                // Add rich formatting
            })
        }.toString()
    }
}
```

**Step 2: Add Slack Configuration** (1 hour)

- Add Slack webhook URL to settings
- Store encrypted in SecureTokenManager
- Add UI for Slack configuration

**Step 3: Integrate into Notifications** (2 hours)

- Update `NotificationManager` to send Slack notifications
- Add Slack notification type
- Test with real webhook

**Files to Create:**

- `data/notification/SlackNotifier.kt`
- `ui/screens/settings/SlackSettingsScreen.kt`

**Files to Modify:**

- `RemediationExecutor.kt`
- `NotificationManager.kt`
- `SettingsScreen.kt`

---

### **5. Email Notifications** ❌ **STUB EXISTS**

**Effort:** 6-8 hours

**Current State:**

- ⚠️ `RemediationExecutor.notifyEmail()` returns fake success
- ⚠️ No actual email sending

#### **Implementation Options:**

**Option A: Use SMTP** (Simpler)

```kotlin
class EmailNotifier(
    private val smtpConfig: SmtpConfig
) {
    suspend fun sendEmail(
        to: List<String>,
        subject: String,
        body: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", smtpConfig.host)
                put("mail.smtp.port", smtpConfig.port)
            }
            
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(smtpConfig.username, smtpConfig.password)
            })
            
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpConfig.fromEmail))
                setRecipients(Message.RecipientType.TO, to.joinToString(","))
                setSubject(subject)
                setText(body, "utf-8", "html")
            }
            
            Transport.send(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Option B: Use SendGrid/Mailgun API** (More reliable)

- Requires API key
- Better deliverability
- Easier to implement

**Step 1: Add Email Dependencies** (1 hour)

```gradle
implementation("com.sun.mail:android-mail:1.6.7")
implementation("com.sun.mail:android-activation:1.6.7")
```

**Step 2: Add Email Configuration** (2 hours)

- SMTP settings UI
- Email templates
- Test email functionality

**Step 3: Integrate** (3 hours)

- Update RemediationExecutor
- Add to NotificationManager
- Format HTML emails nicely

**Files to Create:**

- `data/notification/EmailNotifier.kt`
- `data/notification/EmailTemplate.kt`
- `ui/screens/settings/EmailSettingsScreen.kt`

---

## 📦 Required Dependencies

### **Add to `app/build.gradle.kts`:**

```gradle
dependencies {
    // OAuth2 / Chrome Custom Tabs
    implementation("androidx.browser:browser:1.7.0")
    
    // Email (SMTP)
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    
    // Existing dependencies (verify they exist)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // ... others
}
```

---

## 🗂️ Files to Create (New)

| File | Purpose | Lines | Priority |
|------|---------|-------|----------|
| `OAuth2Manager.kt` | ✅ Created | 309 | High |
| `OAuthCallbackActivity.kt` | Handle OAuth redirects | 50 | High |
| `BuildProgressIndicator.kt` | Progress UI | 150 | Medium |
| `BuildArtifact.kt` | Model | 20 | Medium |
| `ArtifactsSection.kt` | UI | 200 | Medium |
| `ArtifactDownloader.kt` | Logic | 150 | Medium |
| `SlackNotifier.kt` | Slack API | 100 | Low |
| `EmailNotifier.kt` | Email SMTP | 150 | Low |
| `EmailTemplate.kt` | Templates | 100 | Low |
| `SlackSettingsScreen.kt` | UI | 100 | Low |
| `EmailSettingsScreen.kt` | UI | 100 | Low |

**Total New Files:** 11  
**Total New Lines:** ~1,429

---

## 🔧 Files to Modify (Existing)

| File | Changes | Effort |
|------|---------|--------|
| `app/build.gradle.kts` | Add dependencies | 5 min |
| `AndroidManifest.xml` | Add OAuth intent filter | 10 min |
| `AddAccountScreen.kt` | Add OAuth button | 2 hours |
| `BuildDetailsViewModel.kt` | Add streaming | 2 hours |
| `BuildDetailsScreen.kt` | Add streaming UI + artifacts | 6 hours |
| `PipelineRepository.kt` | Add artifact methods | 2 hours |
| All service files (5) | Add artifact endpoints | 4 hours |
| `RemediationExecutor.kt` | Integrate Slack/Email | 2 hours |
| `NotificationManager.kt` | Add Slack/Email | 2 hours |
| `SettingsScreen.kt` | Add links to new settings | 1 hour |

**Total Files to Modify:** 15+  
**Total Effort:** 21+ hours

---

## ⏱️ Time Estimates by Feature

| Feature | Effort | Priority |
|---------|--------|----------|
| OAuth2 Complete | 6-8 hours | ⭐⭐⭐ High |
| WebSocket Integration | 8-10 hours | ⭐⭐⭐ High |
| Artifacts Support | 12-16 hours | ⭐⭐ Medium |
| Slack Notifications | 4-6 hours | ⭐ Low |
| Email Notifications | 6-8 hours | ⭐ Low |
| **TOTAL** | **36-48 hours** | |

---

## 🎯 Implementation Priority

### **Phase 1: Critical (Week 1)** ⭐⭐⭐

1. ✅ OAuth2 Manager created
2. ⏳ OAuth2 UI integration (6 hours)
3. ⏳ WebSocket UI integration (8 hours)

**Deliverable:** OAuth login + Live log streaming

### **Phase 2: Important (Week 2)** ⭐⭐

4. ⏳ Artifacts support (12 hours)
5. ⏳ Build progress indicators (4 hours)

**Deliverable:** Full build details with artifacts

### **Phase 3: Nice-to-Have (Week 3)** ⭐

6. ⏳ Slack notifications (4 hours)
7. ⏳ Email notifications (6 hours)

**Deliverable:** Complete notification system

---

## 🚀 Quick Wins (< 2 hours each)

1. ✅ OAuth2Manager.kt created
2. ⏳ Add OAuth button to AddAccountScreen (1 hour)
3. ⏳ Add "Stream Live" toggle to BuildDetailsScreen (1 hour)
4. ⏳ Create BuildProgressIndicator composable (1.5 hours)
5. ⏳ Add Slack webhook configuration (1 hour)

---

## 📝 Notes

### **OAuth2 Configuration:**

Before OAuth works, you need to:

1. Register OAuth apps at:
    - GitHub: https://github.com/settings/developers
    - GitLab: https://gitlab.com/-/profile/applications
    - Azure: https://portal.azure.com
2. Get client IDs and secrets
3. Update `OAuth2Manager.kt` constants
4. Configure redirect URI in all providers

### **Testing:**

- OAuth: Test with real GitHub/GitLab accounts
- WebSocket: Test with running builds
- Artifacts: Test with builds that produce artifacts
- Slack: Get a test webhook URL
- Email: Use a test SMTP server

---

## ✅ Current Status

| Feature | Status | Progress |
|---------|--------|----------|
| OAuth2 | ⏳ In Progress | 30% (core done, UI pending) |
| WebSocket | ⏳ In Progress | 50% (code exists, not integrated) |
| Artifacts | ❌ Not Started | 0% |
| Slack | ❌ Not Started | 0% (stub exists) |
| Email | ❌ Not Started | 0% (stub exists) |

**Overall Remaining 5%:** Actually ~40 hours of work

---

## 🎉 When Complete

You'll have:

- ✅ OAuth2 login (GitHub, GitLab, Azure)
- ✅ Live log streaming (WebSocket/SSE)
- ✅ Build progress indicators
- ✅ Artifact download/viewing
- ✅ Slack notifications
- ✅ Email notifications
- ✅ **100% feature complete!**

---

**Ready to implement!** Let me know which phase to start with! 🚀
