package com.secureops.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.secureops.app.domain.model.Account
import com.secureops.app.domain.model.CIProvider

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val name: String,
    val baseUrl: String,
    val tokenEncrypted: String,
    val isActive: Boolean,
    val createdAt: Long,
    val lastSyncedAt: Long?
)

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    provider = CIProvider.valueOf(provider),
    name = name,
    baseUrl = baseUrl,
    tokenEncrypted = tokenEncrypted,
    isActive = isActive,
    createdAt = createdAt,
    lastSyncedAt = lastSyncedAt
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    provider = provider.name,
    name = name,
    baseUrl = baseUrl,
    tokenEncrypted = tokenEncrypted,
    isActive = isActive,
    createdAt = createdAt,
    lastSyncedAt = lastSyncedAt
)
