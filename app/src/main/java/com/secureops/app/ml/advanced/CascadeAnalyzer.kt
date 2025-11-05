package com.secureops.app.ml.advanced

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import kotlinx.coroutines.flow.first
import timber.log.Timber

class CascadeAnalyzer(
    private val pipelineDao: PipelineDao
) {

    /**
     * Analyze cascade risk for a failed pipeline
     */
    suspend fun analyzeCascadeRisk(pipeline: Pipeline): CascadeRisk {
        Timber.d("Analyzing cascade risk for pipeline ${pipeline.id}")

        val allPipelines = pipelineDao.getAllPipelines().first()
            .map { it.toDomain() }

        // Find downstream dependencies
        val downstreamPipelines = findDownstreamPipelines(pipeline, allPipelines)

        // Calculate impact
        val impactedCount = downstreamPipelines.size
        val criticalPipelines =
            downstreamPipelines.filter { it.branch == "main" || it.branch == "master" }

        // Determine risk level
        val riskLevel = when {
            criticalPipelines.isNotEmpty() -> CascadeRiskLevel.CRITICAL
            impactedCount > 5 -> CascadeRiskLevel.HIGH
            impactedCount > 2 -> CascadeRiskLevel.MEDIUM
            impactedCount > 0 -> CascadeRiskLevel.LOW
            else -> CascadeRiskLevel.NONE
        }

        // Generate recommendations
        val recommendations = generateCascadeRecommendations(riskLevel, downstreamPipelines)

        return CascadeRisk(
            pipeline = pipeline,
            riskLevel = riskLevel,
            affectedPipelines = downstreamPipelines,
            affectedCount = impactedCount,
            criticalCount = criticalPipelines.size,
            recommendations = recommendations,
            estimatedImpactMinutes = estimateImpactTime(downstreamPipelines)
        )
    }

    /**
     * Find pipelines that depend on the failed pipeline
     */
    private fun findDownstreamPipelines(
        failedPipeline: Pipeline,
        allPipelines: List<Pipeline>
    ): List<Pipeline> {
        // In a real implementation, this would analyze:
        // 1. Build triggers and dependencies
        // 2. Shared artifacts
        // 3. Deployment pipelines
        // 4. Integration tests that depend on this build

        return allPipelines.filter { downstream ->
            // Same repository, later in the pipeline
            downstream.repositoryName == failedPipeline.repositoryName &&
                    downstream.startedAt ?: 0 > failedPipeline.startedAt ?: 0 &&
                    downstream.status == BuildStatus.RUNNING
        }
    }

    /**
     * Estimate total impact time
     */
    private fun estimateImpactTime(pipelines: List<Pipeline>): Int {
        if (pipelines.isEmpty()) return 0

        // Sum up estimated durations
        val totalMinutes = pipelines.sumOf { pipeline ->
            val durationMs = pipeline.duration ?: 300000L // Default 5 minutes
            (durationMs / 60000).toInt()
        }

        return totalMinutes
    }

    /**
     * Generate recommendations based on cascade risk
     */
    private fun generateCascadeRecommendations(
        riskLevel: CascadeRiskLevel,
        affectedPipelines: List<Pipeline>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when (riskLevel) {
            CascadeRiskLevel.CRITICAL -> {
                recommendations.add("🚨 Cancel downstream pipelines immediately")
                recommendations.add("🔒 Block production deployments")
                recommendations.add("📢 Notify team leads and stakeholders")
                recommendations.add("🔄 Prepare rollback plan")
            }

            CascadeRiskLevel.HIGH -> {
                recommendations.add("⚠️ Pause downstream builds")
                recommendations.add("🔍 Investigate root cause before proceeding")
                recommendations.add("📊 Review impact on dependent services")
            }

            CascadeRiskLevel.MEDIUM -> {
                recommendations.add("👀 Monitor downstream builds closely")
                recommendations.add("⏸️ Consider pausing non-critical deployments")
            }

            CascadeRiskLevel.LOW -> {
                recommendations.add("📝 Document failure for future reference")
                recommendations.add("✅ Safe to continue with caution")
            }

            CascadeRiskLevel.NONE -> {
                recommendations.add("✅ No downstream impact detected")
            }
        }

        return recommendations
    }

    /**
     * Check if a pipeline is blocking others
     */
    suspend fun isBlocking(pipeline: Pipeline): Boolean {
        val risk = analyzeCascadeRisk(pipeline)
        return risk.riskLevel == CascadeRiskLevel.CRITICAL ||
                risk.riskLevel == CascadeRiskLevel.HIGH
    }

    /**
     * Get all blocked pipelines
     */
    suspend fun getBlockedPipelines(failedPipeline: Pipeline): List<Pipeline> {
        val risk = analyzeCascadeRisk(failedPipeline)
        return risk.affectedPipelines
    }
}

/**
 * Cascade risk analysis result
 */
data class CascadeRisk(
    val pipeline: Pipeline,
    val riskLevel: CascadeRiskLevel,
    val affectedPipelines: List<Pipeline>,
    val affectedCount: Int,
    val criticalCount: Int,
    val recommendations: List<String>,
    val estimatedImpactMinutes: Int
)

enum class CascadeRiskLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
