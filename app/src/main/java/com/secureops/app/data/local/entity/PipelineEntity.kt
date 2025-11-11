package com.secureops.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.CIProvider
import com.secureops.app.domain.model.FailurePrediction
import com.secureops.app.domain.model.Pipeline

@Entity(tableName = "pipelines")
data class PipelineEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val provider: String,
    val repositoryName: String,
    val repositoryUrl: String,
    val branch: String,
    val buildNumber: Int,
    val status: String,
    val commitHash: String,
    val commitMessage: String,
    val commitAuthor: String,
    val startedAt: Long?,
    val finishedAt: Long?,
    val duration: Long?,
    val triggeredBy: String,
    val webUrl: String,
    val predictionRisk: Float?,
    val predictionConfidence: Float?,
    val predictionFactors: String?,
    val predictionTimestamp: Long?,
    val cachedAt: Long = System.currentTimeMillis(),
    val logs: String? = null,  // Cached build logs
    val logsCachedAt: Long? = null  // When logs were cached
)

fun PipelineEntity.toDomain(): Pipeline {
    val failurePrediction = if (predictionRisk != null && predictionConfidence != null) {
        FailurePrediction(
            riskPercentage = predictionRisk,
            confidence = predictionConfidence,
            causalFactors = predictionFactors?.split("|") ?: emptyList(),
            predictedAt = predictionTimestamp ?: System.currentTimeMillis()
        )
    } else null

    return Pipeline(
        id = id,
        accountId = accountId,
        provider = CIProvider.valueOf(provider),
        repositoryName = repositoryName,
        repositoryUrl = repositoryUrl,
        branch = branch,
        buildNumber = buildNumber,
        status = BuildStatus.valueOf(status),
        commitHash = commitHash,
        commitMessage = commitMessage,
        commitAuthor = commitAuthor,
        startedAt = startedAt,
        finishedAt = finishedAt,
        duration = duration,
        triggeredBy = triggeredBy,
        webUrl = webUrl,
        failurePrediction = failurePrediction,
        logs = logs,
        logsCachedAt = logsCachedAt
    )
}

fun Pipeline.toEntity(): PipelineEntity = PipelineEntity(
    id = id,
    accountId = accountId,
    provider = provider.name,
    repositoryName = repositoryName,
    repositoryUrl = repositoryUrl,
    branch = branch,
    buildNumber = buildNumber,
    status = status.name,
    commitHash = commitHash,
    commitMessage = commitMessage,
    commitAuthor = commitAuthor,
    startedAt = startedAt,
    finishedAt = finishedAt,
    duration = duration,
    triggeredBy = triggeredBy,
    webUrl = webUrl,
    predictionRisk = failurePrediction?.riskPercentage,
    predictionConfidence = failurePrediction?.confidence,
    predictionFactors = failurePrediction?.causalFactors?.joinToString("|"),
    predictionTimestamp = failurePrediction?.predictedAt,
    logs = logs,
    logsCachedAt = logsCachedAt
)
