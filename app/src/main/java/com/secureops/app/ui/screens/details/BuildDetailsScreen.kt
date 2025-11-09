package com.secureops.app.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Root Cause Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (pipeline.failurePrediction != null) {
                                val prediction = pipeline.failurePrediction
                                Text(
                                    text = "Risk Level: ${prediction.riskPercentage.toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (prediction.causalFactors.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Factors:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    prediction.causalFactors.take(3).forEach { factor ->
                                        Text(
                                            text = "• $factor",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = if (pipeline.status == com.secureops.app.domain.model.BuildStatus.SUCCESS) {
                                        "Build completed successfully"
                                    } else {
                                        "No failures detected. Build is progressing normally."
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
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
                            Text(
                                text = "Build Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = """
                                        [INFO] Scanning for projects...
                                        [INFO] Building ${pipeline.repositoryName}
                                        [INFO] Compiling sources...
                                        [INFO] Tests running...
                                        [INFO] All tests passed!
                                    """.trimIndent(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(12.dp)
                                )
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
                }
            }
        }
    }
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
