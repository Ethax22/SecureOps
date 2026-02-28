package com.secureops.app.ui.screens.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.secureops.app.ml.explainability.ExplanationResult
import com.secureops.app.ml.explainability.FeatureContribution
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.components.GradientDivider
import com.secureops.app.ui.theme.*
import kotlin.math.abs

/**
 * Main card displaying SHAP-based AI explanation
 * 
 * Features:
 * - Color-coded risk indicator
 * - Top 5 feature contributions
 * - Expandable full explanation
 * - Null-safe handling
 */
@Composable
fun ExplainabilityCard(
    explanation: ExplanationResult?,
    riskPercentage: Float,
    modifier: Modifier = Modifier
) {
    // Null-safe handling: Only show if explanation exists
    if (explanation == null) return
    
    var isExpanded by remember { mutableStateOf(false) }
    
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        // Header with risk indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🧠 AI Explanation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Risk visual indicator
            RiskVisualIndicator(
                riskPercentage = riskPercentage,
                size = 56.dp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Baseline comparison
        val delta = riskPercentage - explanation.baselinePrediction
        val comparisonText = when {
            delta > 10f -> "significantly higher than average"
            delta > 2f -> "above average"
            delta > -2f -> "near average"
            delta > -10f -> "below average"
            else -> "significantly lower than average"
        }
        
        Text(
            text = "This build's risk is $comparisonText (baseline: ${explanation.baselinePrediction.toInt()}%)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        // Top 5 contributors
        Text(
            text = "Top Contributing Factors",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AccentCyan
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        explanation.topContributors.take(5).forEachIndexed { index, contribution ->
            ContributionBar(
                contribution = contribution,
                rank = (index + 1).toString(),
                maxMagnitude = explanation.topContributors.maxOfOrNull { abs(it.shapValue) } ?: 1f
            )
            if (index < 4 && index < explanation.topContributors.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        val otherContributions = explanation.allContributions.drop(5)
        if (otherContributions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            val otherSum = otherContributions.sumOf { it.shapValue.toDouble() }.toFloat()
            val otherImpact = when {
                otherSum > 0.05f -> FeatureContribution.Impact.POSITIVE
                otherSum < -0.05f -> FeatureContribution.Impact.NEGATIVE
                else -> FeatureContribution.Impact.NEUTRAL
            }
            val otherContribution = FeatureContribution(
                featureName = "other_factors_combined",
                value = 0f,
                shapValue = otherSum,
                impact = otherImpact
            )
            
            ContributionBar(
                contribution = otherContribution,
                rank = "*",
                maxMagnitude = explanation.topContributors.maxOfOrNull { abs(it.shapValue) } ?: 1f
            )
        }
        
        // Expandable full explanation
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isExpanded) "Hide Details" else "Show Full Explanation",
                color = AccentPink
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = AccentPink,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Animated expansion
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                GradientDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Full explanation text
                Text(
                    text = explanation.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5f
                )
                
                // Contribution statistics
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBadge(
                        label = "Risk Factors",
                        value = explanation.contributionStats.positiveCount.toString(),
                        color = ErrorRed
                    )
                    StatBadge(
                        label = "Protective",
                        value = explanation.contributionStats.negativeCount.toString(),
                        color = SuccessGreen
                    )
                    StatBadge(
                        label = "Neutral",
                        value = explanation.contributionStats.neutralCount.toString(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Visual bar showing a feature's contribution to the prediction
 * 
 * Features:
 * - Animated fill
 * - Color-coded by impact (positive/negative/neutral)
 * - Shows magnitude and direction
 */
@Composable
fun ContributionBar(
    contribution: FeatureContribution,
    rank: String,
    maxMagnitude: Float,
    modifier: Modifier = Modifier
) {
    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = (abs(contribution.shapValue) / maxMagnitude).coerceAtMost(1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "contribution_progress"
    )
    
    // Color based on impact
    val barColor = when (contribution.impact) {
        FeatureContribution.Impact.POSITIVE -> ErrorRed
        FeatureContribution.Impact.NEGATIVE -> SuccessGreen
        FeatureContribution.Impact.NEUTRAL -> Color.Gray
    }
    
    Column(modifier = modifier) {
        // Feature name and rank
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(barColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Feature name (formatted)
                Text(
                    text = formatFeatureName(contribution.featureName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // SHAP value
            Text(
                text = "${if (contribution.shapValue >= 0) "+" else ""}${"%.2f".format(contribution.shapValue)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Animated bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.6f),
                                barColor
                            )
                        )
                    )
            )
        }
        
        // Impact label
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = when (contribution.impact) {
                FeatureContribution.Impact.POSITIVE -> "↑ Increases risk"
                FeatureContribution.Impact.NEGATIVE -> "↓ Decreases risk"
                FeatureContribution.Impact.NEUTRAL -> "→ Minimal impact"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Circular visual indicator for risk level
 * 
 * Features:
 * - Animated arc drawing
 * - Color gradient based on risk
 * - Pulsing animation for high risk
 */
@Composable
fun RiskVisualIndicator(
    riskPercentage: Float,
    size: Dp,
    modifier: Modifier = Modifier
) {
    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = riskPercentage / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "risk_progress"
    )
    
    // Pulsing animation for high risk
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (riskPercentage > 70f) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Color based on risk level
    val riskColor = getRiskColor(riskPercentage)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer(
                    scaleX = pulseScale,
                    scaleY = pulseScale
                )
        ) {
            val canvasSize = size.toPx()
            val strokeWidth = canvasSize * 0.15f
            
            // Background arc
            drawArc(
                color = Color.Gray.copy(alpha = 0.2f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
            )
            
            // Foreground arc (animated)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        riskColor.copy(alpha = 0.5f),
                        riskColor
                    )
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
            )
        }
        
        // Risk percentage text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${riskPercentage.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = riskColor
            )
            Text(
                text = getRiskLabel(riskPercentage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Small stat badge for contribution statistics
 */
@Composable
private fun StatBadge(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Get color for risk level
 */
private fun getRiskColor(riskPercentage: Float): Color {
    return when {
        riskPercentage < 30f -> SuccessGreen
        riskPercentage < 60f -> WarningAmber
        else -> ErrorRed
    }
}

/**
 * Get label for risk level
 */
private fun getRiskLabel(riskPercentage: Float): String {
    return when {
        riskPercentage < 30f -> "LOW"
        riskPercentage < 60f -> "MODERATE"
        else -> "HIGH"
    }
}

/**
 * Format feature name to be human-readable
 */
private fun formatFeatureName(featureName: String): String {
    return featureName
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
