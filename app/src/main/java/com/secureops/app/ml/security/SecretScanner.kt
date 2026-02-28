package com.secureops.app.ml.security

import com.secureops.app.data.local.dao.ThreatDao
import com.secureops.app.data.local.entity.ThreatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-grade secret scanner for detecting exposed credentials
 * 
 * Scans code, logs, and commit messages for exposed secrets using
 * production-ready regex patterns. Detects line numbers and inserts
 * findings into the database.
 */
@Singleton
class SecretScanner @Inject constructor(
    private val threatDao: ThreatDao
) {
    
    /**
     * Detected secret finding before database insertion
     */
    data class SecretFinding(
        val pattern: SecretPattern,
        val detectedValue: String,
        val lineNumber: Int,
        val line: String,
        val matchStart: Int,
        val matchEnd: Int
    )
    
    /**
     * Scan context for tracking where secrets are found
     */
    data class ScanContext(
        val source: String,
        val pipelineId: String,
        val buildNumber: Int,
        val repositoryName: String,
        val branch: String,
        val commitHash: String,
        val accountId: String
    )
    
    /**
     * Scan a single file/text content for secrets
     * 
     * @param content The text content to scan
     * @param context The scan context (pipeline, build, repo info)
     * @return List of detected secret findings
     */
    suspend fun scanContent(
        content: String,
        context: ScanContext
    ): List<SecretFinding> = withContext(Dispatchers.Default) {
        val findings = mutableListOf<SecretFinding>()
        val lines = content.lines()
        
        // Scan each line for secrets
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1 // 1-indexed
            
            // Check against all patterns
            SecretPattern.values().forEach { pattern ->
                val matches = pattern.regex.findAll(line)
                
                matches.forEach { matchResult ->
                    // Extract the actual secret value
                    val detectedValue = if (matchResult.groupValues.size > 1) {
                        matchResult.groupValues[1] // First capture group
                    } else {
                        matchResult.value
                    }
                    
                    findings.add(
                        SecretFinding(
                            pattern = pattern,
                            detectedValue = detectedValue,
                            lineNumber = lineNumber,
                            line = line.trim(),
                            matchStart = matchResult.range.first,
                            matchEnd = matchResult.range.last
                        )
                    )
                }
            }
        }
        
        findings
    }
    
    /**
     * Scan content and insert findings into database
     * 
     * @param content The text content to scan
     * @param context The scan context
     * @return List of inserted threat IDs
     */
    suspend fun scanAndInsert(
        content: String,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        val findings = scanContent(content, context)
        
        val threats = findings.map { finding ->
            ThreatEntity(
                patternType = finding.pattern.name,
                severity = finding.pattern.severity.level,
                description = finding.pattern.description,
                detectedValue = maskSecret(finding.detectedValue),
                source = context.source,
                lineNumber = finding.lineNumber,
                pipelineId = context.pipelineId,
                buildNumber = context.buildNumber,
                repositoryName = context.repositoryName,
                branch = context.branch,
                commitHash = context.commitHash,
                contextLine = finding.line,
                accountId = context.accountId
            )
        }
        
        if (threats.isNotEmpty()) {
            threatDao.insertAll(threats)
        } else {
            emptyList()
        }
    }
    
    /**
     * Scan multiple files/contents in batch
     * 
     * @param items Map of source name to content
     * @param context The scan context
     * @return Total number of threats found
     */
    suspend fun scanBatch(
        items: Map<String, String>,
        context: ScanContext
    ): Int = withContext(Dispatchers.IO) {
        var totalThreats = 0
        
        items.forEach { (source, content) ->
            val contextWithSource = context.copy(source = source)
            val threatIds = scanAndInsert(content, contextWithSource)
            totalThreats += threatIds.size
        }
        
        totalThreats
    }
    
    /**
     * Scan a commit diff for secrets
     * 
     * @param diff The git diff content
     * @param context The scan context
     * @return List of inserted threat IDs
     */
    suspend fun scanDiff(
        diff: String,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        // Only scan added lines (lines starting with +)
        val addedLines = diff.lines()
            .filter { it.startsWith("+") && !it.startsWith("+++") }
            .map { it.substring(1) } // Remove the + prefix
            .joinToString("\n")
        
        scanAndInsert(addedLines, context.copy(source = "${context.source} (diff)"))
    }
    
    /**
     * Scan log output for secrets
     * 
     * @param logContent The log content to scan
     * @param logName Name of the log file/source
     * @param context The scan context
     * @return List of inserted threat IDs
     */
    suspend fun scanLogs(
        logContent: String,
        logName: String,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        scanAndInsert(logContent, context.copy(source = "logs/$logName"))
    }
    
    /**
     * Get scan statistics for a pipeline
     * 
     * @param pipelineId The pipeline ID
     * @return Map of severity level to count
     */
    suspend fun getScanStatistics(pipelineId: String): Map<ThreatSeverity, Int> = 
        withContext(Dispatchers.IO) {
            val stats = mutableMapOf<ThreatSeverity, Int>()
            
            ThreatSeverity.values().forEach { severity ->
                val count = threatDao.getUnresolvedCountBySeverity(severity.level)
                stats[severity] = count
            }
            
            stats
        }
    
    /**
     * Mask a secret value for safe storage/display
     * 
     * Shows first 4 and last 4 characters, masks the middle
     * 
     * @param secret The secret to mask
     * @return Masked secret
     */
    private fun maskSecret(secret: String): String {
        return when {
            secret.length <= 8 -> "****"
            secret.length <= 16 -> "${secret.take(4)}****${secret.takeLast(4)}"
            else -> "${secret.take(6)}****${secret.takeLast(6)}"
        }
    }
    
    /**
     * Check if a specific pattern exists in content
     * 
     * @param content Content to check
     * @param pattern Pattern to search for
     * @return True if pattern found
     */
    fun containsPattern(content: String, pattern: SecretPattern): Boolean {
        return pattern.regex.containsMatchIn(content)
    }
    
    /**
     * Quick scan to check if content contains any secrets
     * 
     * @param content Content to scan
     * @return True if any secret detected
     */
    fun containsSecrets(content: String): Boolean {
        return SecretPattern.values().any { pattern ->
            pattern.regex.containsMatchIn(content)
        }
    }
    
    /**
     * Validate that a string is safe (no secrets detected)
     * 
     * @param text Text to validate
     * @return True if safe, false if secrets detected
     */
    fun isSafe(text: String): Boolean {
        return !containsSecrets(text)
    }
}
