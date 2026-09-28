# Onboarding + Google Sync + Account Settings — Implementation Spec

This builds on `EXPENSE_TRACKER_APP_SPEC.md`. It adds:

1. An onboarding flow: **"Welcome to Where's My Money"** → name entry → optional Google sync.
2. Cross-device sync using **Firebase Authentication (Google Sign-In) + Cloud Firestore**,
   with Room staying as your fast local/offline cache.
3. An **Account** section to drop into your existing Settings screen: shows signed-in
   account, Sign Out, and Delete Account.

## 0. Is Google-account sync possible? — Yes

The standard, well-supported way to do this on Android is:
- **Firebase Authentication** with the **Google** provider (sign-in), using the modern
  **Credential Manager API** (Google's current recommended replacement for the older
  `GoogleSignInClient`/One Tap APIs).
- **Cloud Firestore** as the cloud database, which has built-in offline persistence and
  automatic retry — so most of the "queue writes until back online" logic you'd expect to
  hand-roll comes for free.
- It's free for personal-scale use: Firestore's free tier is 50K reads / 20K writes / 20K
  deletes per day, and Firebase Auth has no per-user cost for this sign-in method.

The trade-off is that it requires some **one-time setup in the Firebase/Google Cloud
console** (creating a project, enabling the sign-in provider, registering your app's
signing fingerprint) that only you can do, since it's tied to your Google account and your
app's signing key. §2 below walks through it step by step.

## 1. Assumptions Made (flag me if you want these different)

- **Sign-in is optional**, offered during onboarding and always available later from
  Settings — not required to use the app.
- **Firebase/Firestore** chosen as the sync backend (the standard pairing with Google
  Sign-In; a custom backend would be a much bigger lift for no real benefit here).
- Conflict resolution is simple **last-write-wins** (whichever device's write reaches
  Firestore last, wins) — appropriate for one person using the app on a couple of devices,
  not designed for true multi-user collaboration.
- **Deleting the account** removes your cloud backup and Google sign-in link, but leaves
  your on-device data alone (§13 shows the one-line change if you want it to wipe local
  data too).
- Existing entity **primary keys change** from Room's auto-increment to a manually
  generated unique ID (§4) — required so two devices can each create records offline
  without ID collisions once they sync. This needs a small Room migration.

## 2. External Setup (Firebase & Google Cloud console — do this first)

1. Go to [console.firebase.google.com](https://console.firebase.google.com) → **Add
   project**.
2. **Add an Android app** inside that project. Package name must exactly match your
   `applicationId`: `com.expensetracker.app`.
3. Download the generated **`google-services.json`** and place it in `app/` (next to
   `app/build.gradle.kts`).
4. **Authentication → Sign-in method → Google → Enable.** This auto-creates a **Web client
   ID** — copy it now, you'll paste it into `AuthRepository.kt` in §9.
5. Get your debug signing fingerprint: run `./gradlew signingReport` from the project root
   and copy the `SHA1` value under the `debug` variant. In **Project settings → your
   Android app → Add fingerprint**, paste it in. (Repeat this with your **release**
   keystore's SHA-1 before you publish — sign-in will fail on release builds without it.)
6. **Firestore Database → Create database** → start in production mode → pick a region
   close to you.
7. **Firestore → Rules**, replace the default with the rules in §14, then **Publish**.
   Skipping this step leaves your data either fully locked or, worse, fully open —
   don't skip it.

## 3. Gradle Changes

### Root `build.gradle.kts` — add the Google Services plugin
```kotlin
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

### `app/build.gradle.kts` — apply the plugin
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}
```

### `app/build.gradle.kts` — add to `dependencies { ... }`
```kotlin
    // Firebase (Auth + Firestore)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Google Sign-In via Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Local "what's your name / has onboarding run" storage
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Splash screen — holds the first frame until we know whether to show onboarding
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Loads the Google account's profile photo in Settings
    implementation("io.coil-kt:coil-compose:2.7.0")
```

> **Emulator note:** testing Google Sign-In needs an emulator image with **Google Play**
> (not just "Google APIs"), signed into a Google account — or just use a physical device.

---

## 4. Room Changes — unique IDs + `updatedAt` for sync

### Why the ID scheme has to change
The original spec used `@PrimaryKey(autoGenerate = true)`, where SQLite assigns the next
free integer per-device. If your phone and tablet each create a new category while offline,
they could both generate `id = 7` — and when both sync to Firestore, one silently overwrites
the other. The fix: generate IDs client-side in a way that's unique across devices.

### New file: `util/IdGenerator.kt`
```kotlin
package com.expensetracker.app.util

import kotlin.random.Random

/**
 * Millisecond timestamp + a random component. Two devices creating a record in the exact
 * same millisecond would need to also pick the same 0–99999 random number to collide —
 * more than safe enough for personal, few-device use.
 */
object IdGenerator {
    fun newId(): Long = System.currentTimeMillis() * 100_000L + Random.nextInt(100_000)
}
```

### Updated entities — add `updatedAt`, drop `autoGenerate`

`data/local/entity/ParentCategoryEntity.kt`:
```kotlin
package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_categories")
data class ParentCategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

`data/local/entity/CategoryEntity.kt` — same two changes (`@PrimaryKey val id: Long`, add `updatedAt`):
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
    @PrimaryKey val id: Long,
    val name: String,
    val parentCategoryId: Long?,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

`data/local/entity/ExpenseEntity.kt` — same:
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
    @PrimaryKey val id: Long,
    val categoryId: Long,
    val amount: Double,
    val note: String = "",
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### Migration — `data/local/ExpenseDatabase.kt` (full updated file)
```kotlin
package com.expensetracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.dao.ParentCategoryDao
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity

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
```
> If you don't care about keeping the data you've already entered while testing, you can
> use `.fallbackToDestructiveMigration()` instead of `.addMigrations(MIGRATION_1_2)` — it
> just wipes and recreates the local DB, which is fine pre-release but **not** once you or
> anyone else has real data in the app.

### Updated DAOs — add `getAllOnce`, `upsert`, `deleteById`; `insert` returns `Unit`

`data/local/dao/ParentCategoryDao.kt`:
```kotlin
package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
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
```

`data/local/dao/CategoryDao.kt`:
```kotlin
package com.expensetracker.app.data.local.dao

import androidx.room.*
import com.expensetracker.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllOnce(): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
```

`data/local/dao/ExpenseDao.kt` — add the same three methods (`getAllOnce`, `upsert`,
`deleteById`) alongside the existing queries from the original spec:
```kotlin
    @Query("SELECT * FROM expenses")
    suspend fun getAllOnce(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
```

---

## 5. DataStore — remembering the name & onboarding status

### New file: `data/datastore/UserPreferencesRepository.kt`
```kotlin
package com.expensetracker.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val userName: Flow<String?> = context.dataStore.data.map { it[Keys.USER_NAME] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
```

---

## 6. Auth — Google Sign-In via Credential Manager + Firebase

### New file: `data/repository/AuthRepository.kt`
```kotlin
package com.expensetracker.app.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * From Firebase console: Authentication -> Sign-in method -> Google -> "Web SDK
 * configuration" shows this Web client ID. Paste it in here.
 */
private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = Firebase.auth

    val currentUser: FirebaseUser? get() = auth.currentUser
    fun currentUid(): String? = auth.currentUser?.uid

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(): Result<FirebaseUser> = runCatching {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        val result = credentialManager.getCredential(context, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

        auth.signInWithCredential(firebaseCredential).await().user
            ?: throw IllegalStateException("Sign-in succeeded but returned no user")
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Deletes the user's Firestore data and their Firebase Auth account.
     * Firebase requires a *recent* sign-in for account deletion — if this throws
     * FirebaseAuthRecentLoginRequiredException, prompt the user to sign in again and retry.
     */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user")
        val uid = user.uid
        val firestore = Firebase.firestore
        val collections = listOf("parentCategories", "categories", "expenses")
        for (collection in collections) {
            val docs = firestore.collection("users").document(uid).collection(collection).get().await()
            for (doc in docs.documents) doc.reference.delete().await()
        }
        firestore.collection("users").document(uid).delete().await()
        user.delete().await()
    }
}
```

---

## 7. Firestore Sync

### New file: `data/remote/FirestoreDtos.kt`
```kotlin
package com.expensetracker.app.data.remote

import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity

// Firestore's POJO mapping needs a default value for every property.

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
```

### New file: `data/remote/FirestoreSyncManager.kt`
```kotlin
package com.expensetracker.app.data.remote

import com.expensetracker.app.data.local.ExpenseDatabase
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Push: repositories call push*() right after every local Room write (only if signed in).
 * Firestore's SDK queues writes locally and retries automatically when back online, so
 * there's no hand-rolled offline queue here.
 *
 * Pull: start(uid) attaches a real-time listener per collection. Any change made from any
 * device (including this one, harmlessly) comes back through the listener and is applied
 * to Room directly via the DAOs — not through the repositories — so it doesn't re-trigger
 * another push and loop.
 */
class FirestoreSyncManager(private val database: ExpenseDatabase) {
    private val firestore = Firebase.firestore
    private var listeners: List<ListenerRegistration> = emptyList()
    private var scope: CoroutineScope? = null

    fun start(uid: String) {
        stop()
        val activeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = activeScope
        val userDoc = firestore.collection("users").document(uid)

        // One-time bootstrap: make sure anything created locally before sign-in gets uploaded.
        activeScope.launch { pushAllLocalData(uid) }

        val parentReg = userDoc.collection("parentCategories").addSnapshotListener { snapshot, _ ->
            val snap = snapshot ?: return@addSnapshotListener
            activeScope.launch {
                for (change in snap.documentChanges) {
                    applyParentCategory(change.document.toObject(ParentCategoryDto::class.java))
                }
            }
        }
        val categoryReg = userDoc.collection("categories").addSnapshotListener { snapshot, _ ->
            val snap = snapshot ?: return@addSnapshotListener
            activeScope.launch {
                for (change in snap.documentChanges) {
                    applyCategory(change.document.toObject(CategoryDto::class.java))
                }
            }
        }
        val expenseReg = userDoc.collection("expenses").addSnapshotListener { snapshot, _ ->
            val snap = snapshot ?: return@addSnapshotListener
            activeScope.launch {
                for (change in snap.documentChanges) {
                    applyExpense(change.document.toObject(ExpenseDto::class.java))
                }
            }
        }
        listeners = listOf(parentReg, categoryReg, expenseReg)
    }

    fun stop() {
        listeners.forEach { it.remove() }
        listeners = emptyList()
        scope?.cancel()
        scope = null
    }

    // ---- push local -> remote ----

    fun pushParentCategory(uid: String, entity: ParentCategoryEntity) {
        docFor(uid, "parentCategories", entity.id).set(entity.toDto())
    }
    fun pushCategory(uid: String, entity: CategoryEntity) {
        docFor(uid, "categories", entity.id).set(entity.toDto())
    }
    fun pushExpense(uid: String, entity: ExpenseEntity) {
        docFor(uid, "expenses", entity.id).set(entity.toDto())
    }

    fun pushParentCategoryDeleted(uid: String, id: Long) = pushTombstone(uid, "parentCategories", id)
    fun pushCategoryDeleted(uid: String, id: Long) = pushTombstone(uid, "categories", id)
    fun pushExpenseDeleted(uid: String, id: Long) = pushTombstone(uid, "expenses", id)

    private fun pushTombstone(uid: String, collection: String, id: Long) {
        // We mark-as-deleted rather than actually removing the doc, so other devices'
        // listeners reliably observe the delete as a document change.
        docFor(uid, collection, id).set(
            mapOf("deleted" to true, "updatedAt" to System.currentTimeMillis()),
            SetOptions.merge()
        )
    }

    private fun docFor(uid: String, collection: String, id: Long) =
        firestore.collection("users").document(uid).collection(collection).document(id.toString())

    // ---- pull remote -> local ----

    private suspend fun applyParentCategory(dto: ParentCategoryDto?) {
        dto ?: return
        val dao = database.parentCategoryDao()
        if (dto.deleted) dao.deleteById(dto.id) else dao.upsert(dto.toEntity())
    }
    private suspend fun applyCategory(dto: CategoryDto?) {
        dto ?: return
        val dao = database.categoryDao()
        if (dto.deleted) dao.deleteById(dto.id) else dao.upsert(dto.toEntity())
    }
    private suspend fun applyExpense(dto: ExpenseDto?) {
        dto ?: return
        val dao = database.expenseDao()
        if (dto.deleted) dao.deleteById(dto.id) else dao.upsert(dto.toEntity())
    }

    private suspend fun pushAllLocalData(uid: String) {
        database.parentCategoryDao().getAllOnce().forEach { pushParentCategory(uid, it) }
        database.categoryDao().getAllOnce().forEach { pushCategory(uid, it) }
        database.expenseDao().getAllOnce().forEach { pushExpense(uid, it) }
    }
}
```

### Updated `data/repository/CategoryRepository.kt` (full file)
```kotlin
package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.ParentCategoryDao
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.ParentCategoryEntity
import com.expensetracker.app.data.remote.FirestoreSyncManager
import com.expensetracker.app.util.IdGenerator
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
        listOf("Investments", "Bills", "Rent").forEachIndexed { i, n ->
            categoryDao.insert(CategoryEntity(id = IdGenerator.newId(), name = n, parentCategoryId = fixed.id, sortOrder = i))
        }
        listOf("Travel", "Food", "Outing", "Others").forEachIndexed { i, n ->
            categoryDao.insert(CategoryEntity(id = IdGenerator.newId(), name = n, parentCategoryId = flexi.id, sortOrder = i))
        }
    }
}
```

### Updated `data/repository/ExpenseRepository.kt` (full file)
```kotlin
package com.expensetracker.app.data.repository

import com.expensetracker.app.data.local.dao.ExpenseDao
import com.expensetracker.app.data.local.entity.ExpenseEntity
import com.expensetracker.app.data.remote.FirestoreSyncManager
import com.expensetracker.app.util.IdGenerator

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
```

### Updated `di/AppContainer.kt` (full file)
```kotlin
package com.expensetracker.app.di

import android.content.Context
import com.expensetracker.app.data.datastore.UserPreferencesRepository
import com.expensetracker.app.data.local.ExpenseDatabase
import com.expensetracker.app.data.remote.FirestoreSyncManager
import com.expensetracker.app.data.repository.AuthRepository
import com.expensetracker.app.data.repository.CategoryRepository
import com.expensetracker.app.data.repository.ExpenseRepository

class AppContainer(context: Context) {
    private val database = ExpenseDatabase.getInstance(context)

    val userPreferencesRepository = UserPreferencesRepository(context)
    val authRepository = AuthRepository(context)
    val syncManager = FirestoreSyncManager(database)

    val categoryRepository = CategoryRepository(database.parentCategoryDao(), database.categoryDao(), authRepository, syncManager)
    val expenseRepository = ExpenseRepository(database.expenseDao(), authRepository, syncManager)
}
```

### Updated `ExpenseTrackerApp.kt` (full file)
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

        // Starts/stops the Firestore listeners whenever sign-in state changes.
        CoroutineScope(Dispatchers.IO).launch {
            container.authRepository.authState.collect { user ->
                if (user != null) container.syncManager.start(user.uid) else container.syncManager.stop()
            }
        }
    }
}
```

---

## 8. Onboarding Screens

### New file: `ui/onboarding/OnboardingViewModel.kt`
```kotlin
package com.expensetracker.app.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.datastore.UserPreferencesRepository
import com.expensetracker.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val prefsRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    var name by mutableStateOf(""); private set
    var isSigningIn by mutableStateOf(false); private set
    var signInError by mutableStateOf<String?>(null); private set

    fun onNameChange(value: String) { name = value }
    fun canContinue() = name.isNotBlank()

    fun saveName(onDone: () -> Unit) {
        if (!canContinue()) return
        viewModelScope.launch {
            prefsRepository.setUserName(name.trim())
            onDone()
        }
    }

    fun signInWithGoogle(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSigningIn = true
            signInError = null
            authRepository.signInWithGoogle()
                .onSuccess { onSuccess() }
                .onFailure { signInError = it.message ?: "Sign-in failed. Try again." }
            isSigningIn = false
        }
    }

    fun finishOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            prefsRepository.setOnboardingDone()
            onDone()
        }
    }
}
```

### New file: `ui/onboarding/OnboardingNameScreen.kt`
```kotlin
package com.expensetracker.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.di.AppContainer
import com.expensetracker.app.ui.components.PixelButton
import com.expensetracker.app.ui.components.PixelTextField
import com.expensetracker.app.ui.theme.PixelBackground
import com.expensetracker.app.ui.theme.PixelInk
import com.expensetracker.app.ui.theme.PixelInkLight
import com.expensetracker.app.util.ViewModelFactory

@Composable
fun OnboardingNameScreen(container: AppContainer, onContinue: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = ViewModelFactory { OnboardingViewModel(container.userPreferencesRepository, container.authRepository) }
    )
    Scaffold(containerColor = PixelBackground) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("WELCOME TO", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
            Text("WHERE'S MY MONEY", style = MaterialTheme.typography.headlineLarge, color = PixelInk)
            Spacer(Modifier.height(32.dp))
            Text("What should we call you?", style = MaterialTheme.typography.bodyLarge, color = PixelInk)
            Spacer(Modifier.height(8.dp))
            PixelTextField(value = viewModel.name, onValueChange = viewModel::onNameChange, placeholder = "Your name")
            Spacer(Modifier.height(24.dp))
            PixelButton(
                text = "CONTINUE",
                onClick = { viewModel.saveName(onContinue) },
                modifier = Modifier.fillMaxWidth(),
                containerColor = if (viewModel.canContinue()) PixelInk else PixelInkLight
            )
        }
    }
}
```

### New file: `ui/onboarding/OnboardingSyncScreen.kt`
```kotlin
package com.expensetracker.app.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.expensetracker.app.di.AppContainer
import com.expensetracker.app.ui.components.PixelButton
import com.expensetracker.app.ui.theme.PixelBackground
import com.expensetracker.app.ui.theme.PixelInk
import com.expensetracker.app.ui.theme.PixelInkLight
import com.expensetracker.app.ui.theme.PixelRed
import com.expensetracker.app.util.ViewModelFactory

@Composable
fun OnboardingSyncScreen(container: AppContainer, onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = ViewModelFactory { OnboardingViewModel(container.userPreferencesRepository, container.authRepository) }
    )
    Scaffold(containerColor = PixelBackground) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("SYNC YOUR EXPENSES", style = MaterialTheme.typography.titleLarge, color = PixelInk)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in with Google to back up your data and use it across devices. " +
                    "You can always do this later from Settings.",
                style = MaterialTheme.typography.bodyMedium, color = PixelInkLight
            )
            Spacer(Modifier.height(24.dp))
            PixelButton(
                text = if (viewModel.isSigningIn) "SIGNING IN..." else "SIGN IN WITH GOOGLE",
                onClick = { viewModel.signInWithGoogle(onSuccess = { viewModel.finishOnboarding(onDone) }) },
                modifier = Modifier.fillMaxWidth()
            )
            viewModel.signInError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = PixelRed, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "SKIP FOR NOW",
                style = MaterialTheme.typography.labelLarge,
                color = PixelInkLight,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.finishOnboarding(onDone) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}
```

---

## 9. Navigation + Splash-Gated Start Destination

### Updated `ui/navigation/Screen.kt` — add two routes
```kotlin
object OnboardingName : Screen("onboarding_name")
object OnboardingSync : Screen("onboarding_sync")
```
(add these as two more `object`s inside the existing `sealed class Screen`, alongside `Home`, `Categories`, etc.)

### Updated `ui/navigation/NavGraph.kt` — add a param + two routes
```kotlin
@Composable
fun NavGraph(container: AppContainer, startWithOnboarding: Boolean) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (startWithOnboarding) Screen.OnboardingName.route else Screen.Home.route
    ) {
        composable(Screen.OnboardingName.route) {
            OnboardingNameScreen(
                container = container,
                onContinue = { navController.navigate(Screen.OnboardingSync.route) }
            )
        }
        composable(Screen.OnboardingSync.route) {
            OnboardingSyncScreen(
                container = container,
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.OnboardingName.route) { inclusive = true }
                    }
                }
            )
        }

        // ... keep all your existing Home / Categories / AddExpense / CategoryHistory /
        // Settings routes here, unchanged ...
    }
}
```

### Updated `MainActivity.kt` — hold the splash screen until we know onboarding status
```kotlin
package com.expensetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.expensetracker.app.ui.navigation.NavGraph
import com.expensetracker.app.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        val app = application as ExpenseTrackerApp

        setContent {
            ExpenseTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val onboardingDone by produceState<Boolean?>(initialValue = null) {
                        value = app.container.userPreferencesRepository.onboardingDone.first()
                    }
                    LaunchedEffect(onboardingDone) {
                        if (onboardingDone != null) keepSplash = false
                    }
                    onboardingDone?.let { done ->
                        NavGraph(container = app.container, startWithOnboarding = !done)
                    }
                }
            }
        }
    }
}
```
> Needs `import kotlinx.coroutines.flow.first` for `.first()`.

### `themes.xml` — add a splash theme
```xml
<resources>
    <style name="Theme.ExpenseTracker" parent="Theme.Material3.DayNight.NoActionBar" />

    <style name="Theme.App.Starting" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">@color/splash_background</item>
        <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher</item>
        <item name="postSplashScreenTheme">@style/Theme.ExpenseTracker</item>
    </style>
</resources>
```

### New file: `res/values/colors.xml`
```xml
<resources>
    <color name="splash_background">#F7ECD9</color>
</resources>
```

### `AndroidManifest.xml` — point the activity at the splash theme
```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.App.Starting">
```

---

## 10. Account Section for Your Existing Settings Screen

### New file: `ui/settings/AccountViewModel.kt`
```kotlin
package com.expensetracker.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.datastore.UserPreferencesRepository
import com.expensetracker.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {
    val user: StateFlow<FirebaseUser?> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), authRepository.currentUser)

    var isWorking by mutableStateOf(false); private set
    var errorMessage by mutableStateOf<String?>(null); private set

    fun signIn() {
        viewModelScope.launch {
            isWorking = true; errorMessage = null
            authRepository.signInWithGoogle().onFailure { errorMessage = it.message }
            isWorking = false
        }
    }

    fun signOut() { authRepository.signOut() }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            isWorking = true; errorMessage = null
            authRepository.deleteAccount()
                .onSuccess { onDeleted() }
                .onFailure { errorMessage = it.message ?: "Couldn't delete account. Please sign in again and retry." }
            isWorking = false
        }
    }
}
```

### New file: `ui/settings/AccountSection.kt`
```kotlin
package com.expensetracker.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.expensetracker.app.di.AppContainer
import com.expensetracker.app.ui.components.ConfirmDialog
import com.expensetracker.app.ui.components.PixelButton
import com.expensetracker.app.ui.components.PixelCard
import com.expensetracker.app.ui.theme.*
import com.expensetracker.app.util.ViewModelFactory

/**
 * Drop this into your existing Settings screen, e.g.:
 *   AccountSection(container = container, onAccountDeleted = { /* e.g. show a snackbar */ })
 */
@Composable
fun AccountSection(container: AppContainer, onAccountDeleted: () -> Unit) {
    val viewModel: AccountViewModel = viewModel(
        factory = ViewModelFactory { AccountViewModel(container.authRepository, container.userPreferencesRepository) }
    )
    val user by viewModel.user.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    PixelCard {
        Text("ACCOUNT", style = MaterialTheme.typography.labelLarge, color = PixelInkLight)
        Spacer(Modifier.height(10.dp))

        if (user == null) {
            Text(
                "Not signed in. Sign in with Google to back up and sync your data.",
                color = PixelInk, style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            PixelButton(
                text = if (viewModel.isWorking) "SIGNING IN..." else "SIGN IN WITH GOOGLE",
                onClick = viewModel::signIn,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).background(PixelSurface).border(BorderStroke(2.dp, PixelInk))
                ) {
                    user?.photoUrl?.toString()?.let { url ->
                        AsyncImage(model = url, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(user?.displayName ?: "—", style = MaterialTheme.typography.bodyLarge, color = PixelInk)
                    Text(user?.email ?: "—", style = MaterialTheme.typography.bodyMedium, color = PixelInkLight)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(text = "SIGN OUT", onClick = viewModel::signOut, containerColor = PixelSurface, contentColor = PixelInk)
                PixelButton(text = "DELETE ACCOUNT", onClick = { showDeleteConfirm = true }, containerColor = PixelRed, contentColor = PixelSurface)
            }
        }

        viewModel.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = PixelRed, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete your account?",
            message = "This permanently deletes your cloud backup and unlinks Google sign-in. " +
                "Your expenses already on this device are kept. This cannot be undone.",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteAccount(onDeleted = onAccountDeleted)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
```

---

## 11. Where This Plugs In

- Nothing about Home, Categories, Add Expense, or Category History needs to change — they
  keep working exactly as before, just now writing through repositories that also sync.
- In your existing Settings screen, add one line near the top of its layout:
  `AccountSection(container = container, onAccountDeleted = { /* your choice, e.g. show a
  confirmation message and stay on Settings */ })`.
- The onboarding routes only show up the very first time the app is opened
  (`onboardingDone == false`); after that, `Home` is the start destination as before.

## 12. Firestore Security Rules

Paste this into **Firestore → Rules** and publish. Without it your data is either fully
locked (test mode expires) or, if left in an early "allow all" test-mode state, fully
public — this restricts every document to only the signed-in owner:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## 13. If You'd Rather "Delete Account" Also Wipe Local Data

Add one line to `AccountViewModel.deleteAccount()`'s success branch:
```kotlin
authRepository.deleteAccount()
    .onSuccess {
        prefsRepository.clearAll()
        // container.database.clearAllTables() // wipes local expenses/categories too
        onDeleted()
    }
```
(You'd need to pass the `ExpenseDatabase` — or a repository method that wraps
`clearAllTables()` — into `AccountViewModel` to call it.) Left out by default since
"delete my cloud account" and "delete my expense history" read as two different asks.

## 14. Testing Checklist

- [ ] Fresh install → onboarding name screen appears first, "Continue" disabled until you type something.
- [ ] After name → sync screen appears; "Skip for now" lands on Home with local-only data.
- [ ] Close and reopen the app → onboarding does **not** show again, goes straight to Home.
- [ ] From the sync screen (or Settings), "Sign in with Google" completes and lands you back in the app signed in.
- [ ] Add an expense while signed in → check the Firestore console, a matching document appears under `users/{uid}/expenses`.
- [ ] Sign in with the same Google account on a second device/emulator → existing categories and expenses appear there too.
- [ ] Edit an expense on Device A → change appears on Device B within a few seconds (while both are online).
- [ ] Delete a category on Device A → it disappears on Device B too.
- [ ] Settings → Sign Out → Account section reverts to "Not signed in", local data still intact.
- [ ] Settings → Delete Account → confirm dialog appears; after confirming, Firestore console shows the `users/{uid}` document and its subcollections gone, and the user no longer appears under Authentication → Users.

## 15. Known Limitations (acceptable for personal use, worth knowing)

- **Conflict resolution is "whoever syncs last wins"** — there's no merge of two
  simultaneous edits to the same record; fine for one person on a couple of devices, not
  built for true multi-user collaboration.
- **Every sign-in re-uploads all local data** (`pushAllLocalData`) rather than only what's
  changed since the last sync — harmless and simple at personal scale (well within
  Firestore's free tier), but not bandwidth-optimal if you have thousands of expenses.
- **Tombstoned documents are kept forever** rather than periodically purged — again,
  trivial storage cost for personal use; a Cloud Function to sweep old tombstones would be
  the production-grade fix.
- Account deletion needs a **recent sign-in**; if `deleteAccount()` fails with
  `FirebaseAuthRecentLoginRequiredException`, the fix is asking the user to sign in again
  right before retrying the delete.
