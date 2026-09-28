package com.smish.wheresmymoney.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Generic factory so ViewModels can take constructor params without Hilt. */
class ViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
