package com.example.scraply.ui.vm

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scraply.ScraplyApp
import com.example.scraply.data.ScraplyContainer
import com.example.scraply.ui.calendar.CalendarViewModel
import com.example.scraply.ui.collection.CollectionsViewModel
import com.example.scraply.ui.editor.EditorViewModel
import com.example.scraply.ui.social.FeedViewModel
import com.example.scraply.ui.social.ProfileViewModel
import com.example.scraply.ui.stamp.StampCaptureViewModel

class ScraplyViewModelFactory(
    private val container: ScraplyContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            StampCaptureViewModel::class.java ->
                StampCaptureViewModel(
                    appContext = container.appContext,
                    stampRepository = container.stampRepository,
                    collectionRepository = container.collectionRepository,
                ) as T
            CollectionsViewModel::class.java ->
                CollectionsViewModel(
                    stampRepository = container.stampRepository,
                    collectionRepository = container.collectionRepository,
                ) as T
            EditorViewModel::class.java ->
                EditorViewModel(
                    appContext = container.appContext,
                    projectRepository = container.projectRepository,
                    stampRepository = container.stampRepository,
                    authRepository = container.authRepository,
                    socialRepository = container.socialRepository,
                ) as T
            CalendarViewModel::class.java ->
                CalendarViewModel(container.stampRepository) as T
            FeedViewModel::class.java ->
                FeedViewModel(
                    authRepository = container.authRepository,
                    firestoreSync = container.firestoreSyncRepository,
                    socialRepository = container.socialRepository,
                ) as T
            ProfileViewModel::class.java ->
                ProfileViewModel(
                    authRepository = container.authRepository,
                    firestoreSync = container.firestoreSyncRepository,
                    socialRepository = container.socialRepository,
                    storageRepository = container.storageRepository,
                ) as T
            else -> throw IllegalArgumentException("Unknown VM: $modelClass")
        }
    }
}

@Composable
inline fun <reified T : ViewModel> scraplyViewModel(): T {
    val factory = ScraplyViewModelFactory(ScraplyApp.instance.container)
    return viewModel(factory = factory)
}
