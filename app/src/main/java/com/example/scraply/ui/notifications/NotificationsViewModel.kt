package com.example.scraply.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.auth.ScraplyUser
import com.example.scraply.data.remote.NotificationItem
import com.example.scraply.data.remote.NotificationsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isSignedIn: Boolean = false,
    val items: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
)

class NotificationsViewModel(
    private val authRepository: AuthRepository?,
    private val notificationsRepository: NotificationsRepository?,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private var itemsJob: Job? = null
    private var unreadJob: Job? = null

    init {
        authRepository?.currentUserFlow()
            ?.onEach { u -> onUser(u) }
            ?.launchIn(viewModelScope)
    }

    private fun onUser(user: ScraplyUser?) {
        itemsJob?.cancel(); unreadJob?.cancel()
        if (user == null) {
            _state.value = NotificationsUiState()
            return
        }
        val repo = notificationsRepository ?: return
        _state.value = _state.value.copy(isSignedIn = true)
        itemsJob = repo.observe(user.uid)
            .onEach { items -> _state.value = _state.value.copy(items = items) }
            .launchIn(viewModelScope)
        unreadJob = repo.unreadCount(user.uid)
            .onEach { count -> _state.value = _state.value.copy(unreadCount = count) }
            .launchIn(viewModelScope)
    }

    fun markAllRead() {
        val uid = authRepository?.currentUserId ?: return
        val repo = notificationsRepository ?: return
        viewModelScope.launch { runCatching { repo.markAllRead(uid) } }
    }

    fun markRead(item: NotificationItem) {
        if (item.read) return
        val uid = authRepository?.currentUserId ?: return
        val repo = notificationsRepository ?: return
        viewModelScope.launch { runCatching { repo.markRead(uid, item.id) } }
    }
}
