package com.secureops.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.secureops.app.data.offline.CacheManager
import com.secureops.app.data.offline.DemoDataGenerator
import com.secureops.app.data.offline.OfflineSimulator
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.components.GradientBackground
import com.secureops.app.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Offline & Demo Settings Screen
 * 
 * Provides controls for offline mode simulation and demo data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineSettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val offlineSimulator: OfflineSimulator = koinInject()
    val cacheManager: CacheManager = koinInject()
    val demoDataGenerator: DemoDataGenerator = koinInject()
    
    val offlineModeEnabled by offlineSimulator.isOfflineModeEnabled.collectAsState()
    val offlineStats by offlineSimulator.offlineStats.collectAsState()
    val cacheStats by cacheManager.cacheStats.collectAsState()
    
    val scope = rememberCoroutineScope()
    var showGenerateDialog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    
    GradientBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Offline & Demo Mode",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Offline Mode Section
                item {
                    Text(
                        "Offline Mode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
                
                item {
                    SettingsSwitchItem(
                        icon = Icons.Default.CloudOff,
                        title = "Enable Offline Mode",
                        subtitle = "Block all network requests for testing",
                        checked = offlineModeEnabled,
                        onCheckedChange = {
                            if (it) offlineSimulator.enableOfflineMode()
                            else offlineSimulator.disableOfflineMode()
                        },
                        iconColor = if (offlineModeEnabled) ErrorRed else PrimaryPurple
                    )
                }
                
                // Offline Statistics
                if (offlineModeEnabled) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Offline Statistics",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                StatRow("Blocked Requests", offlineStats.blockedRequests.toString())
                                StatRow("Duration", offlineSimulator.getOfflineDurationFormatted())
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = { offlineSimulator.resetBlockedCount() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WarningAmber
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reset Stats")
                                }
                            }
                        }
                    }
                }
                
                // Cache Statistics Section
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cache Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
                
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Cache Performance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            StatRow("Hit Rate", String.format("%.1f%%", cacheStats.hitRate * 100))
                            StatRow("Hits", cacheStats.hits.toString())
                            StatRow("Misses", cacheStats.misses.toString())
                            StatRow("Cache Size", String.format("%.2f MB", cacheStats.cacheSizeMB))
                            StatRow("Last Sync", cacheManager.getCacheAgeFormatted())
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            cacheManager.calculateCacheSize()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Calculate, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Recalculate")
                                }
                                
                                Button(
                                    onClick = {
                                        scope.launch {
                                            cacheManager.clearCache()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WarningAmber
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear Cache")
                                }
                            }
                        }
                    }
                }
                
                // Demo Mode Section
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Demo Mode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
                
                item {
                    SettingsSwitchItem(
                        icon = Icons.Default.Science,
                        title = "Enable Demo Mode",
                        subtitle = "Use generated demo data",
                        checked = offlineSimulator.isDemoModeEnabled(),
                        onCheckedChange = {
                            if (it) offlineSimulator.enableDemoMode()
                            else offlineSimulator.disableDemoMode()
                        },
                        iconColor = AccentPink
                    )
                }
                
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Demo Data Generation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                "Generate realistic pipelines and threats for testing and demonstrations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Button(
                                onClick = { showGenerateDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isGenerating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen
                                )
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = androidx.compose.ui.graphics.Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(if (isGenerating) "Generating..." else "Generate Demo Data")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        demoDataGenerator.clearDemoData()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isGenerating
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Clear Demo Data")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Generate Demo Data Dialog
    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate Demo Data") },
            text = {
                Text("This will generate 20 demo pipelines with realistic data. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGenerateDialog = false
                        isGenerating = true
                        scope.launch {
                            try {
                                demoDataGenerator.generateDemoData(20)
                            } finally {
                                isGenerating = false
                            }
                        }
                    }
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
