package com.secureops.app.ml.benchmark

import com.secureops.app.ml.FailurePredictionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class ValidationMetrics(
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val truePositives: Int,
    val falsePositives: Int,
    val trueNegatives: Int,
    val falseNegatives: Int
)

/**
 * ModelValidator
 *
 * Evaluates ML model quality using a synthetic balanced test set:
 * - Precision, Recall, F1 Score, Accuracy
 */
class ModelValidator(
    private val model: FailurePredictionModel
) {
    companion object {
        private const val SAMPLE_SIZE = 200
        private const val FAILURE_THRESHOLD = 50f  // percentage threshold
    }

    suspend fun validateModel(): ValidationMetrics = withContext(Dispatchers.Default) {
        Timber.i("Running ML model validation...")

        var truePositives = 0
        var falsePositives = 0
        var trueNegatives = 0
        var falseNegatives = 0

        repeat(SAMPLE_SIZE) { i ->
            val isActualFailure = i % 2 == 0  // balanced 50/50

            val commitDiff: String
            val testHistory: List<Boolean>
            val logs: String

            if (isActualFailure) {
                // Construct inputs that should trigger a high failure score
                commitDiff = buildString {
                    repeat(600) { appendLine("+  val x$it = null // risky change") }
                }
                testHistory = List(10) { it < 7 }  // 30% success = 70% failures
                logs = "ERROR: OutOfMemoryError detected. Timeout exceeded. flaky test found."
            } else {
                // Construct inputs that should yield a low failure score
                commitDiff = "+  val label = \"minor fix\"\n-  val label = \"old\""
                testHistory = List(10) { true }   // 100% success
                logs = "Build successful. All checks passed."
            }

            val (riskPct, _) = model.predictFailure(commitDiff, testHistory, logs)
            val isPredictedFailure = riskPct > FAILURE_THRESHOLD

            when {
                isActualFailure  && isPredictedFailure  -> truePositives++
                isActualFailure  && !isPredictedFailure -> falseNegatives++
                !isActualFailure && isPredictedFailure  -> falsePositives++
                else                                    -> trueNegatives++
            }
        }

        val accuracy  = (truePositives + trueNegatives).toDouble() / SAMPLE_SIZE
        val precision = if (truePositives + falsePositives > 0)
            truePositives.toDouble() / (truePositives + falsePositives) else 0.0
        val recall    = if (truePositives + falseNegatives > 0)
            truePositives.toDouble() / (truePositives + falseNegatives) else 0.0
        val f1Score   = if (precision + recall > 0)
            2 * (precision * recall) / (precision + recall) else 0.0

        Timber.i("Validation → Acc=${"%.3f".format(accuracy)}, P=${"%.3f".format(precision)}, R=${"%.3f".format(recall)}, F1=${"%.3f".format(f1Score)}")

        return@withContext ValidationMetrics(
            accuracy = accuracy,
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            truePositives = truePositives,
            falsePositives = falsePositives,
            trueNegatives = trueNegatives,
            falseNegatives = falseNegatives
        )
    }
}
