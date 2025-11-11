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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.components.GradientBackground
import com.secureops.app.ui.components.StatusBadge
import com.secureops.app.ui.components.NeonChip
import com.secureops.app.ui.components.GradientDivider
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

    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "SecureOps Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.refreshPipelines() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = PrimaryPurple
                            )
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
                            modifier = Modifier.align(Alignment.Center),
                            color = PrimaryPurple
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
                            .align(Alignment.TopCenter),
                        color = PrimaryPurple
                    )
                }

                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter),
                        containerColor = ErrorRed.copy(alpha = 0.9f),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss", color = Color.White)
                            }
                        }
                    ) {
                        Text(error, color = Color.White)
                    }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group by repository
        val groupedPipelines = pipelines.groupBy { it.repositoryName }

        groupedPipelines.forEach { (repoName, repoPipelines) ->
            item {
                Text(
                    text = repoName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
    GlassCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(20.dp)
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
                // Status Badge
                StatusBadge(
                    status = when (pipeline.status) {
                        BuildStatus.SUCCESS -> "Success"
                        BuildStatus.FAILURE -> "Failed"
                        BuildStatus.RUNNING -> "Running"
                        BuildStatus.QUEUED -> "Queued"
                        BuildStatus.PENDING -> "Pending"
                        BuildStatus.CANCELED -> "Canceled"
                        else -> "Unknown"
                    },
                    color = when (pipeline.status) {
                        BuildStatus.SUCCESS -> SuccessGreen
                        BuildStatus.FAILURE -> ErrorRed
                        BuildStatus.RUNNING -> InfoBlue
                        BuildStatus.QUEUED, BuildStatus.PENDING -> WarningAmber
                        else -> Color.Gray
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "#${pipeline.buildNumber}",
                    style = MaterialTheme.typography.headlineSmall,
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
                            prediction.riskPercentage > 60f -> WarningAmber
                            else -> InfoBlue
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Branch Chip
            NeonChip(
                text = pipeline.branch,
                icon = "🌿",
                color = AccentGreen
            )

            // Provider Badge
            NeonChip(
                text = when (pipeline.provider) {
                    com.secureops.app.domain.model.CIProvider.JENKINS -> "Jenkins"
                    com.secureops.app.domain.model.CIProvider.GITHUB_ACTIONS -> "GitHub"
                    com.secureops.app.domain.model.CIProvider.GITLAB_CI -> "GitLab"
                    com.secureops.app.domain.model.CIProvider.CIRCLE_CI -> "CircleCI"
                    com.secureops.app.domain.model.CIProvider.AZURE_DEVOPS -> "Azure"
                },
                color = AccentCyan
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Commit Message
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

        GradientDivider()

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
                        tint = PrimaryPurple,
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
                        tint = AccentPink,
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

        // Timestamp
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

@Composable
fun StatusIndicator(status: BuildStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        BuildStatus.SUCCESS -> SuccessGreen
        BuildStatus.FAILURE -> ErrorRed
        BuildStatus.RUNNING -> InfoBlue
        BuildStatus.QUEUED, BuildStatus.PENDING -> WarningAmber
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
    GlassCard(
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PrimaryPurple.copy(alpha = 0.3f),
                                AccentPink.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = PrimaryPurple
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No pipelines yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add an account to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
