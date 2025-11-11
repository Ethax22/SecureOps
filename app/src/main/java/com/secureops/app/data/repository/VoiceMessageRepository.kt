package com.secureops.app.data.repository

import com.secureops.app.data.local.dao.VoiceMessageDao
import com.secureops.app.data.local.entity.VoiceMessageEntity
import com.secureops.app.data.local.entity.toDomain
import com.secureops.app.data.local.entity.toEntity
import com.secureops.app.ui.screens.voice.VoiceMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class VoiceMessageRepository(
    private val voiceMessageDao: VoiceMessageDao
) {
    fun getAllMessages(): Flow<List<VoiceMessage>> {
        return voiceMessageDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun saveMessage(message: VoiceMessage) {
        try {
            voiceMessageDao.insertMessage(message.toEntity())
            Timber.d("Saved voice message: ${message.content.take(50)}...")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save voice message")
        }
    }

    suspend fun clearAllMessages() {
        try {
            voiceMessageDao.clearAllMessages()
            Timber.d("Cleared all voice messages")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear voice messages")
        }
    }

    suspend fun deleteOldMessages(daysToKeep: Int = 30) {
        val timestamp = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        try {
            voiceMessageDao.deleteOldMessages(timestamp)
            Timber.d("Deleted voice messages older than $daysToKeep days")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete old voice messages")
        }
    }
}
