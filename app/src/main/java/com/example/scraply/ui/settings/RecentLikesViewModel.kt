package com.example.scraply.ui.settings

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedPost
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.SocialRepository
import com.example.scraply.ui.social.BaseAuthViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class RecentLikesUiState(
    val posts: List<FeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedPostIds: Set<String> = emptySet(),
)

class RecentLikesViewModel(
    authRepository: AuthRepository?,
    firestoreSync: FirestoreSyncRepository?,
    private val socialRepository: SocialRepository?,
) : BaseAuthViewModel(authRepository, firestoreSync) {

    private val _uiState = MutableStateFlow(RecentLikesUiState())
    val uiState: StateFlow<RecentLikesUiState> = _uiState.asStateFlow()

    private var feedJob: Job? = null

    override fun onAuthUserChanged(user: ScraplyUser?) {
        if (user != null) startObserving(user.uid) else stopObserving()
    }

    private fun startObserving(uid: String) {
        feedJob?.cancel()
        val social = socialRepository ?: return
        _uiState.value = _uiState.value.copy(posts = emptyList(), isLoading = true)
        
        feedJob = social.observeFeed(uid)
            .onEach { raw ->
                if (raw.isEmpty()) {
                    _uiState.value = _uiState.value.copy(posts = emptyList(), isLoading = false)
                    return@onEach
                }

                // Hydrate engagement và tác giả để biết bài nào đã liked
                val hydrated = runCatching { social.hydrateFeedSnapshot(uid, raw, rank = false) }.getOrDefault(raw)
                val likedPosts = hydrated.filter { it.likedByMe }.sortedByDescending { it.likedAt }
                
                _uiState.value = _uiState.value.copy(
                    posts = likedPosts,
                    isLoading = false,
                    selectedPostIds = _uiState.value.selectedPostIds.intersect(likedPosts.map { it.id }.toSet())
                )
            }
            .launchIn(viewModelScope)
    }

    private fun stopObserving() {
        feedJob?.cancel(); feedJob = null
        _uiState.value = RecentLikesUiState()
    }

    fun setSelectionMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = enabled,
            selectedPostIds = emptySet()
        )
    }

    fun togglePostSelection(postId: String) {
        val currentSelected = _uiState.value.selectedPostIds
        val newSelected = if (postId in currentSelected) {
            currentSelected - postId
        } else {
            currentSelected + postId
        }
        _uiState.value = _uiState.value.copy(selectedPostIds = newSelected)
    }

    fun unlikeSelectedPosts() {
        val social = socialRepository ?: return
        val uid = authState.value.user?.uid ?: return
        val toUnlike = _uiState.value.selectedPostIds.toList()
        if (toUnlike.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            runCatching {
                toUnlike.map { postId ->
                    async {
                        social.toggleLike(postId, uid)
                    }
                }.awaitAll()
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSelectionMode = false,
                    selectedPostIds = emptySet()
                )
            }.onFailure { e ->
                Log.e("RecentLikesVM", "Failed to unlike batch", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
