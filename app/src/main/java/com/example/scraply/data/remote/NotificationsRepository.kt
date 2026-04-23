package com.example.scraply.data.remote

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Kinds of notifications we create from client-side social events. Mirror this enum on the
 * Cloud Functions side when composing push titles/bodies.
 */
object NotificationType {
    const val LIKE = "like"
    const val COMMENT = "comment"
    const val FOLLOW = "follow"
    const val SYSTEM = "system"
}

data class NotificationItem(
    val id: String,
    val type: String,
    val actorId: String?,
    val actorName: String?,
    val actorAvatar: String?,
    val postId: String?,
    val postImageUrl: String?,
    val text: String?,
    val read: Boolean,
    val createdAt: Long,
)

/**
 * In-app notification feed stored under `users/{uid}/notifications/{id}`.
 *
 * The same documents drive:
 *  1. The in-app notifications tab (this repository streams them via [observe]).
 *  2. The push pipeline – a Cloud Function listens to writes under this path and fans out
 *     FCM messages to every token in `users/{uid}/fcmTokens`.
 *
 * Keeping the push payload derived from Firestore means we don't need an FCM server key in
 * the client, and the in-app feed stays in sync with whatever was pushed.
 */
class NotificationsRepository {
    private val firestore = Firebase.firestore

    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("notifications")

    fun observe(uid: String, limit: Long = 50L): Flow<List<NotificationItem>> = callbackFlow {
        val reg = collection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(
                    snap.documents.map { d ->
                        NotificationItem(
                            id = d.id,
                            type = d.getString("type") ?: NotificationType.SYSTEM,
                            actorId = d.getString("actorId"),
                            actorName = d.getString("actorName"),
                            actorAvatar = d.getString("actorAvatar"),
                            postId = d.getString("postId"),
                            postImageUrl = d.getString("postImageUrl"),
                            text = d.getString("text"),
                            read = d.getBoolean("read") ?: false,
                            createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        )
                    }
                )
            }
        awaitClose { reg.remove() }
    }

    fun unreadCount(uid: String): Flow<Int> = callbackFlow {
        val reg = collection(uid)
            .whereEqualTo("read", false)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(snap.size())
            }
        awaitClose { reg.remove() }
    }

    /**
     * Writes a notification for [targetUid]. No-op when target == actor (so e.g. liking
     * your own post doesn't notify yourself). Idempotency is best-effort: callers pass a
     * stable [dedupeKey] (e.g. `"like_${postId}_${actorId}"`) so liking and unliking the
     * same post in quick succession doesn't spam the recipient.
     */
    suspend fun create(
        targetUid: String,
        type: String,
        actorId: String?,
        actorName: String? = null,
        actorAvatar: String? = null,
        postId: String? = null,
        postImageUrl: String? = null,
        text: String? = null,
        dedupeKey: String? = null,
    ) {
        if (targetUid.isBlank() || actorId == targetUid) return
        val payload = mapOf(
            "type" to type,
            "actorId" to actorId,
            "actorName" to actorName,
            "actorAvatar" to actorAvatar,
            "postId" to postId,
            "postImageUrl" to postImageUrl,
            "text" to text,
            "read" to false,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        val doc = if (dedupeKey != null) collection(targetUid).document(dedupeKey)
        else collection(targetUid).document()
        doc.set(payload).await()
    }

    suspend fun markAllRead(uid: String) {
        val snap = collection(uid).whereEqualTo("read", false).get().await()
        if (snap.isEmpty) return
        val batch = firestore.batch()
        snap.documents.forEach { d -> batch.update(d.reference, "read", true) }
        batch.commit().await()
    }

    suspend fun markRead(uid: String, notificationId: String) {
        collection(uid).document(notificationId).update("read", true).await()
    }
}
