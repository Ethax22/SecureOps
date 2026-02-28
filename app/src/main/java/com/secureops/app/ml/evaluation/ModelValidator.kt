package com.secureops.app.ml.evaluation

import com.secureops.app.domain.model.Pipeline
import com.secureops.app.ml.FailurePredictionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelValidator(private val model: FailurePredictionModel) {
    suspend fun validateModel(testDataset: List<Pair<Pipeline, Boolean>>): ValidationMetrics = withContext(Dispatchers.Default) {
        var truePositives = 0
        var falsePositives = 0
        var trueNegatives = 0
        var falseNegatives = 0
        
        for ((pipeline, actualFailure) in testDataset) {
            val (riskScore, _) = model.predictFailure(pipeline.commitMessage, emptyList(), pipeline.logs ?: "")
            val predictedFailure = riskScore > 50f
            
            if (predictedFailure && actualFailure) truePositives++
            if (predictedFailure && !actualFailure) falsePositives++
            if (!predictedFailure && !actualFailure) trueNegatives++
            if (!predictedFailure && actualFailure) falseNegatives++
        }
        
        val accuracy = (truePositives + trueNegatives).toFloat() / testDataset.size.coerceAtLeast(1)
        val precision = truePositives.toFloat() / (truePositives + falsePositives).coerceAtLeast(1)
        val recall = truePositives.toFloat() / (truePositives + falseNegatives).coerceAtLeast(1)
        val f1Score = if (precision + recall > 0) 2 * (precision * recall) / (precision + recall) else 0f
        
        ValidationMetrics(accuracy, precision, recall, f1Score)
    }
}

data class ValidationMetrics(
    val accuracy: Float,
    val precision: Float,
    val recall: Float,
    val f1Score: Float
)
