package com.smish.wheresmymoney.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smish.wheresmymoney.data.local.dao.CategoryDao
import com.smish.wheresmymoney.data.local.dao.ExpenseDao
import com.smish.wheresmymoney.data.local.dao.ParentCategoryDao
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE parent_categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE expenses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [ParentCategoryEntity::class, CategoryEntity::class, ExpenseEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun parentCategoryDao(): ParentCategoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            clearAllTables()
        }
    }

    companion object {
        @Volatile private var INSTANCE: ExpenseDatabase? = null

        fun getInstance(context: Context): ExpenseDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
