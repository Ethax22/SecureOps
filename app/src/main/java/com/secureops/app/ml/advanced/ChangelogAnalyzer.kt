package com.secureops.app.ml.advanced

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import com.secureops.app.ml.RunAnywhereManager
import kotlinx.coroutines.flow.first
import timber.log.Timber

class ChangelogAnalyzer(
    private val pipelineDao: PipelineDao,
    private val runAnywhereManager: RunAnywhereManager
) {

    /**
     * Analyze changelog and correlate with failures
     */
    suspend fun analyzeChangelog(
        pipeline: Pipeline,
        commits: List<Commit>
    ): ChangelogAnalysis {
        Timber.d("Analyzing changelog for pipeline ${pipeline.id}")

        if (commits.isEmpty()) {
            return ChangelogAnalysis(
                pipeline = pipeline,
                commits = emptyList(),
                suspiciousCommits = emptyList(),
                rootCauseCommit = null,
                confidence = 0f,
                analysis = "No commits to analyze",
                recommendation = "Check if commit data is available"
            )
        }

        // Identify suspicious commits
        val suspiciousCommits = identifySuspiciousCommits(commits, pipeline)

        // Use AI to determine root cause
        val rootCauseCommit = if (suspiciousCommits.isNotEmpty()) {
            determineRootCause(suspiciousCommits, pipeline)
        } else {
            null
        }

        // Calculate confidence
        val confidence = calculateConfidence(suspiciousCommits, rootCauseCommit)

        // Generate AI analysis
        val analysis = generateAIAnalysis(pipeline, commits, suspiciousCommits)

        // Generate recommendation
        val recommendation = generateRecommendation(rootCauseCommit, suspiciousCommits)

        return ChangelogAnalysis(
            pipeline = pipeline,
            commits = commits,
            suspiciousCommits = suspiciousCommits,
            rootCauseCommit = rootCauseCommit,
            confidence = confidence,
            analysis = analysis,
            recommendation = recommendation
        )
    }

    /**
     * Identify commits that might have caused the failure
     */
    private fun identifySuspiciousCommits(
        commits: List<Commit>,
        pipeline: Pipeline
    ): List<SuspiciousCommit> {
        val suspicious = mutableListOf<SuspiciousCommit>()

        commits.forEach { commit ->
            val suspicionScore = calculateSuspicionScore(commit, pipeline)

            if (suspicionScore > SUSPICION_THRESHOLD) {
                val reasons = identifySuspicionReasons(commit)
                suspicious.add(
                    SuspiciousCommit(
                        commit = commit,
                        suspicionScore = suspicionScore,
                        reasons = reasons
                    )
                )
            }
        }

        return suspicious.sortedByDescending { it.suspicionScore }
    }

    /**
     * Calculate suspicion score for a commit
     */
    private fun calculateSuspicionScore(commit: Commit, pipeline: Pipeline): Float {
        var score = 0f

        // Large commits are more likely to introduce issues
        if (commit.filesChanged > 10) score += 20f
        if (commit.linesAdded + commit.linesDeleted > 500) score += 15f

        // Recent commits are more likely culprits
        val commitAge = (pipeline.startedAt ?: 0) - commit.timestamp
        val hoursSinceCommit = commitAge / (1000 * 60 * 60)
        if (hoursSinceCommit < 1) score += 30f
        else if (hoursSinceCommit < 24) score += 15f

        // Check commit message for risky keywords
        val message = commit.message.lowercase()
        if (message.contains("refactor")) score += 10f
        if (message.contains("experimental")) score += 20f
        if (message.contains("wip") || message.contains("work in progress")) score += 25f
        if (message.contains("fix")) score -= 5f // Fixes are usually safe
        if (message.contains("typo")) score -= 10f // Typo fixes are very safe

        // Check file types
        val hasConfigChanges = commit.files.any {
            it.endsWith(".yml") || it.endsWith(".yaml") ||
                    it.endsWith(".json") || it.endsWith(".properties")
        }
        if (hasConfigChanges) score += 15f

        val hasDependencyChanges = commit.files.any {
            it.contains("package.json") || it.contains("build.gradle") ||
                    it.contains("pom.xml") || it.contains("requirements.txt")
        }
        if (hasDependencyChanges) score += 20f

        return score.coerceIn(0f, 100f)
    }

    /**
     * Identify specific reasons for suspicion
     */
    private fun identifySuspicionReasons(commit: Commit): List<String> {
        val reasons = mutableListOf<String>()

        if (commit.filesChanged > 10) {
            reasons.add("Large commit (${commit.filesChanged} files changed)")
        }

        if (commit.linesAdded + commit.linesDeleted > 500) {
            reasons.add("Significant code changes (${commit.linesAdded}+ / ${commit.linesDeleted}-)")
        }

        val message = commit.message.lowercase()
        if (message.contains("wip")) {
            reasons.add("Work in progress commit")
        }
        if (message.contains("refactor")) {
            reasons.add("Code refactoring")
        }

        val hasConfigChanges = commit.files.any { it.endsWith(".yml") || it.endsWith(".yaml") }
        if (hasConfigChanges) {
            reasons.add("Configuration file changes")
        }

        val hasDependencyChanges = commit.files.any {
            it.contains("package.json") || it.contains("build.gradle")
        }
        if (hasDependencyChanges) {
            reasons.add("Dependency updates")
        }

        return reasons
    }

    /**
     * Determine the most likely root cause commit
     */
    private suspend fun determineRootCause(
        suspiciousCommits: List<SuspiciousCommit>,
        pipeline: Pipeline
    ): SuspiciousCommit? {
        if (suspiciousCommits.isEmpty()) return null

        // Get historical data for similar failures
        val similarFailures = findSimilarFailures(pipeline)

        // If we have historical data, use it
        if (similarFailures.isNotEmpty()) {
            // Find commits that appear in multiple failure contexts
            return suspiciousCommits.maxByOrNull { it.suspicionScore }
        }

        // Otherwise, return the most suspicious commit
        return suspiciousCommits.first()
    }

    /**
     * Find similar past failures
     */
    private suspend fun findSimilarFailures(pipeline: Pipeline): List<Pipeline> {
        val allPipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }

        return allPipelines.filter { past ->
            past.id != pipeline.id &&
                    past.status == BuildStatus.FAILURE &&
                    past.repositoryName == pipeline.repositoryName &&
                    past.branch == pipeline.branch
        }.take(5)
    }

    /**
     * Calculate confidence in the analysis
     */
    private fun calculateConfidence(
        suspiciousCommits: List<SuspiciousCommit>,
        rootCause: SuspiciousCommit?
    ): Float {
        if (suspiciousCommits.isEmpty()) return 0f
        if (rootCause == null) return 0.3f

        // Higher confidence if root cause has high suspicion score
        val scoreConfidence = rootCause.suspicionScore / 100f

        // Higher confidence if there are multiple reasons
        val reasonsConfidence = (rootCause.reasons.size.toFloat() / 5f).coerceAtMost(1f)

        return ((scoreConfidence + reasonsConfidence) / 2f).coerceIn(0f, 1f)
    }

    /**
     * Generate AI-powered analysis
     */
    private suspend fun generateAIAnalysis(
        pipeline: Pipeline,
        commits: List<Commit>,
        suspicious: List<SuspiciousCommit>
    ): String {
        // Create prompt for AI
        val prompt = buildString {
            appendLine("Analyze this build failure:")
            appendLine("Repository: ${pipeline.repositoryName}")
            appendLine("Branch: ${pipeline.branch}")
            appendLine("Status: ${pipeline.status}")
            appendLine()
            appendLine("Recent commits:")
            commits.take(5).forEach { commit ->
                appendLine("- ${commit.message} by ${commit.author} (${commit.filesChanged} files)")
            }
            appendLine()
            appendLine("Provide a brief analysis of what might have caused the failure.")
        }

        // Use RunAnywhere AI to generate analysis
        return try {
            val result = runAnywhereManager.generateText(prompt)
            result.getOrElse {
                "Unable to generate AI analysis. Manual review recommended."
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate AI analysis")
            generateFallbackAnalysis(suspicious)
        }
    }

    /**
     * Generate fallback analysis without AI
     */
    private fun generateFallbackAnalysis(suspicious: List<SuspiciousCommit>): String {
        if (suspicious.isEmpty()) {
            return "No suspicious commits identified. The failure may be due to external factors."
        }

        val topCommit = suspicious.first()
        return buildString {
            appendLine("Most likely cause:")
            appendLine("Commit: ${topCommit.commit.message}")
            appendLine("By: ${topCommit.commit.author}")
            appendLine()
            appendLine("Suspicion factors:")
            topCommit.reasons.forEach { reason ->
                appendLine("- $reason")
            }
        }
    }

    /**
     * Generate recommendation
     */
    private fun generateRecommendation(
        rootCause: SuspiciousCommit?,
        suspicious: List<SuspiciousCommit>
    ): String {
        if (rootCause == null) {
            return "Review recent changes and check for external factors like infrastructure issues."
        }

        return buildString {
            appendLine("Recommended actions:")
            appendLine("1. Review commit: ${rootCause.commit.sha.take(7)}")
            appendLine("2. Check files: ${rootCause.commit.files.take(3).joinToString(", ")}")
            appendLine("3. Consider reverting if issue persists")

            if (suspicious.size > 1) {
                appendLine("4. Also review ${suspicious.size - 1} other suspicious commits")
            }
        }
    }

    companion object {
        private const val SUSPICION_THRESHOLD = 30f
    }
}

/**
 * Commit information
 */
data class Commit(
    val sha: String,
    val message: String,
    val author: String,
    val timestamp: Long,
    val files: List<String>,
    val filesChanged: Int,
    val linesAdded: Int,
    val linesDeleted: Int
)

/**
 * Suspicious commit with analysis
 */
data class SuspiciousCommit(
    val commit: Commit,
    val suspicionScore: Float,
    val reasons: List<String>
)

/**
 * Changelog analysis result
 */
data class ChangelogAnalysis(
    val pipeline: Pipeline,
    val commits: List<Commit>,
    val suspiciousCommits: List<SuspiciousCommit>,
    val rootCauseCommit: SuspiciousCommit?,
    val confidence: Float,
    val analysis: String,
    val recommendation: String
)
