package com.example.scraply.ui.social

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class AuthMode { SignIn, SignUp }

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val user: ScraplyUser? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val firebaseAvailable: Boolean = true,
    val authMode: AuthMode = AuthMode.SignIn,
)

/**
 * Shared auth state + actions for Feed and Profile screens. Each subclass owns its own instance
 * but both listen to the same FirebaseAuth singleton, so they stay in sync across tabs without
 * needing a cross-screen ViewModel store.
 */
abstract class BaseAuthViewModel(
    protected val authRepository: AuthRepository?,
    protected val firestoreSync: FirestoreSyncRepository?,
) : ViewModel() {

    protected val _auth = MutableStateFlow(
        AuthUiState(firebaseAvailable = authRepository != null)
    )
    val authState: StateFlow<AuthUiState> = _auth.asStateFlow()

    init {
        authRepository?.currentUserFlow()
            ?.onEach { u ->
                _auth.value = _auth.value.copy(isSignedIn = u != null, user = u)
                onAuthUserChanged(u)
            }
            ?.launchIn(viewModelScope)
    }

    /** Hook for subclasses to react to auth state changes (start/stop data observation). */
    protected open fun onAuthUserChanged(user: ScraplyUser?) {}

    fun toggleAuthMode() {
        _auth.value = _auth.value.copy(
            authMode = if (_auth.value.authMode == AuthMode.SignIn) AuthMode.SignUp else AuthMode.SignIn,
            error = null,
            info = null,
        )
    }

    fun dismissError() {
        _auth.value = _auth.value.copy(error = null, info = null)
    }

    fun signInWithGoogle(activityContext: Context) {
        val auth = authRepository ?: run {
            _auth.value = _auth.value.copy(error = "Firebase is not configured.")
            return
        }
        viewModelScope.launch {
            _auth.value = _auth.value.copy(loading = true, error = null, info = null)
            auth.signInWithGoogle(activityContext)
                .onSuccess { onSignedIn(it) }
                .onFailure { t -> _auth.value = _auth.value.copy(loading = false, error = t.message ?: "Google sign-in failed") }
        }
    }

    fun submitEmail(email: String, password: String, displayName: String?) {
        val auth = authRepository ?: run {
            _auth.value = _auth.value.copy(error = "Firebase is not configured.")
            return
        }
        viewModelScope.launch {
            _auth.value = _auth.value.copy(loading = true, error = null, info = null)
            val result = if (_auth.value.authMode == AuthMode.SignIn)
                auth.signInWithEmail(email, password)
            else
                auth.signUpWithEmail(email, password, displayName)
            result
                .onSuccess { onSignedIn(it) }
                .onFailure { t -> _auth.value = _auth.value.copy(loading = false, error = friendly(t)) }
        }
    }

    fun sendPasswordReset(email: String) {
        val auth = authRepository ?: return
        if (email.isBlank()) {
            _auth.value = _auth.value.copy(error = "Enter your email above first, then tap Forgot password.")
            return
        }
        viewModelScope.launch {
            _auth.value = _auth.value.copy(loading = true, error = null, info = null)
            auth.sendPasswordReset(email)
                .onSuccess {
                    _auth.value = _auth.value.copy(loading = false, info = "Password reset email sent to $email.")
                }
                .onFailure { t ->
                    _auth.value = _auth.value.copy(loading = false, error = friendly(t))
                }
        }
    }

    fun signOut() {
        val auth = authRepository ?: return
        viewModelScope.launch { auth.signOut() }
    }

    private suspend fun onSignedIn(user: ScraplyUser) {
        _auth.value = _auth.value.copy(
            loading = false, isSignedIn = true, user = user, error = null, info = null,
        )
        firestoreSync?.let {
            runCatching { it.migrateLocalToCloud(user.uid) }
            runCatching { it.pullAllForUser(user.uid) }
        }
    }

    private fun friendly(t: Throwable): String {
        val msg = t.message.orEmpty()
        return when {
            msg.contains("password is invalid", ignoreCase = true) ||
                msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                msg.contains("malformed", ignoreCase = true) ->
                "Incorrect email or password."
            msg.contains("email address is already in use", ignoreCase = true) ->
                "An account already exists for this email. Try signing in instead."
            msg.contains("no user record", ignoreCase = true) ->
                "No account found for that email."
            msg.contains("network", ignoreCase = true) ->
                "Network error. Check your connection and retry."
            msg.contains("Password must be at least", ignoreCase = true) ->
                "Password must be at least 6 characters."
            else -> msg.ifBlank { "Authentication failed." }
        }
    }
}
