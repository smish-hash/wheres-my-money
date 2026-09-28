package com.smish.wheresmymoney.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.smish.wheresmymoney.ui.theme.ExpenseTrackerTheme

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    borderColor: Color = MaterialTheme.colorScheme.primary
) {
    val spacing = ExpenseTrackerTheme.spacing
    Box(
        modifier = modifier
            .background(containerColor, RectangleShape)
            .border(BorderStroke(2.dp, borderColor))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.xl, vertical = spacing.m),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PixelButtonPreview() {
    ExpenseTrackerTheme {
        Box(Modifier.padding(ExpenseTrackerTheme.spacing.xl)) {
            PixelButton(text = "Pixel\nButton", onClick = {})
        }
    }
}
