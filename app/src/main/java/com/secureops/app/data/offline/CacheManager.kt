package com.secureops.app.data.offline

import android.content.Context
import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.domain.model.Pipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.secureops.app.data.local.entity.toDomain
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache Manager
 * 
 * Manages local data cache and provides statistics
 * Handles cache hits, misses, and eviction
 */
@Singleton
class CacheManager @Inject constructor(
    private val context: Context,
    private val pipelineDao: PipelineDao
) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()
    
    companion object {
        private const val PREFS_NAME = "cache_manager"
        private const val KEY_CACHE_HITS = "cache_hits"
        private const val KEY_CACHE_MISSES = "cache_misses"
        private const val KEY_CACHE_SIZE_BYTES = "cache_size_bytes"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_CACHE_EVICTIONS = "cache_evictions"
        
        // Cache policies
        private const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_CACHE_SIZE_MB = 50L
    }
    
    /**
     * Cache statistics
     */
    data class CacheStats(
        val hits: Int = 0,
        val misses: Int = 0,
        val hitRate: Double = 0.0,
        val totalRequests: Int = 0,
        val cacheSizeBytes: Long = 0,
        val cacheSizeMB: Double = 0.0,
        val itemCount: Int = 0,
        val lastSyncTimestamp: Long = 0,
        val evictions: Int = 0
    )
    
    /**
     * Cache entry with metadata
     */
    data class CacheEntry<T>(
        val data: T,
        val cachedAt: Long,
        val expiresAt: Long,
        val sizeBytes: Long
    )
    
    init {
        updateStats()
    }
    
    /**
     * Record cache hit
     */
    fun recordCacheHit(source: String) {
        val hits = prefs.getInt(KEY_CACHE_HITS, 0)
        prefs.edit()
            .putInt(KEY_CACHE_HITS, hits + 1)
            .apply()
        
        Timber.d("✅ [CACHE HIT] $source")
        updateStats()
    }
    
    /**
     * Record cache miss
     */
    fun recordCacheMiss(source: String) {
        val misses = prefs.getInt(KEY_CACHE_MISSES, 0)
        prefs.edit()
            .putInt(KEY_CACHE_MISSES, misses + 1)
            .apply()
        
        Timber.d("❌ [CACHE MISS] $source")
        updateStats()
    }
    
    /**
     * Record cache eviction
     */
    fun recordCacheEviction() {
        val evictions = prefs.getInt(KEY_CACHE_EVICTIONS, 0)
        prefs.edit()
            .putInt(KEY_CACHE_EVICTIONS, evictions + 1)
            .apply()
        updateStats()
    }
    
    /**
     * Update last sync timestamp
     */
    fun updateLastSync() {
        prefs.edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
        updateStats()
    }
    
    /**
     * Get cached pipelines with freshness check
     */
    suspend fun getCachedPipelines(
        maxAge: Long = MAX_CACHE_AGE_MS
    ): List<Pipeline>? = withContext(Dispatchers.IO) {
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0)
        val age = System.currentTimeMillis() - lastSync
        
        if (age > maxAge) {
            recordCacheMiss("pipelines (stale)")
            return@withContext null
        }
        
        val pipelines = pipelineDao.getAllPipelines().first().map { it.toDomain() }
        if (pipelines.isEmpty()) {
            recordCacheMiss("pipelines (empty)")
            return@withContext null
        }
        
        recordCacheHit("pipelines")
        pipelines
    }
    
    /**
     * Check if cache is fresh
     */
    fun isCacheFresh(maxAge: Long = MAX_CACHE_AGE_MS): Boolean {
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0)
        if (lastSync == 0L) return false
        
        val age = System.currentTimeMillis() - lastSync
        return age <= maxAge
    }
    
    /**
     * Get cache age in milliseconds
     */
    fun getCacheAge(): Long {
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0)
        if (lastSync == 0L) return Long.MAX_VALUE
        
        return System.currentTimeMillis() - lastSync
    }
    
    /**
     * Get formatted cache age
     */
    fun getCacheAgeFormatted(): String {
        val age = getCacheAge()
        if (age == Long.MAX_VALUE) return "Never"
        
        val seconds = age / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ${hours % 24}h ago"
            hours > 0 -> "${hours}h ${minutes % 60}m ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }
    
    /**
     * Calculate estimated cache size
     */
    suspend fun calculateCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            val pipelines = pipelineDao.getAllPipelines().first().map { it.toDomain() }
            
            // Rough estimation: ~2KB per pipeline
            val estimatedSize = pipelines.size * 2048L
            
            prefs.edit()
                .putLong(KEY_CACHE_SIZE_BYTES, estimatedSize)
                .apply()
            
            updateStats()
            estimatedSize
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate cache size")
            0L
        }
    }
    
    /**
     * Clear all caches
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            // Note: We don't actually clear the database, just mark as stale
            prefs.edit()
                .putLong(KEY_LAST_SYNC, 0)
                .apply()
            
            Timber.i("Cache cleared")
            updateStats()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache")
        }
    }
    
    /**
     * Reset cache statistics
     */
    fun resetStats() {
        prefs.edit()
            .putInt(KEY_CACHE_HITS, 0)
            .putInt(KEY_CACHE_MISSES, 0)
            .putInt(KEY_CACHE_EVICTIONS, 0)
            .apply()
        
        updateStats()
        Timber.i("Cache statistics reset")
    }
    
    /**
     * Update cache statistics
     */
    private fun updateStats() {
        val hits = prefs.getInt(KEY_CACHE_HITS, 0)
        val misses = prefs.getInt(KEY_CACHE_MISSES, 0)
        val totalRequests = hits + misses
        val hitRate = if (totalRequests > 0) {
            hits.toDouble() / totalRequests
        } else {
            0.0
        }
        
        val sizeBytes = prefs.getLong(KEY_CACHE_SIZE_BYTES, 0)
        val sizeMB = sizeBytes / (1024.0 * 1024.0)
        
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0)
        val evictions = prefs.getInt(KEY_CACHE_EVICTIONS, 0)
        
        _cacheStats.value = CacheStats(
            hits = hits,
            misses = misses,
            hitRate = hitRate,
            totalRequests = totalRequests,
            cacheSizeBytes = sizeBytes,
            cacheSizeMB = sizeMB,
            itemCount = 0, // Could be calculated from DAO
            lastSyncTimestamp = lastSync,
            evictions = evictions
        )
    }
    
    /**
     * Get cache statistics summary
     */
    fun getStatsSummary(): String {
        val stats = _cacheStats.value
        return buildString {
            appendLine("Cache Statistics:")
            appendLine("  Total Requests: ${stats.totalRequests}")
            appendLine("  Hits: ${stats.hits}")
            appendLine("  Misses: ${stats.misses}")
            appendLine("  Hit Rate: ${String.format("%.1f%%", stats.hitRate * 100)}")
            appendLine("  Cache Size: ${String.format("%.2f MB", stats.cacheSizeMB)}")
            appendLine("  Last Sync: ${getCacheAgeFormatted()}")
            appendLine("  Evictions: ${stats.evictions}")
        }
    }
    
    /**
     * Check if cache needs eviction
     */
    fun needsEviction(): Boolean {
        val stats = _cacheStats.value
        return stats.cacheSizeMB > MAX_CACHE_SIZE_MB || 
               getCacheAge() > MAX_CACHE_AGE_MS
    }
    
    /**
     * Perform cache maintenance
     */
    suspend fun performMaintenance() = withContext(Dispatchers.IO) {
        Timber.d("Performing cache maintenance...")
        
        // Recalculate cache size
        calculateCacheSize()
        
        // Check if eviction needed
        if (needsEviction()) {
            Timber.i("Cache eviction triggered")
            recordCacheEviction()
            // Actual eviction logic would go here
        }
        
        updateStats()
    }
}
