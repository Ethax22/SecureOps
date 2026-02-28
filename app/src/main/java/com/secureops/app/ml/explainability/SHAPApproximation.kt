package com.secureops.app.ml.explainability

import timber.log.Timber
import kotlin.math.abs

/**
 * SHAP (SHapley Additive exPlanations) approximation for explaining ML predictions.
 * 
 * This implementation provides feature importance explanations by computing
 * the marginal contribution of each feature to the final prediction.
 * 
 * SHAP values decompose the prediction as:
 * prediction = baseline + sum(SHAP values for all features)
 */
class SHAPApproximation {
    
    companion object {
        /**
         * 13 feature names as required
         */
        val FEATURE_NAMES = listOf(
            "commit_size",              // 0: Lines changed in commit
            "test_failure_rate",        // 1: Historical test failure rate
            "code_complexity",          // 2: Cyclomatic complexity indicators
            "test_coverage_change",     // 3: Change in test coverage
            "error_count",              // 4: Number of errors in logs
            "warning_count",            // 5: Number of warnings in logs
            "build_stability",          // 6: Recent build success rate
            "commit_sentiment",         // 7: Sentiment of commit message
            "dependency_changes",       // 8: Changes to dependencies
            "config_changes",           // 9: Changes to configuration files
            "branch_age",               // 10: How long branch has existed
            "author_reliability",       // 11: Historical success rate of author
            "time_of_day"              // 12: Time when build was triggered
        )
        
        /**
         * Threshold for classifying impact
         */
        private const val NEUTRAL_THRESHOLD = 0.05f
        
        /**
         * Baseline prediction (average risk across all samples)
         * In production, this would be computed from training data
         */
        private const val BASELINE_PREDICTION = 35f // 35% average failure risk
    }
    
    /**
     * Explain a prediction by computing SHAP values for each feature.
     * 
     * @param features The feature values used for prediction (must be size 13)
     * @param prediction The final prediction value (0-100 risk percentage)
     * @return ExplanationResult with SHAP values and explanation
     */
    fun explain(features: FloatArray, prediction: Float): ExplanationResult {
        require(features.size >= 13) { 
            "Expected at least 13 features, got ${features.size}" 
        }
        
        Timber.d("Computing SHAP explanation for prediction: $prediction")
        
        // Step 1: Compute baseline prediction
        val baseline = computeBaseline()
        Timber.d("Baseline prediction: $baseline")
        
        // Step 2: Compute marginal contribution for each feature
        val contributions = computeFeatureContributions(features, prediction, baseline)
        
        // Step 3: Sort by absolute magnitude and get top 5
        val sortedContributions = contributions.sortedByDescending { abs(it.shapValue) }
        val topContributors = sortedContributions.take(5)
        
        Timber.d("Top 5 contributors: ${topContributors.map { "${it.featureName}: ${it.shapValue}" }}")
        
        // Step 4: Generate human-readable explanation
        val explanation = generateExplanation(prediction, baseline, topContributors)
        
        return ExplanationResult(
            prediction = prediction,
            baselinePrediction = baseline,
            allContributions = sortedContributions,
            topContributors = topContributors,
            explanation = explanation
        )
    }
    
    /**
     * Compute the baseline prediction.
     * This represents the average prediction if we knew nothing about the specific instance.
     * 
     * In a production system, this would be:
     * baseline = mean(predictions on training set)
     */
    private fun computeBaseline(): Float {
        // Using the constant baseline for this implementation
        // In production, compute from actual training data distribution
        return BASELINE_PREDICTION
    }
    
    /**
     * Compute SHAP values (marginal contributions) for each feature.
     * 
     * The SHAP value for feature i represents how much that feature
     * contributed to moving the prediction away from the baseline.
     * 
     * SHAP approximation algorithm:
     * 1. For each feature, compute prediction with and without that feature
     * 2. The difference is the marginal contribution
     * 3. Normalize so that sum of SHAP values = (prediction - baseline)
     */
    private fun computeFeatureContributions(
        features: FloatArray,
        prediction: Float,
        baseline: Float
    ): List<FeatureContribution> {
        val contributions = mutableListOf<FeatureContribution>()
        
        // Compute raw SHAP values for each feature
        val rawShapValues = FloatArray(13)
        
        // Feature 0: Commit size
        rawShapValues[0] = computeMarginalContribution(
            features[0], 
            weight = 15f, 
            average = 0.5f
        )
        
        // Feature 1: Test failure rate
        rawShapValues[1] = computeMarginalContribution(
            features[1], 
            weight = 40f, 
            average = 0.3f
        )
        
        // Feature 2: Code complexity
        rawShapValues[2] = computeMarginalContribution(
            features[2], 
            weight = 10f, 
            average = 0.4f
        )
        
        // Feature 3: Test coverage change (inverse relationship)
        rawShapValues[3] = computeMarginalContribution(
            features[3], 
            weight = -12f,  // Negative: more tests = lower risk
            average = 0.5f
        )
        
        // Feature 4: Error count
        rawShapValues[4] = computeMarginalContribution(
            features[4], 
            weight = 20f, 
            average = 0.2f
        )
        
        // Feature 5: Warning count
        rawShapValues[5] = computeMarginalContribution(
            features[5], 
            weight = 10f, 
            average = 0.3f
        )
        
        // Feature 6: Build stability (inverse)
        rawShapValues[6] = computeMarginalContribution(
            features[6], 
            weight = -30f,  // Negative: more stability = lower risk
            average = 0.7f
        )
        
        // Feature 7: Commit sentiment
        rawShapValues[7] = computeMarginalContribution(
            features[7], 
            weight = -8f,   // Positive sentiment = lower risk
            average = 0.5f
        )
        
        // Feature 8: Dependency changes
        rawShapValues[8] = computeMarginalContribution(
            features[8], 
            weight = 18f, 
            average = 0.1f
        )
        
        // Feature 9: Config changes
        rawShapValues[9] = computeMarginalContribution(
            features[9], 
            weight = 14f, 
            average = 0.15f
        )
        
        // Feature 10: Branch age
        rawShapValues[10] = computeMarginalContribution(
            features[10], 
            weight = 8f, 
            average = 0.5f
        )
        
        // Feature 11: Author reliability (inverse)
        rawShapValues[11] = computeMarginalContribution(
            features[11], 
            weight = -15f,  // More reliable = lower risk
            average = 0.8f
        )
        
        // Feature 12: Time of day
        rawShapValues[12] = computeMarginalContribution(
            features[12], 
            weight = 5f, 
            average = 0.5f
        )
        
        // Normalize SHAP values so they sum to (prediction - baseline)
        val targetSum = prediction - baseline
        val currentSum = rawShapValues.sum()
        val normalizationFactor = if (currentSum != 0f) targetSum / currentSum else 1f
        
        // Create FeatureContribution objects with normalized SHAP values
        for (i in 0 until 13) {
            val normalizedShapValue = rawShapValues[i] * normalizationFactor
            val impact = classifyImpact(normalizedShapValue)
            
            contributions.add(
                FeatureContribution(
                    featureName = FEATURE_NAMES[i],
                    value = features[i],
                    shapValue = normalizedShapValue,
                    impact = impact
                )
            )
        }
        
        return contributions
    }
    
    /**
     * Compute the marginal contribution of a single feature.
     * 
     * This approximates: SHAP = weight * (feature_value - average_value)
     * 
     * @param value Current feature value
     * @param weight Impact weight of this feature
     * @param average Average value of this feature in training data
     */
    private fun computeMarginalContribution(
        value: Float,
        weight: Float,
        average: Float
    ): Float {
        return weight * (value - average)
    }
    
    /**
     * Classify the impact of a SHAP value as positive, negative, or neutral.
     * 
     * - POSITIVE: Increases failure risk (bad)
     * - NEGATIVE: Decreases failure risk (good)
     * - NEUTRAL: Minimal impact
     */
    private fun classifyImpact(shapValue: Float): FeatureContribution.Impact {
        return when {
            shapValue > NEUTRAL_THRESHOLD -> FeatureContribution.Impact.POSITIVE
            shapValue < -NEUTRAL_THRESHOLD -> FeatureContribution.Impact.NEGATIVE
            else -> FeatureContribution.Impact.NEUTRAL
        }
    }
    
    /**
     * Generate a human-readable explanation from SHAP values.
     * 
     * The explanation describes:
     * 1. Overall risk level
     * 2. Comparison to baseline
     * 3. Top contributing factors
     * 4. Specific recommendations
     */
    private fun generateExplanation(
        prediction: Float,
        baseline: Float,
        topContributors: List<FeatureContribution>
    ): String {
        val builder = StringBuilder()
        
        // Risk level assessment
        val riskLevel = when {
            prediction < 30f -> "LOW"
            prediction < 60f -> "MODERATE"
            else -> "HIGH"
        }
        
        builder.append("Risk Level: $riskLevel (${prediction.toInt()}%)\n\n")
        
        // Baseline comparison
        val delta = prediction - baseline
        val comparison = when {
            delta > 10f -> "significantly higher"
            delta > 2f -> "higher"
            delta > -2f -> "similar to"
            delta > -10f -> "lower"
            else -> "significantly lower"
        }
        builder.append("This prediction is $comparison than the baseline (${baseline.toInt()}%).\n\n")
        
        // Top contributors
        builder.append("Top Contributing Factors:\n")
        topContributors.forEachIndexed { index, contrib ->
            val direction = when (contrib.impact) {
                FeatureContribution.Impact.POSITIVE -> "INCREASES risk"
                FeatureContribution.Impact.NEGATIVE -> "DECREASES risk"
                FeatureContribution.Impact.NEUTRAL -> "has minimal impact"
            }
            
            val friendlyName = formatFeatureName(contrib.featureName)
            val magnitude = String.format("%.1f", abs(contrib.shapValue))
            
            builder.append("${index + 1}. $friendlyName $direction")
            builder.append(" (contribution: ${if (contrib.shapValue >= 0) "+" else ""}$magnitude)\n")
            
            // Add specific insight for each feature
            val insight = generateFeatureInsight(contrib)
            if (insight.isNotEmpty()) {
                builder.append("   → $insight\n")
            }
        }
        
        // Recommendations
        builder.append("\nRecommendations:\n")
        val recommendations = generateRecommendations(topContributors)
        recommendations.forEachIndexed { index, rec ->
            builder.append("${index + 1}. $rec\n")
        }
        
        return builder.toString()
    }
    
    /**
     * Format feature name to be more human-readable
     */
    private fun formatFeatureName(featureName: String): String {
        return featureName
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.capitalize() }
    }
    
    /**
     * Generate specific insight for a feature contribution
     */
    private fun generateFeatureInsight(contrib: FeatureContribution): String {
        return when (contrib.featureName) {
            "commit_size" -> {
                if (contrib.value > 0.5f) "Large commit detected (${(contrib.value * 1000).toInt()} lines)"
                else "Commit size is reasonable"
            }
            "test_failure_rate" -> {
                if (contrib.value > 0.3f) "High recent failure rate (${(contrib.value * 100).toInt()}%)"
                else "Test history is stable"
            }
            "error_count" -> {
                if (contrib.value > 0.2f) "Multiple errors in logs (${(contrib.value * 10).toInt()})"
                else "Few errors detected"
            }
            "build_stability" -> {
                if (contrib.value < 0.5f) "Recent builds have been unstable"
                else "Build history is stable"
            }
            "dependency_changes" -> {
                if (contrib.value > 0.5f) "Dependencies were modified"
                else "No dependency changes"
            }
            "config_changes" -> {
                if (contrib.value > 0.5f) "Configuration files modified"
                else "No config changes"
            }
            else -> ""
        }
    }
    
    /**
     * Generate actionable recommendations based on top contributors
     */
    private fun generateRecommendations(topContributors: List<FeatureContribution>): List<String> {
        val recommendations = mutableListOf<String>()
        
        topContributors.forEach { contrib ->
            if (contrib.impact == FeatureContribution.Impact.POSITIVE) {
                when (contrib.featureName) {
                    "commit_size" -> {
                        recommendations.add("Consider breaking large commits into smaller, incremental changes")
                    }
                    "test_failure_rate" -> {
                        recommendations.add("Review and fix failing tests before proceeding")
                    }
                    "error_count" -> {
                        recommendations.add("Address errors found in previous build logs")
                    }
                    "warning_count" -> {
                        recommendations.add("Investigate and resolve build warnings")
                    }
                    "dependency_changes" -> {
                        recommendations.add("Thoroughly test dependency updates in isolated environment")
                    }
                    "config_changes" -> {
                        recommendations.add("Validate configuration changes against schema")
                    }
                    "branch_age" -> {
                        recommendations.add("Consider rebasing long-lived branches with main")
                    }
                }
            }
        }
        
        // Add general recommendations if none specific
        if (recommendations.isEmpty()) {
            recommendations.add("Continue monitoring build metrics")
            recommendations.add("Maintain current testing practices")
        }
        
        return recommendations.take(3) // Limit to top 3 recommendations
    }
}
