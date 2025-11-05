package com.secureops.app.ml.advanced

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.math.sqrt

class FlakyTestDetector(
    private val pipelineDao: PipelineDao
) {

    /**
     * Detect flaky tests across all repositories
     */
    suspend fun detectFlakyTests(
        repository: String? = null,
        minRuns: Int = 10
    ): List<FlakyTest> {
        Timber.d("Detecting flaky tests for repository: $repository")

        val allPipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }
            .let { pipelines ->
                if (repository != null) {
                    pipelines.filter { it.repositoryName == repository }
                } else {
                    pipelines
                }
            }

        // Group by repository and branch
        val groupedPipelines = allPipelines
            .groupBy { "${it.repositoryName}:${it.branch}" }

        val flakyTests = mutableListOf<FlakyTest>()

        groupedPipelines.forEach { (key, pipelines) ->
            if (pipelines.size < minRuns) return@forEach

            val analysis = analyzeTestStability(key, pipelines)
            if (analysis.isFlaky) {
                flakyTests.add(analysis)
            }
        }

        return flakyTests.sortedByDescending { it.flakinessScore }
    }

    /**
     * Analyze test stability for a specific test/pipeline group
     */
    private fun analyzeTestStability(key: String, pipelines: List<Pipeline>): FlakyTest {
        val (repository, branch) = key.split(":")

        // Calculate statistics
        val totalRuns = pipelines.size
        val failures = pipelines.count { it.status == BuildStatus.FAILURE }
        val successes = pipelines.count { it.status == BuildStatus.SUCCESS }
        val failureRate = failures.toFloat() / totalRuns

        // Check for alternating patterns
        val hasAlternatingPattern = detectAlternatingPattern(pipelines)

        // Check for intermittent failures
        val hasIntermittentFailures = detectIntermittentFailures(pipelines)

        // Calculate flakiness score (0-100)
        val flakinessScore = calculateFlakinessScore(
            failureRate = failureRate,
            hasAlternating = hasAlternatingPattern,
            hasIntermittent = hasIntermittentFailures,
            consecutivePatterns = detectConsecutivePatterns(pipelines)
        )

        // Determine if flaky
        val isFlaky = flakinessScore >= FLAKINESS_THRESHOLD

        // Identify likely causes
        val causes = identifyFlakinessPatterns(pipelines)

        // Calculate confidence
        val confidence = calculateConfidence(totalRuns, flakinessScore)

        return FlakyTest(
            repository = repository,
            branch = branch,
            totalRuns = totalRuns,
            failures = failures,
            successes = successes,
            failureRate = failureRate,
            flakinessScore = flakinessScore,
            isFlaky = isFlaky,
            confidence = confidence,
            patterns = causes,
            recommendation = generateRecommendation(flakinessScore, causes),
            lastFailure = pipelines.lastOrNull { it.status == BuildStatus.FAILURE }?.finishedAt
        )
    }

    /**
     * Detect alternating success/failure pattern
     */
    private fun detectAlternatingPattern(pipelines: List<Pipeline>): Boolean {
        if (pipelines.size < 4) return false

        val sorted = pipelines.sortedBy { it.startedAt }
        var alternations = 0

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i].status
            val next = sorted[i + 1].status

            if (current != next) {
                alternations++
            }
        }

        // If >60% of transitions are alternating, it's likely flaky
        val alternationRate = alternations.toFloat() / (sorted.size - 1)
        return alternationRate > 0.6f
    }

    /**
     * Detect intermittent failures (failures surrounded by successes)
     */
    private fun detectIntermittentFailures(pipelines: List<Pipeline>): Boolean {
        if (pipelines.size < 5) return false

        val sorted = pipelines.sortedBy { it.startedAt }
        var intermittentCount = 0

        for (i in 1 until sorted.size - 1) {
            val prev = sorted[i - 1].status
            val current = sorted[i].status
            val next = sorted[i + 1].status

            // Failure surrounded by successes
            if (current == BuildStatus.FAILURE &&
                prev == BuildStatus.SUCCESS &&
                next == BuildStatus.SUCCESS
            ) {
                intermittentCount++
            }
        }

        return intermittentCount >= 2
    }

    /**
     * Detect consecutive patterns
     */
    private fun detectConsecutivePatterns(pipelines: List<Pipeline>): Int {
        val sorted = pipelines.sortedBy { it.startedAt }
        var maxConsecutive = 0
        var currentConsecutive = 1

        for (i in 1 until sorted.size) {
            if (sorted[i].status == sorted[i - 1].status) {
                currentConsecutive++
            } else {
                maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
                currentConsecutive = 1
            }
        }

        return maxOf(maxConsecutive, currentConsecutive)
    }

    /**
     * Calculate flakiness score (0-100)
     */
    private fun calculateFlakinessScore(
        failureRate: Float,
        hasAlternating: Boolean,
        hasIntermittent: Boolean,
        consecutivePatterns: Int
    ): Float {
        var score = 0f

        // Failure rate in the "flaky zone" (10-90%)
        if (failureRate in 0.1f..0.9f) {
            score += 40f
        }

        // Alternating pattern is a strong indicator
        if (hasAlternating) {
            score += 30f
        }

        // Intermittent failures
        if (hasIntermittent) {
            score += 20f
        }

        // Low consecutive patterns indicate instability
        if (consecutivePatterns < 3) {
            score += 10f
        }

        return score.coerceIn(0f, 100f)
    }

    /**
     * Calculate confidence based on sample size
     */
    private fun calculateConfidence(totalRuns: Int, flakinessScore: Float): Float {
        // More runs = higher confidence
        val sampleConfidence = when {
            totalRuns >= 50 -> 0.95f
            totalRuns >= 30 -> 0.85f
            totalRuns >= 20 -> 0.75f
            totalRuns >= 10 -> 0.65f
            else -> 0.50f
        }

        // Higher flakiness score = higher confidence in flakiness
        val scoreConfidence = flakinessScore / 100f

        return (sampleConfidence + scoreConfidence) / 2
    }

    /**
     * Identify specific patterns in test failures
     */
    private fun identifyFlakinessPatterns(pipelines: List<Pipeline>): List<FlakinessPattern> {
        val patterns = mutableListOf<FlakinessPattern>()

        // Check for time-based patterns
        val timePattern = analyzeTimeBasedPattern(pipelines)
        if (timePattern != null) patterns.add(timePattern)

        // Check for environment patterns
        val envPattern = analyzeEnvironmentPattern(pipelines)
        if (envPattern != null) patterns.add(envPattern)

        return patterns
    }

    /**
     * Analyze time-based patterns (e.g., failures during certain hours)
     */
    private fun analyzeTimeBasedPattern(pipelines: List<Pipeline>): FlakinessPattern? {
        // Simplified - in production, analyze day-of-week, time-of-day patterns
        return null
    }

    /**
     * Analyze environment-based patterns
     */
    private fun analyzeEnvironmentPattern(pipelines: List<Pipeline>): FlakinessPattern? {
        // Check if failures correlate with specific providers
        val failuresByProvider = pipelines
            .filter { it.status == BuildStatus.FAILURE }
            .groupBy { it.provider }

        val totalFailures = pipelines.count { it.status == BuildStatus.FAILURE }
        if (totalFailures == 0) return null

        failuresByProvider.forEach { (provider, failures) ->
            val providerFailureRate = failures.size.toFloat() / totalFailures
            if (providerFailureRate > 0.7f) {
                return FlakinessPattern(
                    type = PatternType.ENVIRONMENT,
                    description = "Failures concentrated on ${provider.displayName}",
                    confidence = providerFailureRate
                )
            }
        }

        return null
    }

    /**
     * Generate recommendation based on flakiness analysis
     */
    private fun generateRecommendation(score: Float, patterns: List<FlakinessPattern>): String {
        return when {
            score >= 80f -> "🚨 CRITICAL: Quarantine this test immediately. It's highly unreliable."
            score >= 60f -> "⚠️ HIGH: Investigate and fix this test. Consider retrying logic."
            score >= 40f -> "⚡ MEDIUM: Monitor closely. May need stabilization."
            else -> "✅ LOW: Test appears stable but shows some variation."
        }
    }

    companion object {
        private const val FLAKINESS_THRESHOLD = 40f
    }
}

/**
 * Flaky test analysis result
 */
data class FlakyTest(
    val repository: String,
    val branch: String,
    val totalRuns: Int,
    val failures: Int,
    val successes: Int,
    val failureRate: Float,
    val flakinessScore: Float,
    val isFlaky: Boolean,
    val confidence: Float,
    val patterns: List<FlakinessPattern>,
    val recommendation: String,
    val lastFailure: Long?
)

data class FlakinessPattern(
    val type: PatternType,
    val description: String,
    val confidence: Float
)

enum class PatternType {
    ALTERNATING,
    INTERMITTENT,
    TIME_BASED,
    ENVIRONMENT,
    LOAD_DEPENDENT
}
