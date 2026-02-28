package com.secureops.app.ml.training

import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.PipelineEntity
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.random.Random

/**
 * Builds training datasets from historical pipeline data
 * Extracts features, generates labels, and balances the dataset
 */
class DatasetBuilder(
    private val pipelineDao: PipelineDao,
    private val featureExtractor: FeatureExtractor,
    private val labelGenerator: LabelGenerator
) {
    companion object {
        private const val DEFAULT_MIN_SAMPLES = 100
        private const val DEFAULT_BALANCE_RATIO = 0.5f // 50/50 split
    }

    /**
     * Build a balanced training dataset from all available pipelines
     * 
     * @param minSampleSize Minimum number of samples required
     * @param balanceRatio Target ratio of failures (0.5 = 50% failures, 50% successes)
     * @return List of training samples, or empty list if insufficient data
     */
    suspend fun buildTrainingDataset(
        minSampleSize: Int = DEFAULT_MIN_SAMPLES,
        balanceRatio: Float = DEFAULT_BALANCE_RATIO
    ): List<TrainingSample> {
        Timber.i("Building training dataset with min samples: $minSampleSize, balance ratio: $balanceRatio")

        // Fetch all completed pipelines from database
        val allPipelines = pipelineDao.getAllPipelines().first()
        
        // Filter valid pipelines
        val validPipelines = allPipelines.filter { labelGenerator.isValidForTraining(it) }
        
        if (validPipelines.isEmpty()) {
            Timber.w("No valid pipelines found for training")
            return emptyList()
        }

        Timber.i("Found ${validPipelines.size} valid pipelines for training")

        // Extract features and labels
        val samples = mutableListOf<TrainingSample>()
        
        for (pipeline in validPipelines) {
            try {
                val features = featureExtractor.extractFeatures(pipeline)
                val label = labelGenerator.generateLabel(pipeline)
                
                if (label != null) {
                    samples.add(TrainingSample(features, label, pipeline.id))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract features for pipeline ${pipeline.id}")
            }
        }

        if (samples.size < minSampleSize) {
            Timber.w("Insufficient samples: ${samples.size} < $minSampleSize")
            return emptyList()
        }

        // Balance dataset
        val balancedSamples = balanceDataset(samples, balanceRatio)
        
        Timber.i("Dataset built successfully:")
        Timber.i("  Total samples: ${balancedSamples.size}")
        Timber.i("  Success samples: ${balancedSamples.count { it.label == LabelGenerator.LABEL_SUCCESS }}")
        Timber.i("  Failure samples: ${balancedSamples.count { it.label == LabelGenerator.LABEL_FAILURE }}")

        return balancedSamples
    }

    /**
     * Balance the dataset to achieve target ratio
     * 
     * @param samples Raw samples
     * @param targetFailureRatio Target ratio of failure samples (0.0 to 1.0)
     * @return Balanced samples
     */
    private fun balanceDataset(
        samples: List<TrainingSample>,
        targetFailureRatio: Float
    ): List<TrainingSample> {
        val successSamples = samples.filter { it.label == LabelGenerator.LABEL_SUCCESS }
        val failureSamples = samples.filter { it.label == LabelGenerator.LABEL_FAILURE }

        Timber.i("Pre-balance: ${successSamples.size} success, ${failureSamples.size} failure")

        if (successSamples.isEmpty() || failureSamples.isEmpty()) {
            Timber.w("Cannot balance dataset: missing success or failure samples")
            return samples
        }

        // Calculate target counts
        val totalDesired = minOf(
            (failureSamples.size / targetFailureRatio).toInt(),
            (successSamples.size / (1f - targetFailureRatio)).toInt()
        )

        val targetFailureCount = (totalDesired * targetFailureRatio).toInt()
        val targetSuccessCount = totalDesired - targetFailureCount

        // Sample from each class
        val balancedFailures = sampleRandomly(failureSamples, targetFailureCount)
        val balancedSuccesses = sampleRandomly(successSamples, targetSuccessCount)

        // Combine and shuffle
        val balanced = (balancedFailures + balancedSuccesses).shuffled(Random(42))

        Timber.i("Post-balance: ${balancedSuccesses.size} success, ${balancedFailures.size} failure")

        return balanced
    }

    /**
     * Randomly sample N items from list
     * If list is smaller than N, return all items
     */
    private fun <T> sampleRandomly(items: List<T>, count: Int): List<T> {
        return if (items.size <= count) {
            items
        } else {
            items.shuffled(Random(42)).take(count)
        }
    }

    /**
     * Build dataset for a specific repository
     */
    suspend fun buildDatasetForRepository(
        repositoryName: String,
        minSampleSize: Int = DEFAULT_MIN_SAMPLES,
        balanceRatio: Float = DEFAULT_BALANCE_RATIO
    ): List<TrainingSample> {
        Timber.i("Building dataset for repository: $repositoryName")

        val repoPipelines = pipelineDao.getPipelinesByRepository(repositoryName, 1000).first()
        val validPipelines = repoPipelines.filter { labelGenerator.isValidForTraining(it) }

        if (validPipelines.size < minSampleSize) {
            Timber.w("Insufficient samples for repository $repositoryName: ${validPipelines.size}")
            return emptyList()
        }

        val samples = mutableListOf<TrainingSample>()
        
        for (pipeline in validPipelines) {
            try {
                val features = featureExtractor.extractFeatures(pipeline)
                val label = labelGenerator.generateLabel(pipeline)
                
                if (label != null) {
                    samples.add(TrainingSample(features, label, pipeline.id))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract features for pipeline ${pipeline.id}")
            }
        }

        return balanceDataset(samples, balanceRatio)
    }

    /**
     * Get dataset statistics without building full dataset
     */
    suspend fun getDatasetStatistics(): DatasetStatistics {
        val allPipelines = pipelineDao.getAllPipelines().first()
        val validPipelines = allPipelines.filter { labelGenerator.isValidForTraining(it) }

        val successCount = validPipelines.count { 
            labelGenerator.generateLabel(it) == LabelGenerator.LABEL_SUCCESS 
        }
        val failureCount = validPipelines.count { 
            labelGenerator.generateLabel(it) == LabelGenerator.LABEL_FAILURE 
        }

        val repositories = validPipelines.map { it.repositoryName }.distinct()

        return DatasetStatistics(
            totalPipelines = validPipelines.size,
            successSamples = successCount,
            failureSamples = failureCount,
            repositories = repositories.size,
            averageSamplesPerRepo = if (repositories.isNotEmpty()) 
                validPipelines.size / repositories.size else 0
        )
    }
}

/**
 * Dataset statistics for analysis
 */
data class DatasetStatistics(
    val totalPipelines: Int,
    val successSamples: Int,
    val failureSamples: Int,
    val repositories: Int,
    val averageSamplesPerRepo: Int
) {
    val balanceRatio: Float
        get() = if (failureSamples > 0) successSamples.toFloat() / failureSamples else 0f
    
    val isBalanced: Boolean
        get() = balanceRatio in 0.8f..1.2f
    
    val isSufficientData: Boolean
        get() = totalPipelines >= 100
}
