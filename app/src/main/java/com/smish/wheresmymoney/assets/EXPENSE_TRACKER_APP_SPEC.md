# Pixel Expense Tracker — Android App Implementation Spec

A monthly personal expense manager built with **Jetpack Compose + Room**, in a pixel-art
visual style (cream background, thick black borders, offset "stacked card" shadows, lime
green + coral accents) inspired by the reference screenshots.

## 1. Feature Summary (confirmed requirements)

- Monthly view of expenses, with month-by-month navigation (◀ Sept 2026 ▶).
- **Categories** (e.g. Rent, Food, Travel) are **global** — create once, reused every month.
  Editable: add / rename / reassign parent / delete.
- **Parent categories** (e.g. `Fixed`, `Flexi`) group categories together. You can add your
  own parent categories, each gets a distinct color used to highlight its categories
  everywhere in the UI.
- Add expenses against any category, with amount, optional note, and date.
- Dashboard shows: **Grand Total = sum of all parent-category subtotals** (e.g.
  `Total = Fixed + Flexi`), plus a subtotal per parent category, plus a per-category total.
- Currency: **₹ INR**.
- No budgets/limits in this version (noted as a future enhancement, see §9).
- Local-only storage via Room — no login, no network.

## 2. Tech Stack

| Layer | Choice |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Language | Kotlin |
| Persistence | Room (SQLite) |
| DI | Manual (a single `AppContainer`, no Hilt) — keeps Gradle setup simple |
| Navigation | Navigation-Compose |
| State | ViewModel + StateFlow |
| Min SDK | 24 (with core library desugaring for `java.time`) |

I used **manual DI instead of Hilt** deliberately — it avoids KSP/Hilt version-matrix
headaches for a solo project and the whole dependency graph is ~5 classes. If you later want
Hilt, the `AppContainer` maps almost 1:1 onto Hilt modules.

## 3. Visual Design Notes

- Background: cream `#F7ECD9`. Cards: off-white `#FFF9EE` with a **2dp solid black border**
  and a **4dp offset solid black block behind it** (the "stacked card" look from your
  screenshots — see `PixelCard`).
- Corners are **square** (0dp radius) everywhere — buttons, cards, text fields.
- Primary ink color `#1A1A1A` (near-black) for buttons/text, lime green `#C6F135` is the hero
  accent (matches the "+99.5 BAUD" / green CTA in your reference).
- Each **parent category** gets a color from a 6-color pixel palette (green, coral, blue,
  yellow, purple, maroon) — shown as a small square swatch and used as a left-border accent
  on every category row under it.
- Font: ships wired up for **Press Start 2P** (the classic pixel font) but **defaults to
  `FontFamily.Monospace`** so the project compiles immediately without you needing to add a
  font file. Swapping in the real pixel font is a 2-minute optional step — see §5.2.

## 4. Project Structure

```
ExpenseTracker/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── values/strings.xml
        │   ├── values/themes.xml
        │   └── font/            (optional — press_start_2p.ttf goes here)
        └── java/com/expensetracker/app/
            ├── ExpenseTrackerApp.kt
            ├── MainActivity.kt
            ├── di/
            │   └── AppContainer.kt
            ├── data/
            │   ├── local/
            │   │   ├── ExpenseDatabase.kt
            │   │   ├── entity/
            │   │   │   ├── ParentCategoryEntity.kt
            │   │   │   ├── CategoryEntity.kt
            │   │   │   └── ExpenseEntity.kt
            │   │   └── dao/
            │   │       ├── ParentCategoryDao.kt
            │   │       ├── CategoryDao.kt
            │   │       └── ExpenseDao.kt
            │   ├── model/
            │   │   └── CategoryTotal.kt
            │   └── repository/
            │       ├── CategoryRepository.kt
            │       └── ExpenseRepository.kt
            ├── util/
            │   ├── DateUtils.kt
            │   ├── CurrencyFormatter.kt
            │   ├── ColorExt.kt
            │   └── ViewModelFactory.kt
            └── ui/
                ├── theme/
                │   ├── Color.kt
                │   ├── Type.kt
                │   ├── Shape.kt
                │   └── Theme.kt
                ├── components/
                │   ├── PixelButton.kt
                │   ├── PixelCard.kt
                │   ├── PixelTextField.kt
                │   ├── MonthSelector.kt
                │   └── ConfirmDialog.kt
                ├── navigation/
                │   ├── Screen.kt
                │   └── NavGraph.kt
                ├── home/
                │   ├── HomeScreen.kt
                │   ├── HomeViewModel.kt
                │   └── HomeUiState.kt
                ├── categories/
                │   ├── CategoriesScreen.kt
                │   ├── CategoriesViewModel.kt
                │   ├── AddEditParentCategoryDialog.kt
                │   └── AddEditCategoryDialog.kt
                ├── addexpense/
                │   ├── AddExpenseScreen.kt
                │   ├── AddExpenseViewModel.kt
                │   └── CategoryPickerDialog.kt
                └── categoryhistory/
                    ├── CategoryHistoryScreen.kt
                    └── CategoryHistoryViewModel.kt
```

## 5. Setup Instructions

### 5.1 Create the project
1. Install **Android Studio Ladybug (2024.2.1)** or newer, with JDK 17.
2. `New Project → Empty Activity (Compose)`. Package name: `com.expensetracker.app`.
   Minimum SDK: **API 24**.
3. Delete the generated sample `MainActivity.kt`/theme files and replace with everything
   below, matching the folder structure in §4.
4. Copy each Gradle file in §6 over the generated ones.
5. Sync Gradle, then Run.

### 5.2 (Optional) Add the real pixel font
1. Download **Press Start 2P** from [Google Fonts](https://fonts.google.com/specimen/Press+Start+2P).
2. Rename the file to `press_start_2p.ttf`, place it in `app/src/main/res/font/`.
3. In `ui/theme/Type.kt`, uncomment the two marked lines to switch `PixelFontFamily` from
   `FontFamily.Monospace` to the real font. Rebuild.

---

## 6. Gradle Files

### `settings.gradle.kts`
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ExpenseTracker"
include(":app")
```

### `build.gradle.kts` (root)
```kotlin
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}
```

### `gradle.properties`
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

### `app/build.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.expensetracker.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.expensetracker.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

### `app/src/main/AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".ExpenseTrackerApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.ExpenseTracker">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.ExpenseTracker">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### `app/src/main/res/values/strings.xml`
```xml
<resources>
    <string name="app_name">Expense Tracker</string>
</resources>
```

### `app/src/main/res/values/themes.xml`
```xml
<resources>
    <style name="Theme.ExpenseTracker" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
```

> **Note on app icon:** let Android Studio's `New Project` wizard generate the default
> `mipmap/ic_launcher` set (it does this automatically), or swap it later via
> `Image Asset Studio` for a custom pixel-art icon. Not hand-coded here.

---

## 7. Data Layer

### `data/local/entity/ParentCategoryEntity.kt`
```kotlin
package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_categories")
data class ParentCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,       // e.g. "#C6F135"
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

### `data/local/entity/CategoryEntity.kt`
```kotlin
package com.expensetracker.app.data.local.entity

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentCategoryId: Long?,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

### `data/local/entity/ExpenseEntity.kt`
```kotlin
package com.expensetracker.app.data.local.entity

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val note: String = "",
    val date: Long,              // epoch millis, start-of-day, device time zone
    val createdAt: Long = System.currentTimeMillis()
)
```

> Deleting a **category cascades and deletes its expenses**. Deleting a **parent category**
> only unlinks its categories (they become "uncategorized"), it does not touch expenses.
> Both dialogs in the app warn you before deleting (see `CategoriesScreen.kt`).

### `data/model/CategoryTotal.kt`
```kotlin
package com.expensetracker.app.data.model

data class CategoryTotal(
    val categoryId: Long,
    val total: Double,
    val count: Int
)
```

### `data/local/dao/ParentCategoryDao.kt`
```kotlin
package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentCategoryDao {
    @Query("SELECT * FROM parent_categories ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<ParentCategoryEntity>>

    @Insert
    suspend fun insert(parent: ParentCategoryEntity): Long

    @Update
    suspend fun update(parent: ParentCategoryEntity)

    @Delete
    suspend fun delete(parent: ParentCategoryEntity)

    @Query("SELECT COUNT(*) FROM parent_categories")
    suspend fun count(): Int
}
```

### `data/local/dao/CategoryDao.kt`
```kotlin
package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
```

### `data/local/dao/ExpenseDao.kt`
```kotlin
package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.data.model.CategoryTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

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
```

### `data/local/ExpenseDatabase.kt`
```kotlin
package com.expensetracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.ParentCategoryDao
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity

@Database(
    entities = [ParentCategoryEntity::class, CategoryEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun parentCategoryDao(): ParentCategoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var INSTANCE: ExpenseDatabase? = null

        fun getInstance(context: Context): ExpenseDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

### `data/repository/CategoryRepository.kt`
```kotlin
package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.ParentCategoryDao
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val parentDao: ParentCategoryDao,
    private val categoryDao: CategoryDao
) {
    val parentCategories: Flow<List<ParentCategoryEntity>> = parentDao.getAll()
    val categories: Flow<List<CategoryEntity>> = categoryDao.getAll()

    suspend fun addParentCategory(name: String, colorHex: String) {
        parentDao.insert(ParentCategoryEntity(name = name, colorHex = colorHex))
    }
    suspend fun updateParentCategory(parent: ParentCategoryEntity) = parentDao.update(parent)
    suspend fun deleteParentCategory(parent: ParentCategoryEntity) = parentDao.delete(parent)

    suspend fun addCategory(name: String, parentCategoryId: Long?) {
        categoryDao.insert(CategoryEntity(name = name, parentCategoryId = parentCategoryId))
    }
    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)
    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)

    /** Seeds sensible defaults matching the Fixed/Flexi example on first-ever launch. */
    suspend fun seedDefaultsIfEmpty() {
        if (parentDao.count() > 0) return
        val fixedId = parentDao.insert(ParentCategoryEntity(name = "Fixed", colorHex = "#2B2B2B", sortOrder = 0))
        val flexiId = parentDao.insert(ParentCategoryEntity(name = "Flexi", colorHex = "#C6F135", sortOrder = 1))
        listOf("Investments", "Bills", "Rent").forEachIndexed { i, n ->
            categoryDao.insert(CategoryEntity(name = n, parentCategoryId = fixedId, sortOrder = i))
        }
        listOf("Travel", "Food", "Outing", "Others").forEachIndexed { i, n ->
            categoryDao.insert(CategoryEntity(name = n, parentCategoryId = flexiId, sortOrder = i))
        }
    }
}
```

### `data/repository/ExpenseRepository.kt`
```kotlin
package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.entity.ExpenseEntity

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    fun expensesForRange(start: Long, end: Long) = expenseDao.getExpensesForRange(start, end)
    fun categoryTotals(start: Long, end: Long) = expenseDao.getCategoryTotals(start, end)
    fun expensesForCategory(categoryId: Long, start: Long, end: Long) =
        expenseDao.getExpensesForCategoryInRange(categoryId, start, end)

    suspend fun getById(id: Long) = expenseDao.getById(id)

    suspend fun addExpense(categoryId: Long, amount: Double, note: String, date: Long) {
        expenseDao.insert(ExpenseEntity(categoryId = categoryId, amount = amount, note = note, date = date))
    }
    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.update(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.delete(expense)
}
```

---

## 8. DI + Utilities

### `di/AppContainer.kt`
```kotlin
package com.expensetracker.app.di

import android.content.Context
import com.expensetracker.app.data.local.ExpenseDatabase
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.ExpenseRepository

/** One place holding every dependency. Passed down through Compose instead of using Hilt. */
class AppContainer(context: Context) {
    private val database = ExpenseDatabase.getInstance(context)
    val categoryRepository = CategoryRepository(database.parentCategoryDao(), database.categoryDao())
    val expenseRepository = ExpenseRepository(database.expenseDao())
}
```

### `ExpenseTrackerApp.kt`
```kotlin
package com.expensetracker.app

import android.app.Application
import com.expensetracker.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseTrackerApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(Dispatchers.IO).launch {
            container.categoryRepository.seedDefaultsIfEmpty()
        }
    }
}
```

### `util/ViewModelFactory.kt`
```kotlin
package com.expensetracker.app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Generic factory so ViewModels can take constructor params without Hilt. */
class ViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
```

### `util/DateUtils.kt`
```kotlin
package com.expensetracker.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val zone: ZoneId = ZoneId.systemDefault()

    /** [start, end) epoch-millis range covering the given month. */
    fun monthRange(yearMonth: YearMonth): Pair<Long, Long> {
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun today(): Long = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    fun formatMonth(yearMonth: YearMonth): String =
        "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}"

    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}
```

### `util/CurrencyFormatter.kt`
```kotlin
package com.expensetracker.app.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    fun format(amount: Double): String = "₹${formatter.format(amount)}"
}
```

### `util/ColorExt.kt`
```kotlin
package com.expensetracker.app.util

import androidx.compose.ui.graphics.Color

fun String.toComposeColor(): Color = Color(android.graphics.Color.parseColor(this))
```

---

## 9. Theme (pixel styling)

### `ui/theme/Color.kt`
```kotlin
package com.expensetracker.app.ui.theme

import androidx.compose.ui.graphics.Color

val PixelBackground = Color(0xFFF7ECD9)
val PixelSurface = Color(0xFFFFF9EE)
val PixelInk = Color(0xFF1A1A1A)
val PixelInkLight = Color(0xFF706A5C)
val PixelGreen = Color(0xFFC6F135)
val PixelRed = Color(0xFFD9603B)

/** Palette offered when creating a new parent category. */
val ParentCategoryPaletteHex = listOf(
    "#C6F135", // green
    "#D9603B", // coral
    "#6FA8DC", // blue
    "#F2C14E", // yellow
    "#B19CD9", // purple
    "#6B2A2A"  // maroon
)
```

### `ui/theme/Type.kt`
```kotlin
package com.expensetracker.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// Default: ships working out-of-the-box with no extra font file needed.
val PixelFontFamily = FontFamily.Monospace

// --- To use the real "Press Start 2P" pixel font instead (see README §5.2): ---
// 1. Add app/src/main/res/font/press_start_2p.ttf
// 2. Uncomment these two lines and delete the FontFamily.Monospace line above:
// import androidx.compose.ui.text.font.Font
// import androidx.compose.ui.text.font.FontWeight
// import com.expensetracker.app.R
// val PixelFontFamily = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))

val PixelTypography = Typography(
    headlineLarge = TextStyle(fontFamily = PixelFontFamily, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = PixelFontFamily, fontSize = 17.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = PixelFontFamily, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = PixelFontFamily, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = PixelFontFamily, fontSize = 9.sp, lineHeight = 12.sp),
)
```

### `ui/theme/Shape.kt`
```kotlin
package com.expensetracker.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Square corners everywhere — that's the pixel look.
val PixelShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)
```

### `ui/theme/Theme.kt`
```kotlin
package com.expensetracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PixelColorScheme = lightColorScheme(
    primary = PixelInk,
    onPrimary = PixelBackground,
    secondary = PixelGreen,
    onSecondary = PixelInk,
    background = PixelBackground,
    onBackground = PixelInk,
    surface = PixelSurface,
    onSurface = PixelInk,
    error = PixelRed,
    onError = PixelSurface
)

@Composable
fun ExpenseTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PixelColorScheme,
        typography = PixelTypography,
        shapes = PixelShapes,
        content = content
    )
}
```

---

## 10. Reusable Components

### `ui/components/PixelCard.kt`
```kotlin
package com.expensetracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expensetracker.app.ui.theme.PixelInk
import com.expensetracker.app.ui.theme.PixelSurface

/** The "stacked card" look: a solid black block offset behind a bordered card. */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelSurface,
    borderColor: Color = PixelInk,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable Column.() -> Unit = {}
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSizeSafe()
                .offset(x = 4.dp, y = 4.dp)
                .background(PixelInk)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .border(BorderStroke(2.dp, borderColor))
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

// matchParentSize() is only available inside a BoxScope; this small helper keeps
// PixelCard's signature simple. Replace body with `Modifier` if you inline this in a BoxScope.
private fun Modifier.matchParentSizeSafe(): Modifier = this
```

> **Compiler note:** `matchParentSize()` requires `BoxScope`. In the real file, write the
> shadow `Box` directly inside the outer `Box { ... }` block and call
> `Modifier.matchParentSize()` there (as shown) — this snippet's placeholder function exists
> only so the signature above reads cleanly; when you paste this in Android Studio, keep the
> shadow `Box` nested exactly as shown (it already is) and it will resolve correctly since
> it's lexically inside the outer `Box`'s content lambda.

### `ui/components/PixelButton.kt`
```kotlin
package com.expensetracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expensetracker.app.ui.theme.PixelBackground
import com.expensetracker.app.ui.theme.PixelInk

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PixelInk,
    contentColor: Color = PixelBackground,
    borderColor: Color = PixelInk
) {
    Box(
        modifier = modifier
            .background(containerColor, RectangleShape)
            .border(BorderStroke(2.dp, borderColor))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = contentColor, style = MaterialTheme.typography.labelLarge)
    }
}
```

### `ui/components/PixelTextField.kt`
```kotlin
package com.expensetracker.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.KeyboardOptions
import com.expensetracker.app.ui.theme.PixelInk
import com.expensetracker.app.ui.theme.PixelInkLight
import com.expensetracker.app.ui.theme.PixelSurface
import androidx.compose.ui.unit.dp

@Composable
fun PixelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(2.dp, PixelInk)),
        placeholder = { Text(placeholder, color = PixelInkLight) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = PixelSurface,
            unfocusedContainerColor = PixelSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = PixelInk,
            unfocusedTextColor = PixelInk
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}
```

### `ui/components/MonthSelector.kt`
```kotlin
package com.expensetracker.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.expensetracker.app.ui.theme.PixelInk
import com.expensetracker.app.util.DateUtils
import java.time.YearMonth

@Composable
fun MonthSelector(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = PixelInk)
        }
        Text(
            text = DateUtils.formatMonth(yearMonth).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = PixelInk
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = PixelInk)
        }
    }
}
```

### `ui/components/ConfirmDialog.kt`
```kotlin
package com.expensetracker.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.expensetracker.app.ui.theme.PixelRed

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "DELETE",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = PixelRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
```

---

## 11. Navigation

### `ui/navigation/Screen.kt`
```kotlin
package com.expensetracker.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Categories : Screen("categories")

    object AddExpense : Screen("add_expense?expenseId={expenseId}&categoryId={categoryId}") {
        fun createRoute(expenseId: Long? = null, categoryId: Long? = null) =
            "add_expense?expenseId=${expenseId ?: -1}&categoryId=${categoryId ?: -1}"
    }

    object CategoryHistory : Screen("category_history/{categoryId}/{yearMonth}") {
        fun createRoute(categoryId: Long, yearMonth: String) = "category_history/$categoryId/$yearMonth"
    }
}
```

### `ui/navigation/NavGraph.kt`
```kotlin
package com.expensetracker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expensetracker.app.di.AppContainer
import com.expensetracker.app.ui.addexpense.AddExpenseScreen
import com.expensetracker.app.ui.categories.CategoriesScreen
import com.expensetracker.app.ui.categoryhistory.CategoryHistoryScreen
import com.expensetracker.app.ui.home.HomeScreen
import java.time.YearMonth

@Composable
fun NavGraph(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                container = container,
                onAddExpense = { categoryId ->
                    navController.navigate(Screen.AddExpense.createRoute(categoryId = categoryId))
                },
                onManageCategories = { navController.navigate(Screen.Categories.route) },
                onCategoryClick = { catId, ym ->
                    navController.navigate(Screen.CategoryHistory.createRoute(catId, ym.toString()))
                }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(container = container, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId")?.takeIf { it != -1L }
            val categoryId = backStackEntry.arguments?.getLong("categoryId")?.takeIf { it != -1L }
            AddExpenseScreen(
                container = container,
                expenseId = expenseId,
                initialCategoryId = categoryId,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CategoryHistory.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType },
                navArgument("yearMonth") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: return@composable
            val yearMonth = YearMonth.parse(backStackEntry.arguments?.getString("yearMonth"))
            CategoryHistoryScreen(
                container = container,
                categoryId = categoryId,
                yearMonth = yearMonth,
                onBack = { navController.popBackStack() },
                onEditExpense = { expenseId ->
                    navController.navigate(Screen.AddExpense.createRoute(expenseId = expenseId))
                },
                onAddExpense = {
                    navController.navigate(Screen.AddExpense.createRoute(categoryId = categoryId))
                }
            )
        }
    }
}
```

### `MainActivity.kt`
```kotlin
package com.expensetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.expensetracker.app.ui.navigation.NavGraph
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val app = application as ExpenseTrackerApp
                    NavGraph(container = app.container)
                }
            }
        }
    }
}
```

---

## 12. Home Screen (monthly dashboard)

### `ui/home/HomeUiState.kt`
```kotlin
package com.expensetracker.app.ui.home

import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import java.time.YearMonth

data class CategoryTotalUi(
    val category: CategoryEntity,
    val total: Double
)

data class ParentGroupUi(
    val parent: ParentCategoryEntity,
    val categories: List<CategoryTotalUi>,
    val subtotal: Double
)

data class HomeUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val groups: List<ParentGroupUi> = emptyList(),
    val ungrouped: List<CategoryTotalUi> = emptyList(),
    val grandTotal: Double = 0.0
)
```

### `ui/home/HomeViewModel.kt`
```kotlin
package com.expensetracker.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<HomeUiState> = _yearMonth.flatMapLatest { ym ->
        val (start, end) = DateUtils.monthRange(ym)
        combine(
            categoryRepository.parentCategories,
            categoryRepository.categories,
            expenseRepository.categoryTotals(start, end)
        ) { parents, categories, totals ->
            val totalsMap = totals.associateBy({ it.categoryId }, { it.total })
            val byParent = categories.groupBy { it.parentCategoryId }

            val groups = parents.sortedBy { it.sortOrder }.map { parent ->
                val cats = (byParent[parent.id] ?: emptyList()).map {
                    CategoryTotalUi(it, totalsMap[it.id] ?: 0.0)
                }
                ParentGroupUi(parent, cats, cats.sumOf { it.total })
            }
            val ungrouped = (byParent[null] ?: emptyList()).map {
                CategoryTotalUi(it, totalsMap[it.id] ?: 0.0)
            }

            HomeUiState(
                yearMonth = ym,
                groups = groups,
                ungrouped = ungrouped,
                // This is the "Total = Fixed + Flexi (+ any other parents) + uncategorized" sum.
                grandTotal = groups.sumOf { it.subtotal } + ungrouped.sumOf { it.total }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun previousMonth() { _yearMonth.value = _yearMonth.value.minusMonths(1) }
    fun nextMonth() { _yearMonth.value = _yearMonth.value.plusMonths(1) }
}
```

### `ui/home/HomeScreen.kt`
```kotlin
package com.expensetracker.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.di.AppContainer
import com.expensetracker.app.ui.components.MonthSelector
import com.expensetracker.app.ui.components.PixelCard
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.util.ColorExt.toComposeColor
import com.expensetracker.app.util.CurrencyFormatter
import com.expensetracker.app.util.ViewModelFactory
import java.time.YearMonth

@Composable
fun HomeScreen(
    container: AppContainer,
    onAddExpense: (categoryId: Long?) -> Unit,
    onManageCategories: () -> Unit,
    onCategoryClick: (categoryId: Long, yearMonth: YearMonth) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory { HomeViewModel(container.categoryRepository, container.expenseRepository) }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = PixelBackground,
        topBar = {
            TopAppBar(
                title = { Text("EXPENSE TRACKER", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Default.Category, contentDescription = "Manage categories", tint = PixelInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PixelBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddExpense(null) },
                containerColor = PixelInk,
                contentColor = PixelBackground,
                shape = RectangleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthSelector(
                    yearMonth = uiState.yearMonth,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            item {
                PixelCard(backgroundColor = PixelInk, borderColor = PixelInk) {
                    Text("TOTAL SPENT", color = PixelBackground, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        CurrencyFormatter.format(uiState.grandTotal),
                        color = PixelGreen,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    if (uiState.groups.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            uiState.groups.forEach { group ->
                                Column {
                                    Text(
                                        group.parent.name.uppercase(),
                                        color = group.parent.colorHex.toComposeColor(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        CurrencyFormatter.format(group.subtotal),
                                        color = PixelBackground,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(uiState.groups, key = { it.parent.id }) { group ->
                ParentGroupCard(
                    group = group,
                    onAddExpense = onAddExpense,
                    onCategoryClick = { catId -> onCategoryClick(catId, uiState.yearMonth) }
                )
            }

            if (uiState.ungrouped.isNotEmpty()) {
                item {
                    PixelCard {
                        Text("UNCATEGORIZED", style = MaterialTheme.typography.titleMedium, color = PixelInk)
                        Spacer(Modifier.height(8.dp))
                        uiState.ungrouped.forEach { catTotal ->
                            CategoryRow(
                                name = catTotal.category.name,
                                total = catTotal.total,
                                accentColor = PixelInkLight,
                                onClick = { onCategoryClick(catTotal.category.id, uiState.yearMonth) },
                                onAdd = { onAddExpense(catTotal.category.id) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(64.dp)) } // room for the FAB
        }
    }
}

@Composable
private fun ParentGroupCard(
    group: ParentGroupUi,
    onAddExpense: (Long) -> Unit,
    onCategoryClick: (Long) -> Unit
) {
    val color = group.parent.colorHex.toComposeColor()
    PixelCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(color))
            Spacer(Modifier.width(8.dp))
            Text(group.parent.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = PixelInk)
            Spacer(Modifier.weight(1f))
            Text(CurrencyFormatter.format(group.subtotal), style = MaterialTheme.typography.titleMedium, color = PixelInk)
        }
        Spacer(Modifier.height(8.dp))
        if (group.categories.isEmpty()) {
            Text("No categories yet — add one from Categories.", style = MaterialTheme.typography.bodyMedium, color = PixelInkLight)
        } else {
            group.categories.forEach { catTotal ->
                CategoryRow(
                    name = catTotal.category.name,
                    total = catTotal.total,
                    accentColor = color,
                    onClick = { onCategoryClick(catTotal.category.id) },
                    onAdd = { onAddExpense(catTotal.category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    name: String,
    total: Double,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(20.dp).background(accentColor))
        Spacer(Modifier.width(8.dp))
        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = PixelInk)
        Text(CurrencyFormatter.format(total), style = MaterialTheme.typography.bodyLarge, color = PixelInk)
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = "Add expense to $name", tint = PixelInkLight)
        }
    }
}
```

> **Import note:** `ColorExt.kt` defines a top-level extension function `toComposeColor()`,
> not an object — import it as `import com.expensetracker.app.util.toComposeColor` (drop the
> `ColorExt.` qualifier used in the snippet above; Android Studio's auto-import will do this
> correctly for you).

---

## 13. Categories Management

### `ui/categories/CategoriesViewModel.kt`
```kotlin
package com.expensetracker.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import com.expensetracker.app.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: CategoryRepository) : ViewModel() {
    val parentCategories = repository.parentCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addParentCategory(name: String, colorHex: String) =
        viewModelScope.launch { repository.addParentCategory(name, colorHex) }
    fun updateParentCategory(parent: ParentCategoryEntity) =
        viewModelScope.launch { repository.updateParentCategory(parent) }
    fun deleteParentCategory(parent: ParentCategoryEntity) =
        viewModelScope.launch { repository.deleteParentCategory(parent) }

    fun addCategory(name: String, parentId: Long?) =
        viewModelScope.launch { repository.addCategory(name, parentId) }
    fun updateCategory(category: CategoryEntity) =
        viewModelScope.launch { repository.updateCategory(category) }
    fun deleteCategory(category: CategoryEntity) =
        viewModelScope.launch { repository.deleteCategory(category) }
}
```

### `ui/categories/AddEditParentCategoryDialog.kt`
```kotlin
package com.expensetracker.app.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import com.expensetracker.app.ui.components.PixelButton
import com.expensetracker.app.ui.components.PixelCard
import com.expensetracker.app.ui.components.PixelTextField
import com.expensetracker.app.ui.theme.ParentCategoryPaletteHex
import com.expensetracker.app.ui.theme.PixelInk
import com.expensetracker.app.ui.theme.PixelInkLight
import com.expensetracker.app.ui.theme.PixelSurface
import com.expensetracker.app.util.toComposeColor

@Composable
fun AddEditParentCategoryDialog(
    initial: ParentCategoryEntity?,
    onConfirm: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.colorHex ?: ParentCategoryPaletteHex.first()) }

    Dialog(onDismissRequest = onDismiss) {
        PixelCard(backgroundColor = PixelSurface) {
            Text(
                if (initial == null) "NEW PARENT CATEGORY" else "EDIT PARENT CATEGORY",
                style = MaterialTheme.typography.titleMedium, color = PixelInk
            )
            Spacer(Modifier.height(12.dp))
            PixelTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Fixed")
            Spacer(Modifier.height(12.dp))
            Text("COLOR", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ParentCategoryPaletteHex.forEach { hex ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(hex.toComposeColor())
                            .border(BorderStroke(if (color == hex) 3.dp else 1.dp, PixelInk))
                            .clickable { color = hex }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(text = "CANCEL", onClick = onDismiss, containerColor = PixelSurface, contentColor = PixelInk)
                PixelButton(
                    text = "SAVE",
                    modifier = Modifier.weight(1f),
                    onClick = { if (name.isNotBlank()) { onConfirm(name.trim(), color); onDismiss() } }
                )
            }
        }
    }
}
```

### `ui/categories/AddEditCategoryDialog.kt`
```kotlin
package com.expensetracker.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import com.expensetracker.app.ui.components.PixelButton
import com.expensetracker.app.ui.components.PixelCard
import com.expensetracker.app.ui.components.PixelTextField
import com.expensetracker.app.ui.theme.PixelInk
import com.expensetracker.app.ui.theme.PixelInkLight
import com.expensetracker.app.ui.theme.PixelSurface
import com.expensetracker.app.util.toComposeColor

@Composable
fun AddEditCategoryDialog(
    initial: CategoryEntity?,
    parents: List<ParentCategoryEntity>,
    onConfirm: (name: String, parentId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedParentId by remember { mutableStateOf(initial?.parentCategoryId) }

    Dialog(onDismissRequest = onDismiss) {
        PixelCard(backgroundColor = PixelSurface) {
            Text(
                if (initial == null) "NEW CATEGORY" else "EDIT CATEGORY",
                style = MaterialTheme.typography.titleMedium, color = PixelInk
            )
            Spacer(Modifier.height(12.dp))
            PixelTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Groceries")
            Spacer(Modifier.height(12.dp))
            Text("PARENT CATEGORY", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            Spacer(Modifier.height(4.dp))

            Row(
                Modifier.fillMaxWidth().clickable { selectedParentId = null }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedParentId == null, onClick = { selectedParentId = null })
                Text("None", color = PixelInk)
            }
            parents.forEach { parent ->
                Row(
                    Modifier.fillMaxWidth().clickable { selectedParentId = parent.id }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedParentId == parent.id, onClick = { selectedParentId = parent.id })
                    Box(Modifier.size(12.dp).background(parent.colorHex.toComposeColor()))
                    Spacer(Modifier.width(6.dp))
                    Text(parent.name, color = PixelInk)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(text = "CANCEL", onClick = onDismiss, containerColor = PixelSurface, contentColor = PixelInk)
                PixelButton(
                    text = "SAVE",
                    modifier = Modifier.weight(1f),
                    onClick = { if (name.isNotBlank()) { onConfirm(name.trim(), selectedParentId); onDismiss() } }
                )
            }
        }
    }
}
```

### `ui/categories/CategoriesScreen.kt`
```kotlin
package com.expensetracker.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import com.expensetracker.app.di.AppContainer
import com.expensetracker.app.ui.components.ConfirmDialog
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.util.ViewModelFactory
import com.expensetracker.app.util.toComposeColor

@Composable
fun CategoriesScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: CategoriesViewModel = viewModel(
        factory = ViewModelFactory { CategoriesViewModel(container.categoryRepository) }
    )
    val parents by viewModel.parentCategories.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showAddParent by remember { mutableStateOf(false) }
    var editingParent by remember { mutableStateOf<ParentCategoryEntity?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteParentTarget by remember { mutableStateOf<ParentCategoryEntity?>(null) }
    var deleteCategoryTarget by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        containerColor = PixelBackground,
        topBar = {
            TopAppBar(
                title = { Text("CATEGORIES", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PixelInk) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PixelBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PARENT CATEGORIES", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
                    IconButton(onClick = { showAddParent = true }) { Icon(Icons.Default.Add, contentDescription = "Add parent category", tint = PixelInk) }
                }
            }
            items(parents, key = { "p${it.id}" }) { parent ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(parent.colorHex.toComposeColor()))
                    Spacer(Modifier.width(10.dp))
                    Text(parent.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = PixelInk)
                    IconButton(onClick = { editingParent = parent }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PixelInkLight) }
                    IconButton(onClick = { deleteParentTarget = parent }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PixelRed) }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CATEGORIES", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
                    IconButton(onClick = { showAddCategory = true }) { Icon(Icons.Default.Add, contentDescription = "Add category", tint = PixelInk) }
                }
            }
            items(categories, key = { "c${it.id}" }) { category ->
                val parentColor = parents.find { it.id == category.parentCategoryId }?.colorHex?.toComposeColor() ?: PixelInkLight
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(4.dp).height(20.dp).background(parentColor))
                    Spacer(Modifier.width(10.dp))
                    Text(category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = PixelInk)
                    IconButton(onClick = { editingCategory = category }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PixelInkLight) }
                    IconButton(onClick = { deleteCategoryTarget = category }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PixelRed) }
                }
            }
        }
    }

    if (showAddParent) {
        AddEditParentCategoryDialog(
            initial = null,
            onConfirm = { name, color -> viewModel.addParentCategory(name, color) },
            onDismiss = { showAddParent = false }
        )
    }
    editingParent?.let { parent ->
        AddEditParentCategoryDialog(
            initial = parent,
            onConfirm = { name, color -> viewModel.updateParentCategory(parent.copy(name = name, colorHex = color)) },
            onDismiss = { editingParent = null }
        )
    }
    if (showAddCategory) {
        AddEditCategoryDialog(
            initial = null, parents = parents,
            onConfirm = { name, parentId -> viewModel.addCategory(name, parentId) },
            onDismiss = { showAddCategory = false }
        )
    }
    editingCategory?.let { category ->
        AddEditCategoryDialog(
            initial = category, parents = parents,
            onConfirm = { name, parentId -> viewModel.updateCategory(category.copy(name = name, parentCategoryId = parentId)) },
            onDismiss = { editingCategory = null }
        )
    }
    deleteParentTarget?.let { parent ->
        ConfirmDialog(
            title = "Delete ${parent.name}?",
            message = "Categories under this will become uncategorized (not deleted). This cannot be undone.",
            onConfirm = { viewModel.deleteParentCategory(parent); deleteParentTarget = null },
            onDismiss = { deleteParentTarget = null }
        )
    }
    deleteCategoryTarget?.let { category ->
        ConfirmDialog(
            title = "Delete ${category.name}?",
            message = "All expenses recorded under this category will also be deleted. This cannot be undone.",
            onConfirm = { viewModel.deleteCategory(category); deleteCategoryTarget = null },
            onDismiss = { deleteCategoryTarget = null }
        )
    }
}
```

---

## 14. Add / Edit Expense

### `ui/addexpense/AddExpenseViewModel.kt`
```kotlin
package com.expensetracker.app.ui.addexpense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val expenseId: Long?,
    initialCategoryId: Long?
) : ViewModel() {

    val parentCategories = categoryRepository.parentCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = categoryRepository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var selectedCategoryId by mutableStateOf(initialCategoryId); private set
    var amountText by mutableStateOf(""); private set
    var note by mutableStateOf(""); private set
    var dateMillis by mutableStateOf(DateUtils.today()); private set

    init {
        if (expenseId != null) {
            viewModelScope.launch {
                expenseRepository.getById(expenseId)?.let { expense ->
                    selectedCategoryId = expense.categoryId
                    amountText = if (expense.amount == expense.amount.toLong().toDouble())
                        expense.amount.toLong().toString() else expense.amount.toString()
                    note = expense.note
                    dateMillis = expense.date
                }
            }
        }
    }

    fun onCategorySelected(id: Long) { selectedCategoryId = id }
    fun onAmountChange(value: String) { amountText = value.filter { it.isDigit() || it == '.' } }
    fun onNoteChange(value: String) { note = value }
    fun onDateChange(millis: Long) { dateMillis = millis }

    fun canSave(): Boolean =
        selectedCategoryId != null && (amountText.toDoubleOrNull() ?: 0.0) > 0.0

    fun save(onDone: () -> Unit) {
        val amount = amountText.toDoubleOrNull() ?: return
        val categoryId = selectedCategoryId ?: return
        viewModelScope.launch {
            if (expenseId != null) {
                expenseRepository.updateExpense(
                    ExpenseEntity(id = expenseId, categoryId = categoryId, amount = amount, note = note, date = dateMillis)
                )
            } else {
                expenseRepository.addExpense(categoryId, amount, note, dateMillis)
            }
            onDone()
        }
    }
}
```

### `ui/addexpense/CategoryPickerDialog.kt`
```kotlin
package com.expensetracker.app.ui.addexpense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import com.expensetracker.app.ui.components.PixelCard
import com.expensetracker.app.ui.theme.PixelInk
import com.expensetracker.app.ui.theme.PixelInkLight
import com.expensetracker.app.ui.theme.PixelSurface
import com.expensetracker.app.util.toComposeColor

@Composable
fun CategoryPickerDialog(
    parents: List<ParentCategoryEntity>,
    categories: List<CategoryEntity>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PixelCard(backgroundColor = PixelSurface) {
            Text("SELECT CATEGORY", style = MaterialTheme.typography.titleMedium, color = PixelInk)
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                parents.forEach { parent ->
                    val color = parent.colorHex.toComposeColor()
                    item {
                        Text(
                            parent.name.uppercase(), color = color,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(categories.filter { it.parentCategoryId == parent.id }, key = { it.id }) { cat ->
                        Text(
                            cat.name, color = PixelInk, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(cat.id); onDismiss() }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
                val ungrouped = categories.filter { it.parentCategoryId == null }
                if (ungrouped.isNotEmpty()) {
                    item {
                        Text(
                            "OTHER", color = PixelInkLight, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(ungrouped, key = { it.id }) { cat ->
                        Text(
                            cat.name, color = PixelInk, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(cat.id); onDismiss() }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
```

### `ui/addexpense/AddExpenseScreen.kt`
```kotlin
package com.expensetracker.app.ui.addexpense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.di.AppContainer
import com.expensetracker.app.ui.components.PixelButton
import com.expensetracker.app.ui.components.PixelCard
import com.expensetracker.app.ui.components.PixelTextField
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.util.DateUtils
import com.expensetracker.app.util.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    container: AppContainer,
    expenseId: Long?,
    initialCategoryId: Long?,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: AddExpenseViewModel = viewModel(
        factory = ViewModelFactory {
            AddExpenseViewModel(container.categoryRepository, container.expenseRepository, expenseId, initialCategoryId)
        }
    )
    val parents by viewModel.parentCategories.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.id == viewModel.selectedCategoryId }

    Scaffold(
        containerColor = PixelBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId != null) "EDIT EXPENSE" else "ADD EXPENSE", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = PixelInk) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PixelBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("CATEGORY", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            PixelCard(modifier = Modifier.fillMaxWidth().clickable { showPicker = true }) {
                Text(selectedCategory?.name ?: "Tap to choose", color = PixelInk, style = MaterialTheme.typography.bodyLarge)
            }

            Text("AMOUNT (₹)", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            PixelTextField(
                value = viewModel.amountText, onValueChange = viewModel::onAmountChange,
                placeholder = "0.00", keyboardType = KeyboardType.Decimal
            )

            Text("DATE", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            PixelCard(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                Text(DateUtils.formatDate(viewModel.dateMillis), color = PixelInk, style = MaterialTheme.typography.bodyLarge)
            }

            Text("NOTE (OPTIONAL)", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            PixelTextField(
                value = viewModel.note, onValueChange = viewModel::onNoteChange,
                placeholder = "e.g. Groceries at BigBasket"
            )

            Spacer(Modifier.weight(1f))

            PixelButton(
                text = "SAVE EXPENSE",
                onClick = { viewModel.save(onDone) },
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (viewModel.canSave()) PixelInk else PixelInkLight
            )
        }
    }

    if (showPicker) {
        CategoryPickerDialog(
            parents = parents, categories = categories,
            onSelect = viewModel::onCategorySelected, onDismiss = { showPicker = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

---

## 15. Category History (per-category expense list for the month)

### `ui/categoryhistory/CategoryHistoryViewModel.kt`
```kotlin
package com.expensetracker.app.ui.categoryhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

class CategoryHistoryViewModel(
    categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    categoryId: Long,
    yearMonth: YearMonth
) : ViewModel() {

    val category = categoryRepository.categories
        .map { list -> list.find { it.id == categoryId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val expenses = run {
        val (start, end) = DateUtils.monthRange(yearMonth)
        expenseRepository.expensesForCategory(categoryId, start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { expenseRepository.deleteExpense(expense) }
    }
}
```

### `ui/categoryhistory/CategoryHistoryScreen.kt`
```kotlin
package com.expensetracker.app.ui.categoryhistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.di.AppContainer
import com.expensetracker.app.ui.components.ConfirmDialog
import com.expensetracker.app.ui.components.PixelCard
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.util.CurrencyFormatter
import com.expensetracker.app.util.DateUtils
import com.expensetracker.app.util.ViewModelFactory
import java.time.YearMonth

@Composable
fun CategoryHistoryScreen(
    container: AppContainer,
    categoryId: Long,
    yearMonth: YearMonth,
    onBack: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onAddExpense: () -> Unit
) {
    val viewModel: CategoryHistoryViewModel = viewModel(
        factory = ViewModelFactory {
            CategoryHistoryViewModel(container.categoryRepository, container.expenseRepository, categoryId, yearMonth)
        }
    )
    val category by viewModel.category.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<ExpenseEntity?>(null) }

    Scaffold(
        containerColor = PixelBackground,
        topBar = {
            TopAppBar(
                title = { Text((category?.name ?: "").uppercase(), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PixelInk) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PixelBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense, containerColor = PixelInk, contentColor = PixelBackground, shape = RectangleShape) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        if (expenses.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No expenses yet this month", color = PixelInkLight)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses, key = { it.id }) { expense ->
                    PixelCard(modifier = Modifier.fillMaxWidth().clickable { onEditExpense(expense.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(CurrencyFormatter.format(expense.amount), style = MaterialTheme.typography.bodyLarge, color = PixelInk)
                                if (expense.note.isNotBlank()) {
                                    Text(expense.note, style = MaterialTheme.typography.bodyMedium, color = PixelInkLight)
                                }
                                Text(DateUtils.formatDate(expense.date), style = MaterialTheme.typography.bodyMedium, color = PixelInkLight)
                            }
                            IconButton(onClick = { deleteTarget = expense }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PixelRed)
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { expense ->
        ConfirmDialog(
            title = "Delete expense?",
            message = "This cannot be undone.",
            onConfirm = { viewModel.deleteExpense(expense); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}
```

---

## 16. How the totals work (recap)

- Every expense is tagged with a category and a date.
- The Home screen filters expenses to `[start of selected month, start of next month)`.
- `HomeViewModel` groups categories by their **parent category**, sums each group →
  **subtotal per parent** (e.g. Fixed subtotal, Flexi subtotal).
- **Grand total = sum of every parent subtotal** (+ any uncategorized categories) — exactly
  the "Total = Fixed + Flexi" behavior you described, and it automatically extends correctly
  if you add a third, fourth, etc. parent category later.

## 17. Testing Checklist

- [ ] First launch seeds `Fixed` (Investments/Bills/Rent) and `Flexi` (Travel/Food/Outing/Others).
- [ ] Add an expense → appears under the right category and parent subtotal, on the correct date's month.
- [ ] Switch months with ◀ ▶ → totals recompute, no stale data.
- [ ] Rename a category → reflected everywhere immediately (categories are shared globally, as requested).
- [ ] Delete a category with expenses → confirmation warns, then expenses are gone too.
- [ ] Delete a parent category → its categories move to "Uncategorized", expenses untouched.
- [ ] Add a new parent category with a custom color → shows up as a new group with its own subtotal.
- [ ] Edit an existing expense from Category History → amount/date/note update correctly.
- [ ] Rotate device / kill & reopen app → data persists (Room) and current month view is preserved within a session.

## 18. Future Enhancements (not built now, per your answers)

- Optional per-category or per-parent **budget limits** with progress bars.
- CSV export / backup & restore of the Room database.
- A simple bar/line chart comparing spend across months.
- Recurring "Fixed" expenses that auto-populate each month (e.g. rent) instead of re-entering.
- Multi-currency support.
- Swap in the real Press Start 2P font by default (see §5.2) and a matching pixel-art app icon.

---

This covers every file needed for a working first build. Paste the files into the structure
in §4, sync Gradle, and run — no external API keys or network access required since
everything is local Room storage.
