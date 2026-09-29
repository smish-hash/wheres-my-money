package com.smish.wheresmymoney.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.smish.wheresmymoney.data.local.ExpenseDatabase
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import com.smish.wheresmymoney.widget.ExpenseWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val context: Context,
    private val database: ExpenseDatabase
) {
    private val firestore = Firebase.firestore
    private var listeners: List<ListenerRegistration> = emptyList()
    private var scope: CoroutineScope? = null

    fun start(uid: String) {
        stop()
        val activeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = activeScope
        val userDoc = firestore.collection("users").document(uid)

        activeScope.launch {
            try {
                deduplicateParentCategories(uid)
                database.clearAllData()
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Initial sync deduplication failed", e)
            }

            // Attach real-time snapshot listeners
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
                        applyExpense(uid, change.document.toObject(ExpenseDto::class.java))
                    }
                }
            }
            listeners = listOf(parentReg, categoryReg, expenseReg)
        }
    }

    fun stop() {
        listeners.forEach { it.remove() }
        listeners = emptyList()
        scope?.cancel()
        scope = null
    }

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
        docFor(uid, collection, id).set(
            mapOf("deleted" to true, "updatedAt" to System.currentTimeMillis()),
            SetOptions.merge()
        )
    }

    private fun docFor(uid: String, collection: String, id: Long) =
        firestore.collection("users").document(uid).collection(collection).document(id.toString())

    private suspend fun deduplicateParentCategories(uid: String) {
        val userDoc = firestore.collection("users").document(uid)
        val parentDocs = userDoc.collection("parentCategories").get().await()
        val allParents = parentDocs.documents
            .mapNotNull { it.toObject(ParentCategoryDto::class.java) }
            .filter { !it.deleted }

        val groupedByName = allParents.groupBy { it.name.trim().lowercase() }
        val duplicateParentIdsToTombstone = mutableListOf<Long>()

        for ((_, parentsGroup) in groupedByName) {
            if (parentsGroup.size > 1) {
                val primaryParent = parentsGroup.first()
                val duplicateParents = parentsGroup.drop(1)
                val dupIds = duplicateParents.map { it.id }.toSet()

                val categoryDocs = userDoc.collection("categories").get().await()
                val allSubcats = categoryDocs.documents
                    .mapNotNull { it.toObject(CategoryDto::class.java) }
                    .filter { !it.deleted }

                for (subcat in allSubcats) {
                    if (subcat.parentCategoryId in dupIds) {
                        val updatedSubcat = subcat.copy(
                            parentCategoryId = primaryParent.id,
                            updatedAt = System.currentTimeMillis()
                        )
                        docFor(uid, "categories", updatedSubcat.id).set(updatedSubcat)
                    }
                }

                for (dupParent in duplicateParents) {
                    duplicateParentIdsToTombstone.add(dupParent.id)
                }
            }
        }

        for (dupId in duplicateParentIdsToTombstone) {
            pushParentCategoryDeleted(uid, dupId)
        }
    }

    private suspend fun applyParentCategory(dto: ParentCategoryDto?) {
        dto ?: return
        val dao = database.parentCategoryDao()
        if (dto.deleted) dao.deleteById(dto.id) else dao.upsert(dto.toEntity())
        ExpenseWidgetReceiver.updateWidget(context)
    }

    private suspend fun applyCategory(dto: CategoryDto?) {
        dto ?: return
        val dao = database.categoryDao()
        if (dto.deleted) {
            dao.deleteById(dto.id)
        } else {
            if (dto.parentCategoryId != null) {
                val parentExists = database.parentCategoryDao().getAllOnce().any { it.id == dto.parentCategoryId }
                if (!parentExists) {
                    dao.upsert(dto.toEntity().copy(parentCategoryId = null))
                    ExpenseWidgetReceiver.updateWidget(context)
                    return
                }
            }
            dao.upsert(dto.toEntity())
        }
        ExpenseWidgetReceiver.updateWidget(context)
    }

    private suspend fun applyExpense(uid: String, dto: ExpenseDto?) {
        dto ?: return
        val dao = database.expenseDao()
        if (dto.deleted) {
            dao.deleteById(dto.id)
        } else {
            try {
                val categoryExists = database.categoryDao().getAllOnce().any { it.id == dto.categoryId }
                if (!categoryExists) {
                    val catDoc = docFor(uid, "categories", dto.categoryId).get().await()
                    if (catDoc.exists()) {
                        val catDto = catDoc.toObject(CategoryDto::class.java)
                        applyCategory(catDto)
                    }
                }
                dao.upsert(dto.toEntity())
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Failed to apply expense ${dto.id}", e)
            }
        }
        ExpenseWidgetReceiver.updateWidget(context)
    }
}
