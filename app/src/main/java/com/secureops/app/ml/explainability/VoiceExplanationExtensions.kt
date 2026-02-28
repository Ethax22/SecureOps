package com.secureops.app.ml.explainability

import kotlin.math.abs

/**
 * Voice-optimized extension for ExplanationResult
 * Converts SHAP explanation to TTS-friendly format keeping response under 30 seconds
 */

/**
 * Convert ExplanationResult to voice-friendly explanation
 * 
 * Optimized for TTS with:
 * - Clear, natural language
 * - Concise sentences
 * - <30 seconds speaking time (~75 words max at 150 WPM)
 * - No technical jargon
 * 
 * @param includeBaseline Whether to include baseline comparison (default: true)
 * @param maxContributors Number of top contributors to mention (default: 3)
 * @return TTS-optimized explanation string
 */
fun ExplanationResult.toVoiceExplanation(
    includeBaseline: Boolean = true,
    maxContributors: Int = 3
): String {
    val builder = StringBuilder()
    
    // 1. Risk level summary (5-8 words)
    val riskLevel = when {
        prediction < 30f -> "low"
        prediction < 60f -> "moderate"
        else -> "high"
    }
    
    builder.append("This build has $riskLevel risk at ${prediction.toInt()} percent. ")
    
    // 2. Baseline comparison (optional, 6-10 words)
    if (includeBaseline) {
        val delta = prediction - baselinePrediction
        val comparison = when {
            abs(delta) < 5f -> "This is about average. "
            delta > 0 -> "That's higher than usual. "
            else -> "That's better than average. "
        }
        builder.append(comparison)
    }
    
    // 3. Top contributors (20-40 words for top 3)
    if (topContributors.isNotEmpty()) {
        val topN = topContributors.take(maxContributors)
        
        when {
            topN.size == 1 -> {
                builder.append("The main factor is ")
                builder.append(formatContributorForVoice(topN[0]))
                builder.append(". ")
            }
            topN.size == 2 -> {
                builder.append("The key factors are ")
                builder.append(formatContributorForVoice(topN[0]))
                builder.append(" and ")
                builder.append(formatContributorForVoice(topN[1]))
                builder.append(". ")
            }
            else -> {
                builder.append("The main factors are: ")
                topN.forEachIndexed { index, contrib ->
                    when (index) {
                        0 -> {
                            builder.append(formatContributorForVoice(contrib))
                        }
                        topN.size - 1 -> {
                            builder.append(", and ")
                            builder.append(formatContributorForVoice(contrib))
                        }
                        else -> {
                            builder.append(", ")
                            builder.append(formatContributorForVoice(contrib))
                        }
                    }
                }
                builder.append(". ")
            }
        }
    }
    
    // 4. Quick recommendation (10-15 words)
    val recommendation = generateVoiceRecommendation(topContributors.firstOrNull())
    if (recommendation.isNotEmpty()) {
        builder.append(recommendation)
    }
    
    return builder.toString().trim()
}

/**
 * Format a single feature contribution for voice
 * Returns natural language description of the contribution
 */
private fun formatContributorForVoice(contribution: FeatureContribution): String {
    val featureName = contribution.featureName
    val impact = contribution.impact
    
    // Create human-friendly description
    val description = when (featureName) {
        "commit_size" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "large commit size"
                FeatureContribution.Impact.NEGATIVE -> "small commit size"
                else -> "commit size"
            }
        }
        "test_failure_rate" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "high test failure rate"
                FeatureContribution.Impact.NEGATIVE -> "low test failure rate"
                else -> "test failure rate"
            }
        }
        "code_complexity" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "high code complexity"
                FeatureContribution.Impact.NEGATIVE -> "low code complexity"
                else -> "code complexity"
            }
        }
        "test_coverage_change" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "reduced test coverage"
                FeatureContribution.Impact.NEGATIVE -> "improved test coverage"
                else -> "test coverage changes"
            }
        }
        "error_count" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "errors in logs"
                FeatureContribution.Impact.NEGATIVE -> "clean logs"
                else -> "log errors"
            }
        }
        "warning_count" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "many warnings"
                FeatureContribution.Impact.NEGATIVE -> "few warnings"
                else -> "warning count"
            }
        }
        "build_stability" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "unstable build history"
                FeatureContribution.Impact.NEGATIVE -> "stable build history"
                else -> "build stability"
            }
        }
        "commit_sentiment" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "incomplete commit"
                FeatureContribution.Impact.NEGATIVE -> "clean commit"
                else -> "commit quality"
            }
        }
        "dependency_changes" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "dependency updates"
                FeatureContribution.Impact.NEGATIVE -> "stable dependencies"
                else -> "dependencies"
            }
        }
        "config_changes" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "configuration changes"
                FeatureContribution.Impact.NEGATIVE -> "stable configuration"
                else -> "configuration"
            }
        }
        "branch_age" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "old branch"
                FeatureContribution.Impact.NEGATIVE -> "fresh branch"
                else -> "branch age"
            }
        }
        "author_reliability" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "unreliable author history"
                FeatureContribution.Impact.NEGATIVE -> "reliable author"
                else -> "author history"
            }
        }
        "time_of_day" -> {
            when (impact) {
                FeatureContribution.Impact.POSITIVE -> "off-hours deployment"
                FeatureContribution.Impact.NEGATIVE -> "business hours deployment"
                else -> "deployment timing"
            }
        }
        else -> {
            // Fallback: convert snake_case to readable format
            featureName.replace("_", " ")
        }
    }
    
    return description
}

/**
 * Generate a quick actionable recommendation based on top contributor
 * Keep it to 10-15 words maximum
 */
private fun generateVoiceRecommendation(topContributor: FeatureContribution?): String {
    if (topContributor == null || topContributor.impact != FeatureContribution.Impact.POSITIVE) {
        return ""
    }
    
    return when (topContributor.featureName) {
        "commit_size" -> "Consider breaking this into smaller commits."
        "test_failure_rate" -> "Fix failing tests before deploying."
        "error_count" -> "Review and fix the errors in the logs."
        "warning_count" -> "Address the warnings to improve stability."
        "dependency_changes" -> "Test dependency updates carefully."
        "config_changes" -> "Double check your configuration changes."
        "build_stability" -> "Wait for more stable build history."
        "branch_age" -> "Consider rebasing with the main branch."
        else -> ""
    }
}

/**
 * Generate a very brief summary (10-15 words) for quick TTS
 * Perfect for notifications or quick checks
 */
fun ExplanationResult.toQuickVoiceSummary(): String {
    val riskLevel = when {
        prediction < 30f -> "low"
        prediction < 60f -> "moderate"
        else -> "high"
    }
    
    val topFactor = topContributors.firstOrNull()
    
    return if (topFactor != null) {
        "$riskLevel risk. Main factor: ${formatContributorForVoice(topFactor)}."
    } else {
        "$riskLevel risk at ${prediction.toInt()} percent."
    }
}

/**
 * Estimate speaking time in seconds for the explanation
 * Assumes ~150 words per minute speaking rate
 * 
 * @return Estimated TTS duration in seconds
 */
fun ExplanationResult.estimateSpeakingTime(): Int {
    val voiceText = this.toVoiceExplanation()
    val wordCount = voiceText.split(Regex("\\s+")).size
    val wordsPerMinute = 150
    return ((wordCount.toFloat() / wordsPerMinute) * 60).toInt()
}
