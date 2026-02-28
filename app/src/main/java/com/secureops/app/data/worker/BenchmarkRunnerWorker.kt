package com.secureops.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.secureops.app.ml.benchmark.PerformanceBenchmark
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * BenchmarkRunnerWorker
 *
 * A WorkManager CoroutineWorker that runs the full performance benchmark
 * (inference time, memory, startup, ML metrics) in the background and
 * persists results to the Room database.
 */
class BenchmarkRunnerWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val performanceBenchmark: PerformanceBenchmark by inject()

    companion object {
        const val WORK_NAME = "benchmark_runner"
        const val TAG = "BenchmarkRunner"
    }

    override suspend fun doWork(): Result {
        Timber.i("BenchmarkRunnerWorker started")
        return try {
            val result = performanceBenchmark.runFullBenchmark()
            Timber.i(
                "Benchmark finished: inference=${result.inferenceTimeMs}ms, " +
                "mem=${"%.2f".format(result.memoryUsageMb)}MB, " +
                "F1=${"%.3f".format(result.f1Score)}"
            )
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "BenchmarkRunnerWorker failed")
            Result.retry()
        }
    }
}
