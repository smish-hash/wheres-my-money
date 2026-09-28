package com.smish.wheresmymoney.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = ParentCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("parentCategoryId")]
)
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val parentCategoryId: Long?,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
