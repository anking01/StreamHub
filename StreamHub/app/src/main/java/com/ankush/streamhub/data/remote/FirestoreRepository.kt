package com.ankush.streamhub.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "user",     // "user" or "admin"
    val createdAt: Long = System.currentTimeMillis()
)

data class NotificationRequest(
    val title: String = "",
    val body: String = "",
    val sentBy: String = "",
    val sentAt: Long = System.currentTimeMillis(),
    val status: String = "pending"  // "pending" | "sent"
)

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── User ─────────────────────────────────────────────────────────────────

    suspend fun createUserDocument(uid: String, name: String, email: String) {
        val user = AppUser(uid = uid, name = name, email = email, role = "user")
        db.collection("users").document(uid).set(user).await()
    }

    suspend fun getCurrentUser(): AppUser? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("users").document(uid).get().await()
            .toObject(AppUser::class.java)
    }

    suspend fun isCurrentUserAdmin(): Boolean {
        return getCurrentUser()?.role == "admin"
    }

    suspend fun getAllUsers(): List<AppUser> {
        return db.collection("users").get().await()
            .documents.mapNotNull { it.toObject(AppUser::class.java) }
    }

    suspend fun makeAdmin(uid: String) {
        db.collection("users").document(uid)
            .update("role", "admin").await()
    }

    suspend fun removeAdmin(uid: String) {
        db.collection("users").document(uid)
            .update("role", "user").await()
    }

    // ── Notifications ─────────────────────────────────────────────────────────
    // Queues a notification in Firestore — Firebase Cloud Functions picks it up
    // and sends via FCM to all users

    suspend fun queueNotification(title: String, body: String) {
        val uid = auth.currentUser?.uid ?: return
        val notif = NotificationRequest(
            title = title,
            body = body,
            sentBy = uid
        )
        db.collection("notifications").add(notif).await()
    }
}
