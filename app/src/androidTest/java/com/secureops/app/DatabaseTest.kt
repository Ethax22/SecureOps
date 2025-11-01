package com.secureops.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.secureops.app.data.local.SecureOpsDatabase
import com.secureops.app.data.local.dao.AccountDao
import com.secureops.app.data.local.entity.AccountEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var database: SecureOpsDatabase
    private lateinit var accountDao: AccountDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            SecureOpsDatabase::class.java
        ).build()
        accountDao = database.accountDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveAccount() = runBlocking {
        val account = AccountEntity(
            id = "test-id",
            provider = "GITHUB_ACTIONS",
            name = "Test Account",
            baseUrl = "https://api.github.com",
            tokenEncrypted = "encrypted-token",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            lastSyncedAt = null
        )

        accountDao.insertAccount(account)
        val retrieved = accountDao.getAccountById("test-id")

        assertNotNull(retrieved)
        assertEquals("Test Account", retrieved?.name)
        assertEquals("GITHUB_ACTIONS", retrieved?.provider)
    }

    @Test
    fun updateAccount() = runBlocking {
        val account = AccountEntity(
            id = "test-id",
            provider = "GITLAB_CI",
            name = "Original Name",
            baseUrl = "https://gitlab.com",
            tokenEncrypted = "encrypted-token",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            lastSyncedAt = null
        )

        accountDao.insertAccount(account)

        val updated = account.copy(name = "Updated Name")
        accountDao.updateAccount(updated)

        val retrieved = accountDao.getAccountById("test-id")
        assertEquals("Updated Name", retrieved?.name)
    }

    @Test
    fun deleteAccount() = runBlocking {
        val account = AccountEntity(
            id = "test-id",
            provider = "JENKINS",
            name = "Test Account",
            baseUrl = "https://jenkins.example.com",
            tokenEncrypted = "encrypted-token",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            lastSyncedAt = null
        )

        accountDao.insertAccount(account)
        accountDao.deleteAccountById("test-id")

        val retrieved = accountDao.getAccountById("test-id")
        assertNull(retrieved)
    }
}
