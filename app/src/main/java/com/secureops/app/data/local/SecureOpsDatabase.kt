package com.secureops.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.secureops.app.data.local.dao.AccountDao
import com.secureops.app.data.local.dao.BuildEvaluationDao
import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.dao.RemediationHistoryDao
import com.secureops.app.data.local.dao.ThreatDao
import com.secureops.app.data.local.dao.VoiceMessageDao
import com.secureops.app.data.local.entity.AccountEntity
import com.secureops.app.data.local.entity.BuildEvaluationEntity
import com.secureops.app.data.local.entity.PipelineEntity
import com.secureops.app.data.local.entity.RemediationHistoryEntity
import com.secureops.app.data.local.entity.ThreatEntity
import com.secureops.app.data.local.entity.VoiceMessageEntity
import com.secureops.app.data.local.entity.BenchmarkResultEntity
import com.secureops.app.data.local.dao.BenchmarkResultDao

@Database(
    entities = [
        AccountEntity::class,
        PipelineEntity::class,
        VoiceMessageEntity::class,
        BuildEvaluationEntity::class,
        ThreatEntity::class,
        RemediationHistoryEntity::class,
        BenchmarkResultEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class SecureOpsDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun pipelineDao(): PipelineDao
    abstract fun voiceMessageDao(): VoiceMessageDao
    abstract fun buildEvaluationDao(): BuildEvaluationDao
    abstract fun threatDao(): ThreatDao
    abstract fun remediationHistoryDao(): RemediationHistoryDao
    abstract fun benchmarkResultDao(): BenchmarkResultDao
}

/**
 * Migration from version 4 to 5
 * Adds threats table for secret detection
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create threats table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS threats (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                patternType TEXT NOT NULL,
                severity INTEGER NOT NULL,
                description TEXT NOT NULL,
                detectedValue TEXT NOT NULL,
                source TEXT NOT NULL,
                lineNumber INTEGER,
                pipelineId TEXT NOT NULL,
                buildNumber INTEGER NOT NULL,
                repositoryName TEXT NOT NULL,
                branch TEXT NOT NULL,
                commitHash TEXT NOT NULL,
                contextLine TEXT NOT NULL,
                detectedAt INTEGER NOT NULL,
                isResolved INTEGER NOT NULL DEFAULT 0,
                resolvedAt INTEGER,
                resolutionNotes TEXT,
                accountId TEXT NOT NULL
            )
            """.trimIndent()
        )
        
        // Create indexes for performance
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_threats_pipelineId ON threats(pipelineId)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_threats_repositoryName ON threats(repositoryName)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_threats_severity ON threats(severity)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_threats_isResolved ON threats(isResolved)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_threats_commitHash ON threats(commitHash)"
        )
    }
}

/**
 * Migration from version 5 to 6
 * Adds remediation_history table for learning from remediation outcomes
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create remediation_history table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS remediation_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                pipelineId TEXT NOT NULL,
                buildNumber INTEGER NOT NULL,
                repositoryName TEXT NOT NULL,
                failureType TEXT NOT NULL,
                failurePattern TEXT NOT NULL,
                actionTaken TEXT NOT NULL,
                actionDescription TEXT NOT NULL,
                wasSuccessful INTEGER NOT NULL,
                outcome TEXT NOT NULL,
                remediatedBuildNumber INTEGER,
                durationMs INTEGER NOT NULL,
                confidenceScore REAL NOT NULL,
                errorMessage TEXT,
                failureContext TEXT,
                logSnippet TEXT,
                wasUserApproved INTEGER NOT NULL,
                approvedBy TEXT,
                attemptedAt INTEGER NOT NULL,
                completedAt INTEGER,
                accountId TEXT NOT NULL,
                costMetric REAL NOT NULL DEFAULT 0.0,
                patternFrequency INTEGER NOT NULL DEFAULT 1,
                tags TEXT
            )
            """.trimIndent()
        )
        
        // Create indexes for performance
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_remediation_history_pipelineId ON remediation_history(pipelineId)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_remediation_history_repositoryName ON remediation_history(repositoryName)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_remediation_history_failureType ON remediation_history(failureType)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_remediation_history_failurePattern ON remediation_history(failurePattern)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_remediation_history_actionTaken ON remediation_history(actionTaken)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_remediation_history_wasSuccessful ON remediation_history(wasSuccessful)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_remediation_history_attemptedAt ON remediation_history(attemptedAt)"
        )
    }
}

/**
 * Migration from version 6 to 7
 * Adds benchmark_results table
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS benchmark_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp INTEGER NOT NULL,
                inferenceTimeMs INTEGER NOT NULL,
                memoryUsageMb REAL NOT NULL,
                startupTimeMs INTEGER NOT NULL,
                precision REAL NOT NULL,
                recall REAL NOT NULL,
                f1Score REAL NOT NULL,
                accuracy REAL NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * Migration from version 7 to 8
 * Recreates the threats table with the EXACT schema Room generates
 * (without DEFAULT clauses that Room does not emit in its own schema),
 * fixing the "Migration didn't properly handle: threats" crash.
 * Also recreates all indexes to keep them consistent.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Step 1: Rename old table
        database.execSQL("ALTER TABLE threats RENAME TO threats_old")

        // Step 2: Recreate with Room-exact schema (no DEFAULT on isResolved)
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS threats (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `patternType` TEXT NOT NULL,
                `severity` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `detectedValue` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `lineNumber` INTEGER,
                `pipelineId` TEXT NOT NULL,
                `buildNumber` INTEGER NOT NULL,
                `repositoryName` TEXT NOT NULL,
                `branch` TEXT NOT NULL,
                `commitHash` TEXT NOT NULL,
                `contextLine` TEXT NOT NULL,
                `detectedAt` INTEGER NOT NULL,
                `isResolved` INTEGER NOT NULL,
                `resolvedAt` INTEGER,
                `resolutionNotes` TEXT,
                `accountId` TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Step 3: Copy data, mapping DEFAULT 0 from old isResolved
        database.execSQL(
            """
            INSERT INTO threats
                (id, patternType, severity, description, detectedValue, source, lineNumber,
                 pipelineId, buildNumber, repositoryName, branch, commitHash, contextLine,
                 detectedAt, isResolved, resolvedAt, resolutionNotes, accountId)
            SELECT
                id, patternType, severity, description, detectedValue, source, lineNumber,
                pipelineId, buildNumber, repositoryName, branch, commitHash, contextLine,
                detectedAt, isResolved, resolvedAt, resolutionNotes, accountId
            FROM threats_old
            """.trimIndent()
        )

        // Step 4: Drop old table
        database.execSQL("DROP TABLE threats_old")

        // Step 5: Recreate indexes
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_pipelineId` ON `threats` (`pipelineId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_repositoryName` ON `threats` (`repositoryName`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_severity` ON `threats` (`severity`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_isResolved` ON `threats` (`isResolved`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_commitHash` ON `threats` (`commitHash`)")
    }
}

/**
 * Migration 8 → 9
 * Nuclear fix: drops the threats table entirely and recreates it with
 * Room's exact expected schema. This handles devices stuck in any state:
 * - threats table with DEFAULT 0 (from MIGRATION_4_5)
 * - threats_old leftover (from partially-executed MIGRATION_7_8)
 * - Any other schema variance
 * Threat data is cleared (threats are re-detected on next scan).
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Clean up any leftovers from previous failed migration attempts
        database.execSQL("DROP TABLE IF EXISTS `threats_old`")
        database.execSQL("DROP TABLE IF EXISTS `threats`")

        // Recreate with the EXACT schema Room generates for ThreatEntity
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `threats` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `patternType` TEXT NOT NULL,
                `severity` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `detectedValue` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `lineNumber` INTEGER,
                `pipelineId` TEXT NOT NULL,
                `buildNumber` INTEGER NOT NULL,
                `repositoryName` TEXT NOT NULL,
                `branch` TEXT NOT NULL,
                `commitHash` TEXT NOT NULL,
                `contextLine` TEXT NOT NULL,
                `detectedAt` INTEGER NOT NULL,
                `isResolved` INTEGER NOT NULL,
                `resolvedAt` INTEGER,
                `resolutionNotes` TEXT,
                `accountId` TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Recreate indexes
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_pipelineId` ON `threats` (`pipelineId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_repositoryName` ON `threats` (`repositoryName`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_severity` ON `threats` (`severity`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_isResolved` ON `threats` (`isResolved`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_commitHash` ON `threats` (`commitHash`)")
    }
}
