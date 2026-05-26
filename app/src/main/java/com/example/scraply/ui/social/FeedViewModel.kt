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

data class FeedUiState(
    val feed: List<FeedPost> = emptyList(),
    val isLoading: Boolean = false,
)

class FeedViewModel(
    authRepository: AuthRepository?,
    firestoreSync: FirestoreSyncRepository?,
    private val socialRepository: SocialRepository?,
) : BaseAuthViewModel(authRepository, firestoreSync) {

    private val _feed = MutableStateFlow(FeedUiState())
    val feed: StateFlow<FeedUiState> = _feed.asStateFlow()

    private var feedJob: Job? = null
    private val pendingLikeIds = mutableSetOf<String>()

    override fun onAuthUserChanged(user: ScraplyUser?) {
        if (user != null) startFeed(user.uid) else stopFeed()
    }

    private fun startFeed(uid: String) {
        feedJob?.cancel()
        val social = socialRepository ?: return
        _feed.value = _feed.value.copy(feed = emptyList(), isLoading = true)
        feedJob = social.observeFeed(uid)
            .onEach { raw ->
                if (raw.isEmpty()) {
                    _feed.value = _feed.value.copy(feed = emptyList(), isLoading = false)
                    return@onEach
                }

                val currentFeed = _feed.value.feed
                val merged = raw.map { post ->
                    val existing = currentFeed.find { it.id == post.id }
                    // Preserve existing hydration info (username/avatar) to avoid flickering to encoded IDs
                    var updated = post.copy(
                        username = existing?.username,
                        avatarUrl = existing?.avatarUrl,
                        likedByMe = existing?.likedByMe ?: post.likedByMe,
                        savedByMe = existing?.savedByMe ?: post.savedByMe,
                        previewComments = existing?.previewComments.orEmpty(),
                    )

                    // If we have a pending like/unlike, keep the optimistic state to prevent "jumping"
                    if (post.id in pendingLikeIds && existing != null) {
                        updated = updated.copy(
                            likedByMe = existing.likedByMe,
                            likeCount = existing.likeCount
                        )
                    }
                    updated
                }

                // First update with merged/optimistic data so UI stays responsive and names don't flicker
                _feed.value = _feed.value.copy(feed = merged, isLoading = false)

                // Then perform hydration in background
                val hydrated = runCatching { social.hydratePostAuthors(merged) }.getOrDefault(merged)
                val enriched = runCatching { social.hydratePostEngagement(uid, hydrated) }.getOrDefault(hydrated)
                val withPreviews = runCatching { social.hydratePostCommentPreviews(enriched) }.getOrDefault(enriched)
                val ranked = runCatching { social.rankByFollows(uid, withPreviews) }.getOrDefault(withPreviews)
                _feed.value = _feed.value.copy(feed = ranked, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    private fun stopFeed() {
        feedJob?.cancel(); feedJob = null
        _feed.value = _feed.value.copy(feed = emptyList(), isLoading = false)
    }

    fun toggleLike(post: FeedPost) {
        val social = socialRepository ?: return
        val uid = authState.value.user?.uid ?: return
        pendingLikeIds.add(post.id)
        val optimisticLiked = !post.likedByMe
        val optimisticCount = (post.likeCount + if (optimisticLiked) 1 else -1).coerceAtLeast(0)
        _feed.value = _feed.value.copy(
            feed = _feed.value.feed.map { item ->
                if (item.id == post.id) item.copy(likedByMe = optimisticLiked, likeCount = optimisticCount) else item
            }
        )
        viewModelScope.launch {
            runCatching { social.toggleLike(post.id, uid) }
                .onSuccess {
                    // Small delay ensures the next Firestore snapshot has incorporated the change, preventing count jump
                    delay(800)
                    pendingLikeIds.remove(post.id)
                }
                .onFailure {
                    Log.e("FeedVM", "toggleLike failed for post=${post.id}", it)
                    pendingLikeIds.remove(post.id)
                    _feed.value = _feed.value.copy(
                        feed = _feed.value.feed.map { item ->
                            if (item.id == post.id) item.copy(likedByMe = post.likedByMe, likeCount = post.likeCount) else item
                        }
                    )
                }
        }
    }

    fun updatePostDetails(post: FeedPost, title: String, description: String) {
        val social = socialRepository ?: return
        val uid = authState.value.user?.uid ?: return
        val cleanTitle = title.trim().take(1000).ifBlank { null }
        val cleanDescription = description.trim().take(1000).ifBlank { null }
        val previousFeed = _feed.value.feed

        _feed.value = _feed.value.copy(
            feed = previousFeed.map { item ->
                if (item.id == post.id) {
                    item.copy(title = cleanTitle, description = cleanDescription)
                } else {
                    item
                }
            },
        )
        viewModelScope.launch {
            runCatching { social.updatePostDetails(post.id, uid, title, description) }
                .onFailure {
                    Log.e("FeedVM", "updatePostDetails failed for post=${post.id}", it)
                    _feed.value = _feed.value.copy(feed = previousFeed)
                }
        }
    }

    fun deletePost(post: FeedPost) {
        val social = socialRepository ?: return
        val uid = authState.value.user?.uid ?: return
        val previousFeed = _feed.value.feed

        _feed.value = _feed.value.copy(feed = previousFeed.filterNot { it.id == post.id })
        viewModelScope.launch {
            runCatching { social.deletePost(post.id, uid) }
                .onFailure {
                    Log.e("FeedVM", "deletePost failed for post=${post.id}", it)
                    _feed.value = _feed.value.copy(feed = previousFeed)
                }
        }
    }
}
