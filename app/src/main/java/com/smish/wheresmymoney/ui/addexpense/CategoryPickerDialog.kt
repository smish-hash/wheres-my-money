package com.smish.wheresmymoney.ui.addexpense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.ui.components.PixelCard
import com.smish.wheresmymoney.util.toComposeColor

@Composable
fun CategoryPickerDialog(
    parents: List<ParentCategoryEntity>,
    categories: List<CategoryEntity>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PixelCard(backgroundColor = MaterialTheme.colorScheme.surface) {
            Text("SELECT CATEGORY", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                parents.forEach { parent ->
                    val color = parent.colorHex.toComposeColor()
                    item {
                        Text(
                            parent.name.uppercase(), color = color,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(categories.filter { it.parentCategoryId == parent.id }, key = { it.id }) { cat ->
                        Text(
                            cat.name, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(cat.id); onDismiss() }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
                val ungrouped = categories.filter { it.parentCategoryId == null }
                if (ungrouped.isNotEmpty()) {
                    item {
                        Text(
                            "OTHER", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(ungrouped, key = { it.id }) { cat ->
                        Text(
                            cat.name, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(cat.id); onDismiss() }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
