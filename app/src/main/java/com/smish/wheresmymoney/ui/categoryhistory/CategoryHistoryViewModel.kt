package com.smish.wheresmymoney.ui.categoryhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.data.repository.CategoryRepository
import com.smish.wheresmymoney.data.repository.ExpenseRepository
import com.smish.wheresmymoney.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

class CategoryHistoryViewModel(
    categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    categoryId: Long,
    yearMonth: YearMonth
) : ViewModel() {

    val category = categoryRepository.categories
        .map { list -> list.find { it.id == categoryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val expenses = run {
        val (start, end) = DateUtils.monthRange(yearMonth)
        expenseRepository.expensesForCategory(categoryId, start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { expenseRepository.deleteExpense(expense) }
    }
}
