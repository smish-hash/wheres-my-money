package com.smish.wheresmymoney.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.components.PixelButton
import com.smish.wheresmymoney.ui.components.PixelTextField
import com.smish.wheresmymoney.ui.theme.PixelBackground
import com.smish.wheresmymoney.ui.theme.PixelInk
import com.smish.wheresmymoney.ui.theme.PixelInkLight
import com.smish.wheresmymoney.util.ViewModelFactory

@Composable
fun OnboardingNameScreen(container: AppContainer, onContinue: () -> Unit) {
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
            Text("WELCOME TO", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            Text("WHERE'S MY MONEY", style = MaterialTheme.typography.headlineLarge, color = PixelInk)
            Spacer(Modifier.height(32.dp))
            Text("What should we call you?", style = MaterialTheme.typography.bodyLarge, color = PixelInk)
            Spacer(Modifier.height(8.dp))
            PixelTextField(value = viewModel.name, onValueChange = viewModel::onNameChange, placeholder = "Your name")
            Spacer(Modifier.height(24.dp))
            PixelButton(
                text = "CONTINUE",
                onClick = { viewModel.saveName(onContinue) },
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (viewModel.canContinue()) PixelInk else PixelInkLight
            )
        }
    }
}
