package com.secureops.app.ui.screens.benchmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureops.app.data.local.entity.BenchmarkResultEntity
import com.secureops.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    viewModel: BenchmarkViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState(initial = emptyList())
    val latest by viewModel.latestResult.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Benchmark & Validation", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.runBenchmark() },
                icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                text = { Text("Run Benchmark") },
                containerColor = AccentGreen,
                contentColor = Color.White
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Status / Running Indicator ──
            item {
                when (uiState) {
                    is BenchmarkUiState.Running -> BenchmarkRunningCard()
                    is BenchmarkUiState.Error -> ErrorCard((uiState as BenchmarkUiState.Error).message)
                    else -> {}
                }
            }

            // ── Latest Result ──
            item {
                val resultToShow = when (val s = uiState) {
                    is BenchmarkUiState.Done -> s.result
                    else -> latest
                }
                if (resultToShow != null) {
                    LatestBenchmarkCard(resultToShow, viewModel)
                }
            }

            // ── History Header ──
            if (history.isNotEmpty()) {
                item {
                    Text(
                        text = "Run History",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(history) { result ->
                    BenchmarkHistoryRow(result)
                }
                item {
                    TextButton(
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ErrorRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear History", color = ErrorRed)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun BenchmarkRunningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = AccentGreen, modifier = Modifier.size(28.dp))
            Column {
                Text("Running Benchmark...", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Measuring inference, memory, and ML metrics", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed)
            Text(message, color = ErrorRed, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LatestBenchmarkCard(result: BenchmarkResultEntity, viewModel: BenchmarkViewModel) {
    val isProductionReady = result.precision >= 0.85 && result.recall >= 0.80 && result.f1Score >= 0.82

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Latest Benchmark", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isProductionReady) AccentGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
                ) {
                    Text(
                        if (isProductionReady) "✅ Production Ready" else "⚠️ Needs Work",
                        color = if (isProductionReady) AccentGreen else ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Performance metrics row 1
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerfMetricChip("Inference", "${result.inferenceTimeMs}ms", Icons.Default.Timer, InfoBlue, Modifier.weight(1f))
                PerfMetricChip("Memory", "${"%.1f".format(result.memoryUsageMb)}MB", Icons.Default.Memory, WarningAmber, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PerfMetricChip("Startup", "${result.startupTimeMs}ms", Icons.Default.RocketLaunch, AccentGreen, Modifier.weight(1f))
                PerfMetricChip("Battery", "${"%.2f".format(result.batteryDrainMah)}mAh", Icons.Default.BatteryChargingFull, AccentPink, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // ML metric bars
            Text("ML Validation Metrics", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            MetricBar("Accuracy",  result.accuracy,  0.80, AccentGreen)
            MetricBar("Precision", result.precision, 0.85, InfoBlue)
            MetricBar("Recall",    result.recall,    0.80, WarningAmber)
            MetricBar("F1 Score",  result.f1Score,   0.82, if (result.f1Score >= 0.82) AccentGreen else ErrorRed)

            Spacer(Modifier.height(16.dp))

            // Confusion Matrix
            Text("Confusion Matrix", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            ConfusionMatrixGrid(result.truePositives, result.falsePositives, result.falseNegatives, result.trueNegatives)

            Spacer(Modifier.height(16.dp))

            // Export actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = { viewModel.exportJson(result) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InfoBlue)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export JSON")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.exportPdf(result) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export Report")
                }
            }
        }
    }
}

@Composable
private fun PerfMetricChip(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MetricBar(label: String, value: Double, threshold: Double, barColor: Color) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "metricBar_$label"
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextSecondary, fontSize = 12.sp)
            Text("${"%.1f".format(value * 100)}%  [≥${"%.0f".format(threshold * 100)}%]",
                color = if (value >= threshold) AccentGreen else ErrorRed, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceLight)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animatedValue)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BenchmarkHistoryRow(result: BenchmarkResultEntity) {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val isReady = result.precision >= 0.85 && result.recall >= 0.80 && result.f1Score >= 0.82
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(fmt.format(Date(result.timestamp)), color = TextPrimary, fontSize = 13.sp)
                Text("F1: ${"%.3f".format(result.f1Score)} · ${result.inferenceTimeMs}ms", color = TextSecondary, fontSize = 11.sp)
            }
            Icon(
                if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isReady) AccentGreen else WarningAmber,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ConfusionMatrixGrid(tp: Int, fp: Int, fn: Int, tn: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // True Positives (Good)
            ConfusionMatrixCell(
                title = "True Positives",
                count = tp,
                desc = "Blocked failures",
                color = AccentGreen,
                modifier = Modifier.weight(1f)
            )
            // False Positives (Bad)
            ConfusionMatrixCell(
                title = "False Positives",
                count = fp,
                desc = "False alarms",
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // False Negatives (Critical)
            ConfusionMatrixCell(
                title = "False Negatives",
                count = fn,
                desc = "Missed failures",
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
            // True Negatives (Good)
            ConfusionMatrixCell(
                title = "True Negatives",
                count = tn,
                desc = "Passed safely",
                color = InfoBlue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConfusionMatrixCell(title: String, count: Int, desc: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
