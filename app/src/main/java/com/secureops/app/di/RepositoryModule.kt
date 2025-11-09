package com.secureops.app.di

import com.secureops.app.data.analytics.AnalyticsRepository
import com.secureops.app.data.executor.RemediationExecutor
import com.secureops.app.data.notification.NotificationManager
import com.secureops.app.data.playbook.PlaybookManager
import com.secureops.app.data.remote.PipelineStreamService
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.data.security.SecureTokenManager
import com.secureops.app.ml.*
import com.secureops.app.ml.advanced.*
import com.secureops.app.ml.voice.TextToSpeechManager
import com.secureops.app.ml.voice.VoiceActionExecutor
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    // Security
    single { SecureTokenManager(androidContext()) }

    // Repositories
    single { AccountRepository(get(), get()) }
    single { PipelineRepository(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { AnalyticsRepository(get()) }

    // Services
    single { PipelineStreamService() }
    single { NotificationManager(androidContext()) }
    single { RemediationExecutor(get(), get(), get(), get(), get(), get(), get()) }

    // ML Components
    single { RunAnywhereManager(androidContext()) }
    single { FailurePredictionModel(androidContext()) }
    single { RootCauseAnalyzer() }
    single { VoiceCommandProcessor() }
    single { TextToSpeechManager(androidContext()) }
    single { PlaybookManager(get()) }
    single { VoiceActionExecutor(get(), get(), get(), get(), get(), get()) }

    // Advanced ML Components
    single { CascadeAnalyzer(get()) }
    single { ChangelogAnalyzer(get(), get()) }
    single { DeploymentScheduler(get(), get()) }
    single { FlakyTestDetector(get()) }
}
