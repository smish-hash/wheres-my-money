package com.smish.wheresmymoney.ui.home

import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import java.time.YearMonth

data class CategoryTotalUi(
    val category: CategoryEntity,
    val total: Double
)

data class ParentGroupUi(
    val parent: ParentCategoryEntity,
    val categories: List<CategoryTotalUi>,
    val subtotal: Double
)

data class HomeUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val groups: List<ParentGroupUi> = emptyList(),
    val ungrouped: List<CategoryTotalUi> = emptyList(),
    val grandTotal: Double = 0.0
)
