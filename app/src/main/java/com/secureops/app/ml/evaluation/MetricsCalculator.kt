package com.secureops.app.ml.evaluation

import com.secureops.app.data.local.entity.BuildEvaluationEntity
import timber.log.Timber
import kotlin.math.max

/**
 * Calculates ML model performance metrics from evaluation data
 * Computes confusion matrix, precision, recall, F1 score, and accuracy
 */
class MetricsCalculator {
    
    /**
     * Confusion matrix with derived metrics
     */
    data class ConfusionMatrix(
        val truePositive: Int,      // Predicted failure, was failure
        val falsePositive: Int,     // Predicted failure, was success
        val trueNegative: Int,      // Predicted success, was success
        val falseNegative: Int      // Predicted success, was failure
    ) {
        val total: Int get() = truePositive + falsePositive + trueNegative + falseNegative
        
        /**
         * Precision: Of all predicted failures, how many were actually failures?
         * TP / (TP + FP)
         */
        val precision: Float get() = 
            if (truePositive + falsePositive == 0) 0f
            else truePositive.toFloat() / (truePositive + falsePositive)
        
        /**
         * Recall (Sensitivity): Of all actual failures, how many did we catch?
         * TP / (TP + FN)
         */
        val recall: Float get() = 
            if (truePositive + falseNegative == 0) 0f
            else truePositive.toFloat() / (truePositive + falseNegative)
        
        /**
         * F1 Score: Harmonic mean of precision and recall
         * 2 * (precision * recall) / (precision + recall)
         */
        val f1Score: Float get() = 
            if (precision + recall == 0f) 0f
            else 2 * (precision * recall) / (precision + recall)
        
        /**
         * Accuracy: Overall correctness
         * (TP + TN) / total
         */
        val accuracy: Float get() = 
            if (total == 0) 0f
            else (truePositive + trueNegative).toFloat() / total
        
        /**
         * Specificity (True Negative Rate): Of all actual successes, how many did we correctly predict?
         * TN / (TN + FP)
         */
        val specificity: Float get() =
            if (trueNegative + falsePositive == 0) 0f
            else trueNegative.toFloat() / (trueNegative + falsePositive)
        
        /**
         * False Positive Rate
         * FP / (FP + TN)
         */
        val falsePositiveRate: Float get() =
            if (falsePositive + trueNegative == 0) 0f
            else falsePositive.toFloat() / (falsePositive + trueNegative)
        
        /**
         * False Negative Rate
         * FN / (FN + TP)
         */
        val falseNegativeRate: Float get() =
            if (falseNegative + truePositive == 0) 0f
            else falseNegative.toFloat() / (falseNegative + truePositive)
        
        /**
         * Positive Predictive Value (same as precision)
         */
        val positivePredictiveValue: Float get() = precision
        
        /**
         * Negative Predictive Value
         * TN / (TN + FN)
         */
        val negativePredictiveValue: Float get() =
            if (trueNegative + falseNegative == 0) 0f
            else trueNegative.toFloat() / (trueNegative + falseNegative)
        
        /**
         * Matthews Correlation Coefficient
         * MCC = (TP*TN - FP*FN) / sqrt((TP+FP)(TP+FN)(TN+FP)(TN+FN))
         */
        val matthewsCorrelationCoefficient: Float get() {
            val numerator = (truePositive * trueNegative - falsePositive * falseNegative).toDouble()
            val denominator = kotlin.math.sqrt(
                (truePositive + falsePositive).toDouble() *
                (truePositive + falseNegative).toDouble() *
                (trueNegative + falsePositive).toDouble() *
                (trueNegative + falseNegative).toDouble()
            )
            
            return if (denominator == 0.0) 0f else (numerator / denominator).toFloat()
        }
        
        /**
         * Check if model meets production thresholds
         */
        fun meetsProductionThresholds(): Boolean {
            return precision >= 0.85f && recall >= 0.80f && f1Score >= 0.82f
        }
        
        override fun toString(): String {
            return """
                ConfusionMatrix(
                  TP=$truePositive, FP=$falsePositive
                  FN=$falseNegative, TN=$trueNegative
                  Precision=${String.format("%.2f%%", precision * 100)}
                  Recall=${String.format("%.2f%%", recall * 100)}
                  F1=${String.format("%.2f%%", f1Score * 100)}
                  Accuracy=${String.format("%.2f%%", accuracy * 100)}
                )
            """.trimIndent()
        }
    }
    
    /**
     * Calculate confusion matrix from evaluation data
     * 
     * @param evaluations List of evaluations where actualLabel is not null
     * @return ConfusionMatrix with all metrics
     */
    fun calculateMetrics(evaluations: List<BuildEvaluationEntity>): ConfusionMatrix {
        var tp = 0
        var fp = 0
        var tn = 0
        var fn = 0
        
        evaluations.forEach { eval ->
            // Skip evaluations without actual labels
            if (eval.actualLabel == null) {
                Timber.w("Skipping evaluation ${eval.buildId} - actualLabel is null")
                return@forEach
            }
            
            when {
                // Predicted failure (1), was failure (1) → True Positive
                eval.predictedLabel == 1 && eval.actualLabel == 1 -> tp++
                
                // Predicted failure (1), was success (0) → False Positive
                eval.predictedLabel == 1 && eval.actualLabel == 0 -> fp++
                
                // Predicted success (0), was success (0) → True Negative
                eval.predictedLabel == 0 && eval.actualLabel == 0 -> tn++
                
                // Predicted success (0), was failure (1) → False Negative
                eval.predictedLabel == 0 && eval.actualLabel == 1 -> fn++
            }
        }
        
        val matrix = ConfusionMatrix(tp, fp, tn, fn)
        
        Timber.i("Confusion Matrix calculated: $matrix")
        
        return matrix
    }
    
    /**
     * Calculate average inference time from evaluations
     * 
     * @return Average inference time in milliseconds
     */
    fun calculateAverageInferenceTime(evaluations: List<BuildEvaluationEntity>): Double {
        if (evaluations.isEmpty()) return 0.0
        return evaluations.map { it.inferenceTimeMs }.average()
    }
    
    /**
     * Calculate average confidence score
     * 
     * @return Average confidence (0.0 to 1.0)
     */
    fun calculateAverageConfidence(evaluations: List<BuildEvaluationEntity>): Float {
        if (evaluations.isEmpty()) return 0f
        return evaluations.map { it.confidenceScore }.average().toFloat()
    }
    
    /**
     * Calculate percentile of inference times
     * 
     * @param percentile Percentile to calculate (e.g., 95 for 95th percentile)
     * @return Inference time at given percentile in milliseconds
     */
    fun calculateInferenceTimePercentile(
        evaluations: List<BuildEvaluationEntity>,
        percentile: Int
    ): Long {
        if (evaluations.isEmpty()) return 0L
        
        val sortedTimes = evaluations.map { it.inferenceTimeMs }.sorted()
        val index = ((percentile / 100.0) * sortedTimes.size).toInt()
        val clampedIndex = max(0, index.coerceAtMost(sortedTimes.size - 1))
        
        return sortedTimes[clampedIndex]
    }
    
    /**
     * Calculate metrics summary for display
     */
    data class MetricsSummary(
        val confusionMatrix: ConfusionMatrix,
        val avgInferenceTimeMs: Double,
        val avgConfidence: Float,
        val totalEvaluations: Int,
        val p95InferenceTimeMs: Long,
        val meetsThresholds: Boolean
    )
    
    /**
     * Calculate comprehensive metrics summary
     */
    fun calculateSummary(evaluations: List<BuildEvaluationEntity>): MetricsSummary {
        val validEvaluations = evaluations.filter { it.actualLabel != null }
        
        if (validEvaluations.isEmpty()) {
            return MetricsSummary(
                confusionMatrix = ConfusionMatrix(0, 0, 0, 0),
                avgInferenceTimeMs = 0.0,
                avgConfidence = 0f,
                totalEvaluations = 0,
                p95InferenceTimeMs = 0L,
                meetsThresholds = false
            )
        }
        
        val confusionMatrix = calculateMetrics(validEvaluations)
        
        return MetricsSummary(
            confusionMatrix = confusionMatrix,
            avgInferenceTimeMs = calculateAverageInferenceTime(evaluations),
            avgConfidence = calculateAverageConfidence(validEvaluations),
            totalEvaluations = validEvaluations.size,
            p95InferenceTimeMs = calculateInferenceTimePercentile(evaluations, 95),
            meetsThresholds = confusionMatrix.meetsProductionThresholds()
        )
    }
}
