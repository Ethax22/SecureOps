package com.secureops.app.data.offline

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.dao.ThreatDao
import com.secureops.app.data.local.entity.ThreatEntity
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.CIProvider
import com.secureops.app.domain.model.Pipeline
import com.secureops.app.ml.security.ThreatSeverity
import com.secureops.app.data.local.entity.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Demo Data Generator
 * 
 * Generates realistic demo data for testing and demonstrations
 * Creates pipelines, builds, and threats when in demo mode
 */
@Singleton
class DemoDataGenerator @Inject constructor(
    private val pipelineDao: PipelineDao,
    private val threatDao: ThreatDao
) {
    
    companion object {
        private const val DEMO_ACCOUNT_ID = "demo_account_001"
        
        // Demo repositories
        private val DEMO_REPOS = listOf(
            "frontend-app" to "Main web application",
            "backend-api" to "REST API service",
            "mobile-app" to "Android mobile app",
            "infrastructure" to "Terraform infrastructure",
            "data-pipeline" to "ETL data pipeline",
            "ml-service" to "Machine learning service",
            "auth-service" to "Authentication service",
            "notification-service" to "Notification service"
        )
        
        // Demo branches
        private val BRANCHES = listOf("main", "develop", "staging", "feature/new-ui", "hotfix/security-patch")
        
        // Demo commit messages
        private val COMMIT_MESSAGES = listOf(
            "Add user authentication",
            "Fix memory leak in service",
            "Update dependencies",
            "Implement new feature",
            "Refactor database queries",
            "Add unit tests",
            "Fix security vulnerability",
            "Improve performance",
            "Update documentation",
            "Deploy version 2.1.0"
        )
        
        // Demo authors
        private val AUTHORS = listOf(
            "Alice Johnson",
            "Bob Smith", 
            "Carol White",
            "David Brown",
            "Eve Martinez",
            "Frank Wilson"
        )
    }
    
    /**
     * Generate complete demo dataset
     */
    suspend fun generateDemoData(pipelineCount: Int = 20): DemoDataSummary = withContext(Dispatchers.IO) {
        Timber.i("🎭 Generating demo data: $pipelineCount pipelines")
        
        val pipelines = mutableListOf<Pipeline>()
        val threats = mutableListOf<ThreatEntity>()
        
        // Generate pipelines
        repeat(pipelineCount) { index ->
            val pipeline = generateDemoPipeline(index)
            pipelines.add(pipeline)
            
            // 30% chance to generate threats for this pipeline
            if (Random.nextFloat() < 0.3f && pipeline.status == BuildStatus.FAILURE) {
                val pipelineThreats = generateThreatsForPipeline(pipeline)
                threats.addAll(pipelineThreats)
            }
        }
        
        // Insert into database
        pipelines.forEach { pipelineDao.insertPipeline(it.toEntity()) }
        if (threats.isNotEmpty()) {
            threatDao.insertAll(threats)
        }
        
        Timber.i("✅ Demo data generated: ${pipelines.size} pipelines, ${threats.size} threats")
        
        DemoDataSummary(
            pipelinesGenerated = pipelines.size,
            threatsGenerated = threats.size,
            successfulBuilds = pipelines.count { it.status == BuildStatus.SUCCESS },
            failedBuilds = pipelines.count { it.status == BuildStatus.FAILURE },
            runningBuilds = pipelines.count { it.status == BuildStatus.RUNNING }
        )
    }
    
    /**
     * Generate a single demo pipeline
     */
    private fun generateDemoPipeline(index: Int): Pipeline {
        val (repoName, repoDesc) = DEMO_REPOS.random()
        val buildNumber = Random.nextInt(100, 500)
        val branch = BRANCHES.random()
        val author = AUTHORS.random()
        val commitMessage = COMMIT_MESSAGES.random()
        
        // Status distribution: 60% success, 25% failure, 10% running, 5% other
        val status = when (Random.nextFloat()) {
            in 0f..0.6f -> BuildStatus.SUCCESS
            in 0.6f..0.85f -> BuildStatus.FAILURE
            in 0.85f..0.95f -> BuildStatus.RUNNING
            else -> listOf(BuildStatus.PENDING, BuildStatus.SKIPPED, BuildStatus.CANCELED).random()
        }
        
        val provider = listOf(
            CIProvider.GITHUB_ACTIONS,
            CIProvider.GITLAB_CI,
            CIProvider.JENKINS,
            CIProvider.CIRCLE_CI,
            CIProvider.AZURE_DEVOPS
        ).random()
        
        val timestamp = System.currentTimeMillis() - Random.nextLong(0, 7 * 24 * 60 * 60 * 1000) // Last 7 days
        
        val duration = when (status) {
            BuildStatus.SUCCESS -> Random.nextInt(120, 600) // 2-10 minutes
            BuildStatus.FAILURE -> Random.nextInt(60, 300)  // 1-5 minutes
            BuildStatus.RUNNING -> Random.nextInt(30, 180)  // 30s-3min so far
            else -> Random.nextInt(60, 300)
        }
        
        return Pipeline(
            id = "demo_pipeline_${index}_${System.currentTimeMillis()}",
            accountId = DEMO_ACCOUNT_ID,
            provider = provider,
            repositoryName = repoName,
            repositoryUrl = "https://github.com/demo/$repoName",
            branch = branch,
            commitHash = generateCommitSha(),
            commitMessage = commitMessage,
            commitAuthor = author,
            startedAt = timestamp,
            finishedAt = if (status == BuildStatus.SUCCESS || status == BuildStatus.FAILURE) timestamp + duration * 1000L else null,
            duration = duration.toLong(),
            triggeredBy = author,
            webUrl = "https://github.com/demo/$repoName/actions/runs/${buildNumber}",
            status = status,
            buildNumber = buildNumber,
            logs = generateDemoLogs(status),
            logsCachedAt = if (status != BuildStatus.RUNNING) timestamp else null
        )
    }
    
    /**
     * Generate threats for a pipeline
     */
    private fun generateThreatsForPipeline(pipeline: Pipeline): List<ThreatEntity> {
        val threats = mutableListOf<ThreatEntity>()
        val threatCount = Random.nextInt(1, 4) // 1-3 threats
        
        repeat(threatCount) {
            threats.add(generateDemoThreat(pipeline))
        }
        
        return threats
    }
    
    /**
     * Generate a single demo threat
     */
    private fun generateDemoThreat(pipeline: Pipeline): ThreatEntity {
        val threatTypes = listOf(
            Triple("AWS_ACCESS_KEY", ThreatSeverity.CRITICAL, "AWS Access Key detected in code"),
            Triple("GITHUB_TOKEN", ThreatSeverity.CRITICAL, "GitHub Personal Access Token exposed"),
            Triple("JWT_TOKEN", ThreatSeverity.HIGH, "JSON Web Token found in logs"),
            Triple("API_KEY", ThreatSeverity.HIGH, "Generic API key detected"),
            Triple("PASSWORD_IN_CODE", ThreatSeverity.HIGH, "Hardcoded password found"),
            Triple("DEPENDENCY_HIGH", ThreatSeverity.HIGH, "Vulnerable dependency detected"),
            Triple("ANOMALY_BUILD_DURATION", ThreatSeverity.MEDIUM, "Unusual build duration detected")
        )
        
        val (patternType, severity, description) = threatTypes.random()
        
        return ThreatEntity(
            patternType = patternType,
            severity = severity.level,
            description = description,
            detectedValue = generateMaskedSecret(patternType),
            source = "src/${listOf("main", "config", "utils", "services").random()}.${listOf("kt", "java", "js", "py").random()}",
            lineNumber = Random.nextInt(10, 500),
            pipelineId = pipeline.id,
            buildNumber = pipeline.buildNumber,
            repositoryName = pipeline.repositoryName,
            branch = pipeline.branch,
            commitHash = pipeline.commitHash,
            contextLine = generateContextLine(patternType),
            detectedAt = pipeline.startedAt ?: System.currentTimeMillis(),
            isResolved = Random.nextBoolean(),
            accountId = DEMO_ACCOUNT_ID
        )
    }
    
    /**
     * Generate demo logs based on build status
     */
    private fun generateDemoLogs(status: BuildStatus): String {
        return when (status) {
            BuildStatus.SUCCESS -> """
                [INFO] Starting build...
                [INFO] Installing dependencies...
                [INFO] Dependencies installed successfully
                [INFO] Running tests...
                [INFO] All tests passed ✓
                [INFO] Building application...
                [INFO] Build completed successfully
                [INFO] Deploying to staging...
                [SUCCESS] Deployment successful!
            """.trimIndent()
            
            BuildStatus.FAILURE -> """
                [INFO] Starting build...
                [INFO] Installing dependencies...
                [WARN] Some dependencies outdated
                [INFO] Running tests...
                [ERROR] Test suite failed
                [ERROR] 3 tests failed:
                [ERROR]   - test_user_authentication
                [ERROR]   - test_database_connection
                [ERROR]   - test_api_endpoint
                [ERROR] Build failed with exit code 1
            """.trimIndent()
            
            BuildStatus.RUNNING -> """
                [INFO] Starting build...
                [INFO] Installing dependencies...
                [INFO] Running tests...
                [INFO] Test suite in progress...
            """.trimIndent()
            
            else -> """
                [INFO] Build status: $status
                [INFO] Please check build configuration
            """.trimIndent()
        }
    }
    
    /**
     * Generate commit SHA
     */
    private fun generateCommitSha(): String {
        return (1..40).map { 
            "0123456789abcdef".random() 
        }.joinToString("")
    }
    
    /**
     * Generate masked secret value
     */
    private fun generateMaskedSecret(patternType: String): String {
        return when {
            patternType.contains("AWS") -> "AKIA****EXAMPLE****3XYZ"
            patternType.contains("GITHUB") -> "ghp_****ExampleToken****abcd"
            patternType.contains("JWT") -> "eyJ****token****abcd"
            patternType.contains("KEY") -> "sk_****key****1234"
            patternType.contains("PASSWORD") -> "pass****word"
            else -> "****secret****"
        }
    }
    
    /**
     * Generate context line
     */
    private fun generateContextLine(patternType: String): String {
        return when {
            patternType.contains("AWS") -> "const AWS_ACCESS_KEY = 'AKIA...';"
            patternType.contains("GITHUB") -> "GITHUB_TOKEN=ghp_..."
            patternType.contains("JWT") -> "Authorization: Bearer eyJ..."
            patternType.contains("KEY") -> "apiKey: 'sk_live_...'"
            patternType.contains("PASSWORD") -> "password = 'hardcoded123'"
            patternType.contains("DEPENDENCY") -> "dependencies: vulnerable-package@1.2.3"
            else -> "Anomaly detected in build metrics"
        }
    }
    
    /**
     * Clear all demo data
     */
    suspend fun clearDemoData() = withContext(Dispatchers.IO) {
        try {
            // Delete demo pipelines
            val pipelines = pipelineDao.getAllPipelines().first()
            val demoPipelines = pipelines.filter { it.accountId == DEMO_ACCOUNT_ID }
            
            demoPipelines.forEach { pipelineDao.deletePipeline(it) }
            
            Timber.i("Cleared ${demoPipelines.size} demo pipelines")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear demo data")
        }
    }
    
    /**
     * Check if demo data exists
     */
    suspend fun hasDemoData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pipelines = pipelineDao.getAllPipelines().first()
            pipelines.any { it.accountId == DEMO_ACCOUNT_ID }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Demo data generation summary
     */
    data class DemoDataSummary(
        val pipelinesGenerated: Int,
        val threatsGenerated: Int,
        val successfulBuilds: Int,
        val failedBuilds: Int,
        val runningBuilds: Int
    ) {
        override fun toString(): String {
            return """
                Demo Data Summary:
                  Pipelines: $pipelinesGenerated
                  Threats: $threatsGenerated
                  Success: $successfulBuilds
                  Failed: $failedBuilds
                  Running: $runningBuilds
            """.trimIndent()
        }
    }
}
