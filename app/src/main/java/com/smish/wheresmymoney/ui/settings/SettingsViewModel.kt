package com.smish.wheresmymoney.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smish.wheresmymoney.data.repository.AppTheme
import com.smish.wheresmymoney.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferenceRepository: PreferenceRepository) : ViewModel() {
    val theme = preferenceRepository.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferenceRepository.setTheme(theme)
        }
    }
}
