package com.secureops.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.secureops.app.ui.screens.voice.VoiceMessage

@Entity(tableName = "voice_messages")
data class VoiceMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

fun VoiceMessageEntity.toDomain(): VoiceMessage {
    return VoiceMessage(
        sender = sender,
        content = content,
        isUser = isUser
    )
}

fun VoiceMessage.toEntity(): VoiceMessageEntity {
    return VoiceMessageEntity(
        sender = sender,
        content = content,
        isUser = isUser
    )
}
