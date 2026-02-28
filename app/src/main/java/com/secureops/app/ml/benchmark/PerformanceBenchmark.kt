package com.secureops.app.ml.benchmark

import android.app.ActivityManager
import android.content.Context
import com.secureops.app.data.local.dao.BenchmarkResultDao
import com.secureops.app.data.local.entity.BenchmarkResultEntity
import com.secureops.app.ml.FailurePredictionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * PerformanceBenchmark
 *
 * Measures real performance characteristics of the SecureOps ML model and system:
 * - Inference time (avg over N iterations)
 * - Memory usage during inference
 * - Model startup / warm-up time
 */
class PerformanceBenchmark(
    private val context: Context,
    private val model: FailurePredictionModel,
    private val benchmarkDao: BenchmarkResultDao,
    private val modelValidator: ModelValidator
) {

    companion object {
        private const val INFERENCE_ITERATIONS = 100
    }

    suspend fun runFullBenchmark(): BenchmarkResultEntity = withContext(Dispatchers.Default) {
        Timber.i("Starting full performance benchmark...")

        // --- 1. Startup time (cold-start model warm-up) ---
        val startupTimeMs = measureTimeMillis {
            model.predictFailure(
                commitDiff = "Initial warmup",
                testHistory = listOf(true, true, false),
                logs = ""
            )
        }
        Timber.d("Startup/warmup time: ${startupTimeMs}ms")

        // --- 2. Inference time (avg over multiple iterations) ---
        var totalInferenceMs = 0L
        repeat(INFERENCE_ITERATIONS) { i ->
            val diff = "Change $i in SomeClass.kt\n+  val x = ${i}\n-  val x = 0"
            val history = List(10) { it % 3 != 0 }
            totalInferenceMs += measureTimeMillis {
                model.predictFailure(diff, history, "Build log $i")
            }
        }
        val avgInferenceMs = totalInferenceMs / INFERENCE_ITERATIONS
        Timber.d("Avg inference time: ${avgInferenceMs}ms over $INFERENCE_ITERATIONS runs")

        // --- 3. Memory usage ---
        val rt = Runtime.getRuntime()
        System.gc()
        val memBefore = rt.totalMemory() - rt.freeMemory()
        // Run a batch to force allocations
        repeat(10) {
            model.predictFailure("diff $it", listOf(true, false, true), "log $it")
        }
        val memAfter = rt.totalMemory() - rt.freeMemory()
        val memUsedMb = ((memAfter - memBefore).coerceAtLeast(0) / (1024.0 * 1024.0))
        Timber.d("Approximate memory delta: ${"%.2f".format(memUsedMb)} MB")

        // --- 4. ML Validation Metrics (Precision / Recall / F1) ---
        val metrics = modelValidator.validateModel()

        // --- 5. Synthetic Battery Impact ---
        // Assume active CPU limits draw ~400mA, meaning 400 mAh per hour.
        // Convert ms to hours: ms / 3_600_000
        val totalActiveTimeMs = totalInferenceMs + startupTimeMs
        val batteryDrainMah = (totalActiveTimeMs.toFloat() / 3_600_000f) * 400f

        // --- 6. Persist results ---
        val result = BenchmarkResultEntity(
            timestamp = System.currentTimeMillis(),
            inferenceTimeMs = avgInferenceMs,
            memoryUsageMb = memUsedMb,
            startupTimeMs = startupTimeMs,
            precision = metrics.precision,
            recall = metrics.recall,
            f1Score = metrics.f1Score,
            accuracy = metrics.accuracy,
            truePositives = metrics.truePositives,
            falsePositives = metrics.falsePositives,
            trueNegatives = metrics.trueNegatives,
            falseNegatives = metrics.falseNegatives,
            batteryDrainMah = batteryDrainMah
        )
        benchmarkDao.insertBenchmarkResult(result)
        Timber.i("Benchmark saved: inference=${avgInferenceMs}ms, mem=${"%.2f".format(memUsedMb)}MB, F1=${"%.3f".format(metrics.f1Score)}")

        return@withContext result
    }
}
