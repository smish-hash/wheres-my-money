package com.smish.wheresmymoney

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.smish.wheresmymoney.data.repository.AppTheme
import com.smish.wheresmymoney.ui.navigation.NavGraph
import com.smish.wheresmymoney.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        currentIntent = intent

        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContent {
            val app = application as ExpenseTrackerApp
            val themePreference by app.container.preferenceRepository.theme.collectAsState(initial = AppTheme.SYSTEM)

            val darkTheme = when (themePreference) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            val onboardingDone by produceState<Boolean?>(initialValue = null) {
                value = app.container.userPreferencesRepository.onboardingDone.first()
            }

            LaunchedEffect(onboardingDone) {
                if (onboardingDone != null) keepSplash = false
            }

            ExpenseTrackerTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    onboardingDone?.let { done ->
                        NavGraph(
                            container = app.container,
                            startWithOnboarding = !done,
                            intent = currentIntent,
                            onIntentConsumed = {
                                currentIntent = null
                                intent?.removeExtra("navigate_to")
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntent = intent
    }
}
