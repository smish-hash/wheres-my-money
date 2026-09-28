package com.smish.wheresmymoney.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.ui.components.PixelButton
import com.smish.wheresmymoney.ui.components.PixelCard
import com.smish.wheresmymoney.ui.components.PixelTextField
import com.smish.wheresmymoney.ui.theme.ParentCategoryPaletteHex
import com.smish.wheresmymoney.ui.theme.PixelInk
import com.smish.wheresmymoney.ui.theme.PixelInkLight
import com.smish.wheresmymoney.ui.theme.PixelSurface
import com.smish.wheresmymoney.util.toComposeColor

@Composable
fun AddEditParentCategoryDialog(
    initial: ParentCategoryEntity?,
    onConfirm: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.colorHex ?: ParentCategoryPaletteHex.first()) }

    Dialog(onDismissRequest = onDismiss) {
        PixelCard(backgroundColor = PixelSurface) {
            Text(
                if (initial == null) "NEW PARENT CATEGORY" else "EDIT PARENT CATEGORY",
                style = MaterialTheme.typography.titleMedium, color = PixelInk
            )
            Spacer(Modifier.height(12.dp))
            PixelTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Fixed")
            Spacer(Modifier.height(12.dp))
            Text("COLOR", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ParentCategoryPaletteHex.forEach { hex ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(hex.toComposeColor())
                            .border(BorderStroke(if (color == hex) 3.dp else 1.dp, PixelInk))
                            .clickable { color = hex }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(text = "CANCEL", onClick = onDismiss, containerColor = PixelSurface, contentColor = PixelInk)
                PixelButton(
                    text = "SAVE",
                    modifier = Modifier.weight(1f),
                    onClick = { if (name.isNotBlank()) { onConfirm(name.trim(), color); onDismiss() } }
                )
            }
        }
    }
}
