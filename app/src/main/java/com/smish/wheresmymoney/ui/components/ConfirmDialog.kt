package com.smish.wheresmymoney.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.smish.wheresmymoney.ui.theme.PixelRed
import androidx.compose.ui.tooling.preview.Preview
import com.smish.wheresmymoney.ui.theme.ExpenseTrackerTheme

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "DELETE",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = PixelRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ConfirmDialogPreview() {
    ExpenseTrackerTheme {
        ConfirmDialog(
            title = "Delete item?",
            message = "This action cannot be undone.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
