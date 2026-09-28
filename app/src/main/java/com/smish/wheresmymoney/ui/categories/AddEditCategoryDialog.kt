package com.smish.wheresmymoney.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.ui.components.PixelButton
import com.smish.wheresmymoney.ui.components.PixelCard
import com.smish.wheresmymoney.ui.components.PixelTextField
import com.smish.wheresmymoney.ui.theme.PixelInk
import com.smish.wheresmymoney.ui.theme.PixelInkLight
import com.smish.wheresmymoney.ui.theme.PixelSurface
import com.smish.wheresmymoney.util.toComposeColor

@Composable
fun AddEditCategoryDialog(
    initial: CategoryEntity?,
    parents: List<ParentCategoryEntity>,
    onConfirm: (name: String, parentId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedParentId by remember { mutableStateOf(initial?.parentCategoryId) }

    Dialog(onDismissRequest = onDismiss) {
        PixelCard(backgroundColor = PixelSurface) {
            Text(
                if (initial == null) "NEW CATEGORY" else "EDIT CATEGORY",
                style = MaterialTheme.typography.titleMedium, color = PixelInk
            )
            Spacer(Modifier.height(12.dp))
            PixelTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Groceries")
            Spacer(Modifier.height(12.dp))
            Text("PARENT CATEGORY", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            Spacer(Modifier.height(4.dp))

            Row(
                Modifier.fillMaxWidth().clickable { selectedParentId = null }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedParentId == null, onClick = { selectedParentId = null })
                Text("None", color = PixelInk)
            }
            parents.forEach { parent ->
                Row(
                    Modifier.fillMaxWidth().clickable { selectedParentId = parent.id }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedParentId == parent.id, onClick = { selectedParentId = parent.id })
                    Box(Modifier.size(12.dp).background(parent.colorHex.toComposeColor()))
                    Spacer(Modifier.width(6.dp))
                    Text(parent.name, color = PixelInk)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(text = "CANCEL", onClick = onDismiss, containerColor = PixelSurface, contentColor = PixelInk)
                PixelButton(
                    text = "SAVE",
                    modifier = Modifier.weight(1f),
                    onClick = { if (name.isNotBlank()) { onConfirm(name.trim(), selectedParentId); onDismiss() } }
                )
            }
        }
    }
}
