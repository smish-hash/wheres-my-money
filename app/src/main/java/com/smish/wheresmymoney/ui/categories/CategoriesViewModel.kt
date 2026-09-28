package com.smish.wheresmymoney.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: CategoryRepository) : ViewModel() {
    val parentCategories = repository.parentCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addParentCategory(name: String, colorHex: String) =
        viewModelScope.launch { repository.addParentCategory(name, colorHex) }
    fun updateParentCategory(parent: ParentCategoryEntity) =
        viewModelScope.launch { repository.updateParentCategory(parent) }
    fun deleteParentCategory(parent: ParentCategoryEntity) =
        viewModelScope.launch { repository.deleteParentCategory(parent) }

    fun addCategory(name: String, parentId: Long?) =
        viewModelScope.launch { repository.addCategory(name, parentId) }
    fun updateCategory(category: CategoryEntity) =
        viewModelScope.launch { repository.updateCategory(category) }
    fun deleteCategory(category: CategoryEntity) =
        viewModelScope.launch { repository.deleteCategory(category) }
}
