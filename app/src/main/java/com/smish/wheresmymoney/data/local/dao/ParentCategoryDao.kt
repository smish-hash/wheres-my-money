package com.smish.wheresmymoney.data.local.dao

import androidx.room.*
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentCategoryDao {
    @Query("SELECT * FROM parent_categories ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<ParentCategoryEntity>>

    @Query("SELECT * FROM parent_categories")
    suspend fun getAllOnce(): List<ParentCategoryEntity>

    @Insert
    suspend fun insert(parent: ParentCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(parent: ParentCategoryEntity)

    @Update
    suspend fun update(parent: ParentCategoryEntity)

    @Delete
    suspend fun delete(parent: ParentCategoryEntity)

    @Query("DELETE FROM parent_categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM parent_categories")
    suspend fun count(): Int
}
