package com.secureops.app.data.local.dao

import androidx.room.*
import com.secureops.app.data.local.entity.VoiceMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceMessageDao {
    @Query("SELECT * FROM voice_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<VoiceMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: VoiceMessageEntity)

    @Query("DELETE FROM voice_messages")
    suspend fun clearAllMessages()

    @Query("DELETE FROM voice_messages WHERE timestamp < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)
}
