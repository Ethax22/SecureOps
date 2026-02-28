package com.secureops.app.ml.explainability

/**
 * Complete explanation result for a model prediction.
 * Contains SHAP values, top contributors, and human-readable explanation.
 */
data class ExplanationResult(
    /**
     * The final prediction value (risk percentage 0-100)
     */
    val prediction: Float,
    
    /**
     * Baseline prediction (average prediction across all training samples)
     */
    val baselinePrediction: Float,
    
    /**
     * All feature contributions sorted by absolute magnitude
     */
    val allContributions: List<FeatureContribution>,
    
    /**
     * Top 5 most important feature contributions
     */
    val topContributors: List<FeatureContribution>,
    
    /**
     * Human-readable explanation text
     */
    val explanation: String,
    
    /**
     * Timestamp when this explanation was generated
     */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Summary statistics about the contributions
     */
    val contributionStats: ContributionStats get() = ContributionStats(
        positiveCount = allContributions.count { it.impact == FeatureContribution.Impact.POSITIVE },
        negativeCount = allContributions.count { it.impact == FeatureContribution.Impact.NEGATIVE },
        neutralCount = allContributions.count { it.impact == FeatureContribution.Impact.NEUTRAL },
        totalMagnitude = allContributions.sumOf { it.magnitude.toDouble() }.toFloat()
    )
    
    data class ContributionStats(
        val positiveCount: Int,
        val negativeCount: Int,
        val neutralCount: Int,
        val totalMagnitude: Float
    )
}
