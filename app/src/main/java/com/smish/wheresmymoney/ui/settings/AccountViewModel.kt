package com.smish.wheresmymoney.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.smish.wheresmymoney.data.datastore.UserPreferencesRepository
import com.smish.wheresmymoney.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {
    val user: StateFlow<FirebaseUser?> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser)

    var isWorking by mutableStateOf(false); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    fun signIn() {
        viewModelScope.launch {
            isWorking = true; errorMessage = null
            authRepository.signInWithGoogle().onFailure { errorMessage = it.message }
            isWorking = false
        }
    }

    fun signOut() { authRepository.signOut() }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            isWorking = true; errorMessage = null
            authRepository.deleteAccount()
                .onSuccess { onDeleted() }
                .onFailure { errorMessage = it.message ?: "Couldn't delete account. Please sign in again and retry." }
            isWorking = false
        }
    }
}
