package com.secureops.app.ml

import android.content.Context
import com.google.gson.Gson
import com.secureops.app.data.local.dao.BuildEvaluationDao
import com.secureops.app.data.local.entity.BuildEvaluationEntity
import com.secureops.app.domain.model.Pipeline
import com.secureops.app.ml.explainability.ExplanationResult
import com.secureops.app.ml.explainability.SHAPApproximation
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ML model for predicting CI/CD pipeline failures
 */
class FailurePredictionModel(
    private val context: Context,
    private val buildEvaluationDao: BuildEvaluationDao? = null,
    private val gson: Gson? = null
) {
    private var interpreter: Interpreter? = null
    private val modelInputSize = 512
    
    // Performance optimization: Cache SHAP explainer instance
    private val shapExplainer: SHAPApproximation by lazy { 
        SHAPApproximation()
    }

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            // In a real implementation, this would load a trained TensorFlow Lite model
            // For now, we'll use a simulated model
            Timber.d("Failure prediction model initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load ML model")
        }
    }

    /**
     * Predicts the failure likelihood for a pipeline build
     *
     * @param commitDiff The diff of the commit
     * @param testHistory Historical test results
     * @param logs Pipeline logs
     * @return Pair of risk percentage (0-100) and confidence (0-1)
     */
    fun predictFailure(
        commitDiff: String,
        testHistory: List<Boolean>,
        logs: String
    ): Pair<Float, Float> {
        try {
            // Extract features from inputs
            val features = extractFeatures(commitDiff, testHistory, logs)

            // Run inference
            val result = runInference(features)

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error during failure prediction")
            return Pair(0f, 0f)
        }
    }

    /**
     * Identifies causal factors for potential failure
     */
    fun identifyCausalFactors(
        commitDiff: String,
        testHistory: List<Boolean>,
        logs: String
    ): List<String> {
        val factors = mutableListOf<String>()

        // Analyze commit diff
        if (commitDiff.contains("TODO") || commitDiff.contains("FIXME")) {
            factors.add("Incomplete code (TODO/FIXME found)")
        }
        if (commitDiff.lines().size > 500) {
            factors.add("Large commit size (${commitDiff.lines().size} lines)")
        }
        if (commitDiff.contains("test", ignoreCase = true) && commitDiff.contains("-")) {
            factors.add("Test coverage reduction detected")
        }

        // Analyze test history
        val recentFailures = testHistory.takeLast(10).count { !it }
        if (recentFailures >= 3) {
            factors.add("Unstable build history ($recentFailures recent failures)")
        }

        // Analyze logs
        if (logs.contains("OutOfMemoryError", ignoreCase = true)) {
            factors.add("Memory issues detected in logs")
        }
        if (logs.contains("timeout", ignoreCase = true)) {
            factors.add("Timeout issues in previous builds")
        }
        if (logs.contains("flaky", ignoreCase = true)) {
            factors.add("Flaky test patterns detected")
        }

        return factors
    }

    private fun extractFeatures(
        commitDiff: String,
        testHistory: List<Boolean>,
        logs: String
    ): FloatArray {
        val features = FloatArray(10)

        // Feature 1: Commit size
        features[0] = commitDiff.lines().size.toFloat() / 1000f

        // Feature 2: Test history failure rate
        features[1] = if (testHistory.isNotEmpty()) {
            testHistory.count { !it }.toFloat() / testHistory.size
        } else 0f

        // Feature 3: Code complexity indicators
        features[2] = commitDiff.count { it == '{' }.toFloat() / 100f

        // Feature 4: Test coverage change
        features[3] = if (commitDiff.contains("test", ignoreCase = true)) 1f else 0f

        // Feature 5: Error patterns in logs
        features[4] = logs.split("error", ignoreCase = true).size.toFloat() / 10f

        // Feature 6: Warning patterns
        features[5] = logs.split("warning", ignoreCase = true).size.toFloat() / 20f

        // Feature 7: Recent build stability
        features[6] = testHistory.takeLast(5).count { it }.toFloat() / 5f

        // Feature 8: Commit message sentiment (simplified)
        features[7] = if (commitDiff.contains("fix", ignoreCase = true)) 0.8f else 0.5f

        // Feature 9: Dependencies change
        features[8] = if (commitDiff.contains("dependencies", ignoreCase = true)) 1f else 0f

        // Feature 9: Configuration changes
        features[9] = if (commitDiff.contains(".yml") || commitDiff.contains(".yaml")) 1f else 0f

        return features
    }
    
    /**
     * Extract enhanced features for SHAP explanation (13 features)
     * 
     * @param commitDiff The diff of the commit
     * @param testHistory Historical test results
     * @param logs Pipeline logs
     * @param pipeline Optional pipeline for additional metadata
     * @return FloatArray with 13 features
     */
    private fun extractEnhancedFeatures(
        commitDiff: String,
        testHistory: List<Boolean>,
        logs: String,
        pipeline: Pipeline? = null
    ): FloatArray {
        val features = FloatArray(13)
        
        // Feature 0: Commit size (normalized to 0-1 range, 1000 lines = 1.0)
        features[0] = (commitDiff.lines().size.toFloat() / 1000f).coerceIn(0f, 1f)
        
        // Feature 1: Test failure rate (0-1)
        features[1] = if (testHistory.isNotEmpty()) {
            testHistory.count { !it }.toFloat() / testHistory.size
        } else 0.3f // Default to 30% if no history
        
        // Feature 2: Code complexity (based on braces count)
        features[2] = (commitDiff.count { it == '{' }.toFloat() / 100f).coerceIn(0f, 1f)
        
        // Feature 3: Test coverage change (inverse: 1 = adding tests, 0 = no tests)
        features[3] = if (commitDiff.contains("test", ignoreCase = true)) {
            if (commitDiff.contains("+test") || commitDiff.contains("+ test")) 1f else 0.5f
        } else 0f
        
        // Feature 4: Error count (normalized)
        features[4] = (logs.split("error", ignoreCase = true).size.toFloat() / 10f).coerceIn(0f, 1f)
        
        // Feature 5: Warning count (normalized)
        features[5] = (logs.split("warning", ignoreCase = true).size.toFloat() / 20f).coerceIn(0f, 1f)
        
        // Feature 6: Build stability (recent success rate, 0-1)
        features[6] = if (testHistory.isNotEmpty()) {
            testHistory.takeLast(5).count { it }.toFloat() / 5f
        } else 0.7f // Default to 70% stability
        
        // Feature 7: Commit sentiment (0 = negative, 1 = positive)
        features[7] = when {
            commitDiff.contains("fix", ignoreCase = true) -> 0.8f
            commitDiff.contains("refactor", ignoreCase = true) -> 0.6f
            commitDiff.contains("wip", ignoreCase = true) -> 0.3f
            else -> 0.5f
        }
        
        // Feature 8: Dependency changes (binary)
        features[8] = if (commitDiff.contains("dependencies", ignoreCase = true) ||
                         commitDiff.contains("package.json") ||
                         commitDiff.contains("build.gradle") ||
                         commitDiff.contains("pom.xml")) 1f else 0f
        
        // Feature 9: Configuration changes (binary)
        features[9] = if (commitDiff.contains(".yml") || 
                         commitDiff.contains(".yaml") ||
                         commitDiff.contains(".json") ||
                         commitDiff.contains(".xml")) 1f else 0f
        
        // Feature 10: Branch age (simulated, 0-1 where 1 = very old)
        features[10] = pipeline?.let {
            // Use branch name to estimate age (feature branch vs main)
            when {
                it.branch.contains("feature") -> 0.6f
                it.branch.contains("develop") -> 0.3f
                it.branch == "main" || it.branch == "master" -> 0.1f
                else -> 0.5f
            }
        } ?: 0.5f
        
        // Feature 11: Author reliability (simulated, higher = more reliable)
        features[11] = if (testHistory.isNotEmpty()) {
            // Recent success rate as proxy for author reliability
            testHistory.takeLast(10).count { it }.toFloat() / 10f
        } else 0.8f // Default to 80% reliability
        
        // Feature 12: Time of day effect (simulated, 0-1)
        features[12] = pipeline?.let {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when (hour) {
                in 9..17 -> 0.3f  // Business hours - lower risk
                in 18..22 -> 0.5f // Evening - medium risk
                else -> 0.7f      // Night/early morning - higher risk
            }
        } ?: 0.5f
        
        return features
    }

    private fun runInference(features: FloatArray): Pair<Float, Float> {
        // Simulated ML inference
        // In production, this would use the TensorFlow Lite interpreter

        // Calculate risk score based on features
        var riskScore = 0f
        var confidence = 0.85f

        // Weight the features
        riskScore += features[0] * 15f // Commit size
        riskScore += features[1] * 40f // Test history
        riskScore += features[2] * 10f // Complexity
        riskScore += features[4] * 20f // Errors
        riskScore += features[5] * 10f // Warnings
        riskScore += (1f - features[6]) * 30f // Instability

        // Normalize to 0-100
        riskScore = riskScore.coerceIn(0f, 100f)

        // Adjust confidence based on data quality
        if (features[1] == 0f) confidence *= 0.7f // Low confidence without history

        return Pair(riskScore, confidence)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    /**
     * Predict failure with evaluation tracking
     * Wraps prediction with latency measurement and stores evaluation record
     * 
     * @param pipeline Pipeline to predict
     * @return PredictionResult with risk score, confidence, and causal factors
     */
    suspend fun predictWithEvaluation(pipeline: Pipeline): PredictionResult {
        // Start timing
        val startTime = System.nanoTime()
        
        // Run prediction
        val (riskPercentage, confidence) = predictFailure(
            commitDiff = pipeline.commitMessage,
            testHistory = emptyList(), // TODO: Fetch from history
            logs = pipeline.logs ?: ""
        )
        
        val causalFactors = identifyCausalFactors(
            commitDiff = pipeline.commitMessage,
            testHistory = emptyList(),
            logs = pipeline.logs ?: ""
        )
        
        // End timing
        val endTime = System.nanoTime()
        val inferenceTimeMs = (endTime - startTime) / 1_000_000
        
        Timber.d("Prediction for ${pipeline.id}: risk=$riskPercentage%, confidence=$confidence, latency=${inferenceTimeMs}ms")
        
        // Store evaluation if DAO is available
        buildEvaluationDao?.let { dao ->
            try {
                val predictedLabel = if (riskPercentage > 50f) 1 else 0
                
                val evaluation = BuildEvaluationEntity(
                    buildId = pipeline.id,
                    predictedLabel = predictedLabel,
                    actualLabel = null, // Not yet known
                    predictionRiskScore = riskPercentage / 100f,
                    confidenceScore = confidence,
                    inferenceTimeMs = inferenceTimeMs,
                    features = gson?.toJson(mapOf(
                        "commitSize" to pipeline.commitMessage.length,
                        "branch" to pipeline.branch,
                        "repository" to pipeline.repositoryName
                    )) ?: "{}",
                    predictedAt = System.currentTimeMillis(),
                    evaluatedAt = null
                )
                
                dao.insert(evaluation)
                Timber.i("Evaluation stored for build ${pipeline.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to store evaluation for build ${pipeline.id}")
            }
        }
        
        return PredictionResult(
            riskPercentage = riskPercentage,
            confidence = confidence,
            causalFactors = causalFactors,
            inferenceTimeMs = inferenceTimeMs,
            explanation = null  // No explanation in basic prediction
        )
    }
    
    /**
     * Predict failure with SHAP-based explanation
     * 
     * This method provides explainable predictions using SHAP values to show
     * how each feature contributes to the final prediction.
     * 
     * Performance optimized:
     * - Uses lazy-initialized cached SHAP explainer
     * - Single feature extraction pass
     * - Minimal overhead over standard prediction
     * 
     * @param pipeline Pipeline to predict
     * @return PredictionResult with risk score, confidence, causal factors, and SHAP explanation
     */
    suspend fun predictWithExplanation(pipeline: Pipeline): PredictionResult {
        // Start timing
        val startTime = System.nanoTime()
        
        try {
            // Extract enhanced features (13 features for SHAP)
            val enhancedFeatures = extractEnhancedFeatures(
                commitDiff = pipeline.commitMessage,
                testHistory = emptyList(), // TODO: Fetch from history
                logs = pipeline.logs ?: "",
                pipeline = pipeline
            )
            
            // Run inference using enhanced features
            val (riskPercentage, confidence) = runInferenceEnhanced(enhancedFeatures)
            
            // Generate SHAP explanation (performance optimized with cached explainer)
            val explanation = shapExplainer.explain(enhancedFeatures, riskPercentage)
            
            // Get causal factors (backward compatible)
            val causalFactors = explanation.topContributors
                .filter { it.shapValue > 0 } // Only include factors that INCREASE risk
                .take(3)
                .map { "${it.featureName} INCREASES risk (contribution: ${"%.1f".format(it.shapValue)})" }
                
            // If we don't have enough data from SHAP, fallback to static analysis
            val mergedFactors = if (causalFactors.size < 2) {
                (causalFactors + identifyCausalFactors(
                    commitDiff = pipeline.commitMessage,
                    testHistory = emptyList(),
                    logs = pipeline.logs ?: ""
                )).distinct().take(3)
            } else {
                causalFactors
            }
            
            // End timing
            val endTime = System.nanoTime()
            val inferenceTimeMs = (endTime - startTime) / 1_000_000
            
            Timber.d("Prediction with explanation for ${pipeline.id}: " +
                    "risk=$riskPercentage%, confidence=$confidence, " +
                    "latency=${inferenceTimeMs}ms, " +
                    "top_contributor=${explanation.topContributors.firstOrNull()?.featureName}")
            
            // Store evaluation if DAO is available
            buildEvaluationDao?.let { dao ->
                try {
                    val predictedLabel = if (riskPercentage > 50f) 1 else 0
                    
                    val evaluation = BuildEvaluationEntity(
                        buildId = pipeline.id,
                        predictedLabel = predictedLabel,
                        actualLabel = null,
                        predictionRiskScore = riskPercentage / 100f,
                        confidenceScore = confidence,
                        inferenceTimeMs = inferenceTimeMs,
                        features = gson?.toJson(mapOf(
                            "commitSize" to pipeline.commitMessage.length,
                            "branch" to pipeline.branch,
                            "repository" to pipeline.repositoryName,
                            "topContributor" to explanation.topContributors.firstOrNull()?.featureName,
                            "shapValues" to explanation.topContributors.take(3).associate { 
                                it.featureName to it.shapValue 
                            }
                        )) ?: "{}",
                        predictedAt = System.currentTimeMillis(),
                        evaluatedAt = null
                    )
                    
                    dao.insert(evaluation)
                    Timber.i("Evaluation with explanation stored for build ${pipeline.id}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to store evaluation for build ${pipeline.id}")
                }
            }
            
            return PredictionResult(
                riskPercentage = riskPercentage,
                confidence = confidence,
                causalFactors = mergedFactors,
                inferenceTimeMs = inferenceTimeMs,
                explanation = explanation
            )
        } catch (e: Exception) {
            Timber.e(e, "Error during prediction with explanation")
            
            // Fallback to basic prediction
            return predictWithEvaluation(pipeline)
        }
    }
    
    /**
     * Run inference using enhanced 13-feature set
     * Performance optimized to reuse feature weights
     */
    private fun runInferenceEnhanced(features: FloatArray): Pair<Float, Float> {
        // Simulated ML inference with 13 features
        // In production, this would use the TensorFlow Lite interpreter
        
        // Base risk score (critical to prevent extensive negative weight 
        // subtractions from clamping the final calculation to 0%)
        var riskScore = 45f
        var confidence = 0.85f
        
        // Weight the features (aligned with SHAP weights)
        riskScore += features[0] * 15f   // Commit size
        riskScore += features[1] * 40f   // Test failure rate
        riskScore += features[2] * 10f   // Code complexity
        riskScore -= features[3] * 12f   // Test coverage (inverse)
        riskScore += features[4] * 20f   // Error count
        riskScore += features[5] * 10f   // Warning count
        riskScore -= features[6] * 30f   // Build stability (inverse)
        riskScore -= features[7] * 8f    // Commit sentiment (inverse)
        riskScore += features[8] * 18f   // Dependency changes
        riskScore += features[9] * 14f   // Config changes
        riskScore += features[10] * 8f   // Branch age
        riskScore -= features[11] * 15f  // Author reliability (inverse)
        riskScore += features[12] * 5f   // Time of day
        
        // Normalize to 0-100
        riskScore = riskScore.coerceIn(0f, 100f)
        
        // Adjust confidence based on data quality
        if (features[1] < 0.1f) confidence *= 0.8f  // Low confidence without much history
        if (features[11] < 0.5f) confidence *= 0.9f // Lower confidence with unreliable author
        
        return Pair(riskScore, confidence)
    }

    /**
     * Result of a prediction with evaluation tracking
     * 
     * @param riskPercentage Predicted failure risk (0-100)
     * @param confidence Model confidence in prediction (0-1)
     * @param causalFactors List of identified risk factors
     * @param inferenceTimeMs Time taken for inference in milliseconds
     * @param explanation Optional SHAP-based explanation (null for basic predictions)
     */
    data class PredictionResult(
        val riskPercentage: Float,
        val confidence: Float,
        val causalFactors: List<String>,
        val inferenceTimeMs: Long,
        val explanation: ExplanationResult? = null
    ) {
        /**
         * Check if this result includes an explanation
         */
        val hasExplanation: Boolean
            get() = explanation != null
            
        /**
         * Get a summary of the top contributing factor
         */
        val topContributorSummary: String?
            get() = explanation?.topContributors?.firstOrNull()?.let { contrib ->
                "${contrib.featureName}: ${if (contrib.shapValue >= 0) "+" else ""}%.2f".format(contrib.shapValue)
            }
    }
}
