package com.secureops.app.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.data.local.dao.ThreatDao
import com.secureops.app.data.local.entity.ThreatEntity
import com.secureops.app.ml.security.ThreatSeverity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * UI state for the security dashboard
 */
data class SecurityUiState(
    val threats: List<ThreatEntity> = emptyList(),
    val filteredThreats: List<ThreatEntity> = emptyList(),
    val statistics: ThreatStatistics = ThreatStatistics(),
    val selectedFilter: ThreatFilter = ThreatFilter.ALL,
    val selectedSeverity: ThreatSeverity? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Threat statistics summary
 */
data class ThreatStatistics(
    val totalThreats: Int = 0,
    val unresolvedThreats: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val secretsDetected: Int = 0,
    val dependencyIssues: Int = 0,
    val anomaliesDetected: Int = 0
)

/**
 * Filter options for threat list
 */
enum class ThreatFilter {
    ALL,
    UNRESOLVED,
    SECRETS,
    DEPENDENCIES,
    ANOMALIES,
    CRITICAL
}

/**
 * ViewModel for Security Dashboard
 */
class SecurityViewModel(
    private val threatDao: ThreatDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        loadThreats()
    }

    /**
     * Load threats from database
     */
    private fun loadThreats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Observe all threats
                threatDao.getAllThreats().collect { threats ->
                    val statistics = calculateStatistics(threats)
                    
                    _uiState.update { state ->
                        val filtered = filterThreats(
                            threats = threats,
                            filter = state.selectedFilter,
                            severity = state.selectedSeverity
                        )
                        
                        state.copy(
                            threats = threats,
                            filteredThreats = filtered,
                            statistics = statistics,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading threats")
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Calculate statistics from threat list
     */
    private fun calculateStatistics(threats: List<ThreatEntity>): ThreatStatistics {
        val unresolved = threats.filter { !it.isResolved }
        
        return ThreatStatistics(
            totalThreats = threats.size,
            unresolvedThreats = unresolved.size,
            criticalCount = unresolved.count { it.severity == ThreatSeverity.CRITICAL.level },
            highCount = unresolved.count { it.severity == ThreatSeverity.HIGH.level },
            mediumCount = unresolved.count { it.severity == ThreatSeverity.MEDIUM.level },
            lowCount = unresolved.count { it.severity == ThreatSeverity.LOW.level },
            secretsDetected = unresolved.count { 
                !it.patternType.startsWith("DEPENDENCY_") && 
                !it.patternType.startsWith("ANOMALY_")
            },
            dependencyIssues = unresolved.count { it.patternType.startsWith("DEPENDENCY_") },
            anomaliesDetected = unresolved.count { it.patternType.startsWith("ANOMALY_") }
        )
    }

    /**
     * Filter threats based on selected filter and severity
     */
    private fun filterThreats(
        threats: List<ThreatEntity>,
        filter: ThreatFilter,
        severity: ThreatSeverity?
    ): List<ThreatEntity> {
        var filtered = when (filter) {
            ThreatFilter.ALL -> threats
            ThreatFilter.UNRESOLVED -> threats.filter { !it.isResolved }
            ThreatFilter.SECRETS -> threats.filter { 
                !it.patternType.startsWith("DEPENDENCY_") && 
                !it.patternType.startsWith("ANOMALY_")
            }
            ThreatFilter.DEPENDENCIES -> threats.filter { 
                it.patternType.startsWith("DEPENDENCY_") 
            }
            ThreatFilter.ANOMALIES -> threats.filter { 
                it.patternType.startsWith("ANOMALY_") 
            }
            ThreatFilter.CRITICAL -> threats.filter { 
                it.severity == ThreatSeverity.CRITICAL.level && !it.isResolved
            }
        }

        // Apply severity filter if selected
        if (severity != null) {
            filtered = filtered.filter { it.severity == severity.level }
        }

        // Sort by severity (highest first), then by timestamp (newest first)
        return filtered.sortedWith(
            compareByDescending<ThreatEntity> { it.severity }
                .thenByDescending { it.detectedAt }
        )
    }

    /**
     * Set threat filter
     */
    fun setFilter(filter: ThreatFilter) {
        viewModelScope.launch {
            _uiState.update { state ->
                val filtered = filterThreats(
                    threats = state.threats,
                    filter = filter,
                    severity = state.selectedSeverity
                )
                
                state.copy(
                    selectedFilter = filter,
                    filteredThreats = filtered
                )
            }
        }
    }

    /**
     * Set severity filter
     */
    fun setSeverityFilter(severity: ThreatSeverity?) {
        viewModelScope.launch {
            _uiState.update { state ->
                val filtered = filterThreats(
                    threats = state.threats,
                    filter = state.selectedFilter,
                    severity = severity
                )
                
                state.copy(
                    selectedSeverity = severity,
                    filteredThreats = filtered
                )
            }
        }
    }

    /**
     * Mark threat as resolved
     */
    fun resolveThread(threatId: Long, notes: String = "") {
        viewModelScope.launch {
            try {
                threatDao.markAsResolved(
                    threatId = threatId,
                    resolvedAt = System.currentTimeMillis(),
                    notes = notes.ifEmpty { null }
                )
                Timber.d("Threat $threatId marked as resolved")
            } catch (e: Exception) {
                Timber.e(e, "Error resolving threat")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Delete a threat
     */
    fun deleteThreat(threat: ThreatEntity) {
        viewModelScope.launch {
            try {
                threatDao.delete(threat)
                Timber.d("Threat ${threat.id} deleted")
            } catch (e: Exception) {
                Timber.e(e, "Error deleting threat")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Get unresolved threat count
     */
    suspend fun getUnresolvedCount(): Int {
        return try {
            threatDao.getTotalUnresolvedCount()
        } catch (e: Exception) {
            Timber.e(e, "Error getting unresolved count")
            0
        }
    }

    /**
     * Refresh threats
     */
    fun refresh() {
        loadThreats()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
