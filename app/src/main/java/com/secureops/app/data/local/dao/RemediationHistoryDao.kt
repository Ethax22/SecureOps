package com.secureops.app.data.local.dao

import androidx.room.*
import com.secureops.app.data.local.entity.RemediationHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for remediation history
 */
@Dao
interface RemediationHistoryDao {
    
    /**
     * Insert a new remediation history record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: RemediationHistoryEntity): Long
    
    /**
     * Insert multiple remediation history records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<RemediationHistoryEntity>): List<Long>
    
    /**
     * Update an existing remediation history record
     */
    @Update
    suspend fun update(history: RemediationHistoryEntity)
    
    /**
     * Delete a remediation history record
     */
    @Delete
    suspend fun delete(history: RemediationHistoryEntity)
    
    /**
     * Get all remediation history records
     */
    @Query("SELECT * FROM remediation_history ORDER BY attemptedAt DESC")
    fun getAllHistory(): Flow<List<RemediationHistoryEntity>>
    
    /**
     * Get remediation history by pipeline ID
     */
    @Query("SELECT * FROM remediation_history WHERE pipelineId = :pipelineId ORDER BY attemptedAt DESC")
    fun getHistoryByPipeline(pipelineId: String): Flow<List<RemediationHistoryEntity>>
    
    /**
     * Get remediation history by repository
     */
    @Query("SELECT * FROM remediation_history WHERE repositoryName = :repositoryName ORDER BY attemptedAt DESC")
    fun getHistoryByRepository(repositoryName: String): Flow<List<RemediationHistoryEntity>>
    
    /**
     * Get remediation history by failure type
     */
    @Query("SELECT * FROM remediation_history WHERE failureType = :failureType ORDER BY attemptedAt DESC")
    fun getHistoryByFailureType(failureType: String): Flow<List<RemediationHistoryEntity>>
    
    /**
     * Get successful remediation history
     */
    @Query("SELECT * FROM remediation_history WHERE wasSuccessful = 1 ORDER BY attemptedAt DESC")
    fun getSuccessfulHistory(): Flow<List<RemediationHistoryEntity>>
    
    /**
     * Get failed remediation history
     */
    @Query("SELECT * FROM remediation_history WHERE wasSuccessful = 0 ORDER BY attemptedAt DESC")
    fun getFailedHistory(): Flow<List<RemediationHistoryEntity>>
    
    /**
     * Get remediation history by action type
     */
    @Query("SELECT * FROM remediation_history WHERE actionTaken = :actionTaken ORDER BY attemptedAt DESC")
    fun getHistoryByAction(actionTaken: String): Flow<List<RemediationHistoryEntity>>
    
    /**
     * Get remediation history for a specific failure pattern
     */
    @Query("SELECT * FROM remediation_history WHERE failurePattern = :failurePattern ORDER BY attemptedAt DESC")
    suspend fun getHistoryByPattern(failurePattern: String): List<RemediationHistoryEntity>
    
    /**
     * Get success rate for a specific action on a specific failure type
     */
    @Query("""
        SELECT 
            CAST(SUM(CASE WHEN wasSuccessful = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*) 
        FROM remediation_history 
        WHERE failureType = :failureType AND actionTaken = :actionTaken
    """)
    suspend fun getSuccessRate(failureType: String, actionTaken: String): Double?
    
    /**
     * Get success rate for a specific failure pattern and action
     */
    @Query("""
        SELECT 
            CAST(SUM(CASE WHEN wasSuccessful = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*) 
        FROM remediation_history 
        WHERE failurePattern = :failurePattern AND actionTaken = :actionTaken
    """)
    suspend fun getPatternSuccessRate(failurePattern: String, actionTaken: String): Double?
    
    /**
     * Get average duration for a specific action
     */
    @Query("SELECT AVG(durationMs) FROM remediation_history WHERE actionTaken = :actionTaken AND wasSuccessful = 1")
    suspend fun getAverageDuration(actionTaken: String): Long?
    
    /**
     * Get total number of attempts for a failure type
     */
    @Query("SELECT COUNT(*) FROM remediation_history WHERE failureType = :failureType")
    suspend fun getAttemptCount(failureType: String): Int
    
    /**
     * Get total number of attempts for a failure pattern
     */
    @Query("SELECT COUNT(*) FROM remediation_history WHERE failurePattern = :failurePattern")
    suspend fun getPatternAttemptCount(failurePattern: String): Int
    
    /**
     * Get most successful actions for a failure type
     */
    @Query("""
        SELECT actionTaken, 
               CAST(SUM(CASE WHEN wasSuccessful = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*) as successRate,
               COUNT(*) as totalAttempts
        FROM remediation_history 
        WHERE failureType = :failureType
        GROUP BY actionTaken
        HAVING COUNT(*) >= :minAttempts
        ORDER BY successRate DESC, totalAttempts DESC
        LIMIT :limit
    """)
    suspend fun getTopActionsForFailureType(
        failureType: String, 
        minAttempts: Int = 3,
        limit: Int = 3
    ): List<ActionSuccessRate>
    
    /**
     * Get remediation statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN wasSuccessful = 1 THEN 1 ELSE 0 END) as successful,
            SUM(CASE WHEN wasSuccessful = 0 THEN 1 ELSE 0 END) as failed,
            AVG(durationMs) as avgDuration
        FROM remediation_history
    """)
    suspend fun getStatistics(): RemediationStatistics?
    
    /**
     * Get recent remediation history (last N records)
     */
    @Query("SELECT * FROM remediation_history ORDER BY attemptedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 50): List<RemediationHistoryEntity>
    
    /**
     * Get pending remediations (attempted but not completed)
     */
    @Query("SELECT * FROM remediation_history WHERE completedAt IS NULL ORDER BY attemptedAt DESC")
    fun getPendingRemediations(): Flow<List<RemediationHistoryEntity>>
    
    /**
     * Update remediation outcome
     */
    @Query("""
        UPDATE remediation_history 
        SET wasSuccessful = :wasSuccessful, 
            outcome = :outcome, 
            completedAt = :completedAt,
            remediatedBuildNumber = :remediatedBuildNumber,
            errorMessage = :errorMessage
        WHERE id = :historyId
    """)
    suspend fun updateOutcome(
        historyId: Long,
        wasSuccessful: Boolean,
        outcome: String,
        completedAt: Long,
        remediatedBuildNumber: Int?,
        errorMessage: String?
    )
    
    /**
     * Get history by time range
     */
    @Query("SELECT * FROM remediation_history WHERE attemptedAt BETWEEN :startTime AND :endTime ORDER BY attemptedAt DESC")
    suspend fun getHistoryByTimeRange(startTime: Long, endTime: Long): List<RemediationHistoryEntity>
    
    /**
     * Delete old history records (cleanup)
     */
    @Query("DELETE FROM remediation_history WHERE attemptedAt < :beforeTimestamp")
    suspend fun deleteOldHistory(beforeTimestamp: Long): Int
    
    /**
     * Get distinct failure types
     */
    @Query("SELECT DISTINCT failureType FROM remediation_history ORDER BY failureType")
    suspend fun getDistinctFailureTypes(): List<String>
    
    /**
     * Get distinct failure patterns for a type
     */
    @Query("SELECT DISTINCT failurePattern FROM remediation_history WHERE failureType = :failureType ORDER BY failurePattern")
    suspend fun getDistinctPatternsForType(failureType: String): List<String>
    
    /**
     * Get confidence-weighted success rate
     */
    @Query("""
        SELECT 
            SUM(CASE WHEN wasSuccessful = 1 THEN confidenceScore ELSE 0 END) / SUM(confidenceScore)
        FROM remediation_history 
        WHERE failureType = :failureType AND actionTaken = :actionTaken
    """)
    suspend fun getWeightedSuccessRate(failureType: String, actionTaken: String): Double?
}

/**
 * Data class for action success rate query result
 */
data class ActionSuccessRate(
    val actionTaken: String,
    val successRate: Double,
    val totalAttempts: Int
)

/**
 * Data class for remediation statistics
 */
data class RemediationStatistics(
    val total: Int,
    val successful: Int,
    val failed: Int,
    val avgDuration: Double
)
