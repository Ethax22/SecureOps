package com.secureops.app.ml.explainability

/**
 * Represents the contribution of a single feature to the model's prediction.
 * Used in SHAP-based explanations to show how each feature affects the outcome.
 */
data class FeatureContribution(
    /**
     * Name of the feature (e.g., "commit_size", "test_failure_rate")
     */
    val featureName: String,
    
    /**
     * The actual value of the feature for this prediction
     */
    val value: Float,
    
    /**
     * SHAP value: the marginal contribution of this feature to the prediction
     * Positive values increase failure risk, negative values decrease it
     */
    val shapValue: Float,
    
    /**
     * Impact classification of this feature
     */
    val impact: Impact
) {
    /**
     * Classification of feature impact on prediction
     */
    enum class Impact {
        POSITIVE,   // Increases failure risk
        NEGATIVE,   // Decreases failure risk
        NEUTRAL     // Minimal impact
    }
    
    /**
     * Returns the absolute magnitude of the contribution
     */
    val magnitude: Float
        get() = kotlin.math.abs(shapValue)
}
