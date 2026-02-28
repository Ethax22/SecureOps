package com.secureops.app.data.local.dao

import androidx.room.*
import com.secureops.app.data.local.entity.ThreatEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for security threats
 */
@Dao
interface ThreatDao {
    
    /**
     * Insert a new threat detection
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threat: ThreatEntity): Long
    
    /**
     * Insert multiple threats
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threats: List<ThreatEntity>): List<Long>
    
    /**
     * Update an existing threat
     */
    @Update
    suspend fun update(threat: ThreatEntity)
    
    /**
     * Delete a threat
     */
    @Delete
    suspend fun delete(threat: ThreatEntity)
    
    /**
     * Get all threats as Flow
     */
    @Query("SELECT * FROM threats ORDER BY detectedAt DESC")
    fun getAllThreats(): Flow<List<ThreatEntity>>
    
    /**
     * Get threats by pipeline ID
     */
    @Query("SELECT * FROM threats WHERE pipelineId = :pipelineId ORDER BY detectedAt DESC")
    fun getThreatsByPipeline(pipelineId: String): Flow<List<ThreatEntity>>
    
    /**
     * Get threats by repository
     */
    @Query("SELECT * FROM threats WHERE repositoryName = :repositoryName ORDER BY detectedAt DESC")
    fun getThreatsByRepository(repositoryName: String): Flow<List<ThreatEntity>>
    
    /**
     * Get unresolved threats
     */
    @Query("SELECT * FROM threats WHERE isResolved = 0 ORDER BY severity DESC, detectedAt DESC")
    fun getUnresolvedThreats(): Flow<List<ThreatEntity>>
    
    /**
     * Get threats by severity level
     */
    @Query("SELECT * FROM threats WHERE severity = :severityLevel ORDER BY detectedAt DESC")
    fun getThreatsBySeverity(severityLevel: Int): Flow<List<ThreatEntity>>
    
    /**
     * Get critical unresolved threats
     */
    @Query("SELECT * FROM threats WHERE severity = 4 AND isResolved = 0 ORDER BY detectedAt DESC")
    fun getCriticalThreats(): Flow<List<ThreatEntity>>
    
    /**
     * Get threat count by repository
     */
    @Query("SELECT COUNT(*) FROM threats WHERE repositoryName = :repositoryName AND isResolved = 0")
    suspend fun getUnresolvedCountByRepository(repositoryName: String): Int
    
    /**
     * Get threat count by severity
     */
    @Query("SELECT COUNT(*) FROM threats WHERE severity = :severityLevel AND isResolved = 0")
    suspend fun getUnresolvedCountBySeverity(severityLevel: Int): Int
    
    /**
     * Mark threat as resolved
     */
    @Query("UPDATE threats SET isResolved = 1, resolvedAt = :resolvedAt, resolutionNotes = :notes WHERE id = :threatId")
    suspend fun markAsResolved(threatId: Long, resolvedAt: Long, notes: String?)
    
    /**
     * Get threats for a specific commit
     */
    @Query("SELECT * FROM threats WHERE commitHash = :commitHash ORDER BY lineNumber ASC")
    fun getThreatsByCommit(commitHash: String): Flow<List<ThreatEntity>>
    
    /**
     * Delete old resolved threats (cleanup)
     */
    @Query("DELETE FROM threats WHERE isResolved = 1 AND resolvedAt < :beforeTimestamp")
    suspend fun deleteOldResolvedThreats(beforeTimestamp: Long): Int
    
    /**
     * Get threat by ID
     */
    @Query("SELECT * FROM threats WHERE id = :threatId")
    suspend fun getThreatById(threatId: Long): ThreatEntity?
    
    /**
     * Get total threat count
     */
    @Query("SELECT COUNT(*) FROM threats WHERE isResolved = 0")
    suspend fun getTotalUnresolvedCount(): Int
    
    /**
     * Search threats by pattern type
     */
    @Query("SELECT * FROM threats WHERE patternType = :patternType ORDER BY detectedAt DESC")
    fun getThreatsByPattern(patternType: String): Flow<List<ThreatEntity>>
}
