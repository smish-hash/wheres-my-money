package com.smish.wheresmymoney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smish.wheresmymoney.data.repository.CategoryRepository
import com.smish.wheresmymoney.data.repository.ExpenseRepository
import com.smish.wheresmymoney.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<HomeUiState> = _yearMonth.flatMapLatest { ym ->
        val (start, end) = DateUtils.monthRange(ym)
        combine(
            categoryRepository.parentCategories,
            categoryRepository.categories,
            expenseRepository.categoryTotals(start, end)
        ) { parents, categories, totals ->
            val totalsMap = totals.associateBy({ it.categoryId }, { it.total })
            val byParent = categories.groupBy { it.parentCategoryId }

            val groups = parents.sortedBy { it.sortOrder }.map { parent ->
                val cats = (byParent[parent.id] ?: emptyList()).map {
                    CategoryTotalUi(it, totalsMap[it.id] ?: 0.0)
                }
                ParentGroupUi(parent, cats, cats.sumOf { it.total })
            }
            val ungrouped = (byParent[null] ?: emptyList()).map {
                CategoryTotalUi(it, totalsMap[it.id] ?: 0.0)
            }

            HomeUiState(
                yearMonth = ym,
                groups = groups,
                ungrouped = ungrouped,
                grandTotal = groups.sumOf { it.subtotal } + ungrouped.sumOf { it.total }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun previousMonth() { _yearMonth.value = _yearMonth.value.minusMonths(1) }
    fun nextMonth() { _yearMonth.value = _yearMonth.value.plusMonths(1) }
}
