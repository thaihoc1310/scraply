package com.example.scraply.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.scraply.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class ScraplyUser(
    val uid: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val email: String?,
    val bio: String = "",
    val followerCount: Long = 0,
    val followingCount: Long = 0,
)

/**
 * Thin wrapper over Firebase Auth + Credential Manager (Google Sign-In).
 *
 * Sign-in flow:
 *  1. Caller (UI) invokes [signInWithGoogle] on a UI thread, passing an Activity Context.
 *  2. Credential Manager shows the Google account picker / One-Tap bottom sheet.
 *  3. The resulting Google ID Token is exchanged with Firebase Auth.
 *  4. The user profile document is upserted in Firestore at `users/{uid}`.
 */
class AuthRepository(
    private val appContext: Context,
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore
    private val credentialManager = CredentialManager.create(appContext)

    val currentUserId: String? get() = auth.currentUser?.uid

    fun currentUserFlow(): Flow<ScraplyUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            val u = fa.currentUser
            trySend(
                u?.let {
                    ScraplyUser(
                        uid = it.uid,
                        username = it.displayName ?: it.email?.substringBefore('@') ?: it.uid.take(6),
                        displayName = it.displayName,
                        avatarUrl = it.photoUrl?.toString(),
                        email = it.email,
                    )
                }
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Launches the Credential Manager Google Sign-In flow. Must be called with an Activity context.
     *
     * Requires `R.string.default_web_client_id` to be set to the OAuth 2.0 Web client ID from the
     * Firebase project. If it still holds the placeholder value, the call throws.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<ScraplyUser> = runCatching {
        val webClientId = appContext.getString(R.string.default_web_client_id)
        require(!webClientId.startsWith("REPLACE_")) {
            "default_web_client_id is still the placeholder. Add the Firebase Web Client ID to res/values/firebase_strings.xml."
        }

        // Primary flow: explicit "Sign in with Google" button -> always shows the account chooser,
        // even on the first run when no credential has been saved yet.
        val buttonRequest = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        // Secondary flow: one-tap / auto-select, used only as a fallback if the button option is
        // unavailable (older Play Services, devices without any saved Google account, etc.).
        val oneTapRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        val response: GetCredentialResponse = try {
            credentialManager.getCredential(activityContext, buttonRequest)
        } catch (e: NoCredentialException) {
            try {
                credentialManager.getCredential(activityContext, oneTapRequest)
            } catch (e2: GetCredentialException) {
                throw IllegalStateException(explainCredentialError(e2), e2)
            }
        } catch (e: GetCredentialException) {
            throw IllegalStateException(explainCredentialError(e), e)
        }

        val cred = response.credential
        val idToken = try {
            GoogleIdTokenCredential.createFrom(cred.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            throw IllegalStateException("Unable to parse Google ID token", e)
        }
        val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(firebaseCred).await()
        val fu = authResult.user ?: error("Firebase user is null after sign-in")
        val profile = ScraplyUser(
            uid = fu.uid,
            username = fu.displayName ?: fu.email?.substringBefore('@') ?: fu.uid.take(6),
            displayName = fu.displayName,
            avatarUrl = fu.photoUrl?.toString(),
            email = fu.email,
        )
        upsertProfile(profile)
        profile
    }

    private fun explainCredentialError(e: GetCredentialException): String {
        val raw = e.message.orEmpty()
        return when {
            e is NoCredentialException || raw.contains("No credentials", ignoreCase = true) ->
                "No Google account is available on this device. Add one in Settings > Accounts and retry, or use email sign-in."
            raw.contains("cancell", ignoreCase = true) || raw.contains("cancel", ignoreCase = true) ->
                "Google sign-in was cancelled."
            raw.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                raw.contains("10:", ignoreCase = true) ->
                "Google sign-in mis-configured: add your debug SHA-1 to Firebase Console, download the new google-services.json, and rebuild."
            raw.contains("network", ignoreCase = true) ->
                "Network error during Google sign-in. Check your connection and retry."
            else -> "Google sign-in failed: $raw"
        }
    }

    private suspend fun upsertProfile(user: ScraplyUser) {
        val ref = firestore.collection("users").document(user.uid)
        val existing = ref.get().await()
        val data = mutableMapOf<String, Any?>(
            "uid" to user.uid,
            "displayName" to user.displayName,
            "avatarUrl" to user.avatarUrl,
            "email" to user.email,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (!existing.exists()) {
            data["username"] = user.username
            data["bio"] = ""
            data["followerCount"] = 0L
            data["followingCount"] = 0L
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        ref.set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /**
     * Emits the fully hydrated profile (merging FirebaseAuth info and the Firestore doc) for a
     * given uid. Falls back to auth-only info if the Firestore doc is missing.
     */
    fun observeProfile(uid: String): Flow<ScraplyUser?> = callbackFlow {
        val reg = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                val fu = auth.currentUser
                if (snap != null && snap.exists()) {
                    trySend(
                        ScraplyUser(
                            uid = uid,
                            username = snap.getString("username")
                                ?: snap.getString("displayName")
                                ?: fu?.email?.substringBefore('@')
                                ?: uid.take(6),
                            displayName = snap.getString("displayName") ?: fu?.displayName,
                            avatarUrl = snap.getString("avatarUrl") ?: fu?.photoUrl?.toString(),
                            email = snap.getString("email") ?: fu?.email,
                            bio = snap.getString("bio").orEmpty(),
                            followerCount = snap.getLong("followerCount") ?: 0L,
                            followingCount = snap.getLong("followingCount") ?: 0L,
                        )
                    )
                } else if (fu != null && fu.uid == uid) {
                    trySend(fu.toScraplyUser())
                } else {
                    trySend(null)
                }
            }
        awaitClose { reg.remove() }
    }

    /**
     * Updates the editable profile fields. Null values leave the corresponding field unchanged.
     */
    suspend fun updateProfile(
        displayName: String? = null,
        username: String? = null,
        bio: String? = null,
        avatarUrl: String? = null,
    ): Result<Unit> = runCatching {
        val fu = auth.currentUser ?: error("Not signed in.")
        if (displayName != null) {
            fu.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
            ).await()
        }
        val patch = mutableMapOf<String, Any?>(
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (displayName != null) patch["displayName"] = displayName
        if (username != null) patch["username"] = username
        if (bio != null) patch["bio"] = bio
        if (avatarUrl != null) patch["avatarUrl"] = avatarUrl
        firestore.collection("users").document(fu.uid)
            .set(patch, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /**
     * Signs in with an email + password combo. Fails if the account does not exist or the password
     * is wrong; callers should surface the error and offer [signUpWithEmail] as a fallback.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<ScraplyUser> = runCatching {
        require(email.isNotBlank() && password.isNotBlank()) { "Email and password are required." }
        val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val fu = authResult.user ?: error("Firebase user is null after sign-in")
        val profile = fu.toScraplyUser()
        upsertProfile(profile)
        profile
    }

    /**
     * Creates a new Firebase Auth account with email + password and the given display name.
     * The display name is also saved back to the user profile so feed posts show something nicer
     * than the email prefix.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String?,
    ): Result<ScraplyUser> = runCatching {
        require(email.isNotBlank()) { "Email is required." }
        require(password.length >= 6) { "Password must be at least 6 characters." }
        val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val fu = authResult.user ?: error("Firebase user is null after sign-up")
        val finalName = displayName?.trim()?.ifBlank { null }
        if (finalName != null) {
            fu.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(finalName).build()
            ).await()
        }
        val profile = auth.currentUser?.toScraplyUser() ?: fu.toScraplyUser()
        upsertProfile(profile)
        profile
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        require(email.isNotBlank()) { "Email is required." }
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    private fun com.google.firebase.auth.FirebaseUser.toScraplyUser(): ScraplyUser = ScraplyUser(
        uid = uid,
        username = displayName ?: email?.substringBefore('@') ?: uid.take(6),
        displayName = displayName,
        avatarUrl = photoUrl?.toString(),
        email = email,
    )

    suspend fun signOut() {
        auth.signOut()
        try {
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
        } catch (_: Throwable) { /* no-op */ }
    }
}
