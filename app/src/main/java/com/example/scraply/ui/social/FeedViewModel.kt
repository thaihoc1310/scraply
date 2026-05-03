package com.example.scraply.ui.social

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.SocialRepository
import kotlinx.coroutines.Job
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
                val merged = raw.map { post ->
                    if (post.id in pendingLikeIds) {
                        val current = _feed.value.feed.firstOrNull { it.id == post.id }
                        if (current != null) post.copy(likedByMe = current.likedByMe, likeCount = current.likeCount) else post
                    } else post
                }
                _feed.value = _feed.value.copy(feed = merged, isLoading = false)
                val hydrated = runCatching { social.hydratePostAuthors(merged) }.getOrDefault(merged)
                val enriched = runCatching { social.hydratePostEngagement(uid, hydrated) }.getOrDefault(hydrated)
                val ranked = runCatching { social.rankByFollows(uid, enriched) }.getOrDefault(enriched)
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
                .onSuccess { pendingLikeIds.remove(post.id) }
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
}
