package com.smish.wheresmymoney.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smish.wheresmymoney.data.repository.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val userName: Flow<String?> = context.dataStore.data.map { it[Keys.USER_NAME] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val theme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val themeName = prefs[Keys.APP_THEME] ?: AppTheme.SYSTEM.name
        try { AppTheme.valueOf(themeName) } catch (e: Exception) { AppTheme.SYSTEM }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
