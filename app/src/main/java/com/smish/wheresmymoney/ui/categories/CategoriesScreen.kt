package com.smish.wheresmymoney.ui.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.components.BlurredTopAppBar
import com.smish.wheresmymoney.ui.components.ConfirmDialog
import com.smish.wheresmymoney.ui.components.PixelCard
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
    var addCategoryParentId by remember { mutableStateOf<Long?>(null) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteParentTarget by remember { mutableStateOf<ParentCategoryEntity?>(null) }
    var deleteCategoryTarget by remember { mutableStateOf<CategoryEntity?>(null) }

    val collapsedParentIds = remember { mutableStateSetOf<Long>() }
    var isUncategorizedCollapsed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BlurredTopAppBar(
                hazeState = hazeState,
                title = { Text("CATEGORIES", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { showAddParent = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add parent category")
                    }
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
                top = padding.calculateTopPadding() + spacing.m,
                end = spacing.l + spacing.xs,
                bottom = padding.calculateBottomPadding() + spacing.l
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.m)
        ) {
            parents.forEach { parent ->
                val subcategories = categories.filter { it.parentCategoryId == parent.id }
                val isExpanded = parent.id !in collapsedParentIds

                item(key = "p_${parent.id}") {
                    val arrowRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 90f else 0f,
                        label = "arrowRotation_${parent.id}"
                    )
                    val parentColor = parent.colorHex.toComposeColor()

                    PixelCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isExpanded) collapsedParentIds.add(parent.id)
                                    else collapsedParentIds.remove(parent.id)
                                }
                                .padding(horizontal = spacing.m, vertical = spacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.rotate(arrowRotation)
                            )
                            Spacer(Modifier.width(spacing.xs))
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .background(parentColor)
                            )
                            Spacer(Modifier.width(spacing.s))
                            Text(
                                parent.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = {
                                    addCategoryParentId = parent.id
                                    showAddCategory = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add subcategory under ${parent.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { editingParent = parent }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit parent",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { deleteParentTarget = parent }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete parent",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = spacing.xl, end = spacing.s, bottom = spacing.s)
                            ) {
                                if (subcategories.isEmpty()) {
                                    Text(
                                        "No subcategories yet",
                                        modifier = Modifier.padding(vertical = spacing.xs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    subcategories.forEach { category ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = spacing.xs),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "•",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(spacing.s))
                                            Text(
                                                category.name,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            IconButton(onClick = { editingCategory = category }) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit category",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { deleteCategoryTarget = category }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete category",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val uncategorized = categories.filter { it.parentCategoryId == null }
            if (uncategorized.isNotEmpty()) {
                item(key = "uncategorized_group") {
                    val isUnexpanded = !isUncategorizedCollapsed
                    val arrowRotation by animateFloatAsState(
                        targetValue = if (isUnexpanded) 90f else 0f,
                        label = "arrowRotation_uncategorized"
                    )

                    PixelCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isUncategorizedCollapsed = !isUncategorizedCollapsed }
                                .padding(horizontal = spacing.m, vertical = spacing.s),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isUnexpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.rotate(arrowRotation)
                            )
                            Spacer(Modifier.width(spacing.s))
                            Text(
                                "UNCATEGORIZED",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = {
                                    addCategoryParentId = null
                                    showAddCategory = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add uncategorized category",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isUnexpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = spacing.xl, end = spacing.s, bottom = spacing.s)
                            ) {
                                uncategorized.forEach { category ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = spacing.xs),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "•",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(spacing.s))
                                        Text(
                                            category.name,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        IconButton(onClick = { editingCategory = category }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit category",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { deleteCategoryTarget = category }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete category",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
            initial = null,
            parents = parents,
            defaultParentId = addCategoryParentId,
            onConfirm = { name, parentId -> viewModel.addCategory(name, parentId) },
            onDismiss = { showAddCategory = false; addCategoryParentId = null }
        )
    }
    editingCategory?.let { category ->
        AddEditCategoryDialog(
            initial = category,
            parents = parents,
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
