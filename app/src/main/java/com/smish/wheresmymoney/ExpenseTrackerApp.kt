package com.smish.wheresmymoney

import android.app.Application
import com.smish.wheresmymoney.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseTrackerApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        CoroutineScope(Dispatchers.IO).launch {
            container.categoryRepository.seedDefaultsIfEmpty()
        }

        // Starts/stops the Firestore listeners whenever sign-in state changes.
        CoroutineScope(Dispatchers.IO).launch {
            container.authRepository.authState.collect { user ->
                if (user != null) container.syncManager.start(user.uid) else container.syncManager.stop()
            }
        }
    }
}
