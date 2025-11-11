package com.secureops.app.di

import com.secureops.app.data.analytics.AnalyticsRepository
import com.secureops.app.data.executor.RemediationExecutor
import com.secureops.app.data.notification.NotificationManager
import com.secureops.app.data.notification.SlackNotifier
import com.secureops.app.data.playbook.PlaybookManager
import com.secureops.app.data.remote.PipelineStreamService
import com.secureops.app.data.repository.AccountRepository
import com.secureops.app.data.repository.PipelineRepository
import com.secureops.app.data.repository.VoiceMessageRepository
import com.secureops.app.data.security.SecureTokenManager
import com.secureops.app.data.auth.OAuth2Manager
import com.secureops.app.ml.*
import com.secureops.app.ml.advanced.*
import com.secureops.app.ml.voice.TextToSpeechManager
import com.secureops.app.ml.voice.VoiceActionExecutor
import com.google.gson.Gson
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.secureops.app.data.remediation.AutoRemediationEngine
import okhttp3.OkHttpClient

val repositoryModule = module {
    // Security
    single { SecureTokenManager(androidContext()) }

    // OAuth2
    single { OAuth2Manager(androidContext()) }

    // Repositories
    single { AccountRepository(get(), get()) }
    single { PipelineRepository(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { AnalyticsRepository(get()) }
    single { VoiceMessageRepository(get()) }

    // Services
    single { PipelineStreamService() }
    single { NotificationManager(androidContext()) }

    // Notifications
    single { SlackNotifier(get()) } // Uses OkHttpClient

    single {
        RemediationExecutor(
            context = androidContext(),
            githubService = get(),
            gitlabService = get(),
            jenkinsService = get(),
            circleCIService = get(),
            azureDevOpsService = get(),
            accountRepository = get(),
            slackNotifier = get(),
            gson = get()
        )
    }

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

    // ✅ NEW: Auto-Remediation Engine
    single {
        AutoRemediationEngine(
            remediationExecutor = get(),
            pipelineRepository = get()
        )
    }
}
