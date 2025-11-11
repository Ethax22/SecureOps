package com.secureops.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import com.secureops.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNavigateToBuildDetails: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureOps Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.refreshPipelines() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.pipelines.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    PipelineList(
                        pipelines = uiState.pipelines,
                        onPipelineClick = onNavigateToBuildDetails,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun PipelineList(
    pipelines: List<Pipeline>,
    onPipelineClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Group by repository
        val groupedPipelines = pipelines.groupBy { it.repositoryName }

        groupedPipelines.forEach { (repoName, repoPipelines) ->
            item {
                Text(
                    text = repoName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(repoPipelines) { pipeline ->
                PipelineCard(
                    pipeline = pipeline,
                    onClick = { onPipelineClick(pipeline.id) }
                )
            }
        }
    }
}

@Composable
fun PipelineCard(
    pipeline: Pipeline,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when (pipeline.status) {
                BuildStatus.FAILURE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                BuildStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                BuildStatus.RUNNING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Status Badge + Build Number + Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Enhanced Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (pipeline.status) {
                            BuildStatus.SUCCESS -> SuccessGreen.copy(alpha = 0.15f)
                            BuildStatus.FAILURE -> ErrorRed.copy(alpha = 0.15f)
                            BuildStatus.RUNNING -> InfoBlue.copy(alpha = 0.15f)
                            BuildStatus.QUEUED, BuildStatus.PENDING -> WarningYellow.copy(alpha = 0.15f)
                            BuildStatus.CANCELED -> Color.Gray.copy(alpha = 0.15f)
                            else -> Color.Gray.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusIndicator(
                                status = pipeline.status,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (pipeline.status) {
                                    BuildStatus.SUCCESS -> "Success"
                                    BuildStatus.FAILURE -> "Failed"
                                    BuildStatus.RUNNING -> "Running"
                                    BuildStatus.QUEUED -> "Queued"
                                    BuildStatus.PENDING -> "Pending"
                                    BuildStatus.CANCELED -> "Canceled"
                                    else -> "Unknown"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = when (pipeline.status) {
                                    BuildStatus.SUCCESS -> SuccessGreen
                                    BuildStatus.FAILURE -> ErrorRed
                                    BuildStatus.RUNNING -> InfoBlue
                                    BuildStatus.QUEUED, BuildStatus.PENDING -> WarningYellow
                                    else -> Color.Gray
                                }
                            )
                        }
                    }

                    Text(
                        text = "#${pipeline.buildNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Risk Prediction Badge
                pipeline.failurePrediction?.let { prediction ->
                    if (prediction.riskPercentage > 50f) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                prediction.riskPercentage > 80f -> ErrorRed
                                prediction.riskPercentage > 60f -> WarningYellow
                                else -> InfoBlue
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${prediction.riskPercentage.toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Branch and Provider Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Branch Icon + Name
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌿",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pipeline.branch,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Provider Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = when (pipeline.provider) {
                            com.secureops.app.domain.model.CIProvider.JENKINS -> "Jenkins"
                            com.secureops.app.domain.model.CIProvider.GITHUB_ACTIONS -> "GitHub"
                            com.secureops.app.domain.model.CIProvider.GITLAB_CI -> "GitLab"
                            com.secureops.app.domain.model.CIProvider.CIRCLE_CI -> "CircleCI"
                            com.secureops.app.domain.model.CIProvider.AZURE_DEVOPS -> "Azure"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Commit Message (with better styling)
            if (pipeline.commitMessage.isNotEmpty()) {
                Text(
                    text = pipeline.commitMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Divider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Author, Timestamp, Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Author
                if (pipeline.commitAuthor.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Author",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pipeline.commitAuthor,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Duration
                pipeline.duration?.let { duration ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Duration",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Timestamp (if available)
            pipeline.startedAt?.let { timestamp ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTimestamp(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(status: BuildStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        BuildStatus.SUCCESS -> SuccessGreen
        BuildStatus.FAILURE -> ErrorRed
        BuildStatus.RUNNING -> InfoBlue
        BuildStatus.QUEUED, BuildStatus.PENDING -> WarningYellow
        BuildStatus.CANCELED -> Color.Gray
        else -> Color.Gray
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No pipelines yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add an account to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
