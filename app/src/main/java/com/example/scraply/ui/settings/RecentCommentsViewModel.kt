package com.example.scraply.ui.settings

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.SocialRepository
import com.example.scraply.data.remote.UserCommentGroup
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

data class RecentCommentsUiState(
    val commentGroups: List<UserCommentGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedCommentIds: Set<String> = emptySet(),
    val limit: Long = 20L,
    val hasMore: Boolean = true,
)

class RecentCommentsViewModel(
    authRepository: AuthRepository?,
    firestoreSync: FirestoreSyncRepository?,
    private val socialRepository: SocialRepository?,
) : BaseAuthViewModel(authRepository, firestoreSync) {

    private val _uiState = MutableStateFlow(RecentCommentsUiState())
    val uiState: StateFlow<RecentCommentsUiState> = _uiState.asStateFlow()

    private var commentsJob: Job? = null
    private var currentUid: String? = null

    override fun onAuthUserChanged(user: ScraplyUser?) {
        if (user != null) startObserving(user.uid) else stopObserving()
    }

    private fun startObserving(uid: String) {
        currentUid = uid
        _uiState.value = RecentCommentsUiState()
        observeCurrentPage(uid, 20L)
    }

    private fun observeCurrentPage(uid: String, limit: Long) {
        commentsJob?.cancel()
        val social = socialRepository ?: return

        commentsJob = social.observeUserComments(uid, limit)
            .onEach { items ->
                val allCommentsCount = items.sumOf { it.comments.size }
                val hasMore = allCommentsCount >= limit

                _uiState.value = _uiState.value.copy(
                    commentGroups = items,
                    isLoading = false,
                    limit = limit,
                    hasMore = hasMore,
                    selectedCommentIds = _uiState.value.selectedCommentIds.intersect(
                        items.flatMap { it.comments }.map { it.commentId }.toSet()
                    )
                )
            }
            .launchIn(viewModelScope)
    }

    fun loadMoreComments() {
        val uid = currentUid ?: return
        val currentState = _uiState.value
        if (currentState.isLoading || !currentState.hasMore) return

        _uiState.value = _uiState.value.copy(isLoading = true)
        val newLimit = currentState.limit + 20L
        observeCurrentPage(uid, newLimit)
    }

    private fun stopObserving() {
        commentsJob?.cancel(); commentsJob = null
        currentUid = null
        _uiState.value = RecentCommentsUiState()
    }

    fun setSelectionMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = enabled,
            selectedCommentIds = emptySet()
        )
    }

    fun toggleCommentSelection(commentId: String) {
        val currentSelected = _uiState.value.selectedCommentIds
        val newSelected = if (commentId in currentSelected) {
            currentSelected - commentId
        } else {
            currentSelected + commentId
        }
        _uiState.value = _uiState.value.copy(selectedCommentIds = newSelected)
    }

    fun deleteSelectedComments() {
        val social = socialRepository ?: return
        val toDeleteIds = _uiState.value.selectedCommentIds
        if (toDeleteIds.isEmpty()) return

        val toDeleteList = _uiState.value.commentGroups.flatMap { group ->
            group.comments.filter { it.commentId in toDeleteIds }.map { group.postId to it.commentId }
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            runCatching {
                toDeleteList.map { (postId, commentId) ->
                    async {
                        social.deleteComment(postId, commentId)
                    }
                }.awaitAll()
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSelectionMode = false,
                    selectedCommentIds = emptySet()
                )
            }.onFailure { e ->
                Log.e("RecentCommentsVM", "Failed to delete comments batch", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
