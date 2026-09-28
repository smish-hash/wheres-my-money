package com.smish.wheresmymoney.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.components.BlurredTopAppBar
import com.smish.wheresmymoney.ui.components.MonthSelector
import com.smish.wheresmymoney.ui.components.PixelCard
import com.smish.wheresmymoney.ui.theme.*
import com.smish.wheresmymoney.util.toComposeColor
import com.smish.wheresmymoney.util.CurrencyFormatter
import com.smish.wheresmymoney.util.ViewModelFactory
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onAddExpense: (categoryId: Long?) -> Unit,
    onManageCategories: () -> Unit,
    onSettings: () -> Unit,
    onCategoryClick: (categoryId: Long, yearMonth: YearMonth) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory { HomeViewModel(container.categoryRepository, container.expenseRepository) }
    )
    val hazeState = remember { HazeState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = ExpenseTrackerTheme.spacing

    Scaffold(
        topBar = {
            BlurredTopAppBar(
                hazeState = hazeState,
                title = { Text("Where's My Money", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Default.Category, contentDescription = "Manage categories")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Menu, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddExpense(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RectangleShape,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
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
                bottom = padding.calculateBottomPadding() + spacing.xxl + 64.dp
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.l)
        ) {
            item {
                MonthSelector(
                    yearMonth = uiState.yearMonth,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            item {
                PixelCard(
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("TOTAL SPENT", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(spacing.xs))
                    Text(
                        CurrencyFormatter.format(uiState.grandTotal),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    if (uiState.groups.isNotEmpty()) {
                        Spacer(Modifier.height(spacing.m))
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.xl)) {
                            uiState.groups.forEach { group ->
                                Column {
                                    Text(
                                        group.parent.name.uppercase(),
                                        color = group.parent.colorHex.toComposeColor(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        CurrencyFormatter.format(group.subtotal),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(uiState.groups, key = { it.parent.id }) { group ->
                ParentGroupCard(
                    group = group,
                    onAddExpense = onAddExpense,
                    onCategoryClick = { catId -> onCategoryClick(catId, uiState.yearMonth) }
                )
            }

            if (uiState.ungrouped.isNotEmpty()) {
                item {
                    PixelCard {
                        Text("UNCATEGORIZED", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(spacing.s))
                        uiState.ungrouped.forEach { catTotal ->
                            CategoryRow(
                                name = catTotal.category.name,
                                total = catTotal.total,
                                accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = { onCategoryClick(catTotal.category.id, uiState.yearMonth) },
                                onAdd = { onAddExpense(catTotal.category.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ExpenseTrackerTheme {
        // ...
    }
}

@Composable
private fun ParentGroupCard(
    group: ParentGroupUi,
    onAddExpense: (Long) -> Unit,
    onCategoryClick: (Long) -> Unit
) {
    val color = group.parent.colorHex.toComposeColor()
    val spacing = ExpenseTrackerTheme.spacing
    PixelCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color))
            Spacer(Modifier.width(spacing.s))
            Text(group.parent.name.uppercase(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(CurrencyFormatter.format(group.subtotal), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(spacing.s))
        if (group.categories.isEmpty()) {
            Text("No categories yet — add one from Categories.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            group.categories.forEach { catTotal ->
                CategoryRow(
                    name = catTotal.category.name,
                    total = catTotal.total,
                    accentColor = color,
                    onClick = { onCategoryClick(catTotal.category.id) },
                    onAdd = { onAddExpense(catTotal.category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    name: String,
    total: Double,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    val spacing = ExpenseTrackerTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(20.dp).background(accentColor))
        Spacer(Modifier.width(spacing.s))
        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(CurrencyFormatter.format(total), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "Add expense to $name", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
