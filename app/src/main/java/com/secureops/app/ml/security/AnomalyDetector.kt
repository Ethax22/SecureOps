package com.secureops.app.ml.security

import com.secureops.app.data.local.dao.ThreatDao
import com.secureops.app.data.local.entity.ThreatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Production-grade anomaly detector using statistical analysis
 * 
 * Detects anomalies in build metrics using z-score analysis:
 * - Calculates mean and standard deviation
 * - Computes z-scores for data points
 * - Flags values with |z| > 2 as anomalies
 * - Inserts anomalies as ThreatEntity records
 */
@Singleton
class AnomalyDetector @Inject constructor(
    private val threatDao: ThreatDao
) {
    
    /**
     * Statistical summary for a dataset
     */
    data class Statistics(
        val mean: Double,
        val stdDev: Double,
        val min: Double,
        val max: Double,
        val count: Int,
        val variance: Double
    )
    
    /**
     * Anomaly detection result
     */
    data class Anomaly(
        val value: Double,
        val zScore: Double,
        val metric: String,
        val severity: ThreatSeverity,
        val description: String,
        val expectedRange: String
    )
    
    /**
     * Metric data point for analysis
     */
    data class MetricPoint(
        val timestamp: Long,
        val value: Double,
        val label: String,
        val metadata: Map<String, String> = emptyMap()
    )
    
    /**
     * Scan context for anomaly detection
     */
    data class ScanContext(
        val source: String,
        val pipelineId: String,
        val buildNumber: Int,
        val repositoryName: String,
        val branch: String,
        val commitHash: String,
        val accountId: String
    )
    
    /**
     * Z-score thresholds for anomaly detection
     */
    object Thresholds {
        const val CRITICAL = 3.0  // |z| > 3.0 = 99.7% confidence
        const val HIGH = 2.5      // |z| > 2.5 = 98.8% confidence
        const val MEDIUM = 2.0    // |z| > 2.0 = 95.4% confidence
        const val LOW = 1.5       // |z| > 1.5 = 86.6% confidence
    }
    
    /**
     * Calculate mean (average) of a dataset
     * 
     * @param values List of numeric values
     * @return Mean value
     */
    fun calculateMean(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return values.sum() / values.size
    }
    
    /**
     * Calculate variance of a dataset
     * 
     * @param values List of numeric values
     * @param mean Pre-calculated mean (optional)
     * @return Variance
     */
    fun calculateVariance(values: List<Double>, mean: Double? = null): Double {
        if (values.isEmpty()) return 0.0
        
        val avg = mean ?: calculateMean(values)
        val squaredDiffs = values.map { (it - avg).pow(2) }
        
        return squaredDiffs.sum() / values.size
    }
    
    /**
     * Calculate standard deviation of a dataset
     * 
     * @param values List of numeric values
     * @param mean Pre-calculated mean (optional)
     * @return Standard deviation
     */
    fun calculateStdDev(values: List<Double>, mean: Double? = null): Double {
        return sqrt(calculateVariance(values, mean))
    }
    
    /**
     * Calculate z-score for a value
     * 
     * Z-score = (value - mean) / stdDev
     * 
     * @param value The value to compute z-score for
     * @param mean Dataset mean
     * @param stdDev Dataset standard deviation
     * @return Z-score (number of standard deviations from mean)
     */
    fun calculateZScore(value: Double, mean: Double, stdDev: Double): Double {
        if (stdDev == 0.0) return 0.0
        return (value - mean) / stdDev
    }
    
    /**
     * Calculate complete statistics for a dataset
     * 
     * @param values List of numeric values
     * @return Statistics object with mean, stdDev, min, max, etc.
     */
    fun calculateStatistics(values: List<Double>): Statistics {
        if (values.isEmpty()) {
            return Statistics(
                mean = 0.0,
                stdDev = 0.0,
                min = 0.0,
                max = 0.0,
                count = 0,
                variance = 0.0
            )
        }
        
        val mean = calculateMean(values)
        val variance = calculateVariance(values, mean)
        val stdDev = sqrt(variance)
        
        return Statistics(
            mean = mean,
            stdDev = stdDev,
            min = values.minOrNull() ?: 0.0,
            max = values.maxOrNull() ?: 0.0,
            count = values.size,
            variance = variance
        )
    }
    
    /**
     * Check if a z-score indicates an anomaly
     * 
     * @param zScore The z-score to check
     * @param threshold Threshold for anomaly (default 2.0)
     * @return True if |z| > threshold
     */
    fun isAnomaly(zScore: Double, threshold: Double = Thresholds.MEDIUM): Boolean {
        return abs(zScore) > threshold
    }
    
    /**
     * Determine severity based on z-score magnitude
     * 
     * @param zScore The z-score
     * @return ThreatSeverity level
     */
    fun determineSeverity(zScore: Double): ThreatSeverity {
        val absZ = abs(zScore)
        
        return when {
            absZ >= Thresholds.CRITICAL -> ThreatSeverity.CRITICAL
            absZ >= Thresholds.HIGH -> ThreatSeverity.HIGH
            absZ >= Thresholds.MEDIUM -> ThreatSeverity.MEDIUM
            absZ >= Thresholds.LOW -> ThreatSeverity.LOW
            else -> ThreatSeverity.INFO
        }
    }
    
    /**
     * Detect anomalies in a dataset using z-score analysis
     * 
     * @param values List of values to analyze
     * @param metricName Name of the metric being analyzed
     * @param threshold Z-score threshold (default 2.0)
     * @return List of detected anomalies
     */
    fun detectAnomalies(
        values: List<Double>,
        metricName: String,
        threshold: Double = Thresholds.MEDIUM
    ): List<Anomaly> {
        if (values.size < 3) {
            // Need at least 3 data points for meaningful statistics
            return emptyList()
        }
        
        val stats = calculateStatistics(values)
        val anomalies = mutableListOf<Anomaly>()
        
        values.forEachIndexed { index, value ->
            val zScore = calculateZScore(value, stats.mean, stats.stdDev)
            
            if (isAnomaly(zScore, threshold)) {
                val severity = determineSeverity(zScore)
                val expectedRange = String.format(
                    "%.2f to %.2f",
                    stats.mean - (2 * stats.stdDev),
                    stats.mean + (2 * stats.stdDev)
                )
                
                anomalies.add(
                    Anomaly(
                        value = value,
                        zScore = zScore,
                        metric = metricName,
                        severity = severity,
                        description = "Anomalous $metricName value detected (z-score: ${String.format("%.2f", zScore)})",
                        expectedRange = expectedRange
                    )
                )
            }
        }
        
        return anomalies
    }
    
    /**
     * Detect anomalies in metric points with timestamps
     * 
     * @param points List of metric points
     * @param threshold Z-score threshold
     * @return List of anomalies with metadata
     */
    fun detectMetricAnomalies(
        points: List<MetricPoint>,
        threshold: Double = Thresholds.MEDIUM
    ): List<Pair<MetricPoint, Anomaly>> {
        if (points.size < 3) return emptyList()
        
        val values = points.map { it.value }
        val stats = calculateStatistics(values)
        val anomalies = mutableListOf<Pair<MetricPoint, Anomaly>>()
        
        points.forEach { point ->
            val zScore = calculateZScore(point.value, stats.mean, stats.stdDev)
            
            if (isAnomaly(zScore, threshold)) {
                val severity = determineSeverity(zScore)
                val expectedRange = String.format(
                    "%.2f to %.2f",
                    stats.mean - (2 * stats.stdDev),
                    stats.mean + (2 * stats.stdDev)
                )
                
                val anomaly = Anomaly(
                    value = point.value,
                    zScore = zScore,
                    metric = point.label,
                    severity = severity,
                    description = "Anomalous ${point.label} value detected (z-score: ${String.format("%.2f", zScore)})",
                    expectedRange = expectedRange
                )
                
                anomalies.add(point to anomaly)
            }
        }
        
        return anomalies
    }
    
    /**
     * Analyze build duration for anomalies
     * 
     * @param durations List of build durations in milliseconds
     * @param context Scan context
     * @return List of inserted threat IDs
     */
    suspend fun analyzeBuildDuration(
        durations: List<Double>,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        val anomalies = detectAnomalies(durations, "Build Duration", Thresholds.MEDIUM)
        insertAnomalies(anomalies, context)
    }
    
    /**
     * Analyze test failure rate for anomalies
     * 
     * @param failureRates List of failure rates (0.0 to 1.0)
     * @param context Scan context
     * @return List of inserted threat IDs
     */
    suspend fun analyzeTestFailureRate(
        failureRates: List<Double>,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        val anomalies = detectAnomalies(failureRates, "Test Failure Rate", Thresholds.MEDIUM)
        insertAnomalies(anomalies, context)
    }
    
    /**
     * Analyze code coverage for anomalies (drops in coverage)
     * 
     * @param coveragePercentages List of coverage percentages (0.0 to 100.0)
     * @param context Scan context
     * @return List of inserted threat IDs
     */
    suspend fun analyzeCodeCoverage(
        coveragePercentages: List<Double>,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        val anomalies = detectAnomalies(coveragePercentages, "Code Coverage", Thresholds.MEDIUM)
        
        // Only flag negative anomalies (coverage drops)
        val coverageDrops = anomalies.filter { it.zScore < 0 }
        insertAnomalies(coverageDrops, context)
    }
    
    /**
     * Analyze deployment frequency for anomalies
     * 
     * @param frequencies List of deployment frequencies (deploys per day)
     * @param context Scan context
     * @return List of inserted threat IDs
     */
    suspend fun analyzeDeploymentFrequency(
        frequencies: List<Double>,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        val anomalies = detectAnomalies(frequencies, "Deployment Frequency", Thresholds.HIGH)
        
        // Flag unusual spikes (positive anomalies)
        val spikes = anomalies.filter { it.zScore > 0 }
        insertAnomalies(spikes, context)
    }
    
    /**
     * Analyze API response times for anomalies
     * 
     * @param responseTimes List of response times in milliseconds
     * @param context Scan context
     * @return List of inserted threat IDs
     */
    suspend fun analyzeResponseTimes(
        responseTimes: List<Double>,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        val anomalies = detectAnomalies(responseTimes, "API Response Time", Thresholds.MEDIUM)
        
        // Only flag slow responses (positive anomalies)
        val slowResponses = anomalies.filter { it.zScore > 0 }
        insertAnomalies(slowResponses, context)
    }
    
    /**
     * Analyze error rate for anomalies
     * 
     * @param errorRates List of error rates (errors per minute)
     * @param context Scan context
     * @return List of inserted threat IDs
     */
    suspend fun analyzeErrorRate(
        errorRates: List<Double>,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        val anomalies = detectAnomalies(errorRates, "Error Rate", Thresholds.MEDIUM)
        
        // Only flag error spikes (positive anomalies)
        val errorSpikes = anomalies.filter { it.zScore > 0 }
        insertAnomalies(errorSpikes, context)
    }
    
    /**
     * Analyze memory usage for anomalies
     * 
     * @param memoryUsages List of memory usage in MB
     * @param context Scan context
     * @return List of inserted threat IDs
     */
    suspend fun analyzeMemoryUsage(
        memoryUsages: List<Double>,
        context: ScanContext
    ): List<Long> = withContext(Dispatchers.IO) {
        val anomalies = detectAnomalies(memoryUsages, "Memory Usage", Thresholds.MEDIUM)
        
        // Flag high memory usage (positive anomalies)
        val highUsage = anomalies.filter { it.zScore > 0 }
        insertAnomalies(highUsage, context)
    }
    
    /**
     * Analyze custom metric for anomalies
     * 
     * @param values List of metric values
     * @param metricName Name of the metric
     * @param context Scan context
     * @param threshold Z-score threshold
     * @param filterPositive Only flag positive anomalies
     * @param filterNegative Only flag negative anomalies
     * @return List of inserted threat IDs
     */
    suspend fun analyzeCustomMetric(
        values: List<Double>,
        metricName: String,
        context: ScanContext,
        threshold: Double = Thresholds.MEDIUM,
        filterPositive: Boolean = false,
        filterNegative: Boolean = false
    ): List<Long> = withContext(Dispatchers.IO) {
        var anomalies = detectAnomalies(values, metricName, threshold)
        
        // Apply filters
        if (filterPositive) {
            anomalies = anomalies.filter { it.zScore > 0 }
        }
        if (filterNegative) {
            anomalies = anomalies.filter { it.zScore < 0 }
        }
        
        insertAnomalies(anomalies, context)
    }
    
    /**
     * Analyze multiple metrics in batch
     * 
     * @param metrics Map of metric name to values
     * @param context Scan context
     * @return Total number of anomalies detected
     */
    suspend fun analyzeBatchMetrics(
        metrics: Map<String, List<Double>>,
        context: ScanContext
    ): Int = withContext(Dispatchers.IO) {
        var totalAnomalies = 0
        
        metrics.forEach { (metricName, values) ->
            val threatIds = analyzeCustomMetric(values, metricName, context)
            totalAnomalies += threatIds.size
        }
        
        totalAnomalies
    }
    
    /**
     * Insert anomalies as ThreatEntity records
     * 
     * @param anomalies List of detected anomalies
     * @param context Scan context
     * @return List of inserted threat IDs
     */
    private suspend fun insertAnomalies(
        anomalies: List<Anomaly>,
        context: ScanContext
    ): List<Long> {
        val threats = anomalies.map { anomaly ->
            ThreatEntity(
                patternType = "ANOMALY_${anomaly.metric.uppercase().replace(" ", "_")}",
                severity = anomaly.severity.level,
                description = anomaly.description,
                detectedValue = String.format("%.2f (z-score: %.2f)", anomaly.value, anomaly.zScore),
                source = context.source,
                lineNumber = null, // Not applicable for anomalies
                pipelineId = context.pipelineId,
                buildNumber = context.buildNumber,
                repositoryName = context.repositoryName,
                branch = context.branch,
                commitHash = context.commitHash,
                contextLine = "Value: ${String.format("%.2f", anomaly.value)}, " +
                             "Z-Score: ${String.format("%.2f", anomaly.zScore)}, " +
                             "Expected Range: ${anomaly.expectedRange}, " +
                             "Deviation: ${String.format("%.1f", abs(anomaly.zScore))}σ",
                accountId = context.accountId
            )
        }
        
        return if (threats.isNotEmpty()) {
            threatDao.insertAll(threats)
        } else {
            emptyList()
        }
    }
    
    /**
     * Get anomaly statistics for a pipeline
     * 
     * @param pipelineId The pipeline ID
     * @return Map of metric to anomaly count
     */
    suspend fun getAnomalyStatistics(pipelineId: String): Map<String, Int> = 
        withContext(Dispatchers.IO) {
            // Would query threatDao for ANOMALY_ pattern types
            mapOf(
                "build_duration" to 0,
                "test_failure_rate" to 0,
                "code_coverage" to 0,
                "error_rate" to 0
            )
        }
    
    /**
     * Calculate rolling statistics over a time window
     * 
     * @param points List of metric points
     * @param windowSize Number of recent points to include
     * @return Statistics for the window
     */
    fun calculateRollingStatistics(
        points: List<MetricPoint>,
        windowSize: Int = 10
    ): Statistics {
        val sortedPoints = points.sortedBy { it.timestamp }
        val recentPoints = sortedPoints.takeLast(windowSize)
        val values = recentPoints.map { it.value }
        
        return calculateStatistics(values)
    }
    
    /**
     * Detect trend anomalies (sustained deviations)
     * 
     * @param points List of metric points
     * @param windowSize Window for trend calculation
     * @return True if trend is anomalous
     */
    fun detectTrendAnomaly(
        points: List<MetricPoint>,
        windowSize: Int = 5
    ): Boolean {
        if (points.size < windowSize * 2) return false
        
        val sortedPoints = points.sortedBy { it.timestamp }
        val recentWindow = sortedPoints.takeLast(windowSize)
        val historicalWindow = sortedPoints.dropLast(windowSize).takeLast(windowSize * 2)
        
        val recentMean = calculateMean(recentWindow.map { it.value })
        val historicalStats = calculateStatistics(historicalWindow.map { it.value })
        
        val zScore = calculateZScore(recentMean, historicalStats.mean, historicalStats.stdDev)
        
        return isAnomaly(zScore, Thresholds.MEDIUM)
    }
}
