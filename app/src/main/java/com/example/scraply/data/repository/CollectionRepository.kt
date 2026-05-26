package com.example.scraply.data.repository

import com.example.scraply.data.local.CollectionDao
import com.example.scraply.data.local.CollectionEntity
import com.example.scraply.data.local.CollectionStampDao
import com.example.scraply.data.local.CollectionStampEntity
import com.example.scraply.data.model.Stamp
import com.example.scraply.data.model.StampCollection
import com.example.scraply.data.model.toDomain
import com.example.scraply.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class CollectionRepository(
    private val collectionDao: CollectionDao,
    private val collectionStampDao: CollectionStampDao,
    private val sync: FirestoreSyncRepository? = null,
    private val currentUid: () -> String? = { null },
    private val appScope: CoroutineScope? = null,
) {
    fun observeAll(): Flow<List<StampCollection>> =
        collectionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeStampsIn(collectionId: String): Flow<List<Stamp>> =
        collectionStampDao.observeStampsIn(collectionId).map { list -> list.map { it.toDomain() } }

    fun observeStampLinks(): Flow<List<CollectionStampEntity>> =
        collectionStampDao.observeAll()

    suspend fun ensureDefaultCollection(): StampCollection {
        val existing = collectionDao.getDefault()
        if (existing != null) return existing.toDomain()
        val uid = currentUid()
        val entity = CollectionEntity(
            id = UUID.randomUUID().toString(),
            name = "All Stamps",
            isDefault = true,
            createdAt = System.currentTimeMillis(),
            userId = uid,
        )
        collectionDao.upsert(entity)
        pushAsync(entity)
        return entity.toDomain()
    }

    suspend fun create(name: String): StampCollection {
        val uid = currentUid()
        val entity = CollectionEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            isDefault = false,
            createdAt = System.currentTimeMillis(),
            userId = uid,
        )
        collectionDao.upsert(entity)
        pushAsync(entity)
        return entity.toDomain()
    }

    suspend fun rename(id: String, name: String) {
        val existing = collectionDao.getById(id) ?: return
        if (existing.isDefault) return
        val updated = existing.copy(name = name)
        collectionDao.update(updated)
        pushAsync(updated)
    }

    suspend fun delete(id: String) {
        collectionStampDao.removeAllInCollection(id)
        collectionDao.deleteById(id)
        val uid = currentUid() ?: return
        val s = sync ?: return
        appScope?.launch { s.deleteCollection(uid, id) }
    }

    suspend fun addStamp(collectionId: String, stampId: String) {
        collectionStampDao.insert(CollectionStampEntity(collectionId, stampId))
        collectionDao.getById(collectionId)?.let { pushAsync(it) }
    }

    suspend fun removeStamp(collectionId: String, stampId: String) {
        collectionStampDao.remove(collectionId, stampId)
        collectionDao.getById(collectionId)?.let { pushAsync(it) }
    }

    suspend fun stampIdsIn(collectionId: String): List<String> =
        collectionStampDao.stampIdsIn(collectionId)

    suspend fun snapshot(id: String): StampCollection? = collectionDao.getById(id)?.toDomain()

    private fun pushAsync(entity: CollectionEntity) {
        val uid = currentUid() ?: return
        val s = sync ?: return
        appScope?.launch {
            val ids = collectionStampDao.stampIdsIn(entity.id)
            s.pushCollection(uid, entity.copy(userId = uid), ids)
        }
    }
}
