package com.secureops.app.ml

import com.secureops.app.domain.model.FailedStep
import com.secureops.app.domain.model.RootCauseAnalysis
import timber.log.Timber

/**
 * Analyzes failure logs to identify root causes
 */
class RootCauseAnalyzer {

    /**
     * Analyzes build failure logs and generates root cause analysis
     */
    fun analyzeLogs(logs: String, jobLogs: Map<String, String>): RootCauseAnalysis {
        try {
            val failedSteps = extractFailedSteps(logs, jobLogs)
            val technicalSummary = generateTechnicalSummary(failedSteps, logs)
            val plainEnglishSummary = generatePlainEnglishSummary(failedSteps, logs)
            val suggestedActions = generateSuggestedActions(failedSteps, logs)

            return RootCauseAnalysis(
                failedSteps = failedSteps,
                technicalSummary = technicalSummary,
                plainEnglishSummary = plainEnglishSummary,
                suggestedActions = suggestedActions
            )
        } catch (e: Exception) {
            Timber.e(e, "Error analyzing logs")
            return RootCauseAnalysis(
                failedSteps = emptyList(),
                technicalSummary = "Unable to analyze logs",
                plainEnglishSummary = "An error occurred during log analysis",
                suggestedActions = listOf("Review the logs manually")
            )
        }
    }

    private fun extractFailedSteps(logs: String, jobLogs: Map<String, String>): List<FailedStep> {
        val failedSteps = mutableListOf<FailedStep>()
        val lines = logs.lines()

        // Pattern matching for common failure indicators
        val errorPatterns = listOf(
            Regex("ERROR.*?:(.+?)$", RegexOption.MULTILINE),
            Regex("FAILED.*?:(.+?)$", RegexOption.MULTILINE),
            Regex("Exception in thread.*?:(.+?)$", RegexOption.MULTILINE),
            Regex("Error: (.+?)$", RegexOption.MULTILINE)
        )

        jobLogs.forEach { (jobName, jobLog) ->
            val jobErrors = mutableListOf<String>()

            errorPatterns.forEach { pattern ->
                pattern.findAll(jobLog).forEach { match ->
                    match.groups[1]?.value?.let { error ->
                        jobErrors.add(error.trim())
                    }
                }
            }

            if (jobErrors.isNotEmpty()) {
                val errorMessage = jobErrors.first()
                val stackTrace = extractStackTrace(jobLog, errorMessage)
                val exitCode = extractExitCode(jobLog)

                failedSteps.add(
                    FailedStep(
                        stepName = jobName,
                        errorMessage = errorMessage,
                        stackTrace = stackTrace,
                        exitCode = exitCode
                    )
                )
            }
        }

        // If no specific job logs, analyze main logs
        if (failedSteps.isEmpty()) {
            errorPatterns.forEach { pattern ->
                pattern.find(logs)?.let { match ->
                    match.groups[1]?.value?.let { error ->
                        failedSteps.add(
                            FailedStep(
                                stepName = "Build",
                                errorMessage = error.trim(),
                                stackTrace = extractStackTrace(logs, error),
                                exitCode = extractExitCode(logs)
                            )
                        )
                    }
                }
            }
        }

        return failedSteps
    }

    private fun extractStackTrace(logs: String, errorMessage: String): String? {
        val errorIndex = logs.indexOf(errorMessage)
        if (errorIndex == -1) return null

        val stackTraceBuilder = StringBuilder()
        val lines = logs.substring(errorIndex).lines().take(20)

        for (line in lines) {
            if (line.trim().startsWith("at ") || line.contains(".java:") || line.contains(".kt:")) {
                stackTraceBuilder.appendLine(line)
            } else if (stackTraceBuilder.isNotEmpty() && !line.trim().isEmpty()) {
                break
            }
        }

        return if (stackTraceBuilder.isEmpty()) null else stackTraceBuilder.toString()
    }

    private fun extractExitCode(logs: String): Int? {
        val exitCodePattern = Regex("exit code[: ](\\d+)", RegexOption.IGNORE_CASE)
        return exitCodePattern.find(logs)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun generateTechnicalSummary(failedSteps: List<FailedStep>, logs: String): String {
        if (failedSteps.isEmpty()) {
            return "Build failed without specific error messages. Review full logs for details."
        }

        val summary = StringBuilder()
        summary.appendLine("Build Failure Analysis:")
        summary.appendLine()

        failedSteps.forEachIndexed { index, step ->
            summary.appendLine("${index + 1}. Step: ${step.stepName}")
            summary.appendLine("   Error: ${step.errorMessage}")
            step.exitCode?.let { summary.appendLine("   Exit Code: $it") }
            summary.appendLine()
        }

        return summary.toString().trim()
    }

    private fun generatePlainEnglishSummary(failedSteps: List<FailedStep>, logs: String): String {
        if (failedSteps.isEmpty()) {
            return "The build failed, but the specific cause couldn't be determined automatically. Please review the logs manually."
        }

        val primaryFailure = failedSteps.first()
        val errorMessage = primaryFailure.errorMessage.lowercase()

        return when {
            errorMessage.contains("test") && errorMessage.contains("failed") -> {
                "Your build failed because some tests didn't pass. The '${primaryFailure.stepName}' step encountered test failures. " +
                        "Review the failing tests and fix the issues before trying again."
            }

            errorMessage.contains("timeout") -> {
                "The build timed out during the '${primaryFailure.stepName}' step. This usually means a process took too long to complete. " +
                        "Consider optimizing the step or increasing the timeout limit."
            }

            errorMessage.contains("memory") || errorMessage.contains("oom") -> {
                "The build ran out of memory during '${primaryFailure.stepName}'. " +
                        "Try allocating more memory to the build process or optimizing memory usage."
            }

            errorMessage.contains("not found") || errorMessage.contains("missing") -> {
                "A required file or dependency is missing. The system couldn't find what it needed during '${primaryFailure.stepName}'. " +
                        "Make sure all dependencies are properly configured."
            }

            errorMessage.contains("compile") || errorMessage.contains("syntax") -> {
                "There's a compilation error in your code. The '${primaryFailure.stepName}' step found syntax or compilation issues. " +
                        "Review the code changes and fix the errors."
            }

            errorMessage.contains("permission") || errorMessage.contains("denied") -> {
                "The build doesn't have the necessary permissions. Check that your CI/CD configuration has access to required resources."
            }

            errorMessage.contains("connection") || errorMessage.contains("network") -> {
                "A network or connection issue occurred during '${primaryFailure.stepName}'. " +
                        "This might be temporary - try rerunning the build."
            }

            else -> {
                "The build failed at the '${primaryFailure.stepName}' step with the error: ${primaryFailure.errorMessage}. " +
                        "Review the error details and logs to identify the specific issue."
            }
        }
    }

    private fun generateSuggestedActions(
        failedSteps: List<FailedStep>,
        logs: String
    ): List<String> {
        if (failedSteps.isEmpty()) {
            return listOf(
                "Review the full build logs",
                "Check recent code changes",
                "Verify CI/CD configuration"
            )
        }

        val actions = mutableListOf<String>()
        val primaryFailure = failedSteps.first()
        val errorMessage = primaryFailure.errorMessage.lowercase()

        when {
            errorMessage.contains("test") && errorMessage.contains("failed") -> {
                actions.add("Run the failing tests locally to reproduce the issue")
                actions.add("Review recent changes to the test files")
                actions.add("Check if test data or fixtures have changed")
            }

            errorMessage.contains("timeout") -> {
                actions.add("Increase timeout settings in CI/CD configuration")
                actions.add("Optimize the slow step for better performance")
                actions.add("Check for infinite loops or blocking operations")
            }

            errorMessage.contains("memory") || errorMessage.contains("oom") -> {
                actions.add("Increase memory allocation in build configuration")
                actions.add("Review code for memory leaks")
                actions.add("Optimize memory-intensive operations")
            }

            errorMessage.contains("dependency") || errorMessage.contains("not found") -> {
                actions.add("Verify all dependencies are listed in build configuration")
                actions.add("Check dependency versions for compatibility")
                actions.add("Clear dependency cache and rebuild")
            }

            errorMessage.contains("compile") -> {
                actions.add("Fix syntax errors in the code")
                actions.add("Verify imported packages and libraries")
                actions.add("Check for type mismatches or missing symbols")
            }

            else -> {
                actions.add("Review the error message: ${primaryFailure.errorMessage}")
                actions.add("Check recent commits for related changes")
                actions.add("Try rerunning the build")
            }
        }

        // Always add rollback option for failures
        actions.add("Consider rolling back to the last successful build")

        return actions
    }
}
