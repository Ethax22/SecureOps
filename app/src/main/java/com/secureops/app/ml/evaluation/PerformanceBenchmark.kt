package com.secureops.app.ml.evaluation

import android.content.Context
import com.secureops.app.ml.FailurePredictionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object PerformanceBenchmark {
    suspend fun runBenchmark(context: Context, model: FailurePredictionModel): BenchmarkMetrics = withContext(Dispatchers.Default) {
        Timber.i("Starting performance benchmark...")
        
        // Measure memory before
        val memoryBefore = getMemoryUsageMb()
        var peakMemory = memoryBefore
        
        // Measure startup time (measure model instantiation)
        val startupStartTime = System.currentTimeMillis()
        val dummyModel = FailurePredictionModel(context, null, null)
        val modelLoadTimeMs = System.currentTimeMillis() - startupStartTime
        peakMemory = maxOf(peakMemory, getMemoryUsageMb())
        
        // Measure inference
        val inferenceTimes = mutableListOf<Long>()
        val iterations = 50
        
        for (i in 0 until iterations) {
            val start = System.nanoTime()
            dummyModel.predictFailure(
                commitDiff = "dummy diff + test implementation",
                testHistory = listOf(true, false, true),
                logs = "Test log output error timeout"
            )
            val duration = (System.nanoTime() - start) / 1_000_000
            inferenceTimes.add(duration)
            peakMemory = maxOf(peakMemory, getMemoryUsageMb())
        }
        
        val memoryAfter = getMemoryUsageMb()
        val avgMemory = (memoryBefore + memoryAfter) / 2f
        
        inferenceTimes.sort()
        val avgInference = inferenceTimes.average().toFloat()
        val p95Inference = inferenceTimes[(iterations * 0.95).toInt()].toFloat()
        
        dummyModel.close()
        
        // Calculate synthetic battery impact:
        // Assume active inference draws ~400mA.
        // Drain (mAh) = (Total Active Time in ms / 3_600_000) * 400
        val totalActiveTimeMs = inferenceTimes.sum()
        val batteryDrainMah = (totalActiveTimeMs.toFloat() / 3_600_000f) * 400f
        
        BenchmarkMetrics(
            inferenceTimeMsAvg = avgInference,
            inferenceTimeMsP95 = p95Inference,
            memoryUsageMbAvg = avgMemory,
            memoryUsageMbPeak = peakMemory,
            startupTimeMs = modelLoadTimeMs,
            modelLoadTimeMs = modelLoadTimeMs,
            batteryDrainMah = batteryDrainMah
        )
    }
    
    private fun getMemoryUsageMb(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576F
        return usedMemInMB
    }
}

data class BenchmarkMetrics(
    val inferenceTimeMsAvg: Float,
    val inferenceTimeMsP95: Float,
    val memoryUsageMbAvg: Float,
    val memoryUsageMbPeak: Float,
    val startupTimeMs: Long,
    val modelLoadTimeMs: Long,
    val batteryDrainMah: Float
)
