package com.secureops.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing ML model evaluation data
 * Tracks predictions and actual outcomes for model performance analysis
 */
@Entity(tableName = "build_evaluations")
data class BuildEvaluationEntity(
    @PrimaryKey val buildId: String,
    val predictedLabel: Int,         // 0 = success predicted, 1 = failure predicted
    val actualLabel: Int?,           // 0 = success actual, 1 = failure actual, null = not yet known
    val predictionRiskScore: Float,  // Risk score 0.0 to 1.0
    val confidenceScore: Float,      // Model confidence 0.0 to 1.0
    val inferenceTimeMs: Long,       // Time taken for prediction in milliseconds
    val features: String,            // JSON serialized features used for prediction
    val predictedAt: Long,           // Timestamp when prediction was made
    val evaluatedAt: Long?           // Timestamp when actual outcome was recorded (null if pending)
)
