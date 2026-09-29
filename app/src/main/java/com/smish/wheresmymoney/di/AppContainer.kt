package com.smish.wheresmymoney.di

import android.content.Context
import com.smish.wheresmymoney.data.datastore.UserPreferencesRepository
import com.smish.wheresmymoney.data.local.ExpenseDatabase
import com.smish.wheresmymoney.data.remote.FirestoreSyncManager
import com.smish.wheresmymoney.data.repository.AuthRepository
import com.smish.wheresmymoney.data.repository.CategoryRepository
import com.smish.wheresmymoney.data.repository.ExpenseRepository
import com.smish.wheresmymoney.data.repository.PreferenceRepository

/** One place holding every dependency. Passed down through Compose instead of using Hilt. */
class AppContainer(context: Context) {
    val database = ExpenseDatabase.getInstance(context)

    val userPreferencesRepository = UserPreferencesRepository(context)
    val preferenceRepository = PreferenceRepository(userPreferencesRepository)
    val authRepository = AuthRepository(context)
    val syncManager = FirestoreSyncManager(context, database)

    val categoryRepository = CategoryRepository(context, database.parentCategoryDao(), database.categoryDao(), authRepository, syncManager)
    val expenseRepository = ExpenseRepository(context, database.expenseDao(), authRepository, syncManager)
}
