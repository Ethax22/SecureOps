package com.secureops.app.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _isDarkModeEnabled = MutableStateFlow(
        prefs.getBoolean("dark_mode_enabled", false)
    )
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled.asStateFlow()

    private val _areNotificationsEnabled = MutableStateFlow(
        prefs.getBoolean("notifications_enabled", true)
    )
    val areNotificationsEnabled: StateFlow<Boolean> = _areNotificationsEnabled.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            _isDarkModeEnabled.value = enabled
            prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _areNotificationsEnabled.value = enabled
            prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        }
    }
}
