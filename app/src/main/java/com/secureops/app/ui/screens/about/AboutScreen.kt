package com.secureops.app.ui.screens.about

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.secureops.app.data.local.dao.BenchmarkResultDao
import com.secureops.app.data.local.entity.BenchmarkResultEntity
import com.secureops.app.ui.components.AnimatedGradientBackground
import com.secureops.app.ui.components.GlassCard
import com.secureops.app.ui.theme.*
import org.koin.androidx.compose.get

@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val benchmarkDao: BenchmarkResultDao = get()
    val latestBenchmark by benchmarkDao.getLatestBenchmarkResult().collectAsState(initial = null)

    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = "About SecureOps",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Production-grade Android DevSecOps Platform",
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(20.dp))

            JudgeSummaryCard(latestBenchmark)

            Spacer(Modifier.height(12.dp))
            TechStackCard()

            Spacer(Modifier.height(12.dp))
            CoreDifferentiatorsCard()

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun JudgeSummaryCard(benchmark: BenchmarkResultEntity? = null) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Hackathon Judge Summary",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SurfaceLight)

            Text(
                "SecureOps is a production-grade DevSecOps Android platform providing real-time " +
                "CI/CD pipeline monitoring, on-device ML failure prediction, explainable AI via " +
                "SHAP approximations, and automated security scanning — all without a persistent " +
                "backend dependency.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            // Live ML Benchmark snapshot
            if (benchmark != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = SurfaceLight)
                Spacer(Modifier.height(12.dp))
                Text("Latest ML Benchmark", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniMetricChip("Inference", "${benchmark.inferenceTimeMs}ms", InfoBlue, Modifier.weight(1f))
                    MiniMetricChip("Memory", "${"%.1f".format(benchmark.memoryUsageMb)}MB", WarningAmber, Modifier.weight(1f))
                    MiniMetricChip("F1 Score", "${"%.3f".format(benchmark.f1Score)}", 
                        if (benchmark.f1Score >= 0.82) AccentGreen else ErrorRed, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TechStackCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tech Stack", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(Modifier.height(12.dp))
            val items = listOf(
                "100% Kotlin + Jetpack Compose",
                "MVVM + Clean Architecture + Koin DI",
                "Room Database (v7) with full migration chain",
                "Retrofit + OkHttp (GitHub, GitLab, Jenkins, CircleCI, Azure)",
                "TensorFlow Lite (on-device ML, quantized)",
                "WorkManager for background sync & benchmark scheduling",
                "RunAnywhere SDK for local LLM voice integration"
            )
            items.forEach { item ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("•", color = AccentGreen, fontSize = 14.sp, modifier = Modifier.width(14.dp))
                    Text(item, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CoreDifferentiatorsCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Core Differentiators", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(Modifier.height(12.dp))

            DifferentiatorItem(
                icon = Icons.Default.Psychology,
                color = AccentGreen,
                title = "Real On-Device ML",
                desc = "Offline pipeline failure prediction with TFLite — no cloud dependency."
            )
            DifferentiatorItem(
                icon = Icons.Default.Lightbulb,
                color = WarningAmber,
                title = "Explainable AI (SHAP)",
                desc = "SHAP approximations render human-readable failure insights."
            )
            DifferentiatorItem(
                icon = Icons.Default.Shield,
                color = ErrorRed,
                title = "DevSecOps Engine",
                desc = "Real-time secret scanning, dependency analysis, and anomaly detection."
            )
            DifferentiatorItem(
                icon = Icons.Default.AutoFixHigh,
                color = InfoBlue,
                title = "Adaptive Auto-Remediation",
                desc = "Contextual remediation suggestions with ML-driven confidence scores."
            )
            DifferentiatorItem(
                icon = Icons.Default.Speed,
                color = AccentGreen,
                title = "Performance Benchmarking",
                desc = "In-app benchmarking of inference speed, memory, and model precision/recall."
            )
        }
    }
}

@Composable
private fun DifferentiatorItem(icon: ImageVector, color: Color, title: String, desc: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
            Text(desc, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun MiniMetricChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
