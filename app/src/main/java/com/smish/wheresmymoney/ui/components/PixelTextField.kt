package com.smish.wheresmymoney.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import com.smish.wheresmymoney.ui.theme.PixelInk
import com.smish.wheresmymoney.ui.theme.PixelInkLight
import com.smish.wheresmymoney.ui.theme.PixelSurface
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import com.smish.wheresmymoney.ui.theme.ExpenseTrackerTheme

@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Preview(showBackground = true)
@Composable
fun PixelTextFieldPreview() {
    ExpenseTrackerTheme {
        androidx.compose.foundation.layout.Box(Modifier.padding(ExpenseTrackerTheme.spacing.xl)) {
            PixelTextField(value = "Sample Text", onValueChange = {}, placeholder = "Placeholder")
        }
    }
}
