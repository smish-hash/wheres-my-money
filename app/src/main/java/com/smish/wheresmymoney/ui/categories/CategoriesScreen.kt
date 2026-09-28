package com.smish.wheresmymoney.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.components.BlurredTopAppBar
import com.smish.wheresmymoney.ui.components.ConfirmDialog
import com.smish.wheresmymoney.ui.theme.*
import com.smish.wheresmymoney.util.ViewModelFactory
import com.smish.wheresmymoney.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: CategoriesViewModel = viewModel(
        factory = ViewModelFactory { CategoriesViewModel(container.categoryRepository) }
    )
    val parents by viewModel.parentCategories.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val spacing = ExpenseTrackerTheme.spacing
    val hazeState = remember { HazeState() }

    var showAddParent by remember { mutableStateOf(false) }
    var editingParent by remember { mutableStateOf<ParentCategoryEntity?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteParentTarget by remember { mutableStateOf<ParentCategoryEntity?>(null) }
    var deleteCategoryTarget by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            BlurredTopAppBar(
                hazeState = hazeState,
                title = { Text("CATEGORIES", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
            contentPadding = PaddingValues(
                start = spacing.l,
                top = padding.calculateTopPadding() + spacing.l,
                end = spacing.l,
                bottom = padding.calculateBottomPadding() + spacing.l
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PARENT CATEGORIES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { showAddParent = true }) { Icon(Icons.Default.Add, contentDescription = "Add parent category") }
                }
            }
            items(parents, key = { "p${it.id}" }) { parent ->
                Row(Modifier.fillMaxWidth().padding(vertical = spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(parent.colorHex.toComposeColor()))
                    Spacer(Modifier.width(spacing.s))
                    Text(parent.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { editingParent = parent }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { deleteParentTarget = parent }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = spacing.xl, bottom = spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CATEGORIES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { showAddCategory = true }) { Icon(Icons.Default.Add, contentDescription = "Add category") }
                }
            }
            items(categories, key = { "c${it.id}" }) { category ->
                val parentColor = parents.find { it.id == category.parentCategoryId }?.colorHex?.toComposeColor() ?: MaterialTheme.colorScheme.onSurfaceVariant
                Row(Modifier.fillMaxWidth().padding(vertical = spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(20.dp).background(parentColor))
                    Spacer(Modifier.width(spacing.s))
                    Text(category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { editingCategory = category }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { deleteCategoryTarget = category }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    if (showAddParent) {
        AddEditParentCategoryDialog(
            initial = null,
            onConfirm = { name, color -> viewModel.addParentCategory(name, color) },
            onDismiss = { showAddParent = false }
        )
    }
    editingParent?.let { parent ->
        AddEditParentCategoryDialog(
            initial = parent,
            onConfirm = { name, color -> viewModel.updateParentCategory(parent.copy(name = name, colorHex = color)) },
            onDismiss = { editingParent = null }
        )
    }
    if (showAddCategory) {
        AddEditCategoryDialog(
            initial = null, parents = parents,
            onConfirm = { name, parentId -> viewModel.addCategory(name, parentId) },
            onDismiss = { showAddCategory = false }
        )
    }
    editingCategory?.let { category ->
        AddEditCategoryDialog(
            initial = category, parents = parents,
            onConfirm = { name, parentId -> viewModel.updateCategory(category.copy(name = name, parentCategoryId = parentId)) },
            onDismiss = { editingCategory = null }
        )
    }
    deleteParentTarget?.let { parent ->
        ConfirmDialog(
            title = "Delete ${parent.name}?",
            message = "Categories under this will become uncategorized (not deleted). This cannot be undone.",
            onConfirm = { viewModel.deleteParentCategory(parent); deleteParentTarget = null },
            onDismiss = { deleteParentTarget = null }
        )
    }
    deleteCategoryTarget?.let { category ->
        ConfirmDialog(
            title = "Delete ${category.name}?",
            message = "All expenses recorded under this category will also be deleted. This cannot be undone.",
            onConfirm = { viewModel.deleteCategory(category); deleteCategoryTarget = null },
            onDismiss = { deleteCategoryTarget = null }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CategoriesScreenPreview() {
    ExpenseTrackerTheme {
        // ...
    }
}
