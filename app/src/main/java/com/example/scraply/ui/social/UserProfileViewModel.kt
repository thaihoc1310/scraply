package com.example.scraply.ui.social

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.SocialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val profile: ScraplyUser? = null,
    val posts: List<FeedPost> = emptyList(),
    val isPostsLoading: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowLoading: Boolean = false,
    val isOwnProfile: Boolean = false,
)

class UserProfileViewModel(
    authRepository: AuthRepository?,
    firestoreSync: FirestoreSyncRepository?,
    private val socialRepository: SocialRepository?,
) : BaseAuthViewModel(authRepository, firestoreSync) {

    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private var targetUserId: String? = null
    private var profileJob: Job? = null
    private var postsJob: Job? = null
    private val pendingLikeIds = mutableSetOf<String>()
    /** True while a toggleFollow call is in-flight to prevent observer flicker. */
    @Volatile private var followInFlight = false

    fun loadUser(userId: String) {
        if (userId == targetUserId) return
        targetUserId = userId

        val currentUid = authState.value.user?.uid
        val isOwn = currentUid != null && currentUid == userId
        _state.value = UserProfileUiState(isOwnProfile = isOwn)

        startProfile(userId)
        startPosts(userId)
        if (!isOwn && currentUid != null) {
            checkFollowStatus(currentUid, userId)
        }
    }

    override fun onAuthUserChanged(user: com.example.scraply.data.auth.ScraplyUser?) {
        val uid = user?.uid
        val target = targetUserId ?: return
        val isOwn = uid != null && uid == target
        _state.value = _state.value.copy(isOwnProfile = isOwn)
        if (!isOwn && uid != null) {
            checkFollowStatus(uid, target)
        }
    }

    private fun startProfile(uid: String) {
        profileJob?.cancel()
        val repo = authRepository ?: return
        profileJob = repo.observeProfile(uid)
            .onEach { p ->
                if (p != null && followInFlight) {
                    // Keep the current followerCount while a follow/unfollow
                    // call hasn't finished – the Firestore snapshot may carry a
                    // stale value because the server-side increment hasn't been
                    // applied yet.
                    val kept = _state.value.profile?.followerCount ?: p.followerCount
                    _state.value = _state.value.copy(
                        profile = p.copy(followerCount = kept),
                    )
                } else {
                    _state.value = _state.value.copy(profile = p)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun startPosts(uid: String) {
        postsJob?.cancel()
        _state.value = _state.value.copy(posts = emptyList(), isPostsLoading = true)
        val social = socialRepository ?: run {
            _state.value = _state.value.copy(isPostsLoading = false)
            return
        }
        postsJob = social.observeMyPosts(uid)
            .onEach { rawPosts ->
                _state.value = _state.value.copy(
                    posts = rawPosts,
                    isPostsLoading = false,
                )
                // Hydrate authors + engagement in background
                val hydrated = runCatching {
                    val currentUid = authState.value.user?.uid
                    if (currentUid != null) {
                        social.hydrateFeedSnapshot(currentUid, rawPosts, rank = false)
                    } else {
                        social.hydratePostAuthors(rawPosts)
                    }
                }.getOrDefault(rawPosts)
                _state.value = _state.value.copy(posts = hydrated)
            }
            .launchIn(viewModelScope)
    }

    private fun checkFollowStatus(currentUid: String, targetUid: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isFollowLoading = true)
            val following = runCatching {
                socialRepository?.isFollowing(currentUid, targetUid) ?: false
            }.getOrDefault(false)
            _state.value = _state.value.copy(
                isFollowing = following,
                isFollowLoading = false,
            )
        }
    }

    fun toggleFollow() {
        val currentUid = authState.value.user?.uid ?: return
        val targetUid = targetUserId ?: return
        if (_state.value.isOwnProfile) return
        val social = socialRepository ?: return

        val wasFollowing = _state.value.isFollowing
        _state.value = _state.value.copy(isFollowing = !wasFollowing)
        followInFlight = true

        viewModelScope.launch {
            runCatching { social.toggleFollow(currentUid, targetUid) }
                .onFailure {
                    Log.e("UserProfileVM", "toggleFollow failed", it)
                    _state.value = _state.value.copy(isFollowing = wasFollowing)
                }
            // Let the observer take over again and restart it so it
            // re-emits the now-correct Firestore value (the previous
            // emission was suppressed while followInFlight was true).
            followInFlight = false
            targetUserId?.let { startProfile(it) }
        }
    }

    fun toggleLike(post: FeedPost) {
        val uid = authState.value.user?.uid ?: return
        val social = socialRepository ?: return
        pendingLikeIds.add(post.id)
        val optimisticLiked = !post.likedByMe
        val optimisticCount = (post.likeCount + if (optimisticLiked) 1 else -1).coerceAtLeast(0)
        _state.value = _state.value.copy(
            posts = _state.value.posts.map { item ->
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
                    Log.e("UserProfileVM", "toggleLike failed for post=${post.id}", it)
                    pendingLikeIds.remove(post.id)
                    _state.value = _state.value.copy(
                        posts = _state.value.posts.map { item ->
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
