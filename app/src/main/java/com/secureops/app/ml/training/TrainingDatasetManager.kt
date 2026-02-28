package com.secureops.app.ml.training

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * High-level manager for ML training dataset generation
 * Orchestrates the complete pipeline: build dataset → export CSV/JSON
 */
class TrainingDatasetManager(
    private val context: Context,
    private val datasetBuilder: DatasetBuilder,
    private val datasetExporter: DatasetExporter,
    private val featureExtractor: FeatureExtractor
) {
    
    /**
     * Generate complete training dataset and export to CSV
     * 
     * @param minSamples Minimum samples required
     * @param balanceRatio Target failure ratio (0.5 = balanced)
     * @param exportFormat Export format (CSV or JSON)
     * @return Result with file path or error
     */
    suspend fun generateAndExportDataset(
        minSamples: Int = 100,
        balanceRatio: Float = 0.5f,
        exportFormat: ExportFormat = ExportFormat.CSV
    ): TrainingResult = withContext(Dispatchers.IO) {
        try {
            Timber.i("Starting training dataset generation")
            Timber.i("Parameters: minSamples=$minSamples, balanceRatio=$balanceRatio, format=$exportFormat")
            
            // Step 1: Check if we have enough data
            val stats = datasetBuilder.getDatasetStatistics()
            Timber.i("Dataset statistics: $stats")
            
            if (!stats.isSufficientData) {
                return@withContext TrainingResult.InsufficientData(
                    availableSamples = stats.totalPipelines,
                    requiredSamples = minSamples
                )
            }
            
            // Step 2: Build balanced dataset
            val samples = datasetBuilder.buildTrainingDataset(
                minSampleSize = minSamples,
                balanceRatio = balanceRatio
            )
            
            if (samples.isEmpty()) {
                return@withContext TrainingResult.BuildFailed("Failed to build dataset")
            }
            
            Timber.i("Built dataset with ${samples.size} samples")
            
            // Step 3: Export to file
            val filePath = when (exportFormat) {
                ExportFormat.CSV -> datasetExporter.exportToCSV(
                    samples = samples,
                    featureNames = featureExtractor.getFeatureNames()
                )
                ExportFormat.JSON -> datasetExporter.exportToJSON(
                    samples = samples,
                    featureNames = featureExtractor.getFeatureNames()
                )
            }
            
            if (filePath == null) {
                return@withContext TrainingResult.ExportFailed("Failed to export dataset")
            }
            
            // Step 4: Get final stats
            val datasetStats = datasetExporter.getDatasetStats(samples)
            
            Timber.i("Dataset exported successfully to: $filePath")
            
            TrainingResult.Success(
                filePath = filePath,
                totalSamples = datasetStats.totalSamples,
                successSamples = datasetStats.successSamples,
                failureSamples = datasetStats.failureSamples,
                isBalanced = datasetStats.isBalanced
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error generating training dataset")
            TrainingResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Generate dataset for specific repository
     */
    suspend fun generateDatasetForRepository(
        repositoryName: String,
        minSamples: Int = 50,
        exportFormat: ExportFormat = ExportFormat.CSV
    ): TrainingResult = withContext(Dispatchers.IO) {
        try {
            Timber.i("Generating dataset for repository: $repositoryName")
            
            val samples = datasetBuilder.buildDatasetForRepository(
                repositoryName = repositoryName,
                minSampleSize = minSamples,
                balanceRatio = 0.5f
            )
            
            if (samples.isEmpty()) {
                return@withContext TrainingResult.InsufficientData(0, minSamples)
            }
            
            val filePath = when (exportFormat) {
                ExportFormat.CSV -> datasetExporter.exportToCSV(
                    samples = samples,
                    featureNames = featureExtractor.getFeatureNames()
                )
                ExportFormat.JSON -> datasetExporter.exportToJSON(
                    samples = samples,
                    featureNames = featureExtractor.getFeatureNames()
                )
            }
            
            if (filePath == null) {
                return@withContext TrainingResult.ExportFailed("Export failed")
            }
            
            val stats = datasetExporter.getDatasetStats(samples)
            
            TrainingResult.Success(
                filePath = filePath,
                totalSamples = stats.totalSamples,
                successSamples = stats.successSamples,
                failureSamples = stats.failureSamples,
                isBalanced = stats.isBalanced
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error generating repository dataset")
            TrainingResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Get current dataset statistics without generating
     */
    suspend fun getStatistics(): DatasetStatistics = withContext(Dispatchers.IO) {
        datasetBuilder.getDatasetStatistics()
    }
}

/**
 * Export format options
 */
enum class ExportFormat {
    CSV,
    JSON
}

/**
 * Training result sealed class
 */
sealed class TrainingResult {
    data class Success(
        val filePath: String,
        val totalSamples: Int,
        val successSamples: Int,
        val failureSamples: Int,
        val isBalanced: Boolean
    ) : TrainingResult()
    
    data class InsufficientData(
        val availableSamples: Int,
        val requiredSamples: Int
    ) : TrainingResult()
    
    data class BuildFailed(val reason: String) : TrainingResult()
    data class ExportFailed(val reason: String) : TrainingResult()
    data class Error(val message: String) : TrainingResult()
}
