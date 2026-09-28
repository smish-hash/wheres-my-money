package com.smish.wheresmymoney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_categories")
data class ParentCategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val colorHex: String,       // e.g. "#C6F135"
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
