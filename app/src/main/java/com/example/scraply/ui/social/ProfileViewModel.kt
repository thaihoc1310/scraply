package com.example.scraply.ui.social

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.SocialRepository
import com.example.scraply.data.remote.StorageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val pendingLikeIds = mutableSetOf<String>()

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
            .onEach { rawPosts ->
                val currentPosts = _profile.value.myPosts
                val isInitialLoad = currentPosts.isEmpty()
                val mergedPosts = rawPosts.map { post ->
                    val existing = currentPosts.firstOrNull { it.id == post.id }
                    var updated = post.copy(
                        username = existing?.username ?: post.username,
                        avatarUrl = existing?.avatarUrl ?: post.avatarUrl,
                        likedByMe = existing?.likedByMe ?: post.likedByMe,
                        savedByMe = existing?.savedByMe ?: post.savedByMe,
                        previewComments = existing?.previewComments.orEmpty(),
                    )

                    if (post.id in pendingLikeIds && existing != null) {
                        updated = updated.copy(
                            likedByMe = existing.likedByMe,
                            likeCount = existing.likeCount,
                        )
                    }
                    updated
                }
                if (!isInitialLoad) {
                    _profile.value = _profile.value.copy(myPosts = mergedPosts)
                }

                val withPreviews = runCatching { social.hydrateFeedSnapshot(uid, mergedPosts, rank = false) }.getOrDefault(mergedPosts)
                _profile.value = _profile.value.copy(myPosts = withPreviews)
            }
            .launchIn(viewModelScope)
    }

    private fun stopMyPosts() {
        postsJob?.cancel(); postsJob = null
        pendingLikeIds.clear()
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
                    socialRepository?.invalidateUserSummary(uid)
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
        val previousPosts = _profile.value.myPosts
        _profile.value = _profile.value.copy(myPosts = previousPosts.filterNot { it.id == post.id })
        viewModelScope.launch {
            runCatching { social.deletePost(post.id, uid) }
                .onFailure { t ->
                    _profile.value = _profile.value.copy(
                        myPosts = previousPosts,
                        message = t.message ?: "Failed to delete post.",
                    )
                }
        }
    }

    fun updatePostDetails(post: FeedPost, title: String, description: String) {
        val uid = authState.value.user?.uid ?: return
        val social = socialRepository ?: return
        val cleanTitle = title.trim().take(1000).ifBlank { null }
        val cleanDescription = description.trim().take(1000).ifBlank { null }
        val previousPosts = _profile.value.myPosts

        _profile.value = _profile.value.copy(
            myPosts = previousPosts.map { item ->
                if (item.id == post.id) {
                    item.copy(title = cleanTitle, description = cleanDescription)
                } else {
                    item
                }
            },
        )
        viewModelScope.launch {
            runCatching { social.updatePostDetails(post.id, uid, title, description) }
                .onFailure { t ->
                    _profile.value = _profile.value.copy(
                        myPosts = previousPosts,
                        message = t.message ?: "Failed to update post.",
                    )
                }
        }
    }

    fun toggleLike(post: FeedPost) {
        val uid = authState.value.user?.uid ?: return
        val social = socialRepository ?: return
        pendingLikeIds.add(post.id)
        val optimisticLiked = !post.likedByMe
        val optimisticCount = (post.likeCount + if (optimisticLiked) 1 else -1).coerceAtLeast(0)
        _profile.value = _profile.value.copy(
            myPosts = _profile.value.myPosts.map { item ->
                if (item.id == post.id) {
                    item.copy(likedByMe = optimisticLiked, likeCount = optimisticCount)
                } else {
                    item
                }
            },
        )
        viewModelScope.launch {
            runCatching { social.toggleLike(post.id, uid) }
                .onSuccess {
                    delay(800)
                    pendingLikeIds.remove(post.id)
                }
                .onFailure {
                    Log.e("ProfileVM", "toggleLike failed for post=${post.id}", it)
                    pendingLikeIds.remove(post.id)
                    _profile.value = _profile.value.copy(
                        myPosts = _profile.value.myPosts.map { item ->
                            if (item.id == post.id) {
                                item.copy(likedByMe = post.likedByMe, likeCount = post.likeCount)
                            } else {
                                item
                            }
                        },
                    )
                }
        }
    }
}
