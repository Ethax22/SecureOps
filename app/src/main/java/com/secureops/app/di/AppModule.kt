package com.secureops.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.secureops.app.data.local.SecureOpsDatabase
import com.secureops.app.data.local.dao.AccountDao
import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.dao.VoiceMessageDao
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

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            SecureOpsDatabase::class.java,
            "secureops_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    single<AccountDao> { get<SecureOpsDatabase>().accountDao() }
    single<PipelineDao> { get<SecureOpsDatabase>().pipelineDao() }
    single<VoiceMessageDao> { get<SecureOpsDatabase>().voiceMessageDao() }
}
