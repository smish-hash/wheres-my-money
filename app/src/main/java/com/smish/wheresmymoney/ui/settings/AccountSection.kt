package com.smish.wheresmymoney.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.components.ConfirmDialog
import com.smish.wheresmymoney.ui.components.PixelButton
import com.smish.wheresmymoney.ui.components.PixelCard
import com.smish.wheresmymoney.ui.theme.*
import com.smish.wheresmymoney.util.ViewModelFactory

@Composable
fun AccountSection(container: AppContainer, onAccountDeleted: () -> Unit = {}) {
    val viewModel: AccountViewModel = viewModel(
        factory = ViewModelFactory { AccountViewModel(container.authRepository, container.userPreferencesRepository) }
    )
    val user by viewModel.user.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    PixelCard {
        Text("ACCOUNT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))

        if (user == null) {
            Text(
                "Not signed in. Sign in with Google to back up and sync your data.",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            PixelButton(
                text = if (viewModel.isWorking) "SIGNING IN..." else "SIGN IN WITH GOOGLE",
                onClick = viewModel::signIn,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).background(PixelSurface).border(BorderStroke(2.dp, PixelInk))
                ) {
                    user?.photoUrl?.toString()?.let { url ->
                        AsyncImage(model = url, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user?.displayName ?: "—", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(user?.email ?: "—", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(text = "SIGN OUT", onClick = viewModel::signOut, containerColor = PixelSurface, contentColor = PixelInk)
                PixelButton(text = "DELETE ACCOUNT", onClick = { showDeleteConfirm = true }, containerColor = PixelRed, contentColor = PixelSurface)
            }
        }

        viewModel.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = PixelRed, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete your account?",
            message = "This permanently deletes your cloud backup and unlinks Google sign-in. " +
                "Your expenses already on this device are kept. This cannot be undone.",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteAccount(onDeleted = onAccountDeleted)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
