package com.smish.wheresmymoney.ui.addexpense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smish.wheresmymoney.di.AppContainer
import com.smish.wheresmymoney.ui.components.BlurredTopAppBar
import com.smish.wheresmymoney.ui.components.PixelButton
import com.smish.wheresmymoney.ui.components.PixelCard
import com.smish.wheresmymoney.ui.components.PixelTextField
import com.smish.wheresmymoney.ui.theme.*
import com.smish.wheresmymoney.util.DateUtils
import com.smish.wheresmymoney.util.ViewModelFactory
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    container: AppContainer,
    expenseId: Long?,
    initialCategoryId: Long?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: AddExpenseViewModel = viewModel(
        factory = ViewModelFactory {
            AddExpenseViewModel(container.categoryRepository, container.expenseRepository, expenseId, initialCategoryId)
        }
    )
    val parents by viewModel.parentCategories.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val spacing = ExpenseTrackerTheme.spacing
    var showPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }


    val selectedCategory = categories.find { it.id == viewModel.selectedCategoryId }
    val snackbarHostState = remember { SnackbarHostState() }

    val hazeState = remember { HazeState() }

    LaunchedEffect(viewModel.showSavedMessage) {
        if (viewModel.showSavedMessage) {
            snackbarHostState.showSnackbar("Expense saved!")
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 80.dp + spacing.xxl)) },
        topBar = {
            BlurredTopAppBar(
                hazeState = hazeState,
                title = { Text(if (viewModel.currentExpenseId != null) "EDIT EXPENSE" else "ADD EXPENSE", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(spacing.l)
                .hazeSource(state = hazeState),
            verticalArrangement = Arrangement.spacedBy(spacing.m)
        ) {
            Text("CATEGORY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PixelCard(modifier = Modifier.fillMaxWidth().clickable { showPicker = true }) {
                Text(selectedCategory?.name ?: "Tap to choose", style = MaterialTheme.typography.bodyLarge)
            }

            Text("AMOUNT (₹)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PixelTextField(
                value = viewModel.amountText, onValueChange = viewModel::onAmountChange,
                placeholder = "0.00", keyboardType = KeyboardType.Decimal
            )

            Text("DATE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PixelCard(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                Text(DateUtils.formatDate(viewModel.dateMillis), style = MaterialTheme.typography.bodyLarge)
            }

            Text("NOTE (OPTIONAL)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PixelTextField(
                value = viewModel.note, onValueChange = viewModel::onNoteChange,
                placeholder = "e.g. Groceries at BigBasket"
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s)
            ) {
                PixelButton(
                    text = "SAVE &\nADD ANOTHER",
                    onClick = { viewModel.save() },
                    modifier = Modifier.weight(1f),
                    containerColor = if (viewModel.canSave()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                    contentColor = if (viewModel.canSave()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
                )
                PixelButton(
                    text = "SAVE &\nCLOSE",
                    onClick = { viewModel.save(onDone) },
                    modifier = Modifier.weight(1f),
                    containerColor = if (viewModel.canSave()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    contentColor = if (viewModel.canSave()) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.surface
                )
            }
        }
    }

    if (showPicker) {
        CategoryPickerDialog(
            parents = parents, categories = categories,
            onSelect = viewModel::onCategorySelected, onDismiss = { showPicker = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddExpenseScreenPreview() {
    ExpenseTrackerTheme {
        // ...
    }
}
