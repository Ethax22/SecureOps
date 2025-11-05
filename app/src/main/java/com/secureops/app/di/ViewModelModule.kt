package com.secureops.app.di

import com.secureops.app.ui.screens.aimodels.AIModelsViewModel
import com.secureops.app.ui.screens.analytics.AnalyticsViewModel
import com.secureops.app.ui.screens.dashboard.DashboardViewModel
import com.secureops.app.ui.screens.settings.AddAccountViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { DashboardViewModel(get(), get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { AddAccountViewModel(get()) }
    viewModel { AIModelsViewModel(get()) }
}
