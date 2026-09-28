package com.smish.wheresmymoney.data.local.dao

import androidx.room.*
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.data.model.CategoryTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenses")
    suspend fun getAllOnce(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE date >= :start AND date < :end ORDER BY date DESC, id DESC")
    fun getExpensesForRange(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT categoryId, SUM(amount) as total, COUNT(*) as count
        FROM expenses
        WHERE date >= :start AND date < :end
        GROUP BY categoryId
        """
    )
    fun getCategoryTotals(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query(
        """
        SELECT * FROM expenses
        WHERE categoryId = :categoryId AND date >= :start AND date < :end
        ORDER BY date DESC, id DESC
        """
    )
    fun getExpensesForCategoryInRange(categoryId: Long, start: Long, end: Long): Flow<List<ExpenseEntity>>
}
