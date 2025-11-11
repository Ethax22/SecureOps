package com.secureops.app.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.secureops.app.data.analytics.TimeRange
import com.secureops.app.ui.screens.analytics.ExportStatus
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.components.GradientBackground
import com.secureops.app.ui.components.NeonButton
import com.secureops.app.ui.components.GradientDivider
import com.secureops.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    var selectedTimeRange by remember { mutableStateOf(TimeRange.LAST_30_DAYS) }
    var showExportDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportStatus) {
        when (val status = exportStatus) {
            is ExportStatus.Success -> {
                snackbarHostState.showSnackbar(
                    message = "${status.fileName} saved to Downloads",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearExportStatus()
            }

            is ExportStatus.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Export failed: ${status.message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearExportStatus()
            }

            else -> {}
        }
    }

    LaunchedEffect(selectedTimeRange) {
        viewModel.loadAnalytics(selectedTimeRange)
    }

    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Analytics",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.loadAnalytics(selectedTimeRange) }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = PrimaryPurple
                            )
                        }
                        IconButton(
                            onClick = { showExportDialog = true },
                            enabled = exportStatus !is ExportStatus.Exporting
                        ) {
                            if (exportStatus is ExportStatus.Exporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryPurple
                                )
                            } else {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Export",
                                    tint = AccentCyan
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                }

                is AnalyticsUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Time Range Selector
                        TimeRangeSelector(
                            selectedRange = selectedTimeRange,
                            onRangeSelected = { selectedTimeRange = it }
                        )

                        // Overview Stats
                        OverviewStats(
                            totalBuilds = state.totalBuilds,
                            successRate = state.successRate,
                            avgDuration = state.avgDuration,
                            failedBuilds = state.failedBuilds
                        )

                        // Failure Trends Chart
                        FailureTrendsChart(
                            title = "Failure Rate Trends",
                            data = state.trendData
                        )

                        // Common Failure Causes
                        FailureCausesChart(
                            title = "Top Failure Causes",
                            causes = state.commonCauses
                        )

                        // Time to Fix Chart
                        TimeToFixChart(
                            title = "Average Time to Fix",
                            data = state.timeToFixStats
                        )

                        // Repository Metrics
                        RepositoryMetricsSection(
                            repositories = state.repositoryMetrics
                        )

                        // High Risk Repositories
                        if (state.highRiskRepos.isNotEmpty()) {
                            HighRiskRepositories(
                                repositories = state.highRiskRepos
                            )
                        }
                    }
                }

                is AnalyticsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentPadding = PaddingValues(32.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    ErrorRed.copy(alpha = 0.3f),
                                                    ErrorRed.copy(alpha = 0.1f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⚠️", style = MaterialTheme.typography.headlineLarge)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Failed to load analytics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ErrorRed,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                NeonButton(
                                    text = "Retry",
                                    onClick = { viewModel.loadAnalytics(selectedTimeRange) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                viewModel.exportAnalytics(context, format)
                showExportDialog = false
            }
        )
    }
}

@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRange.values().forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { onRangeSelected(range) },
                    label = {
                        Text(
                            when (range) {
                                TimeRange.LAST_7_DAYS -> "7 Days"
                                TimeRange.LAST_30_DAYS -> "30 Days"
                                TimeRange.LAST_90_DAYS -> "90 Days"
                                TimeRange.ALL_TIME -> "All Time"
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun OverviewStats(
    totalBuilds: Int,
    successRate: Float,
    avgDuration: Long,
    failedBuilds: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Total Builds",
                value = totalBuilds.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Success Rate",
                value = "${String.format("%.1f", successRate)}%",
                modifier = Modifier.weight(1f),
                color = if (successRate >= 90f) Color(0xFF4CAF50) else Color(0xFFFFA726)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Avg Duration",
                value = formatDuration(avgDuration),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Failed Builds",
                value = failedBuilds.toString(),
                modifier = Modifier.weight(1f),
                color = if (failedBuilds > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FailureTrendsChart(
    title: String,
    data: List<Pair<String, Float>>
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Show build count
                Text(
                    text = "${data.size} builds",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 40.dp)
                ) {
                    if (data.isEmpty()) return@Canvas

                    val chartHeight = size.height
                    val chartWidth = size.width
                    
                    val failureColor = Color(0xFFF44336)
                    val successColor = Color(0xFF4CAF50)
                    val runningColor = Color(0xFFFFA726)

                    // Draw horizontal grid lines for reference
                    for (i in 0..4) {
                        val y = (i.toFloat() / 4f) * chartHeight
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(chartWidth, y),
                            strokeWidth = 1f
                        )
                    }

                    // Calculate bar width and spacing
                    val totalBars = data.size
                    val spacing = chartWidth / (totalBars + 1).toFloat()
                    val barWidth = (spacing * 0.7f).coerceAtMost(40f)

                    // Draw bars for each build
                    data.forEachIndexed { index, (label, value) ->
                        val x = (index + 1) * spacing
                        
                        // Determine color based on value (0=success, 50=running, 100=failure)
                        val barColor = when {
                            value >= 80f -> failureColor  // Failure
                            value >= 40f -> runningColor  // Running/Pending
                            else -> successColor          // Success
                        }
                        
                        // Calculate bar height (failures go up, successes stay at bottom)
                        val barHeight = if (value >= 80f) {
                            chartHeight * 0.85f  // Failures: 85% height
                        } else if (value >= 40f) {
                            chartHeight * 0.5f   // Running: 50% height
                        } else {
                            chartHeight * 0.15f  // Success: 15% height
                        }
                        
                        val barTop = chartHeight - barHeight

                        // Draw the bar with rounded top
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x - barWidth / 2, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )

                        // Draw bar outline for clarity
                        drawRoundRect(
                            color = barColor.copy(alpha = 0.5f),
                            topLeft = Offset(x - barWidth / 2, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )

                        // Draw build label at the bottom
                        // Note: In a real implementation, you'd use actual text drawing
                        // For now, just draw a small indicator dot
                        drawCircle(
                            color = barColor.copy(alpha = 0.5f),
                            radius = 3f,
                            center = Offset(x, chartHeight + 10.dp.toPx())
                        )
                    }

                    // Draw bottom axis line
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.3f),
                        start = Offset(0f, chartHeight),
                        end = Offset(chartWidth, chartHeight),
                        strokeWidth = 2f
                    )
                }
                
                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Success indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = " Success  ",
                        style = MaterialTheme.typography.labelSmall
                    )
                    
                    // Failure indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFFF44336), RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = " Failure",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun FailureCausesChart(
    title: String,
    causes: Map<String, Int>
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (causes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No failures recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Use horizontal bar chart instead of line chart for causes
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val maxValue = causes.values.maxOrNull() ?: 1

                    causes.entries.take(5).forEach { (cause, count) ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cause,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = count.toFloat() / maxValue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = when {
                                    count >= maxValue * 0.7f -> Color(0xFFF44336)
                                    count >= maxValue * 0.4f -> Color(0xFFFFA726)
                                    else -> Color(0xFF4CAF50)
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeToFixChart(
    title: String,
    data: List<Triple<String, Int, Int>>
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (data.isEmpty()) {
                Text(
                    "No time-to-fix data available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                data.take(5).forEach { (repo, hours, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = repo,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${hours}h avg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RepositoryMetricsSection(
    repositories: List<RepositoryMetricItem>
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(0.dp)
        ) {
            Text(
                text = "Repository Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (repositories.isEmpty()) {
                Text(
                    "No repositories found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                repositories.take(10).forEach { repo ->
                    RepositoryMetricItem(repo)
                }
            }
        }
    }
}

@Composable
fun RepositoryMetricItem(item: RepositoryMetricItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${item.totalBuilds} builds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (100f - item.failureRate) / 100f,
            modifier = Modifier.fillMaxWidth(),
            color = when {
                item.failureRate < 5f -> Color(0xFF4CAF50)
                item.failureRate < 15f -> Color(0xFFFFA726)
                else -> Color(0xFFF44336)
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${String.format("%.1f", item.failureRate)}% failure rate",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HighRiskRepositories(
    repositories: List<RiskRepositoryItem>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ErrorRed.copy(alpha = 0.2f),
                        ErrorRed.copy(alpha = 0.1f)
                    )
                )
            )
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "High Risk Repositories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }

                repositories.forEach { repo ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = repo.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format("%.1f", repo.failureRate)}% failed",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }
                        Text(
                            text = repo.recommendation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (repo != repositories.last()) {
                        GradientDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (com.secureops.app.data.analytics.ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Analytics") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose export format:")
                com.secureops.app.data.analytics.ExportFormat.values().forEach { format ->
                    TextButton(
                        onClick = { onExport(format) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(format.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
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

// Data classes for UI
data class RepositoryMetricItem(
    val name: String,
    val totalBuilds: Int,
    val failureRate: Float
)

data class RiskRepositoryItem(
    val name: String,
    val failureRate: Float,
    val recommendation: String
)
