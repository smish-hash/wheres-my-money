package com.smish.wheresmymoney.data.repository

import com.smish.wheresmymoney.data.local.dao.CategoryDao
import com.smish.wheresmymoney.data.local.dao.ParentCategoryDao
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.data.remote.FirestoreSyncManager
import com.smish.wheresmymoney.util.IdGenerator
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val parentDao: ParentCategoryDao,
    private val categoryDao: CategoryDao,
    private val authRepository: AuthRepository,
    private val syncManager: FirestoreSyncManager
) {
    val parentCategories: Flow<List<ParentCategoryEntity>> = parentDao.getAll()
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAll()

    suspend fun addParentCategory(name: String, colorHex: String) {
        val entity = ParentCategoryEntity(id = IdGenerator.newId(), name = name, colorHex = colorHex)
        parentDao.insert(entity)
        pushParent(entity)
    }

    suspend fun updateParentCategory(parent: ParentCategoryEntity) {
        val updated = parent.copy(updatedAt = System.currentTimeMillis())
        parentDao.update(updated)
        pushParent(updated)
    }

    suspend fun deleteParentCategory(parent: ParentCategoryEntity) {
        parentDao.delete(parent)
        authRepository.currentUid()?.let { syncManager.pushParentCategoryDeleted(it, parent.id) }
    }

    suspend fun addCategory(name: String, parentCategoryId: Long?) {
        val entity = CategoryEntity(id = IdGenerator.newId(), name = name, parentCategoryId = parentCategoryId)
        categoryDao.insert(entity)
        pushCategory(entity)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        val updated = category.copy(updatedAt = System.currentTimeMillis())
        categoryDao.update(updated)
        pushCategory(updated)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.delete(category)
        authRepository.currentUid()?.let { syncManager.pushCategoryDeleted(it, category.id) }
    }

    private fun pushParent(entity: ParentCategoryEntity) {
        authRepository.currentUid()?.let { syncManager.pushParentCategory(it, entity) }
    }
    private fun pushCategory(entity: CategoryEntity) {
        authRepository.currentUid()?.let { syncManager.pushCategory(it, entity) }
    }

    suspend fun seedDefaultsIfEmpty() {
        if (parentDao.count() > 0) return
        val fixed = ParentCategoryEntity(id = IdGenerator.newId(), name = "Fixed", colorHex = "#2B2B2B", sortOrder = 0)
        val flexi = ParentCategoryEntity(id = IdGenerator.newId(), name = "Flexi", colorHex = "#C6F135", sortOrder = 1)
        parentDao.insert(fixed)
        parentDao.insert(flexi)
        pushParent(fixed)
        pushParent(flexi)
        listOf("Investments", "Bills", "Rent").forEachIndexed { i, n ->
            val cat = CategoryEntity(id = IdGenerator.newId(), name = n, parentCategoryId = fixed.id, sortOrder = i)
            categoryDao.insert(cat)
            pushCategory(cat)
        }
        listOf("Travel", "Food", "Outing", "Others").forEachIndexed { i, n ->
            val cat = CategoryEntity(id = IdGenerator.newId(), name = n, parentCategoryId = flexi.id, sortOrder = i)
            categoryDao.insert(cat)
            pushCategory(cat)
        }
    }
}
