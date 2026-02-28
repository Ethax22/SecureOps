package com.secureops.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.secureops.app.data.local.SecureOpsDatabase
import com.secureops.app.data.local.dao.AccountDao
import com.secureops.app.data.local.dao.BuildEvaluationDao
import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.dao.RemediationHistoryDao
import com.secureops.app.data.local.dao.ThreatDao
import com.secureops.app.data.local.dao.VoiceMessageDao
import com.secureops.app.data.local.dao.BenchmarkResultDao
import com.secureops.app.ml.security.SecretScanner
import com.secureops.app.ml.security.DependencyAnalyzer
import com.secureops.app.ml.security.AnomalyDetector
import com.secureops.app.data.remediation.RemediationLearner
import com.secureops.app.data.remediation.FailureTypeDetector
import com.secureops.app.data.offline.OfflineSimulator
import com.secureops.app.data.offline.CacheManager
import com.secureops.app.data.offline.SyncConflictResolver
import com.secureops.app.data.offline.DemoDataGenerator
import com.secureops.app.ml.benchmark.ModelValidator
import com.secureops.app.ml.benchmark.PerformanceBenchmark
import com.secureops.app.ml.benchmark.BenchmarkReportGenerator
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add logs and logsCachedAt columns to pipelines table
        database.execSQL("ALTER TABLE pipelines ADD COLUMN logs TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE pipelines ADD COLUMN logsCachedAt INTEGER DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create voice_messages table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS voice_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sender TEXT NOT NULL,
                content TEXT NOT NULL,
                isUser INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create build_evaluations table for ML model performance tracking
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS build_evaluations (
                buildId TEXT PRIMARY KEY NOT NULL,
                predictedLabel INTEGER NOT NULL,
                actualLabel INTEGER,
                predictionRiskScore REAL NOT NULL,
                confidenceScore REAL NOT NULL,
                inferenceTimeMs INTEGER NOT NULL,
                features TEXT NOT NULL,
                predictedAt INTEGER NOT NULL,
                evaluatedAt INTEGER
            )
        """.trimIndent()
        )
    }
}

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            SecureOpsDatabase::class.java,
            "secureops_database"
        )
            // All migrations removed intentionally.
            // fallbackToDestructiveMigration() ensures Room recreates the
            // entire database from entity definitions when no migration
            // path is found, guaranteeing a perfect schema match.
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    single<AccountDao> { get<SecureOpsDatabase>().accountDao() }
    single<PipelineDao> { get<SecureOpsDatabase>().pipelineDao() }
    single<VoiceMessageDao> { get<SecureOpsDatabase>().voiceMessageDao() }
    single<BuildEvaluationDao> { get<SecureOpsDatabase>().buildEvaluationDao() }
    single<ThreatDao> { get<SecureOpsDatabase>().threatDao() }
    single<RemediationHistoryDao> { get<SecureOpsDatabase>().remediationHistoryDao() }
    single<BenchmarkResultDao> { get<SecureOpsDatabase>().benchmarkResultDao() }
    
    // Security Analyzers
    single { SecretScanner(get()) }
    single { DependencyAnalyzer(get()) }
    single { AnomalyDetector(get()) }
    
    // Remediation Components
    single { RemediationLearner(get()) }
    single { FailureTypeDetector() }
    
    // Offline & Demo Components
    single { OfflineSimulator(androidContext()) }
    single { CacheManager(androidContext(), get()) }
    single { SyncConflictResolver(get()) }
    single { DemoDataGenerator(get(), get()) }

    // Benchmark Components
    single { ModelValidator(get()) }
    single { PerformanceBenchmark(androidContext(), get(), get(), get()) }
    single { BenchmarkReportGenerator(androidContext()) }
}
