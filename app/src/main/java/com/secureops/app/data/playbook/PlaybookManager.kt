package com.secureops.app.data.playbook

import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import com.secureops.app.ml.RunAnywhereManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybookManager @Inject constructor(
    private val runAnywhereManager: RunAnywhereManager
) {

    /**
     * Get playbook for a specific error/failure
     */
    fun getPlaybookForError(error: String, pipeline: Pipeline): Playbook {
        Timber.d("Finding playbook for error: $error")

        // Try to find predefined playbook
        val predefinedPlaybook = findPredefinedPlaybook(error)
        if (predefinedPlaybook != null) {
            return predefinedPlaybook
        }

        // Return generic playbook if no specific one found
        return getGenericPlaybook(pipeline)
    }

    /**
     * Generate AI-powered playbook for specific failure
     */
    suspend fun generateAIPlaybook(pipeline: Pipeline, errorDetails: String): Playbook {
        Timber.d("Generating AI playbook for pipeline ${pipeline.id}")

        val prompt = buildString {
            appendLine("Generate a step-by-step incident response playbook for this build failure:")
            appendLine()
            appendLine("Repository: ${pipeline.repositoryName}")
            appendLine("Branch: ${pipeline.branch}")
            appendLine("Provider: ${pipeline.provider.displayName}")
            appendLine("Error: $errorDetails")
            appendLine()
            appendLine("Create a concise 5-step remediation guide.")
        }

        return try {
            val aiResponse = runAnywhereManager.generateText(prompt)
            val steps = aiResponse.getOrNull()?.let { parseAISteps(it) }
                ?: getGenericPlaybook(pipeline).steps

            Playbook(
                id = "ai_${System.currentTimeMillis()}",
                title = "AI-Generated Remediation Plan",
                description = "Customized response plan for this specific failure",
                category = PlaybookCategory.CUSTOM,
                severity = determineSeverity(pipeline),
                estimatedTime = "15-30 minutes",
                steps = steps,
                tags = listOf("ai-generated", pipeline.provider.displayName.lowercase())
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate AI playbook")
            getGenericPlaybook(pipeline)
        }
    }

    /**
     * Find predefined playbook based on error type
     */
    private fun findPredefinedPlaybook(error: String): Playbook? {
        val lowerError = error.lowercase()

        return when {
            lowerError.contains("timeout") -> getTimeoutPlaybook()
            lowerError.contains("memory") || lowerError.contains("oom") -> getMemoryPlaybook()
            lowerError.contains("network") || lowerError.contains("connection") -> getNetworkPlaybook()
            lowerError.contains("permission") || lowerError.contains("access denied") -> getPermissionPlaybook()
            lowerError.contains("test") && lowerError.contains("fail") -> getTestFailurePlaybook()
            lowerError.contains("dependency") || lowerError.contains("package") -> getDependencyPlaybook()
            lowerError.contains("docker") || lowerError.contains("container") -> getDockerPlaybook()
            lowerError.contains("deployment") || lowerError.contains("deploy") -> getDeploymentPlaybook()
            else -> null
        }
    }

    /**
     * Get all available playbooks
     */
    fun getAllPlaybooks(): List<Playbook> {
        return listOf(
            getTimeoutPlaybook(),
            getMemoryPlaybook(),
            getNetworkPlaybook(),
            getPermissionPlaybook(),
            getTestFailurePlaybook(),
            getDependencyPlaybook(),
            getDockerPlaybook(),
            getDeploymentPlaybook()
        )
    }

    // Predefined Playbooks

    private fun getTimeoutPlaybook() = Playbook(
        id = "timeout",
        title = "Build Timeout Resolution",
        description = "Steps to resolve build timeout issues",
        category = PlaybookCategory.PERFORMANCE,
        severity = PlaybookSeverity.HIGH,
        estimatedTime = "10-20 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Identify Timeout Stage",
                description = "Check which stage/step is timing out",
                actions = listOf("Review build logs", "Identify stuck process"),
                expectedResult = "Pinpoint exact timeout location"
            ),
            PlaybookStep(
                number = 2,
                title = "Check Resource Usage",
                description = "Verify if timeout is due to resource constraints",
                actions = listOf("Check CPU/Memory usage", "Review concurrent builds"),
                expectedResult = "Understand resource bottleneck"
            ),
            PlaybookStep(
                number = 3,
                title = "Increase Timeout",
                description = "Temporarily increase timeout limits",
                actions = listOf("Update CI/CD config", "Add timeout parameters"),
                expectedResult = "Build completes without timeout"
            ),
            PlaybookStep(
                number = 4,
                title = "Optimize Build",
                description = "Implement long-term optimizations",
                actions = listOf("Add caching", "Parallelize tasks", "Remove redundant steps"),
                expectedResult = "Faster build times"
            ),
            PlaybookStep(
                number = 5,
                title = "Monitor",
                description = "Track build performance over time",
                actions = listOf("Set up alerts", "Review analytics"),
                expectedResult = "Stable build times"
            )
        ),
        tags = listOf("timeout", "performance", "optimization")
    )

    private fun getMemoryPlaybook() = Playbook(
        id = "memory",
        title = "Out of Memory (OOM) Resolution",
        description = "Steps to resolve memory-related build failures",
        category = PlaybookCategory.INFRASTRUCTURE,
        severity = PlaybookSeverity.CRITICAL,
        estimatedTime = "15-25 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Confirm Memory Issue",
                description = "Verify OOM is the actual cause",
                actions = listOf("Check for OOM errors in logs", "Review heap dumps"),
                expectedResult = "Confirmed memory exhaustion"
            ),
            PlaybookStep(
                number = 2,
                title = "Increase Memory Limits",
                description = "Allocate more memory to build",
                actions = listOf("Update JVM heap size", "Increase container memory"),
                expectedResult = "Build has sufficient memory"
            ),
            PlaybookStep(
                number = 3,
                title = "Identify Memory Leaks",
                description = "Find code causing excessive memory use",
                actions = listOf("Profile memory usage", "Review recent changes"),
                expectedResult = "Located memory leak source"
            ),
            PlaybookStep(
                number = 4,
                title = "Optimize Memory Usage",
                description = "Implement memory optimizations",
                actions = listOf("Fix memory leaks", "Reduce object allocations"),
                expectedResult = "Reduced memory footprint"
            ),
            PlaybookStep(
                number = 5,
                title = "Set Up Monitoring",
                description = "Track memory usage trends",
                actions = listOf("Add memory metrics", "Configure alerts"),
                expectedResult = "Proactive memory monitoring"
            )
        ),
        tags = listOf("memory", "oom", "resources")
    )

    private fun getNetworkPlaybook() = Playbook(
        id = "network",
        title = "Network Connectivity Issues",
        description = "Steps to resolve network-related build failures",
        category = PlaybookCategory.INFRASTRUCTURE,
        severity = PlaybookSeverity.HIGH,
        estimatedTime = "10-15 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Verify Network Status",
                description = "Check if external services are reachable",
                actions = listOf("Test API endpoints", "Check DNS resolution"),
                expectedResult = "Confirmed network issue"
            ),
            PlaybookStep(
                number = 2,
                title = "Check Firewall Rules",
                description = "Ensure necessary ports are open",
                actions = listOf("Review firewall config", "Whitelist required IPs"),
                expectedResult = "Network access configured"
            ),
            PlaybookStep(
                number = 3,
                title = "Implement Retries",
                description = "Add retry logic for transient failures",
                actions = listOf("Configure retry attempts", "Add exponential backoff"),
                expectedResult = "Resilient to network blips"
            ),
            PlaybookStep(
                number = 4,
                title = "Use Local Caching",
                description = "Cache dependencies locally",
                actions = listOf("Enable dependency caching", "Mirror critical resources"),
                expectedResult = "Reduced network dependencies"
            ),
            PlaybookStep(
                number = 5,
                title = "Monitor Network Health",
                description = "Track network reliability",
                actions = listOf("Set up uptime monitoring", "Configure alerts"),
                expectedResult = "Network issues detected early"
            )
        ),
        tags = listOf("network", "connectivity", "infrastructure")
    )

    private fun getPermissionPlaybook() = Playbook(
        id = "permission",
        title = "Permission & Access Issues",
        description = "Steps to resolve permission-related failures",
        category = PlaybookCategory.SECURITY,
        severity = PlaybookSeverity.MEDIUM,
        estimatedTime = "10-20 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Identify Permission Error",
                description = "Determine what access is denied",
                actions = listOf("Review error messages", "Check file/resource path"),
                expectedResult = "Know what permission is missing"
            ),
            PlaybookStep(
                number = 2,
                title = "Verify Credentials",
                description = "Check authentication tokens and keys",
                actions = listOf("Verify API tokens", "Check SSH keys", "Validate credentials"),
                expectedResult = "Credentials are valid"
            ),
            PlaybookStep(
                number = 3,
                title = "Update Permissions",
                description = "Grant necessary permissions",
                actions = listOf(
                    "Update IAM roles",
                    "Modify file permissions",
                    "Add to access groups"
                ),
                expectedResult = "Access granted"
            ),
            PlaybookStep(
                number = 4,
                title = "Test Access",
                description = "Verify permissions are working",
                actions = listOf("Run permission check", "Test build manually"),
                expectedResult = "Access confirmed"
            ),
            PlaybookStep(
                number = 5,
                title = "Document Permissions",
                description = "Record required permissions",
                actions = listOf("Update documentation", "Create permission matrix"),
                expectedResult = "Clear permission requirements"
            )
        ),
        tags = listOf("permission", "access", "security")
    )

    private fun getTestFailurePlaybook() = Playbook(
        id = "test_failure",
        title = "Test Failure Investigation",
        description = "Steps to investigate and fix failing tests",
        category = PlaybookCategory.TESTING,
        severity = PlaybookSeverity.MEDIUM,
        estimatedTime = "20-40 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Identify Failing Tests",
                description = "List all tests that failed",
                actions = listOf("Review test results", "Check test logs"),
                expectedResult = "Complete list of failures"
            ),
            PlaybookStep(
                number = 2,
                title = "Check for Flaky Tests",
                description = "Determine if tests are flaky",
                actions = listOf(
                    "Check test history",
                    "Rerun tests",
                    "Look for intermittent patterns"
                ),
                expectedResult = "Identified flaky vs broken tests"
            ),
            PlaybookStep(
                number = 3,
                title = "Review Recent Changes",
                description = "Check commits that might have broken tests",
                actions = listOf("Review commit history", "Compare with last passing build"),
                expectedResult = "Potential culprit identified"
            ),
            PlaybookStep(
                number = 4,
                title = "Fix or Skip",
                description = "Fix test or temporarily skip if blocking",
                actions = listOf("Fix test code", "Update test data", "Skip flaky tests"),
                expectedResult = "Tests passing or skipped"
            ),
            PlaybookStep(
                number = 5,
                title = "Improve Test Reliability",
                description = "Stabilize tests long-term",
                actions = listOf(
                    "Add retry logic",
                    "Improve test isolation",
                    "Add better assertions"
                ),
                expectedResult = "Stable test suite"
            )
        ),
        tags = listOf("test", "failure", "quality")
    )

    private fun getDependencyPlaybook() = Playbook(
        id = "dependency",
        title = "Dependency Resolution Issues",
        description = "Steps to resolve dependency-related failures",
        category = PlaybookCategory.BUILD,
        severity = PlaybookSeverity.HIGH,
        estimatedTime = "15-30 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Identify Problematic Dependency",
                description = "Find which dependency is causing issues",
                actions = listOf("Check error logs", "Review dependency tree"),
                expectedResult = "Problem dependency identified"
            ),
            PlaybookStep(
                number = 2,
                title = "Check Version Conflicts",
                description = "Look for version incompatibilities",
                actions = listOf("Review dependency versions", "Check for conflicts"),
                expectedResult = "Version conflicts identified"
            ),
            PlaybookStep(
                number = 3,
                title = "Update or Pin Versions",
                description = "Fix dependency versions",
                actions = listOf("Update to compatible version", "Pin working version"),
                expectedResult = "Compatible dependencies"
            ),
            PlaybookStep(
                number = 4,
                title = "Clear Cache",
                description = "Remove stale dependency cache",
                actions = listOf("Clear build cache", "Delete lock files", "Force refresh"),
                expectedResult = "Fresh dependencies"
            ),
            PlaybookStep(
                number = 5,
                title = "Lock Dependencies",
                description = "Prevent future version drift",
                actions = listOf("Generate lock file", "Pin all versions"),
                expectedResult = "Reproducible builds"
            )
        ),
        tags = listOf("dependency", "build", "versioning")
    )

    private fun getDockerPlaybook() = Playbook(
        id = "docker",
        title = "Docker/Container Issues",
        description = "Steps to resolve container-related failures",
        category = PlaybookCategory.INFRASTRUCTURE,
        severity = PlaybookSeverity.MEDIUM,
        estimatedTime = "15-25 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Check Container Logs",
                description = "Review container startup and runtime logs",
                actions = listOf("View container logs", "Check exit codes"),
                expectedResult = "Error location identified"
            ),
            PlaybookStep(
                number = 2,
                title = "Verify Image",
                description = "Ensure container image is valid",
                actions = listOf("Check image exists", "Verify image tag", "Test locally"),
                expectedResult = "Valid container image"
            ),
            PlaybookStep(
                number = 3,
                title = "Check Resources",
                description = "Verify container has sufficient resources",
                actions = listOf("Check CPU/memory limits", "Review resource quotas"),
                expectedResult = "Adequate resources allocated"
            ),
            PlaybookStep(
                number = 4,
                title = "Update Dockerfile",
                description = "Fix Dockerfile issues",
                actions = listOf("Review Dockerfile", "Fix configuration", "Update base image"),
                expectedResult = "Working Dockerfile"
            ),
            PlaybookStep(
                number = 5,
                title = "Test Container",
                description = "Verify container works locally",
                actions = listOf("Build image locally", "Run container", "Test functionality"),
                expectedResult = "Container working correctly"
            )
        ),
        tags = listOf("docker", "container", "infrastructure")
    )

    private fun getDeploymentPlaybook() = Playbook(
        id = "deployment",
        title = "Deployment Failure Recovery",
        description = "Steps to recover from failed deployments",
        category = PlaybookCategory.DEPLOYMENT,
        severity = PlaybookSeverity.CRITICAL,
        estimatedTime = "10-30 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Assess Impact",
                description = "Determine if production is affected",
                actions = listOf(
                    "Check service health",
                    "Review error rates",
                    "Test critical paths"
                ),
                expectedResult = "Impact understood"
            ),
            PlaybookStep(
                number = 2,
                title = "Decide: Fix Forward or Rollback",
                description = "Choose recovery strategy",
                actions = listOf("Evaluate fix complexity", "Consider rollback time"),
                expectedResult = "Recovery strategy chosen"
            ),
            PlaybookStep(
                number = 3,
                title = "Execute Rollback (if needed)",
                description = "Revert to previous stable version",
                actions = listOf("Trigger rollback", "Monitor deployment", "Verify service health"),
                expectedResult = "Service restored"
            ),
            PlaybookStep(
                number = 4,
                title = "Fix Root Cause",
                description = "Address underlying issue",
                actions = listOf("Review failure logs", "Fix code/config", "Test fix"),
                expectedResult = "Issue resolved"
            ),
            PlaybookStep(
                number = 5,
                title = "Post-Mortem",
                description = "Document incident and improvements",
                actions = listOf("Write incident report", "Identify preventive measures"),
                expectedResult = "Lessons learned documented"
            )
        ),
        tags = listOf("deployment", "production", "critical")
    )

    private fun getGenericPlaybook(pipeline: Pipeline) = Playbook(
        id = "generic",
        title = "General Build Failure Resolution",
        description = "Generic troubleshooting steps for build failures",
        category = PlaybookCategory.GENERAL,
        severity = determineSeverity(pipeline),
        estimatedTime = "15-30 minutes",
        steps = listOf(
            PlaybookStep(
                number = 1,
                title = "Review Error Logs",
                description = "Examine build logs for error messages",
                actions = listOf(
                    "Open build logs",
                    "Search for 'error' keywords",
                    "Note stack traces"
                ),
                expectedResult = "Error location identified"
            ),
            PlaybookStep(
                number = 2,
                title = "Check Recent Changes",
                description = "Review commits since last successful build",
                actions = listOf("Compare with last passing build", "Review commit messages"),
                expectedResult = "Potential cause identified"
            ),
            PlaybookStep(
                number = 3,
                title = "Reproduce Locally",
                description = "Try to reproduce the failure on local machine",
                actions = listOf(
                    "Check out same commit",
                    "Run build locally",
                    "Compare environments"
                ),
                expectedResult = "Understand if environment-specific"
            ),
            PlaybookStep(
                number = 4,
                title = "Apply Fix",
                description = "Implement solution based on findings",
                actions = listOf("Fix identified issue", "Test fix locally", "Commit changes"),
                expectedResult = "Fix ready to deploy"
            ),
            PlaybookStep(
                number = 5,
                title = "Verify Fix",
                description = "Confirm build passes with fix",
                actions = listOf("Trigger new build", "Monitor progress", "Verify success"),
                expectedResult = "Build passing"
            )
        ),
        tags = listOf("general", "troubleshooting")
    )

    private fun determineSeverity(pipeline: Pipeline): PlaybookSeverity {
        return when {
            pipeline.branch in listOf("main", "master", "production") -> PlaybookSeverity.CRITICAL
            pipeline.status == BuildStatus.FAILURE -> PlaybookSeverity.HIGH
            else -> PlaybookSeverity.MEDIUM
        }
    }

    private fun parseAISteps(aiResponse: String): List<PlaybookStep> {
        // Simple parser for AI-generated steps
        val lines = aiResponse.lines().filter { it.isNotBlank() }
        val steps = mutableListOf<PlaybookStep>()

        var currentStep = 1
        lines.forEach { line ->
            if (line.matches(Regex("^\\d+\\."))) {
                val title = line.substringAfter(". ").trim()
                steps.add(
                    PlaybookStep(
                        number = currentStep++,
                        title = title,
                        description = title,
                        actions = listOf("Follow AI recommendations"),
                        expectedResult = "Issue resolved"
                    )
                )
            }
        }

        return steps.ifEmpty {
            getGenericPlaybook(
                Pipeline(
                    id = "",
                    repositoryName = "",
                    repositoryUrl = "",
                    branch = "",
                    buildNumber = 0,
                    status = BuildStatus.FAILURE,
                    startedAt = 0,
                    finishedAt = 0,
                    duration = 0,
                    provider = com.secureops.app.domain.model.CIProvider.GITHUB_ACTIONS,
                    triggeredBy = "",
                    commitHash = "",
                    commitMessage = "",
                    commitAuthor = "",
                    accountId = "",
                    webUrl = ""
                )
            ).steps
        }
    }
}

/**
 * Incident response playbook
 */
data class Playbook(
    val id: String,
    val title: String,
    val description: String,
    val category: PlaybookCategory,
    val severity: PlaybookSeverity,
    val estimatedTime: String,
    val steps: List<PlaybookStep>,
    val tags: List<String>
)

data class PlaybookStep(
    val number: Int,
    val title: String,
    val description: String,
    val actions: List<String>,
    val expectedResult: String
)

enum class PlaybookCategory {
    GENERAL,
    BUILD,
    TESTING,
    DEPLOYMENT,
    INFRASTRUCTURE,
    SECURITY,
    PERFORMANCE,
    CUSTOM
}

enum class PlaybookSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
