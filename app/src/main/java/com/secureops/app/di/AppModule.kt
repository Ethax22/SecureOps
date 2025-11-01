package com.secureops.app.di

import android.content.Context
import androidx.room.Room
import com.secureops.app.data.local.SecureOpsDatabase
import com.secureops.app.data.local.dao.AccountDao
import com.secureops.app.data.local.dao.PipelineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSecureOpsDatabase(
        @ApplicationContext context: Context
    ): SecureOpsDatabase {
        return Room.databaseBuilder(
            context,
            SecureOpsDatabase::class.java,
            "secureops_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: SecureOpsDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun providePipelineDao(database: SecureOpsDatabase): PipelineDao {
        return database.pipelineDao()
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
