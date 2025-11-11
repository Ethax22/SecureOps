package com.secureops.app.domain.model

data class Pipeline(
    val id: String,
    val accountId: String,
    val provider: CIProvider,
    val repositoryName: String,
    val repositoryUrl: String,
    val branch: String,
    val buildNumber: Int,
    val status: BuildStatus,
    val commitHash: String,
    val commitMessage: String,
    val commitAuthor: String,
    val startedAt: Long?,
    val finishedAt: Long?,
    val duration: Long?,
    val triggeredBy: String,
    val webUrl: String,
    val failurePrediction: FailurePrediction? = null,
    val logs: String? = null,  // Cached build logs
    val logsCachedAt: Long? = null  // When logs were cached
)

data class FailurePrediction(
    val riskPercentage: Float,
    val confidence: Float,
    val causalFactors: List<String>,
    val predictedAt: Long = System.currentTimeMillis()
)
