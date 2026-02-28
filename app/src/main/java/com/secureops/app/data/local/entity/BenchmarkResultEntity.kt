package com.secureops.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmark_results")
data class BenchmarkResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val inferenceTimeMs: Long,
    val memoryUsageMb: Double,
    val startupTimeMs: Long,
    val precision: Double,
    val recall: Double,
    val f1Score: Double,
    val accuracy: Double,
    val truePositives: Int = 0,
    val falsePositives: Int = 0,
    val trueNegatives: Int = 0,
    val falseNegatives: Int = 0,
    val batteryDrainMah: Float = 0f
)
