package com.secureops.app.data.remediation

import com.secureops.app.domain.model.Pipeline
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Failure Type Detector
 * 
 * Analyzes build logs and metadata to classify failure types and detect patterns
 */
@Singleton
class FailureTypeDetector @Inject constructor() {
    
    /**
     * Failure detection result
     */
    data class FailureDetection(
        val failureType: String,
        val failurePattern: String,
        val confidence: Double,
        val context: String,
        val logSnippet: String?,
        val recommendations: List<String>
    )
    
    /**
     * Failure type constants
     */
    object FailureTypes {
        const val TRANSIENT = "TRANSIENT"
        const val FLAKY_TEST = "FLAKY_TEST"
        const val TIMEOUT = "TIMEOUT"
        const val RESOURCE_LIMIT = "RESOURCE_LIMIT"
        const val DEPLOYMENT = "DEPLOYMENT"
        const val BUILD = "BUILD"
        const val TEST = "TEST"
        const val DEPENDENCY = "DEPENDENCY"
        const val CONFIGURATION = "CONFIGURATION"
        const val PERMANENT = "PERMANENT"
        const val UNKNOWN = "UNKNOWN"
    }
    
    /**
     * Detect failure type from pipeline
     */
    fun detectFailure(pipeline: Pipeline): FailureDetection {
        val logs = pipeline.logs ?: ""
        
        // Try to detect specific patterns
        val detection = detectTransientFailure(logs)
            ?: detectFlakyTest(logs)
            ?: detectTimeout(logs)
            ?: detectResourceLimit(logs)
            ?: detectDeploymentFailure(logs)
            ?: detectBuildFailure(logs)
            ?: detectTestFailure(logs)
            ?: detectDependencyFailure(logs)
            ?: detectConfigurationFailure(logs)
            ?: createUnknownFailure(logs)
        
        Timber.d("Detected failure: ${detection.failureType} (${detection.failurePattern})")
        return detection
    }
    
    /**
     * Detect transient failures (network, temporary service issues)
     */
    private fun detectTransientFailure(logs: String): FailureDetection? {
        val transientPatterns = listOf(
            "Connection refused" to "connection_refused",
            "Connection timeout" to "connection_timeout",
            "Connection reset" to "connection_reset",
            "Network is unreachable" to "network_unreachable",
            "Temporary failure" to "temporary_failure",
            "503 Service Unavailable" to "service_unavailable",
            "502 Bad Gateway" to "bad_gateway",
            "504 Gateway Timeout" to "gateway_timeout",
            "ECONNREFUSED" to "econnrefused",
            "ETIMEDOUT" to "etimedout",
            "socket hang up" to "socket_hangup",
            "rate limit exceeded" to "rate_limit"
        )
        
        for ((pattern, patternName) in transientPatterns) {
            if (logs.contains(pattern, ignoreCase = true)) {
                val snippet = extractLogSnippet(logs, pattern)
                return FailureDetection(
                    failureType = FailureTypes.TRANSIENT,
                    failurePattern = patternName,
                    confidence = 0.9,
                    context = "Transient network or service issue detected",
                    logSnippet = snippet,
                    recommendations = listOf(
                        "RETRY_BUILD",
                        "INCREASE_TIMEOUT",
                        "CHECK_SERVICE_STATUS"
                    )
                )
            }
        }
        
        return null
    }
    
    /**
     * Detect flaky tests
     */
    private fun detectFlakyTest(logs: String): FailureDetection? {
        val flakyPatterns = listOf(
            "test.*flaky" to "test_marked_flaky",
            "intermittent.*failure" to "intermittent_failure",
            "test.*passed.*previously" to "test_passed_before",
            "timing.*issue" to "timing_issue",
            "race.*condition" to "race_condition",
            "test.*unstable" to "test_unstable"
        )
        
        for ((pattern, patternName) in flakyPatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(logs)) {
                val snippet = extractLogSnippet(logs, pattern)
                return FailureDetection(
                    failureType = FailureTypes.FLAKY_TEST,
                    failurePattern = patternName,
                    confidence = 0.85,
                    context = "Flaky test detected - inconsistent test behavior",
                    logSnippet = snippet,
                    recommendations = listOf(
                        "RERUN_SPECIFIC_TESTS",
                        "SKIP_FLAKY_TESTS",
                        "INVESTIGATE_TEST"
                    )
                )
            }
        }
        
        // Check for test failures with pass history
        if (logs.contains("FAILED", ignoreCase = true) && 
            logs.contains("test", ignoreCase = true)) {
            val snippet = extractLogSnippet(logs, "FAILED")
            return FailureDetection(
                failureType = FailureTypes.FLAKY_TEST,
                failurePattern = "test_failure_suspected_flaky",
                confidence = 0.6,
                context = "Test failure that may be flaky",
                logSnippet = snippet,
                recommendations = listOf(
                    "RERUN_SPECIFIC_TESTS",
                    "MANUAL_INVESTIGATION"
                )
            )
        }
        
        return null
    }
    
    /**
     * Detect timeout failures
     */
    private fun detectTimeout(logs: String): FailureDetection? {
        val timeoutPatterns = listOf(
            "timeout" to "generic_timeout",
            "timed out" to "timed_out",
            "execution.*exceeded" to "execution_exceeded",
            "deadline exceeded" to "deadline_exceeded",
            "operation took too long" to "operation_too_long",
            "build.*timeout" to "build_timeout",
            "test.*timeout" to "test_timeout",
            "npm install.*timeout" to "npm_install_timeout",
            "download.*timeout" to "download_timeout"
        )
        
        for ((pattern, patternName) in timeoutPatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(logs)) {
                val snippet = extractLogSnippet(logs, pattern)
                return FailureDetection(
                    failureType = FailureTypes.TIMEOUT,
                    failurePattern = patternName,
                    confidence = 0.95,
                    context = "Operation exceeded time limit",
                    logSnippet = snippet,
                    recommendations = listOf(
                        "INCREASE_TIMEOUT",
                        "OPTIMIZE_BUILD",
                        "RETRY_BUILD"
                    )
                )
            }
        }
        
        return null
    }
    
    /**
     * Detect resource limit failures
     */
    private fun detectResourceLimit(logs: String): FailureDetection? {
        val resourcePatterns = listOf(
            "out of memory" to "out_of_memory",
            "OOM" to "oom",
            "heap space" to "heap_space",
            "disk.*full" to "disk_full",
            "no space left" to "no_space_left",
            "resource.*limit" to "resource_limit",
            "too many open files" to "too_many_files",
            "memory.*exhausted" to "memory_exhausted",
            "CPU.*limit" to "cpu_limit"
        )
        
        for ((pattern, patternName) in resourcePatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(logs)) {
                val snippet = extractLogSnippet(logs, pattern)
                return FailureDetection(
                    failureType = FailureTypes.RESOURCE_LIMIT,
                    failurePattern = patternName,
                    confidence = 0.92,
                    context = "System resource limit reached",
                    logSnippet = snippet,
                    recommendations = listOf(
                        "INCREASE_RESOURCES",
                        "CLEAR_CACHE",
                        "OPTIMIZE_BUILD"
                    )
                )
            }
        }
        
        return null
    }
    
    /**
     * Detect deployment failures
     */
    private fun detectDeploymentFailure(logs: String): FailureDetection? {
        val deploymentPatterns = listOf(
            "deployment.*failed" to "deployment_failed",
            "rollout.*failed" to "rollout_failed",
            "container.*failed" to "container_failed",
            "pod.*crash" to "pod_crash",
            "image.*pull.*failed" to "image_pull_failed",
            "health check failed" to "health_check_failed",
            "service.*unavailable" to "service_unavailable",
            "port.*already.*use" to "port_in_use"
        )
        
        for ((pattern, patternName) in deploymentPatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(logs)) {
                val snippet = extractLogSnippet(logs, pattern)
                return FailureDetection(
                    failureType = FailureTypes.DEPLOYMENT,
                    failurePattern = patternName,
                    confidence = 0.88,
                    context = "Deployment or service startup failed",
                    logSnippet = snippet,
                    recommendations = listOf(
                        "ROLLBACK_DEPLOYMENT",
                        "CHECK_SERVICE_STATUS",
                        "RESTART_SERVICES"
                    )
                )
            }
        }
        
        return null
    }
    
    /**
     * Detect build compilation failures
     */
    private fun detectBuildFailure(logs: String): FailureDetection? {
        val buildPatterns = listOf(
            "compilation.*failed" to "compilation_failed",
            "build.*failed" to "build_failed",
            "syntax.*error" to "syntax_error",
            "cannot find symbol" to "symbol_not_found",
            "undefined reference" to "undefined_reference",
            "linker.*error" to "linker_error",
            "gradle.*build.*failed" to "gradle_build_failed",
            "npm.*build.*failed" to "npm_build_failed"
        )
        
        for ((pattern, patternName) in buildPatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(logs)) {
                val snippet = extractLogSnippet(logs, pattern)
                return FailureDetection(
                    failureType = FailureTypes.BUILD,
                    failurePattern = patternName,
                    confidence = 0.87,
                    context = "Build compilation or assembly failed",
                    logSnippet = snippet,
                    recommendations = listOf(
                        "CLEAR_CACHE",
                        "UPDATE_DEPENDENCIES",
                        "MANUAL_INTERVENTION"
                    )
                )
            }
        }
        
        return null
    }
    
    /**
     * Detect test failures
     */
    private fun detectTestFailure(logs: String): FailureDetection? {
        if (logs.contains("test", ignoreCase = true) && 
            (logs.contains("failed", ignoreCase = true) || logs.contains("error", ignoreCase = true))) {
            val snippet = extractLogSnippet(logs, "test.*failed")
            return FailureDetection(
                failureType = FailureTypes.TEST,
                failurePattern = "test_suite_failed",
                confidence = 0.75,
                context = "Test suite execution failed",
                logSnippet = snippet,
                recommendations = listOf(
                    "RERUN_SPECIFIC_TESTS",
                    "SKIP_FAILING_TESTS",
                    "MANUAL_INVESTIGATION"
                )
            )
        }
        return null
    }
    
    /**
     * Detect dependency failures
     */
    private fun detectDependencyFailure(logs: String): FailureDetection? {
        val dependencyPatterns = listOf(
            "dependency.*not.*found" to "dependency_not_found",
            "package.*not.*found" to "package_not_found",
            "module.*not.*found" to "module_not_found",
            "npm.*install.*failed" to "npm_install_failed",
            "cannot resolve" to "cannot_resolve",
            "version.*conflict" to "version_conflict",
            "peer.*dependency" to "peer_dependency_issue"
        )
        
        for ((pattern, patternName) in dependencyPatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(logs)) {
                val snippet = extractLogSnippet(logs, pattern)
                return FailureDetection(
                    failureType = FailureTypes.DEPENDENCY,
                    failurePattern = patternName,
                    confidence = 0.89,
                    context = "Dependency resolution or installation failed",
                    logSnippet = snippet,
                    recommendations = listOf(
                        "UPDATE_DEPENDENCIES",
                        "CLEAR_CACHE",
                        "RETRY_BUILD"
                    )
                )
            }
        }
        
        return null
    }
    
    /**
     * Detect configuration failures
     */
    private fun detectConfigurationFailure(logs: String): FailureDetection? {
        val configPatterns = listOf(
            "configuration.*error" to "config_error",
            "invalid.*config" to "invalid_config",
            "missing.*environment" to "missing_env",
            "environment.*not.*set" to "env_not_set",
            "permission.*denied" to "permission_denied",
            "authentication.*failed" to "auth_failed",
            "credentials.*invalid" to "invalid_credentials"
        )
        
        for ((pattern, patternName) in configPatterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(logs)) {
                val snippet = extractLogSnippet(logs, pattern)
                return FailureDetection(
                    failureType = FailureTypes.CONFIGURATION,
                    failurePattern = patternName,
                    confidence = 0.83,
                    context = "Configuration or environment issue",
                    logSnippet = snippet,
                    recommendations = listOf(
                        "CHECK_CONFIGURATION",
                        "VERIFY_CREDENTIALS",
                        "MANUAL_INTERVENTION"
                    )
                )
            }
        }
        
        return null
    }
    
    /**
     * Create unknown failure detection
     */
    private fun createUnknownFailure(logs: String): FailureDetection {
        val snippet = logs.take(500)
        return FailureDetection(
            failureType = FailureTypes.UNKNOWN,
            failurePattern = "unknown_failure",
            confidence = 0.3,
            context = "Unable to classify failure type",
            logSnippet = snippet,
            recommendations = listOf(
                "MANUAL_INVESTIGATION",
                "CHECK_LOGS",
                "RETRY_BUILD"
            )
        )
    }
    
    /**
     * Extract relevant log snippet around pattern match
     */
    private fun extractLogSnippet(logs: String, pattern: String, contextLines: Int = 3): String {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        val match = regex.find(logs) ?: return logs.take(300)
        
        val lines = logs.lines()
        val matchLine = logs.substring(0, match.range.first).count { it == '\n' }
        
        val start = (matchLine - contextLines).coerceAtLeast(0)
        val end = (matchLine + contextLines + 1).coerceAtMost(lines.size)
        
        return lines.subList(start, end).joinToString("\n")
    }
}
