package com.secureops.app.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.secureops.app.domain.model.NotificationChannelType
import com.secureops.app.domain.model.NotificationPreferences
import com.secureops.app.domain.model.QuietHours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): NotificationPreferences {
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        val ledEnabled = prefs.getBoolean("led_enabled", true)
        val alertOnCriticalOnly = prefs.getBoolean("alert_critical_only", false)
        val riskThreshold = prefs.getInt("risk_threshold", 70)

        // Load enabled channels
        val channelsString = prefs.getString("enabled_channels", null)
        val enabledChannels = if (channelsString != null) {
            channelsString.split(",")
                .mapNotNull { name ->
                    try {
                        NotificationChannelType.valueOf(name)
                    } catch (e: Exception) {
                        null
                    }
                }
                .toSet()
        } else {
            // Default: FAILURES and HIGH_RISK enabled
            setOf(NotificationChannelType.FAILURES, NotificationChannelType.HIGH_RISK)
        }

        // Load quiet hours
        val quietHoursEnabled = prefs.getBoolean("quiet_hours_enabled", false)
        val quietHours = if (quietHoursEnabled) {
            val startHour = prefs.getInt("quiet_start_hour", 22)
            val startMinute = prefs.getInt("quiet_start_minute", 0)
            val endHour = prefs.getInt("quiet_end_hour", 8)
            val endMinute = prefs.getInt("quiet_end_minute", 0)
            val daysString = prefs.getString("quiet_days", "1,2,3,4,5,6,7")
            val daysOfWeek =
                daysString?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(
                    1,
                    2,
                    3,
                    4,
                    5,
                    6,
                    7
                )

            QuietHours(
                enabled = true,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                daysOfWeek = daysOfWeek
            )
        } else {
            QuietHours()
        }

        return NotificationPreferences(
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            ledEnabled = ledEnabled,
            enabledChannels = enabledChannels,
            riskThreshold = riskThreshold,
            alertOnCriticalOnly = alertOnCriticalOnly,
            quietHours = quietHours
        )
    }

    fun updatePreferences(newPreferences: NotificationPreferences) {
        viewModelScope.launch {
            _preferences.value = newPreferences
            savePreferences(newPreferences)
        }
    }

    private fun savePreferences(preferences: NotificationPreferences) {
        prefs.edit().apply {
            putBoolean("sound_enabled", preferences.soundEnabled)
            putBoolean("vibration_enabled", preferences.vibrationEnabled)
            putBoolean("led_enabled", preferences.ledEnabled)
            putBoolean("alert_critical_only", preferences.alertOnCriticalOnly)
            putInt("risk_threshold", preferences.riskThreshold)

            // Save enabled channels
            val channelsString = preferences.enabledChannels.joinToString(",") { it.name }
            putString("enabled_channels", channelsString)

            // Save quiet hours
            val quietHours = preferences.quietHours
            if (quietHours != null) {
                putBoolean("quiet_hours_enabled", quietHours.enabled)
                putInt("quiet_start_hour", quietHours.startHour)
                putInt("quiet_start_minute", quietHours.startMinute)
                putInt("quiet_end_hour", quietHours.endHour)
                putInt("quiet_end_minute", quietHours.endMinute)
                val daysString = quietHours.daysOfWeek.joinToString(",")
                putString("quiet_days", daysString)
            } else {
                putBoolean("quiet_hours_enabled", false)
            }

            apply()
        }
    }
}
