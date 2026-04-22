package com.example.scraply.ui.social

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.SocialRepository
import com.example.scraply.data.remote.StorageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: ScraplyUser? = null,
    val myPosts: List<FeedPost> = emptyList(),
    val saving: Boolean = false,
    val editing: Boolean = false,
    val message: String? = null,
)

class ProfileViewModel(
    authRepository: AuthRepository?,
    firestoreSync: FirestoreSyncRepository?,
    private val socialRepository: SocialRepository?,
    private val storageRepository: StorageRepository?,
) : BaseAuthViewModel(authRepository, firestoreSync) {

    private val _profile = MutableStateFlow(ProfileUiState())
    val profile: StateFlow<ProfileUiState> = _profile.asStateFlow()

    private var postsJob: Job? = null
    private var profileJob: Job? = null

    override fun onAuthUserChanged(user: ScraplyUser?) {
        if (user != null) {
            startProfile(user.uid)
            startMyPosts(user.uid)
        } else {
            stopProfile()
            stopMyPosts()
        }
    }

    private fun startProfile(uid: String) {
        profileJob?.cancel()
        val repo = authRepository ?: return
        profileJob = repo.observeProfile(uid)
            .onEach { p -> _profile.value = _profile.value.copy(profile = p) }
            .launchIn(viewModelScope)
    }

    private fun stopProfile() {
        profileJob?.cancel(); profileJob = null
        _profile.value = _profile.value.copy(profile = null)
    }

    private fun startMyPosts(uid: String) {
        postsJob?.cancel()
        val social = socialRepository ?: return
        postsJob = social.observeMyPosts(uid)
            .onEach { posts -> _profile.value = _profile.value.copy(myPosts = posts) }
            .launchIn(viewModelScope)
    }

    private fun stopMyPosts() {
        postsJob?.cancel(); postsJob = null
        _profile.value = _profile.value.copy(myPosts = emptyList())
    }

    fun beginEdit() {
        _profile.value = _profile.value.copy(editing = true, message = null)
    }

    fun cancelEdit() {
        _profile.value = _profile.value.copy(editing = false)
    }

    fun dismissMessage() {
        _profile.value = _profile.value.copy(message = null)
    }

    fun saveProfile(
        displayName: String,
        username: String,
        bio: String,
        newAvatarLocalPath: String?,
    ) {
        val auth = authRepository ?: return
        val uid = authState.value.user?.uid ?: return
        viewModelScope.launch {
            _profile.value = _profile.value.copy(saving = true, message = null)
            val avatarUrl: String? = if (newAvatarLocalPath != null && storageRepository != null) {
                runCatching { storageRepository.uploadAvatar(uid, newAvatarLocalPath) }.getOrNull()
            } else null
            auth.updateProfile(
                displayName = displayName.ifBlank { null },
                username = username.ifBlank { null },
                bio = bio,
                avatarUrl = avatarUrl,
            )
                .onSuccess {
                    _profile.value = _profile.value.copy(
                        saving = false,
                        editing = false,
                        message = "Profile updated.",
                    )
                }
                .onFailure { t ->
                    _profile.value = _profile.value.copy(
                        saving = false,
                        message = t.message ?: "Failed to update profile.",
                    )
                }
        }
    }

    fun deletePost(post: FeedPost) {
        val uid = authState.value.user?.uid ?: return
        val social = socialRepository ?: return
        viewModelScope.launch {
            runCatching { social.deletePost(post.id, uid) }
                .onFailure { t ->
                    _profile.value = _profile.value.copy(message = t.message ?: "Failed to delete post.")
                }
        }
    }
}
