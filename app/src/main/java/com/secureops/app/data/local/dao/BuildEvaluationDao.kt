package com.secureops.app.data.local.dao

import androidx.room.*
import com.secureops.app.data.local.entity.BuildEvaluationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for build evaluation queries
 * Provides access to ML model performance tracking data
 */
@Dao
interface BuildEvaluationDao {
    
    /**
     * Insert or update a build evaluation
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evaluation: BuildEvaluationEntity)
    
    /**
     * Insert multiple evaluations
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evaluations: List<BuildEvaluationEntity>)
    
    /**
     * Update an existing evaluation
     */
    @Update
    suspend fun update(evaluation: BuildEvaluationEntity)
    
    /**
     * Get all evaluated builds (where actual label is known)
     */
    @Query("SELECT * FROM build_evaluations WHERE actualLabel IS NOT NULL ORDER BY evaluatedAt DESC")
    fun getAllEvaluatedBuilds(): Flow<List<BuildEvaluationEntity>>
    
    /**
     * Get all pending evaluations (where actual label is not yet known)
     */
    @Query("SELECT * FROM build_evaluations WHERE actualLabel IS NULL ORDER BY predictedAt DESC")
    fun getPendingEvaluations(): Flow<List<BuildEvaluationEntity>>
    
    /**
     * Get evaluation by build ID
     */
    @Query("SELECT * FROM build_evaluations WHERE buildId = :buildId")
    suspend fun getEvaluationById(buildId: String): BuildEvaluationEntity?
    
    /**
     * Get evaluations in a time range
     */
    @Query("""
        SELECT * FROM build_evaluations 
        WHERE evaluatedAt IS NOT NULL 
        AND evaluatedAt >= :startTime 
        AND evaluatedAt <= :endTime
        ORDER BY evaluatedAt DESC
    """)
    suspend fun getEvaluationsInTimeRange(startTime: Long, endTime: Long): List<BuildEvaluationEntity>
    
    /**
     * Get total count of evaluated builds
     */
    @Query("SELECT COUNT(*) FROM build_evaluations WHERE actualLabel IS NOT NULL")
    suspend fun getTotalEvaluationsCount(): Int
    
    /**
     * Get count by predicted label
     */
    @Query("SELECT COUNT(*) FROM build_evaluations WHERE predictedLabel = :label AND actualLabel IS NOT NULL")
    suspend fun getCountByPredictedLabel(label: Int): Int
    
    /**
     * Get count by actual label
     */
    @Query("SELECT COUNT(*) FROM build_evaluations WHERE actualLabel = :label")
    suspend fun getCountByActualLabel(label: Int): Int
    
    /**
     * Get average inference time
     */
    @Query("SELECT AVG(inferenceTimeMs) FROM build_evaluations")
    suspend fun getAverageInferenceTime(): Double?
    
    /**
     * Get average confidence score
     */
    @Query("SELECT AVG(confidenceScore) FROM build_evaluations WHERE actualLabel IS NOT NULL")
    suspend fun getAverageConfidence(): Float?
    
    /**
     * Get evaluations for confusion matrix calculation
     * Returns all evaluations where both predicted and actual labels are known
     */
    @Query("""
        SELECT * FROM build_evaluations 
        WHERE actualLabel IS NOT NULL 
        ORDER BY evaluatedAt DESC
    """)
    suspend fun getEvaluationsForMetrics(): List<BuildEvaluationEntity>
    
    /**
     * Delete old evaluations
     */
    @Query("DELETE FROM build_evaluations WHERE predictedAt < :timestamp")
    suspend fun deleteOldEvaluations(timestamp: Long)
    
    /**
     * Delete evaluation by build ID
     */
    @Query("DELETE FROM build_evaluations WHERE buildId = :buildId")
    suspend fun deleteEvaluation(buildId: String)
    
    /**
     * Delete all evaluations
     */
    @Query("DELETE FROM build_evaluations")
    suspend fun deleteAll()
}
