package com.secureops.app.di

import com.secureops.app.ui.screens.aimodels.AIModelsViewModel
import com.secureops.app.ui.screens.analytics.AnalyticsViewModel
import com.secureops.app.ui.screens.dashboard.DashboardViewModel
import com.secureops.app.ui.screens.details.BuildDetailsViewModel
import com.secureops.app.ui.screens.modelperformance.ModelPerformanceViewModel
import com.secureops.app.ui.screens.security.SecurityViewModel
import com.secureops.app.ui.screens.settings.AddAccountViewModel
import com.secureops.app.ui.screens.settings.EditAccountViewModel
import com.secureops.app.ui.screens.settings.ManageAccountsViewModel
import com.secureops.app.ui.screens.settings.NotificationSettingsViewModel
import com.secureops.app.ui.screens.settings.SettingsViewModel
import com.secureops.app.ui.screens.voice.VoiceViewModel
import com.secureops.app.ui.screens.benchmark.BenchmarkViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext

val viewModelModule = module {
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { SecurityViewModel(get()) }
    viewModel { AddAccountViewModel(get(), get()) }
    viewModel { EditAccountViewModel(get(), get()) }
    viewModel { ManageAccountsViewModel(get()) }
    viewModel { AIModelsViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { NotificationSettingsViewModel(get()) }
    viewModel { VoiceViewModel(androidApplication(), get(), get()) }
    viewModel { BuildDetailsViewModel(get(), get()) }
    viewModel { ModelPerformanceViewModel(androidContext(), get(), get()) }
    viewModel { BenchmarkViewModel(get(), get(), get(), androidContext()) }
}
