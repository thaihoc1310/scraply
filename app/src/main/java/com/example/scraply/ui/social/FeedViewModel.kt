package com.example.scraply.ui.social

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
)

class FeedViewModel(
    authRepository: AuthRepository?,
    firestoreSync: FirestoreSyncRepository?,
    private val socialRepository: SocialRepository?,
) : BaseAuthViewModel(authRepository, firestoreSync) {

    private val _feed = MutableStateFlow(FeedUiState())
    val feed: StateFlow<FeedUiState> = _feed.asStateFlow()

    private var feedJob: Job? = null

    override fun onAuthUserChanged(user: ScraplyUser?) {
        if (user != null) startFeed(user.uid) else stopFeed()
    }

    private fun startFeed(uid: String) {
        feedJob?.cancel()
        val social = socialRepository ?: return
        feedJob = social.observeFeed(uid)
            .onEach { raw ->
                val hydrated = runCatching { social.hydratePostAuthors(raw) }.getOrDefault(raw)
                val ranked = runCatching { social.rankByFollows(uid, hydrated) }.getOrDefault(hydrated)
                _feed.value = _feed.value.copy(feed = ranked)
            }
            .launchIn(viewModelScope)
    }

    private fun stopFeed() {
        feedJob?.cancel(); feedJob = null
        _feed.value = _feed.value.copy(feed = emptyList())
    }

    fun toggleLike(post: FeedPost) {
        val social = socialRepository ?: return
        val uid = authState.value.user?.uid ?: return
        viewModelScope.launch { runCatching { social.toggleLike(post.id, uid) } }
    }
}
