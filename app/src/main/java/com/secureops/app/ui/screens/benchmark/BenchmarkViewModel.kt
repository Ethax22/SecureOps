package com.secureops.app.ui.screens.benchmark

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.secureops.app.data.local.dao.BenchmarkResultDao
import com.secureops.app.data.local.entity.BenchmarkResultEntity
import com.secureops.app.data.worker.BenchmarkRunnerWorker
import com.secureops.app.ml.benchmark.BenchmarkReportGenerator
import com.secureops.app.ml.benchmark.PerformanceBenchmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class BenchmarkUiState {
    object Idle : BenchmarkUiState()
    object Running : BenchmarkUiState()
    data class Done(val result: BenchmarkResultEntity) : BenchmarkUiState()
    data class Error(val message: String) : BenchmarkUiState()
}

class BenchmarkViewModel(
    private val benchmarkDao: BenchmarkResultDao,
    private val performanceBenchmark: PerformanceBenchmark,
    private val reportGenerator: BenchmarkReportGenerator,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<BenchmarkUiState>(BenchmarkUiState.Idle)
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    val history = benchmarkDao.getAllBenchmarkResults()
    val latestResult = benchmarkDao.getLatestBenchmarkResult()

    fun runBenchmark() {
        viewModelScope.launch {
            _uiState.value = BenchmarkUiState.Running
            try {
                val result = performanceBenchmark.runFullBenchmark()
                _uiState.value = BenchmarkUiState.Done(result)
            } catch (e: Exception) {
                Timber.e(e, "Benchmark failed")
                _uiState.value = BenchmarkUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun runBenchmarkInBackground() {
        val request = OneTimeWorkRequestBuilder<BenchmarkRunnerWorker>()
            .addTag(BenchmarkRunnerWorker.TAG)
            .build()
        WorkManager.getInstance(context).enqueue(request)
        Timber.i("Background benchmark enqueued")
    }

    fun exportJson(result: BenchmarkResultEntity) {
        viewModelScope.launch {
            try {
                val fileName = reportGenerator.exportJson(result)
                Timber.i("Exported JSON: $fileName")
            } catch (e: Exception) {
                Timber.e(e, "JSON export failed")
            }
        }
    }

    fun exportPdf(result: BenchmarkResultEntity) {
        viewModelScope.launch {
            try {
                val fileName = reportGenerator.exportPdf(result)
                Timber.i("Exported PDF report: $fileName")
            } catch (e: Exception) {
                Timber.e(e, "PDF export failed")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            benchmarkDao.clearAllBenchmarkResults()
            _uiState.value = BenchmarkUiState.Idle
        }
    }
}
