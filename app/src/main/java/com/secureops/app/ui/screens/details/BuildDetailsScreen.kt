package com.secureops.app.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secureops.app.ui.components.StreamingIndicator
import com.secureops.app.ui.components.ArtifactsSection
import com.secureops.app.data.remote.LogEntry
import com.secureops.app.data.remote.LogLevel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildDetailsScreen(
    pipelineId: String,
    onNavigateBack: () -> Unit,
    viewModel: BuildDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pipelineId) {
        viewModel.loadPipeline(pipelineId)
    }

    LaunchedEffect(uiState.actionResult) {
        uiState.actionResult?.let { result ->
            snackbarHostState.showSnackbar(
                message = result.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearActionResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading build",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            uiState.pipeline != null -> {
                val pipeline = uiState.pipeline!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Build Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Build Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Pipeline: ${pipeline.repositoryName}")
                            Text(text = "Build: #${pipeline.buildNumber}")
                            Text(text = "Status: ${pipeline.status.name}")
                            Text(text = "Branch: ${pipeline.branch}")
                            if (pipeline.duration != null) {
                                Text(text = "Duration: ${formatDuration(pipeline.duration)}")
                            }
                            if (pipeline.commitMessage.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Commit: ${pipeline.commitMessage}")
                                if (pipeline.commitAuthor.isNotEmpty()) {
                                    Text(text = "Author: ${pipeline.commitAuthor}")
                                }
                            }
                        }
                    }

                    // Root Cause Analysis
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (pipeline.status) {
                                com.secureops.app.domain.model.BuildStatus.FAILURE ->
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)

                                com.secureops.app.domain.model.BuildStatus.SUCCESS ->
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)

                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (pipeline.status) {
                                        com.secureops.app.domain.model.BuildStatus.FAILURE -> Icons.Default.Error
                                        com.secureops.app.domain.model.BuildStatus.SUCCESS -> Icons.Default.CheckCircle
                                        else -> Icons.Default.Warning
                                    },
                                    contentDescription = null,
                                    tint = when (pipeline.status) {
                                        com.secureops.app.domain.model.BuildStatus.FAILURE ->
                                            MaterialTheme.colorScheme.error

                                        com.secureops.app.domain.model.BuildStatus.SUCCESS ->
                                            MaterialTheme.colorScheme.primary

                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Root Cause Analysis",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Analyze logs if we have them and build failed
                            if (pipeline.status == com.secureops.app.domain.model.BuildStatus.FAILURE) {
                                val rootCauseInfo = analyzeFailureLogs(uiState.logs)

                                if (rootCauseInfo.isNotEmpty()) {
                                    // Show extracted error information
                                    rootCauseInfo.forEach { (title, content) ->
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    // Suggested actions
                                    val suggestions = generateSuggestions(uiState.logs)
                                    if (suggestions.isNotEmpty()) {
                                        Text(
                                            text = "Suggested Actions:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        suggestions.forEach { suggestion ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "• ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = suggestion,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                } else if (uiState.logs != null) {
                                    // We have logs but couldn't extract specific errors
                                    Text(
                                        text = "Build failed but no specific error patterns were detected. Review the full logs below for details.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    // Logs are loading or unavailable
                                    Text(
                                        text = if (uiState.isLoadingLogs)
                                            "Analyzing logs..."
                                        else
                                            "Load logs to see detailed error analysis",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (pipeline.status == com.secureops.app.domain.model.BuildStatus.SUCCESS) {
                                Text(
                                    text = "Build completed successfully! All checks passed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "Build is ${pipeline.status.name.lowercase()}. Status will update when complete.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Show ML prediction if available
                            pipeline.failurePrediction?.let { prediction ->
                                if (prediction.riskPercentage > 50f) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "AI Risk Assessment",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Risk Level: ${prediction.riskPercentage.toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (prediction.causalFactors.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        prediction.causalFactors.take(3).forEach { factor ->
                                            Text(
                                                text = "• $factor",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Logs Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
                                    text = "Build Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState.isStreaming) {
                                        StreamingIndicator()
                                    }

                                    // Stream toggle button for running builds
                                    if (pipeline.status == com.secureops.app.domain.model.BuildStatus.RUNNING) {
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
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = if (uiState.isStreaming)
                                                    MaterialTheme.colorScheme.onErrorContainer
                                                else
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text(
                                                text = if (uiState.isStreaming) "Stop Live" else "Stream Live",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    } else {
                                        if (uiState.isLoadingLogs) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else if (uiState.logsError != null) {
                                            TextButton(onClick = { viewModel.fetchLogs() }) {
                                                Text("Retry")
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 400.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                // Show streaming logs if active
                                if (uiState.isStreaming && uiState.streamingLogs.isNotEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    ) {
                                        items(uiState.streamingLogs) { logEntry ->
                                            LogEntryItem(logEntry)
                                        }
                                    }
                                } else if (uiState.logs != null) {
                                    // Display cached logs
                                    val scrollState = rememberScrollState()
                                    Text(
                                        text = uiState.logs ?: "No logs available",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .verticalScroll(scrollState)
                                    )
                                } else if (uiState.isLoadingLogs) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Loading logs...",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Logs not loaded yet. Pull to refresh.",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.rerunBuild() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isExecutingAction
                        ) {
                            if (uiState.isExecutingAction) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Rerun")
                        }
                        OutlinedButton(
                            onClick = { viewModel.cancelBuild() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isExecutingAction &&
                                    pipeline.status == com.secureops.app.domain.model.BuildStatus.RUNNING
                        ) {
                            Text("Cancel")
                        }
                    }

                    // Artifacts Section
                    if (uiState.artifacts.isNotEmpty() || uiState.isLoadingArtifacts) {
                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.isLoadingArtifacts) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Loading artifacts...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        } else {
                            ArtifactsSection(
                                artifacts = uiState.artifacts,
                                onDownloadArtifact = { artifact ->
                                    viewModel.downloadArtifact(artifact)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

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

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun analyzeFailureLogs(logs: String?): List<Pair<String, String>> {
    if (logs.isNullOrBlank()) return emptyList()

    val results = mutableListOf<Pair<String, String>>()

    // Extract ERROR lines
    val errorPattern = Regex("ERROR:?\\s+(.+?)(?=\\n|$)", RegexOption.IGNORE_CASE)
    errorPattern.findAll(logs).firstOrNull()?.let { match ->
        results.add("❌ Error Found" to match.groupValues[1].trim())
    }

    // Extract exit codes
    val exitCodePattern = Regex("exit code\\s+(\\d+)", RegexOption.IGNORE_CASE)
    exitCodePattern.find(logs)?.let { match ->
        val exitCode = match.groupValues[1]
        results.add("Exit Code" to "Process returned exit code $exitCode (non-zero exit indicates failure)")
    }

    // Extract FAILURE status
    if (logs.contains("FAILURE", ignoreCase = true)) {
        val failureContext = logs.lines()
            .find { it.contains("FAILURE", ignoreCase = true) }
            ?.trim()
        failureContext?.let {
            if (it.length < 200) {
                results.add("Status" to it)
            }
        }
    }

    // Extract stage/step information
    val stagePattern = Regex("\\[Pipeline\\]\\s+\\{\\s*\\(([^)]+)\\)", RegexOption.MULTILINE)
    val failedStages = mutableListOf<String>()
    stagePattern.findAll(logs).forEach { match ->
        val stageName = match.groupValues[1]
        // Check if this stage has errors nearby
        val stageIndex = match.range.first
        val surroundingText = logs.substring(
            maxOf(0, stageIndex - 500),
            minOf(logs.length, stageIndex + 500)
        )
        if (surroundingText.contains("ERROR", ignoreCase = true) ||
            surroundingText.contains("FAILURE", ignoreCase = true) ||
            surroundingText.contains("skipped due to earlier failure", ignoreCase = true)
        ) {
            failedStages.add(stageName)
        }
    }
    if (failedStages.isNotEmpty()) {
        results.add("Failed Stage" to failedStages.first())
    }

    // Extract script errors
    val scriptErrorPattern = Regex("script returned exit code", RegexOption.IGNORE_CASE)
    if (scriptErrorPattern.containsMatchIn(logs)) {
        results.add("Cause" to "A script or command in the pipeline failed to execute successfully")
    }

    return results
}

private fun generateSuggestions(logs: String?): List<String> {
    if (logs.isNullOrBlank()) return emptyList()

    val suggestions = mutableListOf<String>()
    val logsLower = logs.lowercase()

    when {
        logsLower.contains("exit code 1") || logsLower.contains("script returned") -> {
            suggestions.add("Check the script or command that failed")
            suggestions.add("Review the console output above the error")
            suggestions.add("Verify all required tools and dependencies are installed")
        }

        logsLower.contains("skipped due to earlier failure") -> {
            suggestions.add("Fix the earlier failure first - subsequent stages were skipped")
            suggestions.add("Review the first failed stage in the logs")
        }

        logsLower.contains("timeout") -> {
            suggestions.add("Increase timeout settings in pipeline configuration")
            suggestions.add("Optimize the slow operation")
        }

        logsLower.contains("permission") || logsLower.contains("denied") -> {
            suggestions.add("Check file and directory permissions")
            suggestions.add("Verify the Jenkins user has necessary access")
        }

        logsLower.contains("not found") || logsLower.contains("no such file") -> {
            suggestions.add("Verify the file or command exists")
            suggestions.add("Check PATH environment variable")
        }

        logsLower.contains("test") && logsLower.contains("fail") -> {
            suggestions.add("Run tests locally to reproduce the failure")
            suggestions.add("Check test data and dependencies")
        }

        logsLower.contains("compile") || logsLower.contains("syntax") -> {
            suggestions.add("Fix compilation or syntax errors in the code")
            suggestions.add("Check for missing imports or dependencies")
        }

        logsLower.contains("connection") || logsLower.contains("network") -> {
            suggestions.add("Check network connectivity")
            suggestions.add("Verify external service availability")
            suggestions.add("Try rerunning the build")
        }

        logsLower.contains("memory") || logsLower.contains("oom") -> {
            suggestions.add("Increase memory allocation for the job")
            suggestions.add("Optimize memory usage in the application")
        }

        else -> {
            suggestions.add("Review the complete logs for more details")
            suggestions.add("Check recent code changes")
            suggestions.add("Compare with the last successful build")
        }
    }

    // Always add rerun suggestion
    if (suggestions.isNotEmpty()) {
        suggestions.add("Try rerunning the build if the issue might be transient")
    }

    return suggestions.take(4) // Limit to 4 suggestions
}
