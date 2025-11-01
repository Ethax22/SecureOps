package com.secureops.app.domain.model

data class AnalyticsData(
    val projectMetrics: List<ProjectMetrics>,
    val overallStats: OverallStats,
    val timeRange: TimeRange
)

data class ProjectMetrics(
    val repositoryName: String,
    val provider: CIProvider,
    val totalBuilds: Int,
    val successfulBuilds: Int,
    val failedBuilds: Int,
    val averageDuration: Long,
    val averageTimeToFix: Long,
    val failureRate: Float,
    val trendData: List<TrendPoint>
)

data class TrendPoint(
    val timestamp: Long,
    val successCount: Int,
    val failureCount: Int
)

data class OverallStats(
    val totalPipelines: Int,
    val activePipelines: Int,
    val criticalFailures: Int,
    val averageFailureRate: Float,
    val mostUnstableProject: String?
)

enum class TimeRange {
    LAST_24_HOURS,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS
}
