package com.example.scraply.ui.social

import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedLikeUser
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.SocialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class LikesUiState(
    val postId: String = "",
    val likes: List<FeedLikeUser> = emptyList(),
    val isLoading: Boolean = false,
)

class LikesViewModel(
    authRepository: AuthRepository?,
    firestoreSync: FirestoreSyncRepository?,
    private val socialRepository: SocialRepository?,
) : BaseAuthViewModel(authRepository, firestoreSync) {

    private val _state = MutableStateFlow(LikesUiState())
    val state: StateFlow<LikesUiState> = _state.asStateFlow()

    private var likesJob: Job? = null

    override fun onAuthUserChanged(user: ScraplyUser?) {
        if (user == null) {
            likesJob?.cancel()
            likesJob = null
            _state.value = LikesUiState()
        }
    }

    fun setPostId(postId: String) {
        if (postId.isBlank() || postId == _state.value.postId) return
        likesJob?.cancel()
        _state.value = _state.value.copy(
            postId = postId,
            likes = emptyList(),
            isLoading = true,
        )
        val social = socialRepository ?: return
        likesJob = social.observeLikes(postId)
            .onEach { raw ->
                val hydrated = runCatching { social.hydrateLikeUsers(raw) }.getOrDefault(raw)
                _state.value = _state.value.copy(likes = hydrated, isLoading = false)
            }
            .launchIn(viewModelScope)
    }
}
