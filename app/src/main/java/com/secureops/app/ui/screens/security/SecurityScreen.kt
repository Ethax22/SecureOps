package com.secureops.app.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.secureops.app.data.local.entity.ThreatEntity
import com.secureops.app.ml.security.ThreatSeverity
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.components.GradientBackground
import com.secureops.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Security Dashboard Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Security Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
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

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Statistics Section
                            item {
                                Text(
                                    "Overview",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            item {
                                StatisticsSection(
                                    statistics = uiState.statistics,
                                    onSeverityClick = { severity ->
                                        viewModel.setSeverityFilter(severity)
                                    }
                                )
                            }

                            // Filter Section
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Threats",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            item {
                                FilterChips(
                                    selectedFilter = uiState.selectedFilter,
                                    onFilterSelected = { filter ->
                                        viewModel.setFilter(filter)
                                    }
                                )
                            }

                            // Threats List
                            if (uiState.filteredThreats.isEmpty()) {
                                item {
                                    EmptyThreatsState()
                                }
                            } else {
                                items(uiState.filteredThreats) { threat ->
                                    ThreatCard(
                                        threat = threat,
                                        onResolve = { viewModel.resolveThread(threat.id) },
                                        onDelete = { viewModel.deleteThreat(threat) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Snackbar
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
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
}

/**
 * Statistics section with stat cards
 */
@Composable
fun StatisticsSection(
    statistics: ThreatStatistics,
    onSeverityClick: (ThreatSeverity?) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThreatStatCard(
                title = "Total",
                count = statistics.totalThreats,
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = { onSeverityClick(null) }
            )
            ThreatStatCard(
                title = "Unresolved",
                count = statistics.unresolvedThreats,
                icon = Icons.Default.Error,
                color = WarningAmber,
                modifier = Modifier.weight(1f),
                onClick = { onSeverityClick(null) }
            )
        }

        // Severity Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThreatStatCard(
                title = "Critical",
                count = statistics.criticalCount,
                icon = Icons.Default.ErrorOutline,
                color = ErrorRed,
                modifier = Modifier.weight(1f),
                onClick = { onSeverityClick(ThreatSeverity.CRITICAL) }
            )
            ThreatStatCard(
                title = "High",
                count = statistics.highCount,
                icon = Icons.Default.Warning,
                color = WarningAmber,
                modifier = Modifier.weight(1f),
                onClick = { onSeverityClick(ThreatSeverity.HIGH) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThreatStatCard(
                title = "Medium",
                count = statistics.mediumCount,
                icon = Icons.Default.Info,
                color = Color(0xFFFFA726),
                modifier = Modifier.weight(1f),
                onClick = { onSeverityClick(ThreatSeverity.MEDIUM) }
            )
            ThreatStatCard(
                title = "Low",
                count = statistics.lowCount,
                icon = Icons.Default.CheckCircle,
                color = SuccessGreen,
                modifier = Modifier.weight(1f),
                onClick = { onSeverityClick(ThreatSeverity.LOW) }
            )
        }

        // Category Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThreatStatCard(
                title = "Secrets",
                count = statistics.secretsDetected,
                icon = Icons.Default.Key,
                color = PrimaryPurple,
                modifier = Modifier.weight(1f)
            )
            ThreatStatCard(
                title = "Dependencies",
                count = statistics.dependencyIssues,
                icon = Icons.Default.Build,
                color = InfoBlue,
                modifier = Modifier.weight(1f)
            )
            ThreatStatCard(
                title = "Anomalies",
                count = statistics.anomaliesDetected,
                icon = Icons.Default.ShowChart,
                color = AccentPink,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Filter chips row
 */
@Composable
fun FilterChips(
    selectedFilter: ThreatFilter,
    onFilterSelected: (ThreatFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ThreatFilter.values()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = when (filter) {
                            ThreatFilter.ALL -> "All"
                            ThreatFilter.UNRESOLVED -> "Unresolved"
                            ThreatFilter.SECRETS -> "Secrets"
                            ThreatFilter.DEPENDENCIES -> "Dependencies"
                            ThreatFilter.ANOMALIES -> "Anomalies"
                            ThreatFilter.CRITICAL -> "Critical"
                        }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryPurple,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

/**
 * Empty state when no threats
 */
@Composable
fun EmptyThreatsState() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SuccessGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Threats Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Your pipelines are secure",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
