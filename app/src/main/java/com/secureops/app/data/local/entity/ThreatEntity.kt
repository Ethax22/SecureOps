package com.secureops.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a detected security threat (exposed secret/credential)
 * 
 * Stores information about secrets detected in code, logs, or commits
 */
@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Type of secret detected (e.g., "AWS_ACCESS_KEY", "GITHUB_TOKEN")
     */
    val patternType: String,
    
    /**
     * Severity level (0=INFO, 1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL)
     */
    val severity: Int,
    
    /**
     * Human-readable description of the threat
     */
    val description: String,
    
    /**
     * The actual detected secret (may be partially masked)
     */
    val detectedValue: String,
    
    /**
     * Source where the secret was found (e.g., file path, log name)
     */
    val source: String,
    
    /**
     * Line number where the secret was detected (1-indexed)
     * Null if line number is not applicable
     */
    val lineNumber: Int?,
    
    /**
     * Pipeline ID associated with this threat
     */
    val pipelineId: String,
    
    /**
     * Build number where this threat was detected
     */
    val buildNumber: Int,
    
    /**
     * Repository name where the threat was found
     */
    val repositoryName: String,
    
    /**
     * Branch where the threat was detected
     */
    val branch: String,
    
    /**
     * Commit hash associated with this threat
     */
    val commitHash: String,
    
    /**
     * Full line of code/text where the secret was found
     */
    val contextLine: String,
    
    /**
     * Timestamp when the threat was detected (milliseconds)
     */
    val detectedAt: Long = System.currentTimeMillis(),
    
    /**
     * Whether this threat has been acknowledged/resolved
     */
    val isResolved: Boolean = false,
    
    /**
     * When the threat was resolved (milliseconds)
     * Null if not yet resolved
     */
    val resolvedAt: Long? = null,
    
    /**
     * Notes about resolution or why it's a false positive
     */
    val resolutionNotes: String? = null,
    
    /**
     * Account ID associated with the pipeline
     */
    val accountId: String
)
