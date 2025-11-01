package com.secureops.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.secureops.app.data.local.dao.AccountDao
import com.secureops.app.data.local.dao.PipelineDao
import com.secureops.app.data.local.entity.AccountEntity
import com.secureops.app.data.local.entity.PipelineEntity

@Database(
    entities = [
        AccountEntity::class,
        PipelineEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SecureOpsDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun pipelineDao(): PipelineDao
}
