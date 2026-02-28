package com.secureops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.secureops.app.data.local.entity.BenchmarkResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarkResult(result: BenchmarkResultEntity): Long

    @Query("SELECT * FROM benchmark_results ORDER BY timestamp DESC")
    fun getAllBenchmarkResults(): Flow<List<BenchmarkResultEntity>>

    @Query("SELECT * FROM benchmark_results ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBenchmarkResult(): Flow<BenchmarkResultEntity?>

    @Query("DELETE FROM benchmark_results")
    suspend fun clearAllBenchmarkResults()
}
