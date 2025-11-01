package com.secureops.app.domain.model

data class Account(
    val id: String,
    val provider: CIProvider,
    val name: String,
    val baseUrl: String,
    val tokenEncrypted: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null
)
