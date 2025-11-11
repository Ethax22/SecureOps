# Complete Implementation Guide - Remaining 5% 🚀

**Total Effort:** 36-48 hours  
**Files to Create:** 11  
**Files to Modify:** 15+  
**Status:** Ready to implement

---

## 🎯 Implementation Approach

I recommend implementing in 3 phases over 2-3 weeks, OR we can do it together in multiple sessions.

**This guide provides ALL the code you need**, ready to copy-paste.

---

## ✅ PHASE 1: OAuth2 Authentication (6-8 hours)

### **Step 1.1: Fix OAuth2Manager** ✅ DONE

File already created: `OAuth2Manager.kt`  
**Action:** Dependencies added to `build.gradle.kts` ✅

---

### **Step 1.2: Create OAuthCallbackActivity**

**Create:** `app/src/main/java/com/secureops/app/ui/auth/OAuthCallbackActivity.kt`

```kotlin
package com.secureops.app.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.secureops.app.data.auth.OAuth2Manager
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Activity to handle OAuth callback redirects
 */
class OAuthCallbackActivity : ComponentActivity() {

    private val oauth2Manager: OAuth2Manager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri != null) {
            handleOAuthCallback(uri)
        } else {
            Timber.e("OAuth callback received with no data")
            finish()
        }
    }

    private fun handleOAuthCallback(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = oauth2Manager.handleCallback(uri)
                
                result.fold(
                    onSuccess = { token ->
                        // Store token and return to AddAccountScreen
                        val resultIntent = Intent().apply {
                            putExtra("access_token", token.accessToken)
                            putExtra("refresh_token", token.refreshToken)
                            putExtra("expires_in", token.expiresIn)
                        }
                        setResult(RESULT_OK, resultIntent)
                    },
                    onFailure = { error ->
                        Timber.e(error, "OAuth callback failed")
                        val resultIntent = Intent().apply {
                            putExtra("error", error.message)
                        }
                        setResult(RESULT_CANCELED, resultIntent)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error handling OAuth callback")
                setResult(RESULT_CANCELED)
            } finally {
                finish()
            }
        }
    }
}
```

---

### **Step 1.3: Update AndroidManifest.xml**

**Modify:** `app/src/main/AndroidManifest.xml`

Add inside `<application>` tag:

```xml
<!-- OAuth Callback Activity -->
<activity
    android:name=".ui.auth.OAuthCallbackActivity"
    android:exported="true"
    android:launchMode="singleTask">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <!-- OAuth redirect URI -->
        <data
            android:scheme="secureops"
            android:host="oauth"
            android:pathPrefix="/callback" />
    </intent-filter>
</activity>
```

---

### **Step 1.4: Update AddAccountViewModel**

**Modify:** `app/src/main/java/com/secureops/app/ui/screens/settings/AddAccountViewModel.kt`

Add OAuth functionality:

```kotlin
// Add to class
private val oauth2Manager: OAuth2Manager by inject()

/**
 * Start OAuth flow
 */
fun startOAuthFlow(provider: CIProvider) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        try {
            val result = oauth2Manager.authenticate(provider)
            
            result.fold(
                onSuccess = { token ->
                    // Token received, now create account
                    // This will be called from OAuthCallbackActivity result
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        oauthToken = token
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "OAuth failed: ${error.message}"
                    )
                }
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "OAuth error: ${e.message}"
            )
        }
    }
}

/**
 * Complete account creation with OAuth token
 */
fun completeOAuthAccount(
    provider: CIProvider,
    name: String,
    baseUrl: String,
    token: OAuthToken
) {
    viewModelScope.launch {
        // Same as addAccount but use OAuth token
        addAccount(
            provider = provider,
            name = name,
            baseUrl = baseUrl,
            token = token.accessToken
        )
    }
}
```

Update UiState:

```kotlin
data class AddAccountUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val oauthToken: OAuthToken? = null // NEW
)
```

---

### **Step 1.5: Update AddAccountScreen UI**

**Modify:** `app/src/main/java/com/secureops/app/ui/screens/settings/AddAccountScreen.kt`

Add OAuth button after provider selection:

```kotlin
// Add after provider selection, before account name field
if (selectedProvider != null && supportsOAuth(selectedProvider)) {
    Button(
        onClick = {
            viewModel.startOAuthFlow(selectedProvider!!)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.Login,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign in with OAuth")
    }

    Text(
        text = "OR",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}

// Helper function
private fun supportsOAuth(provider: CIProvider): Boolean {
    return provider in listOf(
        CIProvider.GITHUB_ACTIONS,
        CIProvider.GITLAB_CI,
        CIProvider.AZURE_DEVOPS
    )
}
```

---

### **Step 1.6: Register OAuth2Manager in Koin**

**Modify:** `app/src/main/java/com/secureops/app/di/RepositoryModule.kt`

Add:

```kotlin
// OAuth2
single { OAuth2Manager(androidContext()) }
```

---

## ✅ PHASE 2: WebSocket Live Streaming (8-10 hours)

### **Step 2.1: Update BuildDetailsViewModel**

**Modify:** `app/src/main/java/com/secureops/app/ui/screens/details/BuildDetailsViewModel.kt`

Add streaming functionality:

```kotlin
// Add property
private val pipelineStreamService: PipelineStreamService by inject()
private var logStreamJob: Job? = null

// Add to UiState
data class BuildDetailsUiState(
    val pipeline: Pipeline? = null,
    val logs: String? = null,
    val isLoading: Boolean = false,
    val isLoadingLogs: Boolean = false,
    val isExecutingAction: Boolean = false,
    val actionResult: ActionResult? = null,
    val error: String? = null,
    val logsError: String? = null,
    val isStreaming: Boolean = false, // NEW
    val streamingLogs: List<LogEntry> = emptyList() // NEW
)

/**
 * Start live log streaming
 */
fun startLogStreaming() {
    val pipeline = _uiState.value.pipeline ?: return
    
    viewModelScope.launch {
        try {
            val token = accountRepository.getAccountToken(pipeline.accountId)
                ?: return@launch

            _uiState.value = _uiState.value.copy(isStreaming = true)

            logStreamJob = launch {
                pipelineStreamService.streamBuildLogs(pipeline, token)
                    .collect { logEntry ->
                        val currentLogs = _uiState.value.streamingLogs
                        _uiState.value = _uiState.value.copy(
                            streamingLogs = currentLogs + logEntry
                        )
                    }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start log streaming")
            _uiState.value = _uiState.value.copy(
                isStreaming = false,
                logsError = "Streaming failed: ${e.message}"
            )
        }
    }
}

/**
 * Stop log streaming
 */
fun stopLogStreaming() {
    logStreamJob?.cancel()
    logStreamJob = null
    _uiState.value = _uiState.value.copy(isStreaming = false)
}

override fun onCleared() {
    super.onCleared()
    stopLogStreaming()
}
```

---

### **Step 2.2: Create BuildProgressIndicator**

**Create:** `app/src/main/java/com/secureops/app/ui/components/BuildProgressIndicator.kt`

```kotlin
package com.secureops.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animated build progress indicator
 */
@Composable
fun BuildProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Progress text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Step $currentStep of $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${(currentStep.toFloat() / totalSteps * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Current step name
        Text(
            text = stepName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Animated dots
        AnimatedDots()
    }
}

@Composable
private fun AnimatedDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (index == 0) alpha else 0.5f
                        )
                    )
            )
        }
    }
}

/**
 * Streaming indicator (pulsing dot)
 */
@Composable
fun StreamingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "streaming")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size((8 * scale).dp)
                .clip(CircleShape)
                .background(Color(0xFFFF4444))
        )
        Text(
            text = "Live",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFF4444)
        )
    }
}
```

---

### **Step 2.3: Update BuildDetailsScreen**

**Modify:** `app/src/main/java/com/secureops/app/ui/screens/details/BuildDetailsScreen.kt`

Add streaming controls:

```kotlin
// Add in the build logs section, before the ScrollableTabRow
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = "Build Logs",
        style = MaterialTheme.typography.titleMedium
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.isStreaming) {
            StreamingIndicator()
        }

        // Stream toggle button
        if (uiState.pipeline?.status == BuildStatus.RUNNING) {
            Button(
                onClick = {
                    if (uiState.isStreaming) {
                        viewModel.stopLogStreaming()
                    } else {
                        viewModel.startLogStreaming()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isStreaming)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = if (uiState.isStreaming) "Stop Live" else "Stream Live"
                )
            }
        }
    }
}

// Show streaming logs if active
if (uiState.isStreaming && uiState.streamingLogs.isNotEmpty()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(16.dp)
    ) {
        items(uiState.streamingLogs) { logEntry ->
            LogEntryItem(logEntry)
        }
    }
} else {
    // Show cached logs (existing code)
    Text(
        text = uiState.logs ?: "No logs available",
        modifier = Modifier.padding(16.dp)
    )
}
```

Add LogEntryItem composable:

```kotlin
@Composable
private fun LogEntryItem(logEntry: LogEntry) {
    val color = when (logEntry.level) {
        LogLevel.ERROR -> Color(0xFFFF4444)
        LogLevel.WARNING -> Color(0xFFFFAA00)
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = logEntry.message,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace
        ),
        color = color,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
```

---

## ✅ PHASE 3: Artifacts Support (12-16 hours)

### **Step 3.1: Create BuildArtifact Model**

**Create:** `app/src/main/java/com/secureops/app/domain/model/BuildArtifact.kt`

```kotlin
package com.secureops.app.domain.model

data class BuildArtifact(
    val id: String,
    val name: String,
    val size: Long, // bytes
    val downloadUrl: String,
    val contentType: String = "application/octet-stream",
    val createdAt: Long = System.currentTimeMillis()
)

fun Long.formatFileSize(): String {
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.2f MB".format(mb)
        kb >= 1 -> "%.2f KB".format(kb)
        else -> "$this bytes"
    }
}
```

---

### **Step 3.2: Add Artifact Endpoints to Services**

**Modify:** `app/src/main/java/com/secureops/app/data/remote/api/GitHubService.kt`

```kotlin
@GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
suspend fun getArtifacts(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("run_id") runId: Long
): Response<GitHubArtifactsResponse>

@GET
@Streaming
suspend fun downloadArtifact(@Url url: String): Response<ResponseBody>
```

Add DTO:

```kotlin
data class GitHubArtifactsResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("artifacts") val artifacts: List<GitHubArtifact>
)

data class GitHubArtifact(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("size_in_bytes") val sizeInBytes: Long,
    @SerializedName("archive_download_url") val archiveDownloadUrl: String,
    @SerializedName("created_at") val createdAt: String
)
```

Repeat for other services (GitLab, Jenkins, CircleCI, Azure).

---

### **Step 3.3: Add Repository Methods**

**Modify:** `app/src/main/java/com/secureops/app/data/repository/PipelineRepository.kt`

```kotlin
/**
 * Get artifacts for a pipeline
 */
suspend fun getArtifacts(pipeline: Pipeline): Result<List<BuildArtifact>> {
    return try {
        val account = accountRepository.getAccountById(pipeline.accountId)
            ?: return Result.failure(Exception("Account not found"))

        val token = accountRepository.getAccountToken(pipeline.accountId)
            ?: return Result.failure(Exception("Token not found"))

        when (pipeline.provider) {
            CIProvider.GITHUB_ACTIONS -> fetchGitHubArtifacts(pipeline, token)
            CIProvider.JENKINS -> fetchJenkinsArtifacts(pipeline, token)
            // ... other providers
            else -> Result.success(emptyList())
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch artifacts")
        Result.failure(e)
    }
}

private suspend fun fetchGitHubArtifacts(
    pipeline: Pipeline,
    token: String
): Result<List<BuildArtifact>> {
    val parts = pipeline.repositoryUrl.split("/")
    val owner = parts.getOrNull(parts.size - 2) ?: return Result.success(emptyList())
    val repo = parts.getOrNull(parts.size - 1) ?: return Result.success(emptyList())

    val response = githubService.getArtifacts(owner, repo, pipeline.id.toLong())

    return if (response.isSuccessful && response.body() != null) {
        val artifacts = response.body()!!.artifacts.map { artifact ->
            BuildArtifact(
                id = artifact.id.toString(),
                name = artifact.name,
                size = artifact.sizeInBytes,
                downloadUrl = artifact.archiveDownloadUrl,
                contentType = "application/zip"
            )
        }
        Result.success(artifacts)
    } else {
        Result.failure(Exception("Failed to fetch artifacts"))
    }
}

/**
 * Download artifact to file
 */
suspend fun downloadArtifact(
    artifact: BuildArtifact,
    destination: File
): Result<File> = withContext(Dispatchers.IO) {
    try {
        val response = githubService.downloadArtifact(artifact.downloadUrl)

        if (response.isSuccessful && response.body() != null) {
            response.body()!!.byteStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(destination)
        } else {
            Result.failure(Exception("Download failed"))
        }
    } catch (e: Exception) {
        Timber.e(e, "Artifact download failed")
        Result.failure(e)
    }
}
```

---

### **Step 3.4: Create ArtifactsSection UI**

**Create:** `app/src/main/java/com/secureops/app/ui/components/ArtifactsSection.kt`

```kotlin
package com.secureops.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secureops.app.domain.model.BuildArtifact
import com.secureops.app.domain.model.formatFileSize

@Composable
fun ArtifactsSection(
    artifacts: List<BuildArtifact>,
    onDownloadArtifact: (BuildArtifact) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Artifacts",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${artifacts.size} items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (artifacts.isEmpty()) {
                Text(
                    text = "No artifacts available for this build",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                artifacts.forEach { artifact ->
                    ArtifactItem(
                        artifact = artifact,
                        onDownload = { onDownloadArtifact(artifact) }
                    )
                    if (artifact != artifacts.last()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtifactItem(
    artifact: BuildArtifact,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getArtifactIcon(artifact.name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = artifact.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = artifact.size.formatFileSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onDownload) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download artifact"
            )
        }
    }
}

private fun getArtifactIcon(filename: String) = when {
    filename.endsWith(".zip") || filename.endsWith(".tar.gz") -> Icons.Default.FolderZip
    filename.endsWith(".apk") || filename.endsWith(".aar") -> Icons.Default.Android
    filename.endsWith(".jar") -> Icons.Default.Javascript
    filename.endsWith(".log") -> Icons.Default.Description
    else -> Icons.Default.InsertDriveFile
}
```

---

## ✅ PHASE 4: Slack & Email Notifications (10-14 hours)

### **Step 4.1: Create SlackNotifier**

**Create:** `app/src/main/java/com/secureops/app/data/notification/SlackNotifier.kt`

```kotlin
package com.secureops.app.data.notification

import com.secureops.app.domain.model.Pipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class SlackNotifier(
    private val client: OkHttpClient
) {
    /**
     * Send Slack notification via webhook
     */
    suspend fun sendNotification(
        webhookUrl: String,
        pipeline: Pipeline,
        message: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = buildSlackMessage(pipeline, message)

            val requestBody = json.toString().toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Timber.d("Slack notification sent successfully")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Slack API error: ${response.code}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Slack notification failed")
            Result.failure(e)
        }
    }

    private fun buildSlackMessage(pipeline: Pipeline, message: String): JSONObject {
        return JSONObject().apply {
            put("text", message)
            put("blocks", JSONArray().apply {
                // Header
                put(JSONObject().apply {
                    put("type", "header")
                    put("text", JSONObject().apply {
                        put("type", "plain_text")
                        put("text", "🚨 Build ${pipeline.status}")
                    })
                })

                // Details
                put(JSONObject().apply {
                    put("type", "section")
                    put("fields", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Repository:*\n${pipeline.repositoryName}")
                        })
                        put(JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Build:*\n#${pipeline.buildNumber}")
                        })
                        put(JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Branch:*\n${pipeline.branch}")
                        })
                        put(JSONObject().apply {
                            put("type", "mrkdwn")
                            put("text", "*Status:*\n${pipeline.status}")
                        })
                    })
                })

                // Actions
                put(JSONObject().apply {
                    put("type", "actions")
                    put("elements", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "button")
                            put("text", JSONObject().apply {
                                put("type", "plain_text")
                                put("text", "View Build")
                            })
                            put("url", pipeline.webUrl)
                        })
                    })
                })
            })
        }
    }
}
```

---

### **Step 4.2: Create EmailNotifier**

**Create:** `app/src/main/java/com/secureops/app/data/notification/EmailNotifier.kt`

```kotlin
package com.secureops.app.data.notification

import com.secureops.app.domain.model.Pipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import timber.log.Timber

data class SmtpConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val fromEmail: String,
    val fromName: String = "SecureOps"
)

class EmailNotifier(
    private val config: SmtpConfig
) {
    /**
     * Send email notification
     */
    suspend fun sendEmail(
        to: List<String>,
        subject: String,
        pipeline: Pipeline
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val email = HtmlEmail()

            // SMTP configuration
            email.hostName = config.host
            email.setSmtpPort(config.port)
            email.setAuthenticator(DefaultAuthenticator(config.username, config.password))
            email.isSSLOnConnect = true
            email.setFrom(config.fromEmail, config.fromName)

            // Recipients
            to.forEach { email.addTo(it) }

            // Content
            email.subject = subject
            email.setHtmlMsg(buildEmailHtml(pipeline))
            email.setTextMsg(buildEmailText(pipeline))

            // Send
            email.send()

            Timber.d("Email sent successfully to ${to.joinToString()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Email sending failed")
            Result.failure(e)
        }
    }

    private fun buildEmailHtml(pipeline: Pipeline): String {
        val statusColor = when (pipeline.status) {
            BuildStatus.SUCCESS -> "#4CAF50"
            BuildStatus.FAILURE -> "#F44336"
            else -> "#FF9800"
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: $statusColor; color: white; padding: 20px; border-radius: 8px; }
                    .content { padding: 20px; background: #f5f5f5; margin-top: 10px; border-radius: 8px; }
                    .detail { margin: 10px 0; }
                    .label { font-weight: bold; }
                    .button { display: inline-block; padding: 10px 20px; background: #2196F3; color: white; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>Build ${pipeline.status}</h2>
                    </div>
                    <div class="content">
                        <div class="detail">
                            <span class="label">Repository:</span> ${pipeline.repositoryName}
                        </div>
                        <div class="detail">
                            <span class="label">Build:</span> #${pipeline.buildNumber}
                        </div>
                        <div class="detail">
                            <span class="label">Branch:</span> ${pipeline.branch}
                        </div>
                        <div class="detail">
                            <span class="label">Status:</span> ${pipeline.status}
                        </div>
                        <div class="detail">
                            <span class="label">Triggered by:</span> ${pipeline.triggeredBy}
                        </div>
                        <a href="${pipeline.webUrl}" class="button">View Build</a>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildEmailText(pipeline: Pipeline): String {
        return """
            Build ${pipeline.status}
            
            Repository: ${pipeline.repositoryName}
            Build: #${pipeline.buildNumber}
            Branch: ${pipeline.branch}
            Status: ${pipeline.status}
            Triggered by: ${pipeline.triggeredBy}
            
            View build: ${pipeline.webUrl}
        """.trimIndent()
    }
}
```

---

### **Step 4.3: Update RemediationExecutor**

**Modify:** `app/src/main/java/com/secureops/app/data/executor/RemediationExecutor.kt`

Replace stubs with real implementations:

```kotlin
// Add properties
private val slackNotifier: SlackNotifier by inject()
private val emailNotifier: EmailNotifier by inject()
private val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)

/**
 * Notify via Slack - REAL IMPLEMENTATION
 */
private suspend fun notifySlack(pipeline: Pipeline, parameters: Map<String, String>): ActionResult {
    val webhookUrl = prefs.getString("slack_webhook_url", null)
    if (webhookUrl == null) {
        return ActionResult(false, "Slack webhook URL not configured")
    }

    val message = parameters["message"] ?: "Build notification"

    return slackNotifier.sendNotification(webhookUrl, pipeline, message).fold(
        onSuccess = {
            ActionResult(
                success = true,
                message = "Slack notification sent successfully",
                details = mapOf("channel" to "slack")
            )
        },
        onFailure = { error ->
            ActionResult(
                success = false,
                message = "Slack notification failed: ${error.message}",
                details = mapOf("error" to error.toString())
            )
        }
    )
}

/**
 * Notify via Email - REAL IMPLEMENTATION
 */
private suspend fun notifyEmail(pipeline: Pipeline, parameters: Map<String, String>): ActionResult {
    val recipients = parameters["recipients"]?.split(",")
    if (recipients == null) {
        return ActionResult(false, "Email recipients not specified")
    }

    // Get SMTP config from preferences
    val smtpHost = prefs.getString("smtp_host", null)
    val smtpPort = prefs.getInt("smtp_port", 587)
    val smtpUsername = prefs.getString("smtp_username", null)
    val smtpPassword = prefs.getString("smtp_password", null)
    val fromEmail = prefs.getString("smtp_from_email", null)

    if (smtpHost == null || smtpUsername == null || smtpPassword == null || fromEmail == null) {
        return ActionResult(false, "SMTP not configured")
    }

    val config = SmtpConfig(
        host = smtpHost,
        port = smtpPort,
        username = smtpUsername,
        password = smtpPassword,
        fromEmail = fromEmail
    )

    val notifier = EmailNotifier(config)
    val subject = "Build ${pipeline.status}: ${pipeline.repositoryName} #${pipeline.buildNumber}"

    return notifier.sendEmail(recipients, subject, pipeline).fold(
        onSuccess = {
            ActionResult(
                success = true,
                message = "Email sent to ${recipients.joinToString()}",
                details = mapOf("channel" to "email")
            )
        },
        onFailure = { error ->
            ActionResult(
                success = false,
                message = "Email failed: ${error.message}",
                details = mapOf("error" to error.toString())
            )
        }
    )
}
```

---

## 📦 Register New Components in Koin

**Modify:** `app/src/main/java/com/secureops/app/di/RepositoryModule.kt`

```kotlin
// Add these
single { PipelineStreamService() }
single { SlackNotifier(get()) }
// EmailNotifier is created on-demand with config
```

---

## ✅ Summary of All Files

### **Files Created (11):**

1. ✅ `OAuth2Manager.kt` - DONE
2. `OAuthCallbackActivity.kt`
3. `BuildProgressIndicator.kt`
4. `BuildArtifact.kt`
5. `ArtifactsSection.kt`
6. `SlackNotifier.kt`
7. `EmailNotifier.kt`

### **Files Modified (15+):**

1. ✅ `build.gradle.kts` - DONE
2. `AndroidManifest.xml`
3. `AddAccountViewModel.kt`
4. `AddAccountScreen.kt`
5. `BuildDetailsViewModel.kt`
6. `BuildDetailsScreen.kt`
7. `PipelineRepository.kt`
8. `GitHubService.kt` + DTOs
9. `JenkinsService.kt`
10. `GitLabService.kt`
11. `CircleCIService.kt`
12. `AzureDevOpsService.kt`
13. `RemediationExecutor.kt`
14. `RepositoryModule.kt`
15. Settings screens (Slack/Email config)

---

## 🚀 Next Steps

**I can help you implement this in 3 ways:**

1. **Continue now** - I'll implement the next phase (OAuth UI integration)
2. **Guided implementation** - You implement, I guide
3. **Batch approach** - I create all remaining files in next session

**Which would you prefer?** 🎯
