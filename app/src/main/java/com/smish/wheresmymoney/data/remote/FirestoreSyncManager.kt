package com.smish.wheresmymoney.data.remote

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.smish.wheresmymoney.data.local.ExpenseDatabase
import com.smish.wheresmymoney.data.local.entity.CategoryEntity
import com.smish.wheresmymoney.data.local.entity.ExpenseEntity
import com.smish.wheresmymoney.data.local.entity.ParentCategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FirestoreSyncManager(private val database: ExpenseDatabase) {
    private val firestore = Firebase.firestore
    private var listeners: List<ListenerRegistration> = emptyList()
    private var scope: CoroutineScope? = null

    fun start(uid: String) {
        stop()
        val activeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = activeScope
        val userDoc = firestore.collection("users").document(uid)

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
