package com.smish.wheresmymoney.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smish.wheresmymoney.data.datastore.UserPreferencesRepository
import com.smish.wheresmymoney.data.repository.AuthRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val prefsRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    var name by mutableStateOf(""); private set
    var isSigningIn by mutableStateOf(false); private set
    var signInError by mutableStateOf<String?>(null); private set

    fun onNameChange(value: String) { name = value }
    fun canContinue() = name.isNotBlank()

    fun saveName(onDone: () -> Unit) {
        if (!canContinue()) return
        viewModelScope.launch {
            prefsRepository.setUserName(name.trim())
            onDone()
        }
    }

    fun signInWithGoogle(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSigningIn = true
            signInError = null
            authRepository.signInWithGoogle()
                .onSuccess { onSuccess() }
                .onFailure { signInError = it.message ?: "Sign-in failed. Try again." }
            isSigningIn = false
        }
    }

    fun finishOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            prefsRepository.setOnboardingDone()
            onDone()
        }
    }
}
