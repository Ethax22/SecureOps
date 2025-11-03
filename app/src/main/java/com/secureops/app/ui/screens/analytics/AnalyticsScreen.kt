package com.secureops.app.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secureops.app.data.analytics.TimeRange
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTimeRange by remember { mutableStateOf(TimeRange.LAST_30_DAYS) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTimeRange) {
        viewModel.loadAnalytics(selectedTimeRange)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                actions = {
                    IconButton(onClick = { viewModel.loadAnalytics(selectedTimeRange) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AnalyticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Failed to load analytics",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAnalytics(selectedTimeRange) }) {
                            Text("Retry")
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
                viewModel.exportAnalytics(format)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val maxValue = data.maxOf { it.second }
                    val minValue = data.minOf { it.second }

                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )

                    data.forEachIndexed { index, (label, value) ->
                        val x = (index * size.width) / (data.size - 1)
                        val y = (1 - (value - minValue) / (maxValue - minValue)) * size.height

                        drawCircle(
                            color = Color(0xFF4CAF50),
                            radius = 5f,
                            center = Offset(x, y)
                        )

                        drawLine(
                            color = Color(0xFF4CAF50),
                            start = Offset(x, y),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                    }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val maxValue = causes.values.maxOrNull() ?: 1
                    val minValue = causes.values.minOrNull() ?: 0
                    val entries = causes.entries.toList()

                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )

                    entries.forEachIndexed { index, entry ->
                        val value = entry.value
                        val x = if (entries.size > 1) {
                            (index.toFloat() * size.width) / (entries.size - 1)
                        } else {
                            size.width / 2
                        }
                        val normalizedValue = if (maxValue > minValue) {
                            (value - minValue).toFloat() / (maxValue - minValue)
                        } else {
                            0.5f
                        }
                        val y = (1 - normalizedValue) * size.height

                        drawCircle(
                            color = Color(0xFF4CAF50),
                            radius = 5f,
                            center = Offset(x, y)
                        )

                        drawLine(
                            color = Color(0xFF4CAF50),
                            start = Offset(x, y),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
            }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚠️ High Risk Repositories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

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
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${String.format("%.1f", repo.failureRate)}% failed",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = repo.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                if (repo != repositories.last()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
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
