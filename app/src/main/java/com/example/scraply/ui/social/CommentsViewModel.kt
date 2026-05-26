package com.example.scraply.ui.social

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FeedComment
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.SocialRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class CommentsUiState(
    val postId: String = "",
    val comments: List<FeedComment> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class CommentsViewModel(
    authRepository: AuthRepository?,
    firestoreSync: FirestoreSyncRepository?,
    private val socialRepository: SocialRepository?,
) : BaseAuthViewModel(authRepository, firestoreSync) {

    private val _state = MutableStateFlow(CommentsUiState())
    val state: StateFlow<CommentsUiState> = _state.asStateFlow()

    private var commentsJob: Job? = null

    override fun onAuthUserChanged(user: ScraplyUser?) {
        _state.value = _state.value.copy(error = null)
    }

    fun setPostId(postId: String) {
        if (postId.isBlank() || postId == _state.value.postId) return
        commentsJob?.cancel()
        _state.value = _state.value.copy(
            postId = postId,
            comments = emptyList(),
            isLoading = true,
            error = null,
        )
        val social = socialRepository ?: return
        commentsJob = social.observeComments(postId)
            .onEach { raw ->
                val hydrated = runCatching { social.hydrateCommentAuthors(raw) }.getOrDefault(raw)
                _state.value = _state.value.copy(comments = hydrated, isLoading = false)
            }
            .launchIn(viewModelScope)
    }

    fun updateInput(value: String) {
        _state.value = _state.value.copy(input = value, error = null)
    }

    fun submitComment() {
        val social = socialRepository ?: return
        val postId = _state.value.postId
        val uid = authState.value.user?.uid
        val text = _state.value.input.trim()
        if (postId.isBlank() || text.isBlank()) return
        if (uid == null) {
            _state.value = _state.value.copy(error = "Sign in to comment.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(sending = true, error = null)
            runCatching { social.addComment(postId, uid, text) }
                .onSuccess {
                    _state.value = _state.value.copy(input = "", sending = false)
                }
                .onFailure { t ->
                    Log.e("CommentsVM", "addComment failed for post=$postId", t)
                    _state.value = _state.value.copy(
                        sending = false,
                        error = t.message ?: "Failed to send comment.",
                    )
                }
        }
    }
}
