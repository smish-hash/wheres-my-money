package com.smish.wheresmymoney.data.remote

import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity

data class ParentCategoryDto(
    val id: Long = 0,
    val name: String = "",
    val colorHex: String = "",
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deleted: Boolean = false
)
fun ParentCategoryEntity.toDto() = ParentCategoryDto(id, name, colorHex, sortOrder, createdAt, updatedAt)
fun ParentCategoryDto.toEntity() = ParentCategoryEntity(id, name, colorHex, sortOrder, createdAt, updatedAt)

data class CategoryDto(
    val id: Long = 0,
    val name: String = "",
    val parentCategoryId: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deleted: Boolean = false
)
fun CategoryEntity.toDto() = CategoryDto(id, name, parentCategoryId, sortOrder, createdAt, updatedAt)
fun CategoryDto.toEntity() = CategoryEntity(id, name, parentCategoryId, sortOrder, createdAt, updatedAt)

data class ExpenseDto(
    val id: Long = 0,
    val categoryId: Long = 0,
    val amount: Double = 0.0,
    val note: String = "",
    val date: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deleted: Boolean = false
)
fun ExpenseEntity.toDto() = ExpenseDto(id, categoryId, amount, note, date, createdAt, updatedAt)
fun ExpenseDto.toEntity() = ExpenseEntity(id, categoryId, amount, note, date, createdAt, updatedAt)
