package com.secureops.app.ui.screens.modelperformance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.local.dao.BuildEvaluationDao
import com.secureops.app.ml.evaluation.MetricsCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * ViewModel for Model Performance Dashboard
 * Provides live metrics about ML model performance
 */
class ModelPerformanceViewModel(
    private val context: Context,
    private val buildEvaluationDao: BuildEvaluationDao,
    private val metricsCalculator: MetricsCalculator
) : ViewModel() {

    /**
     * Model metrics data class
     */
    data class ModelMetrics(
        val confusionMatrix: MetricsCalculator.ConfusionMatrix,
        val avgInferenceTimeMs: Double,
        val p95InferenceTimeMs: Long,
        val avgConfidence: Float,
        val totalPredictions: Int,
        val evaluatedPredictions: Int,
        val modelSizeMB: Float,
        val batteryDrainPer100: Float
    )

    /**
     * UI State
     */
    sealed class UiState {
        object Loading : UiState()
        data class Success(val metrics: ModelMetrics) : UiState()
        data class Empty(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    /**
     * Live metrics from database
     */
    val uiState: StateFlow<UiState> = buildEvaluationDao.getAllEvaluatedBuilds()
        .map { evaluations ->
            if (evaluations.isEmpty()) {
                UiState.Empty("No evaluation data available. Run predictions first.")
            } else {
                try {
                    val summary = metricsCalculator.calculateSummary(evaluations)
                    
                    UiState.Success(
                        ModelMetrics(
                            confusionMatrix = summary.confusionMatrix,
                            avgInferenceTimeMs = summary.avgInferenceTimeMs,
                            p95InferenceTimeMs = summary.p95InferenceTimeMs,
                            avgConfidence = summary.avgConfidence,
                            totalPredictions = evaluations.size,
                            evaluatedPredictions = summary.totalEvaluations,
                            modelSizeMB = getModelSize(),
                            batteryDrainPer100 = estimateBatteryDrain(summary.avgInferenceTimeMs)
                        )
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error calculating metrics")
                    UiState.Error("Failed to calculate metrics: ${e.message}")
                }
            }
        }
        .catch { e ->
            Timber.e(e, "Error loading evaluations")
            emit(UiState.Error("Failed to load data: ${e.message}"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )

    /**
     * Get TFLite model file size
     */
    private fun getModelSize(): Float {
        return try {
            // Check for v2 model first
            val v2ModelPath = "failure_prediction_v2.tflite"
            val v2Fd = context.assets.openFd(v2ModelPath)
            val sizeMB = v2Fd.length / (1024f * 1024f)
            v2Fd.close()
            Timber.d("Model v2 size: $sizeMB MB")
            sizeMB
        } catch (e: Exception) {
            try {
                // Fallback to v1 model
                val v1ModelPath = "failure_prediction.tflite"
                val v1Fd = context.assets.openFd(v1ModelPath)
                val sizeMB = v1Fd.length / (1024f * 1024f)
                v1Fd.close()
                Timber.d("Model v1 size: $sizeMB MB")
                sizeMB
            } catch (e2: Exception) {
                Timber.w("No model file found in assets")
                0f
            }
        }
    }

    /**
     * Estimate battery drain per 100 predictions
     * Based on inference time and typical mobile CPU power consumption
     */
    private fun estimateBatteryDrain(avgInferenceMs: Double): Float {
        if (avgInferenceMs <= 0) return 0f
        
        // Rough estimation:
        // - Mobile CPU at 100% uses ~2-3W
        // - Typical battery: 3000-4000mAh at 3.7V = ~11-15Wh
        // - 100ms inference at 50% CPU load = ~0.00014 Wh
        // - Per 100 predictions: ~0.014 Wh = ~0.09-0.12% of battery
        
        val totalInferenceTimeSeconds = (avgInferenceMs * 100) / 1000.0
        val cpuLoadFactor = 0.5 // Assume 50% CPU load
        val cpuPowerWatts = 2.5 // Average mobile CPU power
        val energyWh = (totalInferenceTimeSeconds / 3600.0) * cpuPowerWatts * cpuLoadFactor
        val batteryCapacityWh = 13.0 // Average smartphone battery
        
        val drainPercentage = ((energyWh / batteryCapacityWh) * 100).toFloat()
        
        return drainPercentage.coerceIn(0f, 100f)
    }

    /**
     * Refresh metrics manually
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                // Trigger re-evaluation of pending predictions
                Timber.d("Manual refresh requested")
                // The Flow will automatically update
            } catch (e: Exception) {
                Timber.e(e, "Error during refresh")
            }
        }
    }

    /**
     * Get detailed statistics for export
     */
    suspend fun getDetailedStats(): String {
        return try {
            val evaluations = buildEvaluationDao.getEvaluationsForMetrics()
            if (evaluations.isEmpty()) {
                "No data available"
            } else {
                val summary = metricsCalculator.calculateSummary(evaluations)
                val cm = summary.confusionMatrix
                
                """
                Model Performance Report
                ========================
                
                Total Predictions: ${evaluations.size}
                Evaluated: ${summary.totalEvaluations}
                
                Confusion Matrix:
                  TP: ${cm.truePositive}  |  FP: ${cm.falsePositive}
                  FN: ${cm.falseNegative}  |  TN: ${cm.trueNegative}
                
                Key Metrics:
                  Precision: ${String.format("%.2f%%", cm.precision * 100)}
                  Recall:    ${String.format("%.2f%%", cm.recall * 100)}
                  F1 Score:  ${String.format("%.2f%%", cm.f1Score * 100)}
                  Accuracy:  ${String.format("%.2f%%", cm.accuracy * 100)}
                
                Performance:
                  Avg Inference: ${String.format("%.2f ms", summary.avgInferenceTimeMs)}
                  P95 Inference: ${summary.p95InferenceTimeMs} ms
                  Avg Confidence: ${String.format("%.2f%%", summary.avgConfidence * 100)}
                  
                Production Status: ${if (summary.meetsThresholds) "✅ PASS" else "❌ FAIL"}
                """.trimIndent()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error generating detailed stats")
            "Error: ${e.message}"
        }
    }
}
