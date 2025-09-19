package com.example.smartcalendar.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private val KEY_REMINDER_MIN = intPreferencesKey("default_reminder_minutes")

    fun defaultReminderMinutesFlow(): Flow<Int> =
        context.dataStore.data.map { it[KEY_REMINDER_MIN] ?: 5 } // дефолт: 5 минут

    suspend fun setDefaultReminderMinutes(value: Int) {
        context.dataStore.edit { it[KEY_REMINDER_MIN] = value }
    }


    suspend fun getDefaultReminderMinutesOnce(): Int =
        defaultReminderMinutesFlow().first()
}
