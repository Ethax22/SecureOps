package com.secureops.app

import android.app.Application
import androidx.work.*
import com.secureops.app.data.worker.PipelineSyncWorker
import com.secureops.app.di.appModule
import com.secureops.app.di.networkModule
import com.secureops.app.di.repositoryModule
import com.secureops.app.di.viewModelModule
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import com.runanywhere.sdk.public.extensions.addModelFromURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SecureOpsApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Koin DI
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@SecureOpsApplication)
            modules(
                appModule,
                networkModule,
                repositoryModule,
                viewModelModule
            )
        }

        Timber.d("SecureOps Application initialized with Koin")

        // Initialize RunAnywhere SDK asynchronously following official documentation pattern
        initializeRunAnywhereSDK()
        
        // Initialize Background Sync
        initializeBackgroundSync()
    }

    private fun initializeRunAnywhereSDK() {
        applicationScope.launch {
            try {
                Timber.i("Starting RunAnywhere SDK initialization...")

                // Step 1: Initialize SDK (following official documentation pattern)
                RunAnywhere.initialize(
                    context = this@SecureOpsApplication,
                    apiKey = "dev",  // Works for DEVELOPMENT mode
                    environment = SDKEnvironment.DEVELOPMENT
                )
                Timber.i("✓ RunAnywhere SDK core initialized")

                // Step 2: Register LLM Service Provider
                LlamaCppServiceProvider.register()
                Timber.i("✓ LlamaCpp Service Provider registered")

                // Step 3: Register Models
                registerModels()
                Timber.i("✓ Models registered")

                // Step 4: Scan for previously downloaded models
                RunAnywhere.scanForDownloadedModels()
                Timber.i("✓ Scanned for downloaded models")

                Timber.i("✅ RunAnywhere SDK initialized successfully - Ready for use!")

            } catch (e: ClassNotFoundException) {
                Timber.e(
                    e,
                    "RunAnywhere SDK classes not found - SDK may not be properly integrated"
                )
            } catch (e: NoClassDefFoundError) {
                Timber.e(e, "RunAnywhere SDK class definition error - check AAR files")
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Failed to initialize RunAnywhere SDK - app will continue with fallback mode"
                )
            }
        }
    }

    private suspend fun registerModels() {
        try {
            // SmolLM2 360M - Ultra-light model for quick responses (119 MB)
            addModelFromURL(
                url = "https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf",
                name = "SmolLM2 360M Q8_0",
                type = "LLM"
            )

            // Qwen 2.5 0.5B - Balanced model for general chat (374 MB)
            addModelFromURL(
                url = "https://huggingface.co/Triangle104/Qwen2.5-0.5B-Instruct-Q6_K-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf",
                name = "Qwen 2.5 0.5B Instruct Q6_K",
                type = "LLM"
            )

            Timber.d("Registered 2 AI models")
        } catch (e: Exception) {
            Timber.e(e, "Failed to register models")
        }
    }

    /**
     * Initialize background pipeline sync with WorkManager
     */
    private fun initializeBackgroundSync() {
        try {
            // Configure WorkManager constraints
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Requires internet
                .setRequiresBatteryNotLow(true) // Don't drain battery
                .build()

            // Create periodic work request (minimum 15 minutes)
            val syncRequest = PeriodicWorkRequestBuilder<PipelineSyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("pipeline_sync")
                .build()

            // Schedule the work (replaces existing work with same name)
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                PipelineSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                syncRequest
            )

            Timber.i("✅ Background sync scheduled - runs every 15 minutes")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule background sync")
        }
    }
}
