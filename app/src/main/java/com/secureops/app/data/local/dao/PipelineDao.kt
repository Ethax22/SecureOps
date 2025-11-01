package com.secureops.app.data.local.dao

import androidx.room.*
import com.secureops.app.data.local.entity.PipelineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PipelineDao {
    @Query("SELECT * FROM pipelines ORDER BY startedAt DESC")
    fun getAllPipelines(): Flow<List<PipelineEntity>>

    @Query("SELECT * FROM pipelines WHERE accountId = :accountId ORDER BY startedAt DESC")
    fun getPipelinesByAccount(accountId: String): Flow<List<PipelineEntity>>

    @Query("SELECT * FROM pipelines WHERE repositoryName = :repoName ORDER BY startedAt DESC LIMIT :limit")
    fun getPipelinesByRepository(repoName: String, limit: Int = 50): Flow<List<PipelineEntity>>

    @Query("SELECT * FROM pipelines WHERE id = :pipelineId")
    suspend fun getPipelineById(pipelineId: String): PipelineEntity?

    @Query("SELECT * FROM pipelines WHERE status = :status ORDER BY startedAt DESC")
    fun getPipelinesByStatus(status: String): Flow<List<PipelineEntity>>

    @Query("SELECT * FROM pipelines WHERE predictionRisk > :threshold ORDER BY predictionRisk DESC")
    fun getHighRiskPipelines(threshold: Float): Flow<List<PipelineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPipeline(pipeline: PipelineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPipelines(pipelines: List<PipelineEntity>)

    @Update
    suspend fun updatePipeline(pipeline: PipelineEntity)

    @Delete
    suspend fun deletePipeline(pipeline: PipelineEntity)

    @Query("DELETE FROM pipelines WHERE accountId = :accountId")
    suspend fun deletePipelinesByAccount(accountId: String)

    @Query("DELETE FROM pipelines WHERE cachedAt < :timestamp")
    suspend fun deleteOldPipelines(timestamp: Long)

    @Query("SELECT COUNT(*) FROM pipelines WHERE repositoryName = :repoName AND status = 'FAILURE' AND startedAt > :sinceTimestamp")
    suspend fun getFailureCount(repoName: String, sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM pipelines WHERE repositoryName = :repoName AND status = 'SUCCESS' AND startedAt > :sinceTimestamp")
    suspend fun getSuccessCount(repoName: String, sinceTimestamp: Long): Int
}
