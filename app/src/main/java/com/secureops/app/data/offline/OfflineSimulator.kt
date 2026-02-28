package com.secureops.app.data.offline

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline Simulator
 * 
 * Simulates offline mode by controlling network availability
 * Used for testing offline resilience without disabling device network
 */
@Singleton
class OfflineSimulator @Inject constructor(
    private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    private val _isOfflineModeEnabled = MutableStateFlow(getOfflineMode())
    val isOfflineModeEnabled: StateFlow<Boolean> = _isOfflineModeEnabled.asStateFlow()
    
    private val _offlineStats = MutableStateFlow(OfflineStats())
    val offlineStats: StateFlow<OfflineStats> = _offlineStats.asStateFlow()
    
    companion object {
        private const val PREFS_NAME = "offline_simulator"
        private const val KEY_OFFLINE_MODE = "offline_mode_enabled"
        private const val KEY_BLOCKED_REQUESTS = "blocked_requests_count"
        private const val KEY_OFFLINE_SINCE = "offline_since_timestamp"
        private const val KEY_DEMO_MODE = "demo_mode_enabled"
    }
    
    /**
     * Offline statistics
     */
    data class OfflineStats(
        val isOffline: Boolean = false,
        val blockedRequests: Int = 0,
        val offlineSince: Long? = null,
        val offlineDurationMs: Long = 0
    )
    
    init {
        updateStats()
    }
    
    /**
     * Enable offline mode - blocks all network requests
     */
    fun enableOfflineMode() {
        Timber.i("🔴 Offline mode ENABLED - Network requests will be blocked")
        prefs.edit()
            .putBoolean(KEY_OFFLINE_MODE, true)
            .putLong(KEY_OFFLINE_SINCE, System.currentTimeMillis())
            .apply()
        
        _isOfflineModeEnabled.value = true
        updateStats()
    }
    
    /**
     * Disable offline mode - restore network connectivity
     */
    fun disableOfflineMode() {
        Timber.i("🟢 Offline mode DISABLED - Network requests restored")
        prefs.edit()
            .putBoolean(KEY_OFFLINE_MODE, false)
            .remove(KEY_OFFLINE_SINCE)
            .apply()
        
        _isOfflineModeEnabled.value = false
        updateStats()
    }
    
    /**
     * Toggle offline mode
     */
    fun toggleOfflineMode() {
        if (isOfflineModeEnabled.value) {
            disableOfflineMode()
        } else {
            enableOfflineMode()
        }
    }
    
    /**
     * Check if offline mode is enabled
     */
    fun isOffline(): Boolean {
        return _isOfflineModeEnabled.value
    }
    
    /**
     * Record a blocked network request
     */
    fun recordBlockedRequest(url: String) {
        val count = prefs.getInt(KEY_BLOCKED_REQUESTS, 0)
        prefs.edit()
            .putInt(KEY_BLOCKED_REQUESTS, count + 1)
            .apply()
        
        Timber.d("🚫 Blocked request #${count + 1}: $url")
        updateStats()
    }
    
    /**
     * Reset blocked request counter
     */
    fun resetBlockedCount() {
        prefs.edit()
            .putInt(KEY_BLOCKED_REQUESTS, 0)
            .apply()
        updateStats()
    }
    
    /**
     * Get current offline mode state
     */
    private fun getOfflineMode(): Boolean {
        return prefs.getBoolean(KEY_OFFLINE_MODE, false)
    }
    
    /**
     * Update statistics
     */
    private fun updateStats() {
        val isOffline = _isOfflineModeEnabled.value
        val blockedCount = prefs.getInt(KEY_BLOCKED_REQUESTS, 0)
        val offlineSince = prefs.getLong(KEY_OFFLINE_SINCE, 0L).takeIf { it > 0 }
        
        val duration = if (isOffline && offlineSince != null) {
            System.currentTimeMillis() - offlineSince
        } else {
            0L
        }
        
        _offlineStats.value = OfflineStats(
            isOffline = isOffline,
            blockedRequests = blockedCount,
            offlineSince = offlineSince,
            offlineDurationMs = duration
        )
    }
    
    /**
     * Enable demo mode (auto-generates data)
     */
    fun enableDemoMode() {
        prefs.edit()
            .putBoolean(KEY_DEMO_MODE, true)
            .apply()
        Timber.i("📊 Demo mode ENABLED")
    }
    
    /**
     * Disable demo mode
     */
    fun disableDemoMode() {
        prefs.edit()
            .putBoolean(KEY_DEMO_MODE, false)
            .apply()
        Timber.i("Demo mode DISABLED")
    }
    
    /**
     * Check if demo mode is enabled
     */
    fun isDemoModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEMO_MODE, false)
    }
    
    /**
     * Get formatted offline duration
     */
    fun getOfflineDurationFormatted(): String {
        val stats = _offlineStats.value
        if (!stats.isOffline || stats.offlineDurationMs == 0L) {
            return "N/A"
        }
        
        val seconds = stats.offlineDurationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Get offline status summary
     */
    fun getStatusSummary(): String {
        val stats = _offlineStats.value
        return buildString {
            appendLine("Offline Mode: ${if (stats.isOffline) "ENABLED" else "DISABLED"}")
            appendLine("Blocked Requests: ${stats.blockedRequests}")
            if (stats.isOffline) {
                appendLine("Offline Duration: ${getOfflineDurationFormatted()}")
            }
            appendLine("Demo Mode: ${if (isDemoModeEnabled()) "ENABLED" else "DISABLED"}")
        }
    }
    
    /**
     * Reset all offline mode settings and stats
     */
    fun reset() {
        prefs.edit().clear().apply()
        _isOfflineModeEnabled.value = false
        updateStats()
        Timber.i("Offline simulator reset")
    }
}
