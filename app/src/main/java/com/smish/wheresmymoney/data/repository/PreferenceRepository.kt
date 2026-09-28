package com.smish.wheresmymoney.data.repository

import com.smish.wheresmymoney.data.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

enum class AppTheme { LIGHT, DARK, SYSTEM }

class PreferenceRepository(private val userPreferencesRepository: UserPreferencesRepository) {
    val theme: Flow<AppTheme> = userPreferencesRepository.theme

    suspend fun setTheme(theme: AppTheme) {
        userPreferencesRepository.setTheme(theme)
    }
}
