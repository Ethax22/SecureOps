package com.secureops.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing remediation action history for learning
 * 
 * Tracks outcomes of remediation attempts to improve future recommendations
 */
@Entity(tableName = "remediation_history")
data class RemediationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Pipeline ID where remediation was attempted
     */
    val pipelineId: String,
    
    /**
     * Build number that failed
     */
    val buildNumber: Int,
    
    /**
     * Repository name
     */
    val repositoryName: String,
    
    /**
     * Failure type detected (e.g., "TRANSIENT", "FLAKY_TEST", "TIMEOUT")
     */
    val failureType: String,
    
    /**
     * Specific failure pattern detected (e.g., "npm install timeout", "test_user_login flaky")
     */
    val failurePattern: String,
    
    /**
     * Remediation action taken (e.g., "RETRY_BUILD", "SKIP_TESTS", "INCREASE_TIMEOUT")
     */
    val actionTaken: String,
    
    /**
     * Detailed description of action
     */
    val actionDescription: String,
    
    /**
     * Whether the remediation was successful
     */
    val wasSuccessful: Boolean,
    
    /**
     * Outcome status (e.g., "SUCCESS", "FAILURE", "PARTIAL", "TIMEOUT")
     */
    val outcome: String,
    
    /**
     * Build number after remediation (if retriggered)
     */
    val remediatedBuildNumber: Int?,
    
    /**
     * Time taken for remediation in milliseconds
     */
    val durationMs: Long,
    
    /**
     * Confidence score when action was taken (0.0 to 1.0)
     */
    val confidenceScore: Double,
    
    /**
     * Error message if remediation failed
     */
    val errorMessage: String?,
    
    /**
     * Additional context about the failure
     */
    val failureContext: String?,
    
    /**
     * Log snippet showing the failure
     */
    val logSnippet: String?,
    
    /**
     * Whether this was user-approved or automatic
     */
    val wasUserApproved: Boolean,
    
    /**
     * User who approved (if applicable)
     */
    val approvedBy: String?,
    
    /**
     * Timestamp when remediation was attempted
     */
    val attemptedAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp when outcome was determined
     */
    val completedAt: Long?,
    
    /**
     * Account ID associated with the pipeline
     */
    val accountId: String,
    
    /**
     * Cost of remediation (e.g., CI minutes used)
     */
    val costMetric: Double = 0.0,
    
    /**
     * Number of times this pattern has been seen
     */
    val patternFrequency: Int = 1,
    
    /**
     * Tags for categorization
     */
    val tags: String? = null
)
