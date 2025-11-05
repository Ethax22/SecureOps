package com.secureops.app.di

import android.content.Context
import androidx.room.Room
import com.secureops.app.data.local.SecureOpsDatabase
import com.secureops.app.data.local.dao.AccountDao
import com.secureops.app.data.local.dao.PipelineDao
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            SecureOpsDatabase::class.java,
            "secureops_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    single<AccountDao> { get<SecureOpsDatabase>().accountDao() }
    single<PipelineDao> { get<SecureOpsDatabase>().pipelineDao() }
}
