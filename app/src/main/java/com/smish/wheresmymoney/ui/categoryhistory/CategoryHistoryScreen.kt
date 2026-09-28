package com.smish.wheresmymoney.ui.categoryhistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.components.BlurredTopAppBar
import com.smish.wheresmymoney.ui.components.ConfirmDialog
import com.smish.wheresmymoney.ui.components.PixelCard
import com.smish.wheresmymoney.ui.theme.*
import com.smish.wheresmymoney.util.CurrencyFormatter
import com.smish.wheresmymoney.util.DateUtils
import com.smish.wheresmymoney.util.ViewModelFactory
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryHistoryScreen(
    container: AppContainer,
    categoryId: Long,
    yearMonth: YearMonth,
    onBack: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onAddExpense: () -> Unit
) {
    val viewModel: CategoryHistoryViewModel = viewModel(
        factory = ViewModelFactory {
            CategoryHistoryViewModel(container.categoryRepository, container.expenseRepository, categoryId, yearMonth)
        }
    )
    val category by viewModel.category.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val spacing = ExpenseTrackerTheme.spacing
    val hazeState = remember { HazeState() }
    var deleteTarget by remember { mutableStateOf<ExpenseEntity?>(null) }

    Scaffold(
        topBar = {
            BlurredTopAppBar(
                hazeState = hazeState,
                title = { Text((category?.name ?: "").uppercase(), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
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
        if (expenses.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No expenses yet this month", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                contentPadding = PaddingValues(
                    start = spacing.l,
                    top = padding.calculateTopPadding() + spacing.l,
                    end = spacing.l,
                    bottom = padding.calculateBottomPadding() + spacing.xxl + 64.dp
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.s)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    PixelCard(modifier = Modifier.fillMaxWidth().clickable { onEditExpense(expense.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(CurrencyFormatter.format(expense.amount), style = MaterialTheme.typography.bodyLarge)
                                if (expense.note.isNotBlank()) {
                                    Text(expense.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(DateUtils.formatDate(expense.date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { deleteTarget = expense }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { expense ->
        ConfirmDialog(
            title = "Delete expense?",
            message = "This cannot be undone.",
            onConfirm = { viewModel.deleteExpense(expense); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryHistoryScreenPreview() {
    ExpenseTrackerTheme {
        // ...
    }
}
