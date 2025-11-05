package com.secureops.app.ml.advanced

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import com.secureops.app.ml.RunAnywhereManager
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.*

class DeploymentScheduler(
    private val pipelineDao: PipelineDao,
    private val runAnywhereManager: RunAnywhereManager
) {

    /**
     * Analyze best deployment windows based on historical data
     */
    suspend fun analyzeOptimalDeploymentWindows(
        repository: String,
        branch: String = "main"
    ): DeploymentRecommendation {
        Timber.d("Analyzing optimal deployment windows for $repository/$branch")

        val allPipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }
            .filter { it.repositoryName == repository && it.branch == branch }

        if (allPipelines.isEmpty()) {
            return DeploymentRecommendation(
                repository = repository,
                branch = branch,
                optimalWindows = emptyList(),
                riskWindows = emptyList(),
                recommendation = "No historical data available for analysis",
                confidence = 0f
            )
        }

        // Analyze by time patterns
        val hourlyAnalysis = analyzeByHour(allPipelines)
        val dayOfWeekAnalysis = analyzeByDayOfWeek(allPipelines)

        // Find optimal windows
        val optimalWindows = identifyOptimalWindows(hourlyAnalysis, dayOfWeekAnalysis)

        // Find risky windows
        val riskWindows = identifyRiskyWindows(hourlyAnalysis, dayOfWeekAnalysis)

        // Generate recommendation
        val recommendation = generateDeploymentRecommendation(
            optimalWindows,
            riskWindows,
            hourlyAnalysis
        )

        // Calculate confidence
        val confidence = calculateConfidence(allPipelines.size)

        return DeploymentRecommendation(
            repository = repository,
            branch = branch,
            optimalWindows = optimalWindows,
            riskWindows = riskWindows,
            recommendation = recommendation,
            confidence = confidence
        )
    }

    /**
     * Recommend immediate deployment based on current conditions
     */
    suspend fun shouldDeployNow(
        repository: String,
        branch: String = "main"
    ): DeploymentDecision {
        val recommendation = analyzeOptimalDeploymentWindows(repository, branch)
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentDay = now.get(Calendar.DAY_OF_WEEK)

        // Check if now is in an optimal window
        val inOptimalWindow = recommendation.optimalWindows.any { window ->
            isInTimeWindow(currentHour, currentDay, window)
        }

        // Check if now is in a risky window
        val inRiskyWindow = recommendation.riskWindows.any { window ->
            isInTimeWindow(currentHour, currentDay, window)
        }

        val shouldDeploy = when {
            inRiskyWindow -> false
            inOptimalWindow -> true
            recommendation.confidence > 0.7f -> true
            else -> true // Default to allowing deployment
        }

        val reason = when {
            inRiskyWindow -> "Current time is in a high-risk deployment window"
            inOptimalWindow -> "Current time is in an optimal deployment window"
            recommendation.confidence > 0.7f -> "Deployment conditions are favorable"
            else -> "No historical patterns suggest high risk"
        }

        val nextOptimalTime = if (!shouldDeploy && recommendation.optimalWindows.isNotEmpty()) {
            findNextOptimalTime(recommendation.optimalWindows)
        } else {
            null
        }

        return DeploymentDecision(
            shouldDeploy = shouldDeploy,
            confidence = recommendation.confidence,
            reason = reason,
            nextOptimalTime = nextOptimalTime,
            alternativeWindows = recommendation.optimalWindows.take(3)
        )
    }

    /**
     * Analyze success/failure patterns by hour of day
     */
    private fun analyzeByHour(pipelines: List<Pipeline>): Map<Int, HourAnalysis> {
        val hourlyGroups = pipelines.groupBy { pipeline ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = pipeline.startedAt ?: 0
            calendar.get(Calendar.HOUR_OF_DAY)
        }

        return hourlyGroups.mapValues { (hour, pipelinesInHour) ->
            val total = pipelinesInHour.size
            val failures = pipelinesInHour.count { it.status == BuildStatus.FAILURE }
            val successRate = if (total > 0) {
                ((total - failures).toFloat() / total) * 100
            } else {
                100f
            }

            HourAnalysis(
                hour = hour,
                total = total,
                failures = failures,
                successRate = successRate
            )
        }
    }

    /**
     * Analyze success/failure patterns by day of week
     */
    private fun analyzeByDayOfWeek(pipelines: List<Pipeline>): Map<Int, DayAnalysis> {
        val dailyGroups = pipelines.groupBy { pipeline ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = pipeline.startedAt ?: 0
            calendar.get(Calendar.DAY_OF_WEEK)
        }

        return dailyGroups.mapValues { (day, pipelinesOnDay) ->
            val total = pipelinesOnDay.size
            val failures = pipelinesOnDay.count { it.status == BuildStatus.FAILURE }
            val successRate = if (total > 0) {
                ((total - failures).toFloat() / total) * 100
            } else {
                100f
            }

            DayAnalysis(
                dayOfWeek = day,
                dayName = getDayName(day),
                total = total,
                failures = failures,
                successRate = successRate
            )
        }
    }

    /**
     * Identify optimal deployment windows
     */
    private fun identifyOptimalWindows(
        hourlyAnalysis: Map<Int, HourAnalysis>,
        dayAnalysis: Map<Int, DayAnalysis>
    ): List<TimeWindow> {
        val windows = mutableListOf<TimeWindow>()

        // Find hours with high success rates
        val goodHours = hourlyAnalysis.filter { (_, analysis) ->
            analysis.successRate >= 90f && analysis.total >= 5
        }

        // Find days with high success rates
        val goodDays = dayAnalysis.filter { (_, analysis) ->
            analysis.successRate >= 85f && analysis.total >= 10
        }

        // Combine into time windows
        goodDays.forEach { (day, dayAnalysis) ->
            goodHours.forEach { (hour, hourAnalysis) ->
                windows.add(
                    TimeWindow(
                        dayOfWeek = day,
                        dayName = dayAnalysis.dayName,
                        startHour = hour,
                        endHour = hour + 1,
                        successRate = (dayAnalysis.successRate + hourAnalysis.successRate) / 2,
                        type = TimeWindowType.OPTIMAL
                    )
                )
            }
        }

        return windows.sortedByDescending { it.successRate }.take(10)
    }

    /**
     * Identify risky deployment windows
     */
    private fun identifyRiskyWindows(
        hourlyAnalysis: Map<Int, HourAnalysis>,
        dayAnalysis: Map<Int, DayAnalysis>
    ): List<TimeWindow> {
        val windows = mutableListOf<TimeWindow>()

        // Find hours with low success rates
        val riskyHours = hourlyAnalysis.filter { (_, analysis) ->
            analysis.successRate < 70f && analysis.total >= 3
        }

        // Find days with low success rates
        val riskyDays = dayAnalysis.filter { (_, analysis) ->
            analysis.successRate < 75f && analysis.total >= 5
        }

        // Combine into time windows
        riskyDays.forEach { (day, dayAnalysis) ->
            riskyHours.forEach { (hour, hourAnalysis) ->
                windows.add(
                    TimeWindow(
                        dayOfWeek = day,
                        dayName = dayAnalysis.dayName,
                        startHour = hour,
                        endHour = hour + 1,
                        successRate = (dayAnalysis.successRate + hourAnalysis.successRate) / 2,
                        type = TimeWindowType.RISKY
                    )
                )
            }
        }

        return windows.sortedBy { it.successRate }.take(5)
    }

    /**
     * Generate recommendation text
     */
    private fun generateDeploymentRecommendation(
        optimalWindows: List<TimeWindow>,
        riskWindows: List<TimeWindow>,
        hourlyAnalysis: Map<Int, HourAnalysis>
    ): String {
        return buildString {
            if (optimalWindows.isNotEmpty()) {
                appendLine("✅ Best deployment times:")
                optimalWindows.take(3).forEach { window ->
                    appendLine(
                        "  - ${window.dayName} ${window.startHour}:00-${window.endHour}:00 (${
                            String.format(
                                "%.1f",
                                window.successRate
                            )
                        }% success)"
                    )
                }
            }

            if (riskWindows.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ Avoid deploying during:")
                riskWindows.take(3).forEach { window ->
                    appendLine(
                        "  - ${window.dayName} ${window.startHour}:00-${window.endHour}:00 (${
                            String.format(
                                "%.1f",
                                window.successRate
                            )
                        }% success)"
                    )
                }
            }

            if (optimalWindows.isEmpty() && riskWindows.isEmpty()) {
                appendLine("No clear patterns detected. Deploy anytime with caution.")
            }
        }
    }

    /**
     * Calculate confidence based on data volume
     */
    private fun calculateConfidence(totalPipelines: Int): Float {
        return when {
            totalPipelines >= 100 -> 0.95f
            totalPipelines >= 50 -> 0.85f
            totalPipelines >= 30 -> 0.75f
            totalPipelines >= 10 -> 0.60f
            else -> 0.40f
        }
    }

    /**
     * Check if current time is in a time window
     */
    private fun isInTimeWindow(hour: Int, dayOfWeek: Int, window: TimeWindow): Boolean {
        return dayOfWeek == window.dayOfWeek &&
                hour >= window.startHour &&
                hour < window.endHour
    }

    /**
     * Find the next optimal deployment time
     */
    private fun findNextOptimalTime(windows: List<TimeWindow>): String {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        // Find next window today
        val todayWindow = windows
            .filter { it.dayOfWeek == currentDay && it.startHour > currentHour }
            .minByOrNull { it.startHour }

        if (todayWindow != null) {
            return "Today at ${todayWindow.startHour}:00"
        }

        // Find next window this week
        val futureWindow = windows
            .filter { it.dayOfWeek > currentDay }
            .minByOrNull { it.dayOfWeek }

        if (futureWindow != null) {
            return "${futureWindow.dayName} at ${futureWindow.startHour}:00"
        }

        // Next week
        val nextWeekWindow = windows.minByOrNull { it.dayOfWeek }
        return if (nextWeekWindow != null) {
            "Next ${nextWeekWindow.dayName} at ${nextWeekWindow.startHour}:00"
        } else {
            "No optimal time found"
        }
    }

    /**
     * Get day name from day of week constant
     */
    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Sunday"
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            else -> "Unknown"
        }
    }
}

/**
 * Deployment recommendation result
 */
data class DeploymentRecommendation(
    val repository: String,
    val branch: String,
    val optimalWindows: List<TimeWindow>,
    val riskWindows: List<TimeWindow>,
    val recommendation: String,
    val confidence: Float
)

/**
 * Deployment decision for immediate deployment
 */
data class DeploymentDecision(
    val shouldDeploy: Boolean,
    val confidence: Float,
    val reason: String,
    val nextOptimalTime: String?,
    val alternativeWindows: List<TimeWindow>
)

/**
 * Time window for deployments
 */
data class TimeWindow(
    val dayOfWeek: Int,
    val dayName: String,
    val startHour: Int,
    val endHour: Int,
    val successRate: Float,
    val type: TimeWindowType
)

enum class TimeWindowType {
    OPTIMAL,
    RISKY,
    NEUTRAL
}

/**
 * Hourly analysis result
 */
private data class HourAnalysis(
    val hour: Int,
    val total: Int,
    val failures: Int,
    val successRate: Float
)

/**
 * Daily analysis result
 */
private data class DayAnalysis(
    val dayOfWeek: Int,
    val dayName: String,
    val total: Int,
    val failures: Int,
    val successRate: Float
)
