package com.smish.wheresmymoney.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("date")]
)
data class ExpenseEntity(
    @PrimaryKey val id: Long,
    val categoryId: Long,
    val amount: Double,
    val note: String = "",
    val date: Long,              // epoch millis, start-of-day, device time zone
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
