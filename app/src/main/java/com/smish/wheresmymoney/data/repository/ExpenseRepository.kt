package com.smish.wheresmymoney.data.repository

import com.smish.wheresmymoney.data.local.dao.ExpenseDao
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.data.remote.FirestoreSyncManager
import com.smish.wheresmymoney.util.IdGenerator

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val authRepository: AuthRepository,
    private val syncManager: FirestoreSyncManager
) {
    fun expensesForRange(start: Long, end: Long) = expenseDao.getExpensesForRange(start, end)
    fun categoryTotals(start: Long, end: Long) = expenseDao.getCategoryTotals(start, end)
    fun expensesForCategory(categoryId: Long, start: Long, end: Long) =
        expenseDao.getExpensesForCategoryInRange(categoryId, start, end)

    suspend fun getById(id: Long) = expenseDao.getById(id)

    suspend fun addExpense(categoryId: Long, amount: Double, note: String, date: Long) {
        val entity = ExpenseEntity(id = IdGenerator.newId(), categoryId = categoryId, amount = amount, note = note, date = date)
        expenseDao.insert(entity)
        push(entity)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        val updated = expense.copy(updatedAt = System.currentTimeMillis())
        expenseDao.update(updated)
        push(updated)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.delete(expense)
        authRepository.currentUid()?.let { syncManager.pushExpenseDeleted(it, expense.id) }
    }

    private fun push(entity: ExpenseEntity) {
        authRepository.currentUid()?.let { syncManager.pushExpense(it, entity) }
    }
}
