package com.secureops.app.ui.screens.analytics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.analytics.AnalyticsRepository
import com.secureops.app.data.analytics.ExportFormat
import com.secureops.app.data.analytics.TimeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    init {
        loadAnalytics(TimeRange.LAST_30_DAYS)
    }

    fun loadAnalytics(timeRange: TimeRange) {
        viewModelScope.launch {
            try {
                _uiState.value = AnalyticsUiState.Loading

                // Load all analytics data
                val trends = analyticsRepository.getFailureTrends(timeRange)
                val causes = analyticsRepository.getCommonFailureCauses()
                val timeToFix = analyticsRepository.getTimeToFixMetrics()
                val repoMetrics = analyticsRepository.getRepositoryMetrics()
                val highRisk = analyticsRepository.getHighRiskRepositories(threshold = 30f)

                // Calculate totals
                val totalBuilds = repoMetrics.sumOf { it.totalBuilds }
                val totalFailed = repoMetrics.sumOf { it.failedBuilds }
                val totalSuccess = repoMetrics.sumOf { it.successfulBuilds }
                val successRate = if (totalBuilds > 0) {
                    (totalSuccess.toFloat() / totalBuilds) * 100
                } else {
                    0f
                }
                val avgDuration = repoMetrics
                    .mapNotNull { it.averageDuration }
                    .average()
                    .toLong()
                    .coerceAtLeast(0)

                // Convert to UI models
                val trendData = trends.dailyMetrics.map { it.date to it.failureRate }
                val timeToFixData = timeToFix.map {
                    Triple(it.repository, it.timeToFixHours, it.timeToFixHours)
                }
                val repoMetricItems = repoMetrics.map {
                    RepositoryMetricItem(
                        name = it.repository,
                        totalBuilds = it.totalBuilds,
                        failureRate = it.failureRate
                    )
                }
                val riskItems = highRisk.map {
                    RiskRepositoryItem(
                        name = it.repository,
                        failureRate = it.failureRate,
                        recommendation = it.recommendation
                    )
                }

                _uiState.value = AnalyticsUiState.Success(
                    totalBuilds = totalBuilds,
                    successRate = successRate,
                    avgDuration = avgDuration,
                    failedBuilds = totalFailed,
                    trendData = trendData,
                    commonCauses = causes,
                    timeToFixStats = timeToFixData,
                    repositoryMetrics = repoMetricItems,
                    highRiskRepos = riskItems
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load analytics")
                _uiState.value = AnalyticsUiState.Error(
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun exportAnalytics(context: Context, format: ExportFormat) {
        viewModelScope.launch {
            try {
                _exportStatus.value = ExportStatus.Exporting
                val result = analyticsRepository.exportAnalytics(context, format)
                if (result.success) {
                    Timber.d("Analytics exported successfully: ${format.name} to ${result.fileName}")
                    _exportStatus.value = ExportStatus.Success(
                        fileName = result.fileName ?: "analytics.${format.name.lowercase()}",
                        message = result.message
                    )
                } else {
                    Timber.e("Export failed: ${result.message}")
                    _exportStatus.value = ExportStatus.Error(result.message)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to export analytics")
                _exportStatus.value = ExportStatus.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun clearExportStatus() {
        _exportStatus.value = ExportStatus.Idle
    }
}

/**
 * UI State for Analytics Screen
 */
sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()

    data class Success(
        val totalBuilds: Int,
        val successRate: Float,
        val avgDuration: Long,
        val failedBuilds: Int,
        val trendData: List<Pair<String, Float>>,
        val commonCauses: Map<String, Int>,
        val timeToFixStats: List<Triple<String, Int, Int>>,
        val repositoryMetrics: List<RepositoryMetricItem>,
        val highRiskRepos: List<RiskRepositoryItem>
    ) : AnalyticsUiState()

    data class Error(
        val message: String
    ) : AnalyticsUiState()
}

/**
 * Export status for showing user feedback
 */
sealed class ExportStatus {
    object Idle : ExportStatus()
    object Exporting : ExportStatus()
    data class Success(val fileName: String, val message: String) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}
