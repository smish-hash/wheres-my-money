package com.smish.wheresmymoney.ui.addexpense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.data.repository.CategoryRepository
import com.smish.wheresmymoney.data.repository.ExpenseRepository
import com.smish.wheresmymoney.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    initialExpenseId: Long?,
    initialCategoryId: Long?
) : ViewModel() {

    val parentCategories = categoryRepository.parentCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentExpenseId by mutableStateOf(initialExpenseId); private set
    var selectedCategoryId by mutableStateOf(initialCategoryId); private set
    var amountText by mutableStateOf(""); private set
    var note by mutableStateOf(""); private set
    var dateMillis by mutableStateOf(DateUtils.today()); private set
    var showSavedMessage by mutableStateOf(false); private set

    init {
        loadExpense(initialExpenseId)
    }

    private fun loadExpense(id: Long?) {
        if (id != null) {
            viewModelScope.launch {
                expenseRepository.getById(id)?.let { expense ->
                    selectedCategoryId = expense.categoryId
                    amountText = if (expense.amount == expense.amount.toLong().toDouble())
                        expense.amount.toLong().toString() else expense.amount.toString()
                    note = expense.note
                    dateMillis = expense.date
                }
            }
        }
    }

    fun onCategorySelected(id: Long) { selectedCategoryId = id }
    fun onAmountChange(value: String) { amountText = value.filter { it.isDigit() || it == '.' } }
    fun onNoteChange(value: String) { note = value }
    fun onDateChange(millis: Long) { dateMillis = millis }
    fun clearMessage() { showSavedMessage = false }

    fun canSave(): Boolean =
        selectedCategoryId != null && (amountText.toDoubleOrNull() ?: 0.0) > 0.0

    fun save(onDone: (() -> Unit)? = null) {
        val amount = amountText.toDoubleOrNull() ?: return
        val categoryId = selectedCategoryId ?: return
        viewModelScope.launch {
            if (currentExpenseId != null) {
                expenseRepository.updateExpense(
                    ExpenseEntity(id = currentExpenseId!!, categoryId = categoryId, amount = amount, note = note, date = dateMillis)
                )
            } else {
                expenseRepository.addExpense(categoryId, amount, note, dateMillis)
            }
            
            if (onDone != null) {
                onDone()
            } else {
                // "Add another" path
                amountText = ""
                note = ""
                currentExpenseId = null // After editing or adding, the next one is always "New"
                showSavedMessage = true
            }
        }
    }
}
