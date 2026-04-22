package com.example.scraply.data.repository

import com.example.scraply.data.local.CollectionStampDao
import com.example.scraply.data.local.CollectionStampEntity
import com.example.scraply.data.local.StampDao
import com.example.scraply.data.local.StampEntity
import com.example.scraply.data.model.Stamp
import com.example.scraply.data.model.toDomain
import com.example.scraply.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class StampRepository(
    private val stampDao: StampDao,
    private val collectionStampDao: CollectionStampDao,
    private val collectionRepository: CollectionRepository,
    private val sync: FirestoreSyncRepository? = null,
    private val currentUid: () -> String? = { null },
    private val appScope: CoroutineScope? = null,
) {
    fun observeAll(): Flow<List<Stamp>> =
        stampDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByDateRange(start: Long, end: Long): Flow<List<Stamp>> =
        stampDao.observeByDateRange(start, end).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): Stamp? = stampDao.getById(id)?.toDomain()

    suspend fun createStamp(
        imageUri: String,
        title: String?,
        caption: String?,
        extraCollectionIds: List<String> = emptyList(),
    ): Stamp {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val uid = currentUid()
        val entity = StampEntity(
            id = id,
            imageUri = imageUri,
            frameType = "postage_stamp",
            title = title?.ifBlank { null },
            caption = caption?.ifBlank { null },
            createdAt = now,
            userId = uid,
        )
        stampDao.upsert(entity)

        val defaultCol = collectionRepository.ensureDefaultCollection()
        val defaultId = defaultCol.id
        collectionStampDao.insert(CollectionStampEntity(defaultId, id))
        extraCollectionIds.forEach { collectionStampDao.insert(CollectionStampEntity(it, id)) }

        syncCreateOrUpdate(entity, listOf(defaultId) + extraCollectionIds)
        return entity.toDomain()
    }

    suspend fun updateStamp(id: String, title: String?, caption: String?) {
        val existing = stampDao.getById(id) ?: return
        val updated = existing.copy(
            title = title?.ifBlank { null },
            caption = caption?.ifBlank { null },
        )
        stampDao.update(updated)
        syncCreateOrUpdate(updated, affectedCollections = null)
    }

    suspend fun deleteStamp(id: String) {
        val existing = stampDao.getById(id)
        val affectedCollections = existing?.let {
            collectionStampDao.getAllSync().filter { cs -> cs.stampId == id }.map { it.collectionId }
        } ?: emptyList()
        collectionStampDao.removeAllForStamp(id)
        stampDao.deleteById(id)

        val uid = currentUid() ?: return
        val s = sync ?: return
        appScope?.launch {
            s.deleteStamp(uid, id)
            affectedCollections.forEach { cid ->
                val col = collectionRepository.snapshot(cid) ?: return@forEach
                val ids = collectionStampDao.stampIdsIn(cid)
                s.pushCollection(uid, com.example.scraply.data.local.CollectionEntity(
                    id = col.id, name = col.name, isDefault = col.isDefault,
                    createdAt = col.createdAt, userId = uid,
                ), ids)
            }
        }
    }

    private fun syncCreateOrUpdate(entity: StampEntity, affectedCollections: List<String>?) {
        val uid = currentUid() ?: return
        val s = sync ?: return
        appScope?.launch {
            s.pushStamp(uid, entity)
            affectedCollections?.forEach { cid ->
                val col = collectionRepository.snapshot(cid) ?: return@forEach
                val ids = collectionStampDao.stampIdsIn(cid)
                s.pushCollection(uid, com.example.scraply.data.local.CollectionEntity(
                    id = col.id, name = col.name, isDefault = col.isDefault,
                    createdAt = col.createdAt, userId = uid,
                ), ids)
            }
        }
    }
}
