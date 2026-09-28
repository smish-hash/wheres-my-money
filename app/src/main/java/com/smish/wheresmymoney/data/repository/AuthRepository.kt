package com.smish.wheresmymoney.data.repository

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

private const val WEB_CLIENT_ID = "962366977245-isjnm8rgn3ah99a15hinm2sfj4nurf7a.apps.googleusercontent.com"

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
