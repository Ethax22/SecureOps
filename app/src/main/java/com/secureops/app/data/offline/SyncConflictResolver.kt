package com.secureops.app.data.offline

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.domain.model.BuildStatus
import com.secureops.app.domain.model.Pipeline
import kotlinx.coroutines.Dispatchers
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.data.local.entity.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync Conflict Resolver
 * 
 * Resolves conflicts between local and remote data when syncing
 * Strategy: Remote wins (remote data takes precedence)
 */
@Singleton
class SyncConflictResolver @Inject constructor(
    private val pipelineDao: PipelineDao
) {
    
    /**
     * Conflict resolution strategy
     */
    enum class ResolutionStrategy {
        REMOTE_WINS,      // Remote data takes precedence (default)
        LOCAL_WINS,       // Local data takes precedence
        MERGE,            // Merge both datasets
        NEWEST_WINS       // Most recently updated wins
    }
    
    /**
     * Conflict detection result
     */
    data class ConflictDetection(
        val hasConflicts: Boolean,
        val conflicts: List<Conflict>,
        val conflictCount: Int
    )
    
    /**
     * Individual conflict
     */
    data class Conflict(
        val pipelineId: String,
        val localData: Pipeline,
        val remoteData: Pipeline,
        val conflictType: ConflictType,
        val description: String
    )
    
    /**
     * Type of conflict
     */
    enum class ConflictType {
        STATUS_MISMATCH,        // Build status differs
        BUILD_NUMBER_MISMATCH,  // Build number differs
        TIMESTAMP_MISMATCH,     // Timestamps differ
        DATA_MISMATCH,          // Other data differs
        DELETED_REMOTELY        // Exists locally but not remotely
    }
    
    /**
     * Conflict resolution result
     */
    data class ResolutionResult(
        val success: Boolean,
        val resolvedCount: Int,
        val strategy: ResolutionStrategy,
        val conflicts: List<Conflict>,
        val resolutions: List<Resolution>,
        val message: String
    )
    
    /**
     * Individual resolution action
     */
    data class Resolution(
        val pipelineId: String,
        val action: ResolutionAction,
        val chosenData: Pipeline,
        val reason: String
    )
    
    /**
     * Resolution action taken
     */
    enum class ResolutionAction {
        KEEP_REMOTE,
        KEEP_LOCAL,
        MERGED,
        DELETED
    }
    
    /**
     * Detect conflicts between local and remote data
     * 
     * @param localPipelines Locally cached pipelines
     * @param remotePipelines Freshly fetched remote pipelines
     * @return Conflict detection result
     */
    suspend fun detectConflicts(
        localPipelines: List<Pipeline>,
        remotePipelines: List<Pipeline>
    ): ConflictDetection = withContext(Dispatchers.Default) {
        val conflicts = mutableListOf<Conflict>()
        
        // Create maps for easy lookup
        val localMap = localPipelines.associateBy { it.id }
        val remoteMap = remotePipelines.associateBy { it.id }
        
        // Check for conflicts in common pipelines
        localMap.forEach { (id, localPipeline) ->
            val remotePipeline = remoteMap[id]
            
            if (remotePipeline == null) {
                // Pipeline exists locally but not remotely (deleted or not synced)
                conflicts.add(
                    Conflict(
                        pipelineId = id,
                        localData = localPipeline,
                        remoteData = localPipeline, // Use local as placeholder
                        conflictType = ConflictType.DELETED_REMOTELY,
                        description = "Pipeline exists locally but not found remotely"
                    )
                )
            } else {
                // Check for data mismatches
                val conflict = findConflict(localPipeline, remotePipeline)
                if (conflict != null) {
                    conflicts.add(conflict)
                }
            }
        }
        
        Timber.i("Detected ${conflicts.size} sync conflicts")
        
        ConflictDetection(
            hasConflicts = conflicts.isNotEmpty(),
            conflicts = conflicts,
            conflictCount = conflicts.size
        )
    }
    
    /**
     * Find conflict between local and remote pipeline
     */
    private fun findConflict(local: Pipeline, remote: Pipeline): Conflict? {
        // Check build status
        if (local.status != remote.status) {
            return Conflict(
                pipelineId = local.id,
                localData = local,
                remoteData = remote,
                conflictType = ConflictType.STATUS_MISMATCH,
                description = "Status differs: local=${local.status}, remote=${remote.status}"
            )
        }
        
        // Check build number
        if (local.buildNumber != remote.buildNumber) {
            return Conflict(
                pipelineId = local.id,
                localData = local,
                remoteData = remote,
                conflictType = ConflictType.BUILD_NUMBER_MISMATCH,
                description = "Build number differs: local=${local.buildNumber}, remote=${remote.buildNumber}"
            )
        }
        
        // Check timestamps
        if (local.startedAt != remote.startedAt) {
            return Conflict(
                pipelineId = local.id,
                localData = local,
                remoteData = remote,
                conflictType = ConflictType.TIMESTAMP_MISMATCH,
                description = "Timestamp differs"
            )
        }
        
        // Check other significant data
        if (local.commitHash != remote.commitHash || 
            local.branch != remote.branch ||
            local.triggeredBy != remote.triggeredBy) {
            return Conflict(
                pipelineId = local.id,
                localData = local,
                remoteData = remote,
                conflictType = ConflictType.DATA_MISMATCH,
                description = "Other data fields differ"
            )
        }
        
        return null // No conflict
    }
    
    /**
     * Resolve conflicts using specified strategy
     * Default: REMOTE_WINS (remote data takes precedence)
     * 
     * @param conflicts List of detected conflicts
     * @param strategy Resolution strategy (default: REMOTE_WINS)
     * @return Resolution result
     */
    suspend fun resolveConflicts(
        conflicts: List<Conflict>,
        strategy: ResolutionStrategy = ResolutionStrategy.REMOTE_WINS
    ): ResolutionResult = withContext(Dispatchers.IO) {
        if (conflicts.isEmpty()) {
            return@withContext ResolutionResult(
                success = true,
                resolvedCount = 0,
                strategy = strategy,
                conflicts = emptyList(),
                resolutions = emptyList(),
                message = "No conflicts to resolve"
            )
        }
        
        Timber.i("Resolving ${conflicts.size} conflicts using strategy: $strategy")
        
        val resolutions = mutableListOf<Resolution>()
        var successCount = 0
        
        conflicts.forEach { conflict ->
            try {
                val resolution = when (strategy) {
                    ResolutionStrategy.REMOTE_WINS -> resolveRemoteWins(conflict)
                    ResolutionStrategy.LOCAL_WINS -> resolveLocalWins(conflict)
                    ResolutionStrategy.MERGE -> resolveMerge(conflict)
                    ResolutionStrategy.NEWEST_WINS -> resolveNewestWins(conflict)
                }
                
                resolutions.add(resolution)
                
                // Apply resolution to database
                applyResolution(resolution)
                successCount++
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to resolve conflict for pipeline ${conflict.pipelineId}")
            }
        }
        
        Timber.i("Resolved $successCount/${conflicts.size} conflicts")
        
        ResolutionResult(
            success = successCount == conflicts.size,
            resolvedCount = successCount,
            strategy = strategy,
            conflicts = conflicts,
            resolutions = resolutions,
            message = "Resolved $successCount of ${conflicts.size} conflicts using $strategy strategy"
        )
    }
    
    /**
     * Resolve conflict: Remote wins (default strategy)
     */
    private fun resolveRemoteWins(conflict: Conflict): Resolution {
        return Resolution(
            pipelineId = conflict.pipelineId,
            action = ResolutionAction.KEEP_REMOTE,
            chosenData = conflict.remoteData,
            reason = "Remote data takes precedence (REMOTE_WINS strategy)"
        )
    }
    
    /**
     * Resolve conflict: Local wins
     */
    private fun resolveLocalWins(conflict: Conflict): Resolution {
        return Resolution(
            pipelineId = conflict.pipelineId,
            action = ResolutionAction.KEEP_LOCAL,
            chosenData = conflict.localData,
            reason = "Local data takes precedence (LOCAL_WINS strategy)"
        )
    }
    
    /**
     * Resolve conflict: Merge data
     */
    private fun resolveMerge(conflict: Conflict): Resolution {
        // Merge strategy: Prefer remote for status/build, keep local logs if available
        val merged = conflict.remoteData.copy(
            logs = conflict.localData.logs ?: conflict.remoteData.logs,
            logsCachedAt = conflict.localData.logsCachedAt ?: conflict.remoteData.logsCachedAt
        )
        
        return Resolution(
            pipelineId = conflict.pipelineId,
            action = ResolutionAction.MERGED,
            chosenData = merged,
            reason = "Merged local and remote data (MERGE strategy)"
        )
    }
    
    /**
     * Resolve conflict: Newest wins
     */
    private fun resolveNewestWins(conflict: Conflict): Resolution {
        val localTimestamp = conflict.localData.startedAt ?: 0L
        val remoteTimestamp = conflict.remoteData.startedAt ?: 0L
        
        val chosen = if (remoteTimestamp > localTimestamp) {
            conflict.remoteData
        } else {
            conflict.localData
        }
        
        return Resolution(
            pipelineId = conflict.pipelineId,
            action = if (chosen == conflict.remoteData) 
                ResolutionAction.KEEP_REMOTE 
            else 
                ResolutionAction.KEEP_LOCAL,
            chosenData = chosen,
            reason = "Chose data with newest timestamp (NEWEST_WINS strategy)"
        )
    }
    
    /**
     * Apply resolution to database
     */
    private suspend fun applyResolution(resolution: Resolution) {
        when (resolution.action) {
            ResolutionAction.KEEP_REMOTE, 
            ResolutionAction.MERGED -> {
                // Update with chosen data
                pipelineDao.insertPipeline(resolution.chosenData.toEntity())
                Timber.d("Applied resolution: ${resolution.action} for ${resolution.pipelineId}")
            }
            
            ResolutionAction.KEEP_LOCAL -> {
                // Do nothing, keep local data as-is
                Timber.d("Keeping local data for ${resolution.pipelineId}")
            }
            
            ResolutionAction.DELETED -> {
                // Delete from local database
                pipelineDao.deletePipeline(resolution.chosenData.toEntity())
                Timber.d("Deleted pipeline ${resolution.pipelineId} from local cache")
            }
        }
    }
    
    /**
     * Sync and resolve conflicts in one operation
     * 
     * @param remotePipelines Newly fetched remote pipelines
     * @param strategy Resolution strategy (default: REMOTE_WINS)
     * @return Resolution result
     */
    suspend fun syncAndResolve(
        remotePipelines: List<Pipeline>,
        strategy: ResolutionStrategy = ResolutionStrategy.REMOTE_WINS
    ): ResolutionResult = withContext(Dispatchers.IO) {
        // Get local pipelines
        val localPipelines = pipelineDao.getAllPipelines().first().map { it.toDomain() }
        
        // Detect conflicts
        val detection = detectConflicts(localPipelines, remotePipelines)
        
        // Resolve conflicts
        val result = if (detection.hasConflicts) {
            resolveConflicts(detection.conflicts, strategy)
        } else {
            // No conflicts, just update with remote data
            remotePipelines.forEach { pipelineDao.insertPipeline(it.toEntity()) }
            
            ResolutionResult(
                success = true,
                resolvedCount = 0,
                strategy = strategy,
                conflicts = emptyList(),
                resolutions = emptyList(),
                message = "No conflicts detected, updated ${remotePipelines.size} pipelines"
            )
        }
        
        Timber.i("Sync complete: ${result.message}")
        result
    }
}
