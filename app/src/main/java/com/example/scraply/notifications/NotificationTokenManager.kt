package com.example.scraply.notifications

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Owns the FCM registration token lifecycle against Firestore.
 *
 * Tokens are stored per-user at `users/{uid}/fcmTokens/{tokenId}` (where `tokenId` is the
 * URL-safe, MD5-ish hash of the token; see [tokenDocId]). Storing multiple tokens per user
 * supports the case where the same account is signed in on several devices – when Cloud
 * Functions fan out a push they read this subcollection and issue a multicast.
 *
 * Call [saveCurrentToken] right after every successful sign-in / sign-up and from
 * [ScraplyMessagingService.onNewToken] so refreshes propagate. Call [removeCurrentToken]
 * right before signing out so abandoned devices don't keep receiving notifications.
 */
object NotificationTokenManager {
    private const val TAG = "NotifTokenManager"

    suspend fun saveCurrentToken(uid: String): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        saveToken(uid, token)
    }

    suspend fun saveToken(uid: String, token: String) {
        if (token.isBlank()) return
        val docId = tokenDocId(token)
        val payload = mapOf(
            "token" to token,
            "platform" to "android",
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        Firebase.firestore.collection("users").document(uid)
            .collection("fcmTokens").document(docId)
            .set(payload)
            .await()
        Log.d(TAG, "Saved FCM token for $uid (doc=$docId)")
    }

    suspend fun removeCurrentToken(uid: String): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        val docId = tokenDocId(token)
        Firebase.firestore.collection("users").document(uid)
            .collection("fcmTokens").document(docId)
            .delete()
            .await()
        Log.d(TAG, "Removed FCM token for $uid")
    }

    // Firestore doc ids cannot contain '/' and must stay under 1500 bytes; hashing the raw
    // token keeps ids deterministic (so refreshing the token on the same device overwrites
    // the previous entry) while staying well under the limit.
    private fun tokenDocId(token: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(40)
    }
}
