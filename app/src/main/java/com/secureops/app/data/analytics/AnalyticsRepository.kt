package com.secureops.app.data.analytics

import android.content.Context
import android.net.Uri
import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.CIProvider
import com.secureops.app.util.FileExportUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AnalyticsRepository(
    private val pipelineDao: PipelineDao
) {

    /**
     * Get failure trends over time
     */
    suspend fun getFailureTrends(timeRange: TimeRange): TrendData {
        val startTime = getStartTimeForRange(timeRange)
        val pipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }
            .filter { (it.startedAt ?: 0) >= startTime }

        // Group by day and calculate failure rate
        val dailyData = pipelines
            .groupBy { getDayKey(it.startedAt ?: 0) }
            .mapValues { entry ->
                val total = entry.value.size
                val failed = entry.value.count { it.status == BuildStatus.FAILURE }
                DailyMetric(
                    date = entry.key,
                    total = total,
                    failed = failed,
                    failureRate = if (total > 0) (failed.toFloat() / total * 100) else 0f
                )
            }
            .values
            .sortedBy { it.date }

        return TrendData(
            timeRange = timeRange,
            dailyMetrics = dailyData.toList(),
            overallFailureRate = calculateOverallFailureRate(pipelines)
        )
    }

    /**
     * Get common failure causes
     */
    suspend fun getCommonFailureCauses(): Map<String, Int> {
        val failedPipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }
            .filter { it.status == BuildStatus.FAILURE }

        // Analyze failure patterns (simplified - could be enhanced with ML)
        val causes = mutableMapOf<String, Int>()

        failedPipelines.forEach { pipeline ->
            // Categorize by provider
            val provider = pipeline.provider.displayName
            causes[provider] = (causes[provider] ?: 0) + 1

            // Could add more sophisticated analysis here
            // - Parse error messages
            // - Identify common patterns
            // - Use ML for categorization
        }

        return causes.toList()
            .sortedByDescending { it.second }
            .take(10)
            .toMap()
    }

    /**
     * Get time-to-fix metrics
     */
    suspend fun getTimeToFixMetrics(): List<TimeToFixStat> {
        val allPipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }
            .sortedBy { it.startedAt }

        val stats = mutableListOf<TimeToFixStat>()

        // Find consecutive build sequences
        val groupedByRepo = allPipelines.groupBy { it.repositoryName }

        groupedByRepo.forEach { (repo, pipelines) ->
            var lastFailureTime: Long? = null

            pipelines.forEach { pipeline ->
                if (pipeline.status == BuildStatus.FAILURE) {
                    lastFailureTime = pipeline.finishedAt
                } else if (pipeline.status == BuildStatus.SUCCESS && lastFailureTime != null) {
                    val fixTime = (pipeline.finishedAt ?: 0) - lastFailureTime!!
                    if (fixTime > 0) {
                        stats.add(
                            TimeToFixStat(
                                repository = repo,
                                failedAt = lastFailureTime!!,
                                fixedAt = pipeline.finishedAt ?: 0,
                                timeToFix = fixTime,
                                timeToFixHours = TimeUnit.MILLISECONDS.toHours(fixTime).toInt()
                            )
                        )
                    }
                    lastFailureTime = null
                }
            }
        }

        return stats.sortedByDescending { it.fixedAt }.take(20)
    }

    /**
     * Get repository-specific metrics
     */
    suspend fun getRepositoryMetrics(): List<RepositoryMetric> {
        val allPipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }

        return allPipelines
            .groupBy { it.repositoryName }
            .map { (repo, pipelines) ->
                val total = pipelines.size
                val failed = pipelines.count { it.status == BuildStatus.FAILURE }
                val success = pipelines.count { it.status == BuildStatus.SUCCESS }
                val avgDuration = pipelines
                    .mapNotNull { it.duration }
                    .average()
                    .let { if (it.isNaN()) 0.0 else it }

                RepositoryMetric(
                    repository = repo,
                    totalBuilds = total,
                    failedBuilds = failed,
                    successfulBuilds = success,
                    failureRate = if (total > 0) (failed.toFloat() / total * 100) else 0f,
                    averageDuration = avgDuration.toLong()
                )
            }
            .sortedByDescending { it.totalBuilds }
    }

    /**
     * Get provider comparison metrics
     */
    suspend fun getProviderMetrics(): List<ProviderMetric> {
        val allPipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }

        return allPipelines
            .groupBy { it.provider }
            .map { (provider, pipelines) ->
                val total = pipelines.size
                val failed = pipelines.count { it.status == BuildStatus.FAILURE }
                val success = pipelines.count { it.status == BuildStatus.SUCCESS }

                ProviderMetric(
                    provider = provider,
                    totalBuilds = total,
                    failedBuilds = failed,
                    successfulBuilds = success,
                    failureRate = if (total > 0) (failed.toFloat() / total * 100) else 0f
                )
            }
            .sortedByDescending { it.totalBuilds }
    }

    /**
     * Get high-risk repositories
     */
    suspend fun getHighRiskRepositories(threshold: Float = 30f): List<RiskAssessment> {
        val repoMetrics = getRepositoryMetrics()

        return repoMetrics
            .filter { it.failureRate >= threshold }
            .map { metric ->
                RiskAssessment(
                    repository = metric.repository,
                    riskLevel = when {
                        metric.failureRate >= 70f -> RiskLevel.CRITICAL
                        metric.failureRate >= 50f -> RiskLevel.HIGH
                        metric.failureRate >= 30f -> RiskLevel.MEDIUM
                        else -> RiskLevel.LOW
                    },
                    failureRate = metric.failureRate,
                    recentFailures = metric.failedBuilds,
                    recommendation = generateRecommendation(metric.failureRate)
                )
            }
            .sortedByDescending { it.failureRate }
    }

    /**
     * Export analytics data
     */
    suspend fun exportAnalytics(context: Context, format: ExportFormat): ExportResult {
        return try {
            val trends = getFailureTrends(TimeRange.LAST_30_DAYS)
            val causes = getCommonFailureCauses()
            val repoMetrics = getRepositoryMetrics()

            val data = AnalyticsExport(
                generatedAt = System.currentTimeMillis(),
                trends = trends,
                causes = causes,
                repositories = repoMetrics
            )

            val fileResult = FileExportUtil.exportAnalytics(context, data, format)

            ExportResult(
                success = fileResult.success,
                format = format,
                data = data,
                uri = fileResult.uri,
                fileName = fileResult.fileName,
                message = fileResult.message
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to export analytics")
            ExportResult(
                success = false,
                format = format,
                data = null,
                uri = null,
                fileName = null,
                message = "Export failed: ${e.message}"
            )
        }
    }

    // Helper functions

    private fun getStartTimeForRange(timeRange: TimeRange): Long {
        val now = System.currentTimeMillis()
        return when (timeRange) {
            TimeRange.LAST_7_DAYS -> now - TimeUnit.DAYS.toMillis(7)
            TimeRange.LAST_30_DAYS -> now - TimeUnit.DAYS.toMillis(30)
            TimeRange.LAST_90_DAYS -> now - TimeUnit.DAYS.toMillis(90)
            TimeRange.ALL_TIME -> 0
        }
    }

    private fun getDayKey(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return format.format(date)
    }

    private fun calculateOverallFailureRate(pipelines: List<com.secureops.app.domain.model.Pipeline>): Float {
        if (pipelines.isEmpty()) return 0f
        val failed = pipelines.count { it.status == BuildStatus.FAILURE }
        return (failed.toFloat() / pipelines.size) * 100
    }

    private fun generateRecommendation(failureRate: Float): String {
        return when {
            failureRate >= 70f -> "Critical: Immediate attention required. Review recent changes and consider rollback."
            failureRate >= 50f -> "High risk: Investigate common failure patterns and stabilize tests."
            failureRate >= 30f -> "Medium risk: Monitor closely and address flaky tests."
            else -> "Low risk: Continue monitoring."
        }
    }
}

// Data models

enum class TimeRange {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    ALL_TIME
}

data class TrendData(
    val timeRange: TimeRange,
    val dailyMetrics: List<DailyMetric>,
    val overallFailureRate: Float
)

data class DailyMetric(
    val date: String,
    val total: Int,
    val failed: Int,
    val failureRate: Float
)

data class TimeToFixStat(
    val repository: String,
    val failedAt: Long,
    val fixedAt: Long,
    val timeToFix: Long,
    val timeToFixHours: Int
)

data class RepositoryMetric(
    val repository: String,
    val totalBuilds: Int,
    val failedBuilds: Int,
    val successfulBuilds: Int,
    val failureRate: Float,
    val averageDuration: Long
)

data class ProviderMetric(
    val provider: CIProvider,
    val totalBuilds: Int,
    val failedBuilds: Int,
    val successfulBuilds: Int,
    val failureRate: Float
)

data class RiskAssessment(
    val repository: String,
    val riskLevel: RiskLevel,
    val failureRate: Float,
    val recentFailures: Int,
    val recommendation: String
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class ExportFormat {
    CSV, PDF, JSON
}

data class ExportResult(
    val success: Boolean,
    val format: ExportFormat,
    val data: AnalyticsExport?,
    val uri: Uri?,
    val fileName: String?,
    val message: String
)

data class AnalyticsExport(
    val generatedAt: Long,
    val trends: TrendData,
    val causes: Map<String, Int>,
    val repositories: List<RepositoryMetric>
)
