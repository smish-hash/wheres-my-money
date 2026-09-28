package com.smish.wheresmymoney.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.components.PixelButton
import com.smish.wheresmymoney.ui.theme.PixelBackground
import com.smish.wheresmymoney.ui.theme.PixelInk
import com.smish.wheresmymoney.ui.theme.PixelInkLight
import com.smish.wheresmymoney.ui.theme.PixelRed
import com.smish.wheresmymoney.util.ViewModelFactory

@Composable
fun OnboardingSyncScreen(container: AppContainer, onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = ViewModelFactory { OnboardingViewModel(container.userPreferencesRepository, container.authRepository) }
    )
    Scaffold(containerColor = PixelBackground) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("SYNC YOUR EXPENSES", style = MaterialTheme.typography.titleLarge, color = PixelInk)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in with Google to back up your data and use it across devices. " +
                    "You can always do this later from Settings.",
                style = MaterialTheme.typography.bodyMedium, color = PixelInkLight
            )
            Spacer(Modifier.height(24.dp))
            PixelButton(
                text = if (viewModel.isSigningIn) "SIGNING IN..." else "SIGN IN WITH GOOGLE",
                onClick = { viewModel.signInWithGoogle(onSuccess = { viewModel.finishOnboarding(onDone) }) },
                modifier = Modifier.fillMaxWidth()
            )
            viewModel.signInError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = PixelRed, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "SKIP FOR NOW",
                style = MaterialTheme.typography.labelLarge,
                color = PixelInkLight,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.finishOnboarding(onDone) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}
