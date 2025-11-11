package com.secureops.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secureops.app.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NotificationSettingsViewModel = viewModel()
) {
    val preferences by viewModel.preferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sound, Vibration, LED Section
            item {
                SectionHeader("General Settings")
            }

            item {
                NotificationToggle(
                    icon = Icons.Default.VolumeUp,
                    title = "Sound",
                    description = "Play sound for notifications",
                    checked = preferences.soundEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences(preferences.copy(soundEnabled = it))
                    }
                )
            }

            item {
                NotificationToggle(
                    icon = Icons.Default.Vibration,
                    title = "Vibration",
                    description = "Vibrate on notifications",
                    checked = preferences.vibrationEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences(preferences.copy(vibrationEnabled = it))
                    }
                )
            }

            item {
                NotificationToggle(
                    icon = Icons.Default.LightMode,
                    title = "LED Indicator",
                    description = "Show LED light for notifications",
                    checked = preferences.ledEnabled,
                    onCheckedChange = {
                        viewModel.updatePreferences(preferences.copy(ledEnabled = it))
                    }
                )
            }

            // Channel Selection
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SectionHeader("Notification Types")
            }

            item {
                NotificationChannelSelector(
                    channels = preferences.enabledChannels,
                    onChannelsChange = {
                        viewModel.updatePreferences(preferences.copy(enabledChannels = it))
                    }
                )
            }

            // Risk Threshold
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SectionHeader("Risk Alerts")
            }

            item {
                RiskThresholdSelector(
                    threshold = preferences.riskThreshold,
                    onThresholdChange = {
                        viewModel.updatePreferences(preferences.copy(riskThreshold = it))
                    }
                )
            }

            item {
                NotificationToggle(
                    icon = Icons.Default.Warning,
                    title = "Critical Only",
                    description = "Only notify for critical failures and high risks",
                    checked = preferences.alertOnCriticalOnly,
                    onCheckedChange = {
                        viewModel.updatePreferences(preferences.copy(alertOnCriticalOnly = it))
                    }
                )
            }

            // Quiet Hours
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                SectionHeader("Quiet Hours")
            }

            item {
                QuietHoursSettings(
                    quietHours = preferences.quietHours ?: QuietHours(),
                    onQuietHoursChange = {
                        viewModel.updatePreferences(preferences.copy(quietHours = it))
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun NotificationToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun NotificationChannelSelector(
    channels: Set<NotificationChannelType>,
    onChannelsChange: (Set<NotificationChannelType>) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select notification types to receive",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            NotificationChannelType.values().forEach { channel ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = channel in channels,
                        onCheckedChange = { isChecked ->
                            val newChannels = if (isChecked) {
                                channels + channel
                            } else {
                                channels - channel
                            }
                            onChannelsChange(newChannels)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = channel.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = getChannelDescription(channel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskThresholdSelector(
    threshold: Int,
    onThresholdChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Risk Alert Threshold",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$threshold%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = threshold.toFloat(),
                onValueChange = { onThresholdChange(it.toInt()) },
                valueRange = 50f..100f,
                steps = 9
            )

            Text(
                text = "Notify when failure risk exceeds $threshold%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuietHoursSettings(
    quietHours: QuietHours,
    onQuietHoursChange: (QuietHours) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var editingStart by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Quiet Hours",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = quietHours.enabled,
                    onCheckedChange = {
                        onQuietHoursChange(quietHours.copy(enabled = it))
                    }
                )
            }

            if (quietHours.enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Start Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            editingStart = true
                            showTimePicker = true
                        }) {
                            Text(
                                text = String.format(
                                    "%02d:%02d",
                                    quietHours.startHour,
                                    quietHours.startMinute
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "End Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            editingStart = false
                            showTimePicker = true
                        }) {
                            Text(
                                text = String.format(
                                    "%02d:%02d",
                                    quietHours.endHour,
                                    quietHours.endMinute
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                Text(
                    text = "Days of Week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "Mon",
                        "Tue",
                        "Wed",
                        "Thu",
                        "Fri",
                        "Sat",
                        "Sun"
                    ).forEachIndexed { index, day ->
                        val dayOfWeek = index + 1
                        FilterChip(
                            selected = dayOfWeek in quietHours.daysOfWeek,
                            onClick = {
                                val newDays = if (dayOfWeek in quietHours.daysOfWeek) {
                                    quietHours.daysOfWeek - dayOfWeek
                                } else {
                                    quietHours.daysOfWeek + dayOfWeek
                                }
                                onQuietHoursChange(quietHours.copy(daysOfWeek = newDays))
                            },
                            label = { Text(day) }
                        )
                    }
                }
            }
        }
    }
}

private fun getChannelDescription(channel: NotificationChannelType): String {
    return when (channel) {
        NotificationChannelType.FAILURES -> "Notify when builds fail"
        NotificationChannelType.SUCCESS -> "Notify when builds succeed"
        NotificationChannelType.WARNINGS -> "Notify for warnings and issues"
        NotificationChannelType.HIGH_RISK -> "Notify for high-risk predictions"
        NotificationChannelType.BUILD_STARTED -> "Notify when builds start"
        NotificationChannelType.BUILD_COMPLETED -> "Notify when builds complete"
    }
}
