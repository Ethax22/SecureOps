package com.secureops.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.secureops.app.ui.theme.*

/**
 * Glass-style loading indicator with purple accent
 */
@Composable
fun GlassLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentPadding = PaddingValues(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedLoadingRings(
                modifier = Modifier.size(60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Glass-style empty state with icon, title, subtitle, and optional action
 */
@Composable
fun GlassEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    iconColor: androidx.compose.ui.graphics.Color = PrimaryPurple
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentPadding = PaddingValues(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon with gradient background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                iconColor.copy(alpha = 0.3f),
                                iconColor.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))

                NeonButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Glass-style error state with error message and retry action
 */
@Composable
fun GlassErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong"
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentPadding = PaddingValues(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Error icon with gradient background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
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
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            NeonButton(
                text = "Try Again",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Inline loading state (smaller, for use within screens)
 */
@Composable
fun InlineLoading(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = PrimaryPurple,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Success state with checkmark
 */
@Composable
fun GlassSuccessState(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentPadding = PaddingValues(32.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Success icon with gradient background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SuccessGreen.copy(alpha = 0.3f),
                                SuccessGreen.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))

                NeonButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
