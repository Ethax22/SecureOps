package com.secureops.app.data.remediation

import com.secureops.app.data.local.dao.RemediationHistoryDao
import com.secureops.app.data.local.entity.RemediationHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * Remediation Learner
 * 
 * Learns from historical remediation outcomes to recommend the best actions
 * Uses a ranking formula: 50% successRate + 30% confidence + 20% timeFactor
 */
@Singleton
class RemediationLearner @Inject constructor(
    private val remediationHistoryDao: RemediationHistoryDao
) {
    
    /**
     * Remediation recommendation with ranking score
     */
    data class RemediationRecommendation(
        val actionType: String,
        val actionDescription: String,
        val score: Double,
        val successRate: Double,
        val confidence: Double,
        val avgDurationMs: Long,
        val totalAttempts: Int,
        val successfulAttempts: Int,
        val reasoning: String
    )
    
    /**
     * Action performance metrics
     */
    private data class ActionMetrics(
        val actionType: String,
        val successRate: Double,
        val avgConfidence: Double,
        val avgDurationMs: Long,
        val totalAttempts: Int,
        val successfulAttempts: Int
    )
    
    /**
     * Get top 3 remediation recommendations for a failure type
     * 
     * Ranking Formula:
     * score = 0.50 * successRate + 0.30 * confidence + 0.20 * timeFactor
     * 
     * @param failureType The type of failure
     * @param failurePattern Optional specific pattern
     * @return Top 3 recommendations sorted by score
     */
    suspend fun getTopRecommendations(
        failureType: String,
        failurePattern: String? = null
    ): List<RemediationRecommendation> = withContext(Dispatchers.IO) {
        try {
            val history = if (failurePattern != null) {
                remediationHistoryDao.getHistoryByPattern(failurePattern)
            } else {
                remediationHistoryDao.getHistoryByFailureType(failureType)
                    .let { flow ->
                        // Convert Flow to List for processing
                        val list = mutableListOf<RemediationHistoryEntity>()
                        flow.collect { list.addAll(it) }
                        list
                    }
            }
            
            if (history.isEmpty()) {
                Timber.d("No historical data for failure type: $failureType")
                return@withContext emptyList()
            }
            
            // Group by action type and calculate metrics
            val actionMetrics = calculateActionMetrics(history)
            
            // Calculate scores and rank
            val recommendations = actionMetrics.map { metrics ->
                val score = calculateRankingScore(metrics)
                
                RemediationRecommendation(
                    actionType = metrics.actionType,
                    actionDescription = generateActionDescription(metrics.actionType),
                    score = score,
                    successRate = metrics.successRate,
                    confidence = metrics.avgConfidence,
                    avgDurationMs = metrics.avgDurationMs,
                    totalAttempts = metrics.totalAttempts,
                    successfulAttempts = metrics.successfulAttempts,
                    reasoning = generateReasoning(metrics, score)
                )
            }
            
            // Return top 3 recommendations
            recommendations
                .sortedByDescending { it.score }
                .take(3)
                .also { top3 ->
                    Timber.i("Top 3 recommendations for $failureType:")
                    top3.forEachIndexed { index, rec ->
                        Timber.i("  ${index + 1}. ${rec.actionType} (score: ${String.format("%.3f", rec.score)})")
                    }
                }
            
        } catch (e: Exception) {
            Timber.e(e, "Error getting recommendations for failure type: $failureType")
            emptyList()
        }
    }
    
    /**
     * Calculate metrics for each action type
     */
    private fun calculateActionMetrics(
        history: List<RemediationHistoryEntity>
    ): List<ActionMetrics> {
        return history
            .groupBy { it.actionTaken }
            .filter { (_, records) -> records.size >= 2 } // Require at least 2 attempts
            .map { (actionType, records) ->
                val successful = records.count { it.wasSuccessful }
                val successRate = successful.toDouble() / records.size
                
                val avgConfidence = records.map { it.confidenceScore }.average()
                val avgDuration = records
                    .filter { it.wasSuccessful } // Only successful attempts
                    .map { it.durationMs }
                    .average()
                    .toLong()
                
                ActionMetrics(
                    actionType = actionType,
                    successRate = successRate,
                    avgConfidence = avgConfidence,
                    avgDurationMs = avgDuration,
                    totalAttempts = records.size,
                    successfulAttempts = successful
                )
            }
    }
    
    /**
     * Calculate ranking score using the formula:
     * score = 0.50 * successRate + 0.30 * confidence + 0.20 * timeFactor
     * 
     * Where:
     * - successRate: 0.0 to 1.0 (percentage of successful remediations)
     * - confidence: 0.0 to 1.0 (average confidence when action was taken)
     * - timeFactor: 0.0 to 1.0 (faster is better, normalized)
     */
    private fun calculateRankingScore(metrics: ActionMetrics): Double {
        // Success rate component (50%)
        val successRateComponent = metrics.successRate * 0.50
        
        // Confidence component (30%)
        val confidenceComponent = metrics.avgConfidence * 0.30
        
        // Time factor component (20%)
        // Use exponential decay: faster actions score higher
        // Normalize to typical duration range (0-30 minutes = 0-1800000ms)
        val maxDurationMs = 1_800_000.0 // 30 minutes
        val normalizedDuration = (metrics.avgDurationMs / maxDurationMs).coerceIn(0.0, 1.0)
        val timeFactor = 1.0 - normalizedDuration // Inverse: faster is better
        val timeComponent = timeFactor * 0.20
        
        // Total score
        return successRateComponent + confidenceComponent + timeComponent
    }
    
    /**
     * Generate human-readable action description
     */
    private fun generateActionDescription(actionType: String): String {
        return when (actionType) {
            "RETRY_BUILD" -> "Retry the build without changes"
            "SKIP_FLAKY_TESTS" -> "Skip flaky tests and continue"
            "INCREASE_TIMEOUT" -> "Increase timeout limits"
            "INCREASE_RESOURCES" -> "Increase memory/CPU resources"
            "CLEAR_CACHE" -> "Clear build cache and retry"
            "UPDATE_DEPENDENCIES" -> "Update outdated dependencies"
            "ROLLBACK_DEPLOYMENT" -> "Rollback to previous version"
            "RESTART_SERVICES" -> "Restart dependent services"
            "RERUN_SPECIFIC_TESTS" -> "Rerun only failed tests"
            "MANUAL_INTERVENTION" -> "Require manual review and fix"
            else -> actionType.replace("_", " ").lowercase().capitalize()
        }
    }
    
    /**
     * Generate reasoning explanation for the recommendation
     */
    private fun generateReasoning(metrics: ActionMetrics, score: Double): String {
        val successPercent = (metrics.successRate * 100).toInt()
        val durationMin = metrics.avgDurationMs / 60_000
        
        return buildString {
            append("This action has a ${successPercent}% success rate ")
            append("based on ${metrics.totalAttempts} previous attempts. ")
            
            when {
                metrics.successRate >= 0.8 -> append("Highly reliable. ")
                metrics.successRate >= 0.6 -> append("Moderately reliable. ")
                else -> append("Limited reliability, use with caution. ")
            }
            
            if (durationMin < 5) {
                append("Fast execution (${durationMin}min avg). ")
            } else if (durationMin < 15) {
                append("Moderate execution time (${durationMin}min avg). ")
            } else {
                append("Longer execution time (${durationMin}min avg). ")
            }
            
            append("Overall score: ${String.format("%.3f", score)}.")
        }
    }
    
    /**
     * Record a remediation attempt
     */
    suspend fun recordAttempt(
        pipelineId: String,
        buildNumber: Int,
        repositoryName: String,
        failureType: String,
        failurePattern: String,
        actionTaken: String,
        actionDescription: String,
        confidenceScore: Double,
        wasUserApproved: Boolean,
        approvedBy: String?,
        accountId: String,
        failureContext: String? = null,
        logSnippet: String? = null
    ): Long = withContext(Dispatchers.IO) {
        try {
            val history = RemediationHistoryEntity(
                pipelineId = pipelineId,
                buildNumber = buildNumber,
                repositoryName = repositoryName,
                failureType = failureType,
                failurePattern = failurePattern,
                actionTaken = actionTaken,
                actionDescription = actionDescription,
                wasSuccessful = false, // Will be updated later
                outcome = "PENDING",
                remediatedBuildNumber = null,
                durationMs = 0, // Will be calculated
                confidenceScore = confidenceScore,
                errorMessage = null,
                failureContext = failureContext,
                logSnippet = logSnippet,
                wasUserApproved = wasUserApproved,
                approvedBy = approvedBy,
                attemptedAt = System.currentTimeMillis(),
                completedAt = null,
                accountId = accountId
            )
            
            val id = remediationHistoryDao.insert(history)
            Timber.d("Recorded remediation attempt: $id")
            id
        } catch (e: Exception) {
            Timber.e(e, "Error recording remediation attempt")
            -1L
        }
    }
    
    /**
     * Update remediation outcome
     */
    suspend fun recordOutcome(
        historyId: Long,
        wasSuccessful: Boolean,
        outcome: String,
        remediatedBuildNumber: Int?,
        errorMessage: String?,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        try {
            remediationHistoryDao.updateOutcome(
                historyId = historyId,
                wasSuccessful = wasSuccessful,
                outcome = outcome,
                completedAt = System.currentTimeMillis(),
                remediatedBuildNumber = remediatedBuildNumber,
                errorMessage = errorMessage
            )
            
            Timber.i("Updated remediation outcome: $historyId -> ${if (wasSuccessful) "SUCCESS" else "FAILURE"}")
        } catch (e: Exception) {
            Timber.e(e, "Error updating remediation outcome")
        }
    }
    
    /**
     * Get success rate for a specific action on a failure type
     */
    suspend fun getSuccessRate(
        failureType: String,
        actionType: String
    ): Double = withContext(Dispatchers.IO) {
        try {
            remediationHistoryDao.getSuccessRate(failureType, actionType) ?: 0.0
        } catch (e: Exception) {
            Timber.e(e, "Error getting success rate")
            0.0
        }
    }
    
    /**
     * Get pattern-specific success rate
     */
    suspend fun getPatternSuccessRate(
        failurePattern: String,
        actionType: String
    ): Double = withContext(Dispatchers.IO) {
        try {
            remediationHistoryDao.getPatternSuccessRate(failurePattern, actionType) ?: 0.0
        } catch (e: Exception) {
            Timber.e(e, "Error getting pattern success rate")
            0.0
        }
    }
    
    /**
     * Get overall remediation statistics
     */
    suspend fun getStatistics(): RemediationStats? = withContext(Dispatchers.IO) {
        try {
            val stats = remediationHistoryDao.getStatistics()
            stats?.let {
                RemediationStats(
                    totalAttempts = it.total,
                    successfulAttempts = it.successful,
                    failedAttempts = it.failed,
                    overallSuccessRate = if (it.total > 0) it.successful.toDouble() / it.total else 0.0,
                    avgDurationMs = it.avgDuration.toLong()
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting statistics")
            null
        }
    }
    
    /**
     * Learn from recent outcomes and adjust confidence
     * This can be used to implement dynamic confidence adjustment
     */
    suspend fun analyzeRecentPerformance(
        failureType: String,
        lookbackHours: Int = 24
    ): PerformanceAnalysis = withContext(Dispatchers.IO) {
        try {
            val lookbackTime = System.currentTimeMillis() - (lookbackHours * 3600_000)
            val recentHistory = remediationHistoryDao.getHistoryByTimeRange(
                lookbackTime,
                System.currentTimeMillis()
            ).filter { it.failureType == failureType }
            
            if (recentHistory.isEmpty()) {
                return@withContext PerformanceAnalysis(
                    failureType = failureType,
                    recentSuccessRate = 0.0,
                    trend = "UNKNOWN",
                    recommendation = "Insufficient recent data"
                )
            }
            
            val successRate = recentHistory.count { it.wasSuccessful }.toDouble() / recentHistory.size
            val trend = determineTrend(recentHistory)
            
            PerformanceAnalysis(
                failureType = failureType,
                recentSuccessRate = successRate,
                trend = trend,
                recommendation = when {
                    successRate >= 0.8 -> "Current strategies are highly effective"
                    successRate >= 0.5 -> "Moderate effectiveness, continue monitoring"
                    else -> "Low effectiveness, consider alternative approaches"
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error analyzing recent performance")
            PerformanceAnalysis(failureType, 0.0, "ERROR", "Analysis failed")
        }
    }
    
    /**
     * Determine trend from recent history
     */
    private fun determineTrend(history: List<RemediationHistoryEntity>): String {
        if (history.size < 4) return "STABLE"
        
        val recentHalf = history.takeLast(history.size / 2)
        val olderHalf = history.take(history.size / 2)
        
        val recentSuccess = recentHalf.count { it.wasSuccessful }.toDouble() / recentHalf.size
        val olderSuccess = olderHalf.count { it.wasSuccessful }.toDouble() / olderHalf.size
        
        return when {
            recentSuccess > olderSuccess + 0.2 -> "IMPROVING"
            recentSuccess < olderSuccess - 0.2 -> "DECLINING"
            else -> "STABLE"
        }
    }
    
    data class RemediationStats(
        val totalAttempts: Int,
        val successfulAttempts: Int,
        val failedAttempts: Int,
        val overallSuccessRate: Double,
        val avgDurationMs: Long
    )
    
    data class PerformanceAnalysis(
        val failureType: String,
        val recentSuccessRate: Double,
        val trend: String, // IMPROVING, DECLINING, STABLE
        val recommendation: String
    )
}
