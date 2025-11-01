package com.secureops.app.domain.model

data class BuildDetails(
    val pipeline: Pipeline,
    val jobs: List<Job>,
    val logs: String,
    val rootCauseAnalysis: RootCauseAnalysis? = null
)

data class Job(
    val id: String,
    val name: String,
    val status: BuildStatus,
    val startedAt: Long?,
    val finishedAt: Long?,
    val duration: Long?,
    val logs: String? = null
)

data class RootCauseAnalysis(
    val failedSteps: List<FailedStep>,
    val technicalSummary: String,
    val plainEnglishSummary: String,
    val suggestedActions: List<String>,
    val analyzedAt: Long = System.currentTimeMillis()
)

data class FailedStep(
    val stepName: String,
    val errorMessage: String,
    val stackTrace: String? = null,
    val exitCode: Int? = null
)
