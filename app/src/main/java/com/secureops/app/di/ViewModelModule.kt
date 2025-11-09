package com.secureops.app.di

import com.secureops.app.ui.screens.aimodels.AIModelsViewModel
import com.secureops.app.ui.screens.analytics.AnalyticsViewModel
import com.secureops.app.ui.screens.dashboard.DashboardViewModel
import com.secureops.app.ui.screens.details.BuildDetailsViewModel
import com.secureops.app.ui.screens.settings.AddAccountViewModel
import com.secureops.app.ui.screens.settings.ManageAccountsViewModel
import com.secureops.app.ui.screens.voice.VoiceViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidApplication

val viewModelModule = module {
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { AddAccountViewModel(get(), get()) }
    viewModel { ManageAccountsViewModel(get()) }
    viewModel { AIModelsViewModel(get()) }
    viewModel { VoiceViewModel(androidApplication(), get()) }
    viewModel { BuildDetailsViewModel(get(), get()) }
}
