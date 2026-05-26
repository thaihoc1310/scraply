package com.example.scraply.ui.stamp

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.model.Stamp
import com.example.scraply.data.model.StampCollection
import com.example.scraply.data.repository.CollectionRepository
import com.example.scraply.data.repository.StampRepository
import com.example.scraply.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StampCaptureViewModel(
    private val appContext: Context,
    private val stampRepository: StampRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    private val _collections = MutableStateFlow<List<StampCollection>>(emptyList())
    val collections: StateFlow<List<StampCollection>> = _collections.asStateFlow()

    init {
        viewModelScope.launch {
            collectionRepository.observeAll().collect { _collections.value = it }
        }
    }

    /**
     * Renders the captured photo into a stamp-shaped bitmap, writes it to internal storage,
     * and returns the resulting file URI string.
     */
    suspend fun renderAndSaveStampImage(
        sourceUri: Uri,
        translateX: Float,
        translateY: Float,
        scale: Float,
    ): String? = withContext(Dispatchers.IO) {
        val src = ImageUtils.loadBitmap(appContext, sourceUri) ?: return@withContext null
        val stamped = ImageUtils.renderPostageStamp(
            context = appContext,
            source = src,
            translateX = translateX,
            translateY = translateY,
            scale = scale,
        )
        ImageUtils.saveStampPng(appContext, stamped)
    }

    suspend fun saveStamp(
        imageUri: String,
        title: String?,
        caption: String?,
        extraCollectionIds: List<String>,
    ): Stamp = stampRepository.createStamp(
        imageUri = imageUri,
        title = title,
        caption = caption,
        extraCollectionIds = extraCollectionIds,
    )
}
