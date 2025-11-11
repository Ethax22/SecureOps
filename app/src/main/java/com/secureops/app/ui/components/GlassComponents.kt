package com.secureops.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.secureops.app.ui.theme.*

/**
 * Glass Card with frosted glass effect
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == BackgroundDark

    Card(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = if (onClick != null) 4.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (isDark) GlassBorderDark else GlassBorderLight,
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Purple gradient background
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == BackgroundDark

    Box(
        modifier = modifier
            .background(
                brush = if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            GradientDarkStart,
                            BackgroundDark,
                            GradientDarkEnd
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundLight,
                            BackgroundLightSecondary,
                            BackgroundLight
                        )
                    )
                }
            ),
        content = content
    )
}

/**
 * Service card with icon and gradient accent
 */
@Composable
fun ServiceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryPurple
) {
    GlassCard(
        onClick = onClick,
        modifier = modifier.size(width = 160.dp, height = 130.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.3f),
                            accentColor.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}

/**
 * Neon glow button with purple gradient
 */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    glowColor: Color = NeonPurple
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (enabled) {
                            listOf(GradientPurpleStart, GradientPurpleEnd)
                        } else {
                            listOf(Color.Gray, Color.DarkGray)
                        }
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (enabled) glowColor.copy(alpha = 0.5f) else Color.Transparent,
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Neon chip/badge
 */
@Composable
fun NeonChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    color: Color = PrimaryPurple
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

/**
 * Floating glass input field
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val isDark = MaterialTheme.colorScheme.background == BackgroundDark

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight,
            unfocusedContainerColor = if (isDark) GlassSurfaceDark else GlassSurfaceLight,
            focusedBorderColor = PrimaryPurple,
            unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorderLight,
            cursorColor = PrimaryPurple,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

/**
 * Status badge with glow effect
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    color: Color = SuccessGreen
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

/**
 * Gradient divider
 */
@Composable
fun GradientDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        PrimaryPurple.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
    )
}
