package com.secureops.app.ui.screens.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureops.app.data.local.entity.ThreatEntity
import com.secureops.app.ml.security.ThreatSeverity
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Threat Card Component
 * Displays detailed information about a detected threat
 */
@Composable
fun ThreatCard(
    threat: ThreatEntity,
    onResolve: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Severity Badge and Title
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeverityBadge(severity = ThreatSeverity.fromLevel(threat.severity))
                        
                        ThreatTypeChip(threat.patternType)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = threat.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (threat.isResolved) SuccessGreen.copy(alpha = 0.2f)
                            else WarningAmber.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (threat.isResolved) Icons.Default.CheckCircle
                        else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (threat.isResolved) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ThreatInfoItem(
                    icon = Icons.Default.Storage,
                    text = threat.repositoryName,
                    modifier = Modifier.weight(1f)
                )
                
                ThreatInfoItem(
                    icon = Icons.Default.CalendarToday,
                    text = formatTimestamp(threat.detectedAt),
                    modifier = Modifier.weight(1f)
                )
            }

            // Expanded Details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Divider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    // Detected Value
                    DetailRow(
                        label = "Detected Value",
                        value = threat.detectedValue,
                        isMonospace = true
                    )

                    // Source
                    DetailRow(
                        label = "Source",
                        value = threat.source
                    )

                    // Line Number (if applicable)
                    threat.lineNumber?.let { line ->
                        DetailRow(
                            label = "Line Number",
                            value = line.toString()
                        )
                    }

                    // Branch
                    DetailRow(
                        label = "Branch",
                        value = threat.branch
                    )

                    // Commit Hash
                    DetailRow(
                        label = "Commit",
                        value = threat.commitHash.take(8),
                        isMonospace = true
                    )

                    // Context Line
                    if (threat.contextLine.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Context:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = threat.contextLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Resolution Notes (if resolved)
                    if (threat.isResolved && threat.resolutionNotes != null) {
                        Column {
                            Text(
                                text = "Resolution Notes:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = threat.resolutionNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Action Buttons
                    if (!threat.isResolved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onResolve,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessGreen
                                )
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resolve")
                            }
                            
                            OutlinedButton(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ErrorRed
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Severity Badge
 */
@Composable
fun SeverityBadge(
    severity: ThreatSeverity,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (severity) {
        ThreatSeverity.CRITICAL -> ErrorRed to "CRITICAL"
        ThreatSeverity.HIGH -> WarningAmber to "HIGH"
        ThreatSeverity.MEDIUM -> Color(0xFFFFA726) to "MEDIUM"
        ThreatSeverity.LOW -> Color(0xFFFFD54F) to "LOW"
        ThreatSeverity.INFO -> Color(0xFF81C784) to "INFO"
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 10.sp
        )
    }
}

/**
 * Threat Type Chip
 */
@Composable
fun ThreatTypeChip(
    patternType: String,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when {
        patternType.startsWith("ANOMALY_") -> Icons.Default.ShowChart to AccentPink
        patternType.startsWith("DEPENDENCY_") -> Icons.Default.Build to InfoBlue
        else -> Icons.Default.Key to PrimaryPurple
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = patternType.replace("_", " "),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Threat Info Item
 */
@Composable
fun ThreatInfoItem(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Detail Row (label + value)
 */
@Composable
fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Format timestamp to readable string
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
