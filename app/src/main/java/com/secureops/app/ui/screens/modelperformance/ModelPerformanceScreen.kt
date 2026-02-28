package com.secureops.app.ui.screens.modelperformance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.secureops.app.ml.evaluation.MetricsCalculator
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.components.GradientBackground
import com.secureops.app.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Model Performance Dashboard Screen
 * Displays ML model metrics, confusion matrix, and performance statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPerformanceScreen(
    viewModel: ModelPerformanceViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Performance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        GradientBackground {
            when (val state = uiState) {
                is ModelPerformanceViewModel.UiState.Loading -> {
                    LoadingState(modifier = Modifier.padding(paddingValues))
                }
                is ModelPerformanceViewModel.UiState.Empty -> {
                    EmptyState(
                        message = state.message,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                is ModelPerformanceViewModel.UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                is ModelPerformanceViewModel.UiState.Success -> {
                    SuccessContent(
                        metrics = state.metrics,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = NeonPurple)
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.padding(32.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.QueryStats,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(modifier = Modifier.padding(32.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun SuccessContent(
    metrics: ModelPerformanceViewModel.ModelMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            "ML Model Performance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Key Metrics Card
        MetricsCard(confusionMatrix = metrics.confusionMatrix)

        // Confusion Matrix
        ConfusionMatrixCard(confusionMatrix = metrics.confusionMatrix)

        // Performance Metrics
        LatencyCard(
            avgInferenceMs = metrics.avgInferenceTimeMs,
            p95InferenceMs = metrics.p95InferenceTimeMs,
            modelSizeMB = metrics.modelSizeMB,
            totalPredictions = metrics.totalPredictions,
            evaluatedPredictions = metrics.evaluatedPredictions
        )

        // Battery Impact
        BatteryImpactCard(
            drainPer100 = metrics.batteryDrainPer100,
            avgInferenceMs = metrics.avgInferenceTimeMs
        )

        // Production Status
        ProductionStatusCard(confusionMatrix = metrics.confusionMatrix)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Key Metrics Card - Precision, Recall, F1, Accuracy
 */
@Composable
fun MetricsCard(confusionMatrix: MetricsCalculator.ConfusionMatrix) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Key Metrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // First Row: Precision, Recall
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Precision",
                    value = confusionMatrix.precision,
                    icon = Icons.Default.TrendingUp
                )
                MetricItem(
                    label = "Recall",
                    value = confusionMatrix.recall,
                    icon = Icons.Default.Visibility
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Second Row: F1 Score, Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "F1 Score",
                    value = confusionMatrix.f1Score,
                    icon = Icons.Default.Balance
                )
                MetricItem(
                    label = "Accuracy",
                    value = confusionMatrix.accuracy,
                    icon = Icons.Default.CheckCircle
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = getMetricColor(value),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${(value * 100).roundToInt()}%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = getMetricColor(value)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Confusion Matrix Card
 */
@Composable
fun ConfusionMatrixCard(confusionMatrix: MetricsCalculator.ConfusionMatrix) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Confusion Matrix",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Matrix Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f))
                Text(
                    "Actual: Fail",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    "Actual: Pass",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // First Row: Predicted Fail
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Pred: Fail",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                MatrixCell(
                    label = "TP",
                    value = confusionMatrix.truePositive,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                MatrixCell(
                    label = "FP",
                    value = confusionMatrix.falsePositive,
                    color = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second Row: Predicted Pass
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Pred: Pass",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                MatrixCell(
                    label = "FN",
                    value = confusionMatrix.falseNegative,
                    color = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
                MatrixCell(
                    label = "TN",
                    value = confusionMatrix.trueNegative,
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MatrixCell(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Latency Card
 */
@Composable
fun LatencyCard(
    avgInferenceMs: Double,
    p95InferenceMs: Long,
    modelSizeMB: Float,
    totalPredictions: Int,
    evaluatedPredictions: Int
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Performance Metrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avg Inference Time
            PerformanceItem(
                label = "Avg Inference Time",
                value = "${String.format("%.2f", avgInferenceMs)} ms",
                icon = Icons.Default.Timer,
                color = if (avgInferenceMs < 100) SuccessGreen else WarningOrange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // P95 Inference Time
            PerformanceItem(
                label = "95th Percentile",
                value = "$p95InferenceMs ms",
                icon = Icons.Default.TrendingUp,
                color = if (p95InferenceMs < 150) SuccessGreen else WarningOrange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Model Size
            PerformanceItem(
                label = "Model Size",
                value = "${String.format("%.2f", modelSizeMB)} MB",
                icon = Icons.Default.Storage,
                color = if (modelSizeMB < 10) SuccessGreen else WarningOrange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Total Predictions
            PerformanceItem(
                label = "Total Predictions",
                value = "$totalPredictions",
                icon = Icons.Default.Calculate,
                color = NeonPurple
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Evaluated Predictions
            PerformanceItem(
                label = "Evaluated",
                value = "$evaluatedPredictions",
                icon = Icons.Default.CheckCircleOutline,
                color = NeonCyan
            )
        }
    }
}

@Composable
private fun PerformanceItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Battery Impact Card
 */
@Composable
fun BatteryImpactCard(
    drainPer100: Float,
    avgInferenceMs: Double
) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Battery Impact",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Drain per 100 predictions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Per 100 Predictions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    "${String.format("%.3f", drainPer100)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = getBatteryColor(drainPer100)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Estimated predictions per 1% battery
            val predictionsPerPercent = if (drainPer100 > 0) (100 / drainPer100).toInt() else 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Predictions per 1%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    "~$predictionsPerPercent",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info text
            Text(
                "Based on ${String.format("%.2f", avgInferenceMs)}ms avg inference time",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Production Status Card
 */
@Composable
fun ProductionStatusCard(confusionMatrix: MetricsCalculator.ConfusionMatrix) {
    val meetsThresholds = confusionMatrix.meetsProductionThresholds()

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (meetsThresholds) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (meetsThresholds) SuccessGreen else WarningOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Production Readiness",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ThresholdItem(
                label = "Precision ≥ 85%",
                actual = confusionMatrix.precision,
                threshold = 0.85f
            )

            Spacer(modifier = Modifier.height(8.dp))

            ThresholdItem(
                label = "Recall ≥ 80%",
                actual = confusionMatrix.recall,
                threshold = 0.80f
            )

            Spacer(modifier = Modifier.height(8.dp))

            ThresholdItem(
                label = "F1 Score ≥ 82%",
                actual = confusionMatrix.f1Score,
                threshold = 0.82f
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (meetsThresholds) SuccessGreen.copy(alpha = 0.2f)
                        else WarningOrange.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (meetsThresholds) "✅ PRODUCTION READY" else "⚠️ NEEDS IMPROVEMENT",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (meetsThresholds) SuccessGreen else WarningOrange
                )
            }
        }
    }
}

@Composable
private fun ThresholdItem(label: String, actual: Float, threshold: Float) {
    val passes = actual >= threshold

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${(actual * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (passes) SuccessGreen else WarningOrange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                if (passes) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (passes) SuccessGreen else WarningOrange,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Helper functions for color coding
 */
private fun getMetricColor(value: Float): Color = when {
    value >= 0.90f -> SuccessGreen
    value >= 0.75f -> WarningOrange
    else -> ErrorRed
}

private fun getBatteryColor(drain: Float): Color = when {
    drain < 0.1f -> SuccessGreen
    drain < 0.2f -> WarningOrange
    else -> ErrorRed
}

// Color definitions (if not already in theme)
private val SuccessGreen = Color(0xFF4CAF50)
private val WarningOrange = Color(0xFFFF9800)
private val ErrorRed = Color(0xFFF44336)
private val NeonGreen = Color(0xFF00FF94)
private val NeonBlue = Color(0xFF00B4D8)
