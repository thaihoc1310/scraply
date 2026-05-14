package com.example.scraply.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scraply.data.model.Stamp
import com.example.scraply.data.model.StampCollection
import com.example.scraply.data.repository.CollectionRepository
import com.example.scraply.data.repository.StampRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CollectionCard(
    val collection: StampCollection,
    val stampCount: Int,
    val thumbUris: List<String>,
)

class CollectionsViewModel(
    private val stampRepository: StampRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {

    private val _cards = MutableStateFlow<List<CollectionCard>>(emptyList())
    val cards: StateFlow<List<CollectionCard>> = _cards.asStateFlow()

    private val _allStamps = MutableStateFlow<List<Stamp>>(emptyList())
    val allStamps: StateFlow<List<Stamp>> = _allStamps.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                collectionRepository.observeAll(),
                stampRepository.observeAll(),
                collectionRepository.observeStampLinks(),
            ) { collections, stamps, links ->
                _allStamps.value = stamps
                lastCollections = collections

                collections.map { collection ->
                    val stampIds = links
                        .asSequence()
                        .filter { it.collectionId == collection.id }
                        .map { it.stampId }
                        .toSet()
                    val filtered = stamps.filter { it.id in stampIds }
                    CollectionCard(
                        collection = collection,
                        stampCount = filtered.size,
                        thumbUris = filtered.take(4).map { it.imageUri },
                    )
                }
            }.collect { cards ->
                _cards.value = cards
            }
        }
    }

    private var lastCollections: List<StampCollection> = emptyList()

    fun stampsIn(collectionId: String) = collectionRepository.observeStampsIn(collectionId)
    fun getStamp(id: String) = viewModelScope.launch { }

    fun createCollection(name: String) {
        viewModelScope.launch { collectionRepository.create(name) }
    }
    fun renameCollection(id: String, name: String) {
        viewModelScope.launch { collectionRepository.rename(id, name) }
    }
    fun deleteCollection(id: String) {
        viewModelScope.launch { collectionRepository.delete(id) }
    }
    fun addStampToCollection(collectionId: String, stampId: String) {
        viewModelScope.launch { collectionRepository.addStamp(collectionId, stampId) }
    }
    fun removeStampFromCollection(collectionId: String, stampId: String) {
        viewModelScope.launch { collectionRepository.removeStamp(collectionId, stampId) }
    }

    suspend fun loadStamp(id: String): Stamp? = stampRepository.getById(id)

    fun updateStamp(id: String, title: String?, caption: String?) {
        viewModelScope.launch { stampRepository.updateStamp(id, title, caption) }
    }

    fun deleteStamp(id: String) {
        viewModelScope.launch { stampRepository.deleteStamp(id) }
    }

    suspend fun getCollectionName(id: String): String? =
        collectionRepository.observeAll().let {
            lastCollections.firstOrNull { c -> c.id == id }?.name
        }
}
