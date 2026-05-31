package com.example.scraply.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.model.CanvasElement
import com.example.scraply.data.model.CanvasElementType
import com.example.scraply.data.model.CanvasState
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.model.ScrapbookProject
import com.example.scraply.data.model.Stamp
import com.example.scraply.data.remote.SocialRepository
import com.example.scraply.data.repository.ProjectRepository
import com.example.scraply.data.repository.StampRepository
import com.example.scraply.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ProjectCard(
    val project: ScrapbookProject,
    val elementCount: Int,
    val elements: List<CanvasElement> = emptyList()
)

sealed class PublishState {
    data object Idle : PublishState()
    data object Publishing : PublishState()
    data class Error(val message: String) : PublishState()
    data class Success(val postId: String) : PublishState()
}

private data class UndoState(
    val canvasState: CanvasState,
    val background: String
)

class EditorViewModel(
    private val appContext: Context,
    private val projectRepository: ProjectRepository,
    private val stampRepository: StampRepository,
    private val authRepository: AuthRepository? = null,
    private val socialRepository: SocialRepository? = null,
) : ViewModel() {

    private val _publish = MutableStateFlow<PublishState>(PublishState.Idle)
    val publishState: StateFlow<PublishState> = _publish.asStateFlow()

    val canPublish: Boolean get() = authRepository != null && socialRepository != null

    private val _projects = MutableStateFlow<List<ProjectCard>>(emptyList())
    val projects: StateFlow<List<ProjectCard>> = _projects.asStateFlow()

    private val _projectFocusRequest = MutableStateFlow<String?>(null)
    val projectFocusRequest: StateFlow<String?> = _projectFocusRequest.asStateFlow()

    private val _current = MutableStateFlow<ScrapbookProject?>(null)
    val current: StateFlow<ScrapbookProject?> = _current.asStateFlow()

    private val _canvas = MutableStateFlow(CanvasState())
    val canvas: StateFlow<CanvasState> = _canvas.asStateFlow()

    private val _stamps = MutableStateFlow<List<Stamp>>(emptyList())
    val stamps: StateFlow<List<Stamp>> = _stamps.asStateFlow()

    private val _background = MutableStateFlow("paper")
    val background: StateFlow<String> = _background.asStateFlow()

    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()

    private val undoStack: ArrayDeque<UndoState> = ArrayDeque()

    init {
        viewModelScope.launch {
            projectRepository.observeAll().collect { list ->
                _projects.value = list.map { p ->
                    val elements = CanvasState.fromJson(p.canvasJson).elements
                    ProjectCard(p, elements.size, elements)
                }
            }
        }
        viewModelScope.launch {
            stampRepository.observeAll().collect { _stamps.value = it }
        }
    }

    fun loadProject(id: String) {
        viewModelScope.launch {
            projectRepository.getById(id)?.let {
                _current.value = it
                _background.value = it.backgroundType
                _canvas.value = CanvasState.fromJson(it.canvasJson)
                undoStack.clear()
            }
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            val created = projectRepository.create(name)
            _projectFocusRequest.value = created.id
        }
    }
    fun renameProject(id: String, name: String) {
        viewModelScope.launch {
            if (projectRepository.rename(id, name)) {
                _projectFocusRequest.value = id
            }
        }
    }
    fun deleteProject(id: String) {
        viewModelScope.launch { projectRepository.delete(id) }
    }

    fun consumeProjectFocusRequest(id: String) {
        if (_projectFocusRequest.value == id) {
            _projectFocusRequest.value = null
        }
    }

    fun saveCurrent() {
        val p = _current.value ?: return
        val backgroundType = _background.value
        val canvasJson = CanvasState.toJson(_canvas.value)
        viewModelScope.launch {
            if (projectRepository.updateCanvas(
                id = p.id,
                backgroundType = backgroundType,
                canvasJson = canvasJson,
            )) {
                _projectFocusRequest.value = p.id
            }
        }
    }

    fun setBackground(type: String) {
        pushUndo()
        _background.value = type
    }

    fun setAspectRatio(ratio: Float) {
        pushUndo()
        _canvas.value = _canvas.value.copy(
            elements = _canvas.value.elements.map { EditorGeometry.fitStampInsideCanvas(it, ratio) },
            aspectRatio = ratio,
        )
    }

    private fun pushUndo() {
        undoStack.addLast(UndoState(_canvas.value, _background.value))
        if (undoStack.size > 40) undoStack.removeFirst()
    }

    fun undo() {
        if (preTransformState != null) {
            val editingId = _selectedId.value
            val editingElement = preTransformState!!.elements.firstOrNull { it.id == editingId }
            if (editingElement != null && editingElement.type == CanvasElementType.TEXT && editingElement.text.isEmpty()) {
                // Element was empty before editing started (e.g. newly created)
                // Discard temporary edit state to allow stack pop to delete it
                preTransformState = null
            } else {
                _canvas.value = preTransformState!!
                preTransformState = null
                return
            }
        }

        if (undoStack.isNotEmpty()) {
            val popped = undoStack.removeLast()
            _canvas.value = popped.canvasState
            _background.value = popped.background
        }
    }

    fun selectElement(id: String?) {
        _selectedId.value = id
    }

    fun addElement(type: CanvasElementType, assetKey: String = "", stampId: String? = null, text: String = "") {
        pushUndo()
        val nextZ = (_canvas.value.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        val el = CanvasElement(
            id = UUID.randomUUID().toString(),
            type = type,
            assetKey = assetKey,
            stampId = stampId,
            text = text,
            zIndex = nextZ,
        )
        _canvas.value = _canvas.value.copy(elements = _canvas.value.elements + el)
        _selectedId.value = el.id
    }

    fun updateElement(id: String, saveUndo: Boolean = false, transform: (CanvasElement) -> CanvasElement) {
        if (saveUndo) {
            pushUndo()
        }
        _canvas.value = _canvas.value.copy(
            elements = _canvas.value.elements.map { if (it.id == id) transform(it) else it },
        )
    }

    private var preTransformState: CanvasState? = null

    fun onTransformStart() {
        if (preTransformState == null) {
            preTransformState = _canvas.value
        }
    }

    fun onTransformEnd() {
        preTransformState?.let {
            if (it != _canvas.value) {
                undoStack.addLast(UndoState(it, _background.value))
                if (undoStack.size > 40) undoStack.removeFirst()
            }
        }
        preTransformState = null
    }

    fun bringToFront(id: String) {
        pushUndo()
        val maxZ = (_canvas.value.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        _canvas.value = _canvas.value.copy(
            elements = _canvas.value.elements.map { if (it.id == id) it.copy(zIndex = maxZ) else it },
        )
    }

    fun sendToBack(id: String) {
        pushUndo()
        val minZ = (_canvas.value.elements.minOfOrNull { it.zIndex } ?: 0) - 1
        _canvas.value = _canvas.value.copy(
            elements = _canvas.value.elements.map { if (it.id == id) it.copy(zIndex = minZ) else it },
        )
    }

    fun deleteElement(id: String) {
        pushUndo()
        _canvas.value = _canvas.value.copy(
            elements = _canvas.value.elements.filterNot { it.id == id },
        )
        if (_selectedId.value == id) {
            _selectedId.value = null
            preTransformState = null
        }
    }

    suspend fun shareBitmap(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val dir = File(appContext.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "share_${System.currentTimeMillis()}.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun dismissPublishState() {
        _publish.value = PublishState.Idle
    }

    /**
     * Publishes the current scrapbook project to the shared feed. Requires the user to be signed
     * in (checked against [AuthRepository.currentUserId]).
     *
     *  1. The provided rendered [bitmap] of the canvas is saved as a PNG in the cache dir.
     *  2. It is uploaded through [SocialRepository.publishScrapbook], which handles Supabase
     *     storage + a `published_scrapbooks/{id}` Firestore document.
     *  3. The resulting post shows up in the Feed tab for everyone, and in the Profile tab grid
     *     for the author.
     */
    fun publishToFeed(bitmap: Bitmap, title: String? = null, description: String? = null) {
        val project = _current.value ?: return
        val auth = authRepository ?: run {
            _publish.value = PublishState.Error("Firebase is not configured.")
            return
        }
        val social = socialRepository ?: run {
            _publish.value = PublishState.Error("Firebase is not configured.")
            return
        }
        val uid = auth.currentUserId ?: run {
            _publish.value = PublishState.Error("Sign in from the Profile tab to publish.")
            return
        }
        _publish.value = PublishState.Publishing
        viewModelScope.launch {
            val result = runCatching {
                val dir = File(appContext.cacheDir, "posts").apply { mkdirs() }
                val file = File(dir, "post_${System.currentTimeMillis()}.png")
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                social.publishScrapbook(
                    uid = uid,
                    projectId = project.id,
                    localImagePath = file.absolutePath,
                    canvasJson = CanvasState.toJson(_canvas.value),
                    title = title,
                    description = description,
                )
            }
            result
                .onSuccess { postId -> _publish.value = PublishState.Success(postId) }
                .onFailure { t ->
                    _publish.value = PublishState.Error(t.message ?: "Failed to publish.")
                }
        }
    }
}
