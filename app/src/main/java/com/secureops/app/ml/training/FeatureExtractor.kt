package com.secureops.app.ml.training

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.PipelineEntity
import timber.log.Timber
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Extracts and normalizes 13 features from pipeline data for ML training
 */
class FeatureExtractor(
    private val pipelineDao: PipelineDao
) {
    companion object {
        // Feature indices
        const val FEATURE_COMMIT_SIZE = 0
        const val FEATURE_TEST_FAILURE_RATE = 1
        const val FEATURE_CODE_COMPLEXITY = 2
        const val FEATURE_TEST_COVERAGE_CHANGE = 3
        const val FEATURE_ERROR_PATTERN_COUNT = 4
        const val FEATURE_WARNING_COUNT = 5
        const val FEATURE_BUILD_STABILITY = 6
        const val FEATURE_COMMIT_SENTIMENT = 7
        const val FEATURE_DEPENDENCY_CHANGES = 8
        const val FEATURE_CONFIG_CHANGES = 9
        const val FEATURE_HOUR_OF_DAY = 10
        const val FEATURE_DAY_OF_WEEK = 11
        const val FEATURE_AUTHOR_FAILURE_RATE = 12

        // Normalization constants
        private const val MAX_COMMIT_SIZE = 10000f
        private const val MAX_ERROR_PATTERNS = 50f
        private const val MAX_WARNING_COUNT = 100f
    }

    /**
     * Extract all 13 features for a given pipeline
     */
    suspend fun extractFeatures(pipeline: PipelineEntity): FloatArray {
        val features = FloatArray(13)

        features[FEATURE_COMMIT_SIZE] = extractCommitSize(pipeline)
        features[FEATURE_TEST_FAILURE_RATE] = extractTestFailureRate(pipeline)
        features[FEATURE_CODE_COMPLEXITY] = extractCodeComplexity(pipeline)
        features[FEATURE_TEST_COVERAGE_CHANGE] = extractCoverageChange(pipeline)
        features[FEATURE_ERROR_PATTERN_COUNT] = extractErrorPatternCount(pipeline)
        features[FEATURE_WARNING_COUNT] = extractWarningCount(pipeline)
        features[FEATURE_BUILD_STABILITY] = extractBuildStability(pipeline)
        features[FEATURE_COMMIT_SENTIMENT] = extractCommitSentiment(pipeline)
        features[FEATURE_DEPENDENCY_CHANGES] = extractDependencyChanges(pipeline)
        features[FEATURE_CONFIG_CHANGES] = extractConfigChanges(pipeline)
        features[FEATURE_HOUR_OF_DAY] = extractHourOfDay(pipeline)
        features[FEATURE_DAY_OF_WEEK] = extractDayOfWeek(pipeline)
        features[FEATURE_AUTHOR_FAILURE_RATE] = extractAuthorFailureRate(pipeline)

        return features
    }

    /**
     * Feature 0: Commit size (normalized 0-1)
     * Estimates lines changed based on commit message patterns
     */
    private fun extractCommitSize(pipeline: PipelineEntity): Float {
        val message = pipeline.commitMessage.lowercase()
        
        // Heuristic: estimate size based on keywords
        val estimatedSize = when {
            message.contains("refactor") || message.contains("rewrite") -> 500f
            message.contains("add") || message.contains("implement") -> 300f
            message.contains("fix") || message.contains("patch") -> 100f
            message.contains("update") || message.contains("modify") -> 200f
            message.contains("remove") || message.contains("delete") -> 150f
            message.contains("wip") || message.contains("minor") -> 50f
            else -> 150f // Default
        }

        // If commit message has "+" indicators, adjust
        val plusMatches = Regex("\\+(\\d+)").find(message)
        val actualSize = plusMatches?.groupValues?.get(1)?.toFloatOrNull() ?: estimatedSize

        return min(actualSize / MAX_COMMIT_SIZE, 1f)
    }

    /**
     * Feature 1: Historical test failure rate for this repository (0-1)
     */
    private suspend fun extractTestFailureRate(pipeline: PipelineEntity): Float {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        
        val failures = pipelineDao.getFailureCount(pipeline.repositoryName, thirtyDaysAgo)
        val successes = pipelineDao.getSuccessCount(pipeline.repositoryName, thirtyDaysAgo)
        
        val total = failures + successes
        if (total == 0) return 0.1f // Default for new repos
        
        return failures.toFloat() / total
    }

    /**
     * Feature 2: Code complexity (0-1)
     * Based on commit message indicators
     */
    private fun extractCodeComplexity(pipeline: PipelineEntity): Float {
        val message = pipeline.commitMessage.lowercase()
        val logs = pipeline.logs?.lowercase() ?: ""

        var complexity = 0.5f // Default

        // Increase complexity based on keywords
        if (message.contains("refactor") || message.contains("restructure")) complexity += 0.2f
        if (message.contains("algorithm") || message.contains("optimization")) complexity += 0.15f
        if (message.contains("api") || message.contains("integration")) complexity += 0.1f
        if (message.contains("database") || message.contains("migration")) complexity += 0.15f
        
        // Count bracket depth as proxy for complexity
        val bracketCount = logs.count { it == '{' || it == '[' }
        complexity += min(bracketCount / 1000f, 0.2f)

        return min(complexity, 1f)
    }

    /**
     * Feature 3: Test coverage change (normalized -1 to 1, then scaled to 0-1)
     */
    private fun extractCoverageChange(pipeline: PipelineEntity): Float {
        val logs = pipeline.logs ?: return 0.5f // Neutral

        // Parse coverage from logs
        val coverageRegex = Regex("coverage[:\\s]+(\\d+)%", RegexOption.IGNORE_CASE)
        val match = coverageRegex.find(logs)
        
        if (match != null) {
            val coverage = match.groupValues[1].toFloatOrNull() ?: 80f
            // Assume previous coverage was 80%, calculate change
            val change = (coverage - 80f) / 100f // Range: -0.8 to 0.2
            return (change + 1f) / 2f // Scale to 0-1
        }

        return 0.5f // Neutral if not found
    }

    /**
     * Feature 4: Error pattern count in logs (normalized 0-1)
     */
    private fun extractErrorPatternCount(pipeline: PipelineEntity): Float {
        val logs = pipeline.logs ?: return 0f

        val errorPatterns = listOf(
            Regex("\\berror\\b", RegexOption.IGNORE_CASE),
            Regex("\\bfailed\\b", RegexOption.IGNORE_CASE),
            Regex("\\bexception\\b", RegexOption.IGNORE_CASE),
            Regex("\\bfatal\\b", RegexOption.IGNORE_CASE),
            Regex("\\bcrash\\b", RegexOption.IGNORE_CASE)
        )

        val count = errorPatterns.sumOf { pattern ->
            pattern.findAll(logs).count()
        }.toFloat()

        return min(count / MAX_ERROR_PATTERNS, 1f)
    }

    /**
     * Feature 5: Warning count in logs (normalized 0-1)
     */
    private fun extractWarningCount(pipeline: PipelineEntity): Float {
        val logs = pipeline.logs ?: return 0f

        val warningCount = Regex("\\bwarn(ing)?\\b", RegexOption.IGNORE_CASE)
            .findAll(logs)
            .count()
            .toFloat()

        return min(warningCount / MAX_WARNING_COUNT, 1f)
    }

    /**
     * Feature 6: Build stability - recent success rate for this branch (0-1)
     */
    private suspend fun extractBuildStability(pipeline: PipelineEntity): Float {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        
        val failures = pipelineDao.getFailureCount(pipeline.repositoryName, sevenDaysAgo)
        val successes = pipelineDao.getSuccessCount(pipeline.repositoryName, sevenDaysAgo)
        
        val total = failures + successes
        if (total == 0) return 0.8f // Assume stable for new repos
        
        return successes.toFloat() / total
    }

    /**
     * Feature 7: Commit sentiment (0-1)
     * 0 = negative, 0.5 = neutral, 1 = positive
     */
    private fun extractCommitSentiment(pipeline: PipelineEntity): Float {
        val message = pipeline.commitMessage.lowercase()

        val positiveWords = listOf("fix", "improve", "optimize", "enhance", "add", "complete")
        val negativeWords = listOf("wip", "temp", "hack", "todo", "broken", "debug", "attempt")

        val positiveCount = positiveWords.count { message.contains(it) }
        val negativeCount = negativeWords.count { message.contains(it) }

        return when {
            positiveCount > negativeCount -> 0.7f
            negativeCount > positiveCount -> 0.3f
            else -> 0.5f
        }
    }

    /**
     * Feature 8: Dependency changes detected (0-1)
     */
    private fun extractDependencyChanges(pipeline: PipelineEntity): Float {
        val message = pipeline.commitMessage.lowercase()
        val logs = pipeline.logs?.lowercase() ?: ""

        val dependencyKeywords = listOf(
            "package.json", "build.gradle", "pom.xml", "requirements.txt",
            "dependency", "dependencies", "upgrade", "downgrade", "npm install"
        )

        val hasChanges = dependencyKeywords.any { 
            message.contains(it) || logs.contains(it)
        }

        return if (hasChanges) 1f else 0f
    }

    /**
     * Feature 9: Configuration changes detected (0-1)
     */
    private fun extractConfigChanges(pipeline: PipelineEntity): Float {
        val message = pipeline.commitMessage.lowercase()

        val configKeywords = listOf(
            "config", "configuration", ".env", "settings", "properties",
            ".yml", ".yaml", ".json", "dockerfile"
        )

        val hasChanges = configKeywords.any { message.contains(it) }

        return if (hasChanges) 1f else 0f
    }

    /**
     * Feature 10: Hour of day when build started (normalized 0-1)
     */
    private fun extractHourOfDay(pipeline: PipelineEntity): Float {
        val startTime = pipeline.startedAt ?: System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour / 24f
    }

    /**
     * Feature 11: Day of week (normalized 0-1)
     * Monday = 0, Sunday = 6
     */
    private fun extractDayOfWeek(pipeline: PipelineEntity): Float {
        val startTime = pipeline.startedAt ?: System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
        return dayOfWeek / 6f
    }

    /**
     * Feature 12: Author's historical failure rate (0-1)
     */
    private suspend fun extractAuthorFailureRate(pipeline: PipelineEntity): Float {
        // Since we don't have author-specific queries, use repository rate as proxy
        // In production, this should query by commitAuthor
        return extractTestFailureRate(pipeline)
    }

    /**
     * Get feature names for CSV headers
     */
    fun getFeatureNames(): List<String> = listOf(
        "commit_size",
        "test_failure_rate",
        "code_complexity",
        "test_coverage_change",
        "error_pattern_count",
        "warning_count",
        "build_stability",
        "commit_sentiment",
        "dependency_changes",
        "config_changes",
        "hour_of_day",
        "day_of_week",
        "author_failure_rate"
    )
}
