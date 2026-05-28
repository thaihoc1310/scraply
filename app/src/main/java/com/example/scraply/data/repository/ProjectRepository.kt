package com.example.scraply.data.repository

import com.example.scraply.data.local.ProjectDao
import com.example.scraply.data.local.ProjectEntity
import com.example.scraply.data.model.ScrapbookProject
import com.example.scraply.data.model.toDomain
import com.example.scraply.data.remote.FirestoreSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val sync: FirestoreSyncRepository? = null,
    private val currentUid: () -> String? = { null },
    private val appScope: CoroutineScope? = null,
) {
    fun observeAll(): Flow<List<ScrapbookProject>> =
        projectDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): ScrapbookProject? = projectDao.getById(id)?.toDomain()

    suspend fun create(name: String): ScrapbookProject {
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            backgroundType = "paper",
            canvasJson = "{\"elements\":[]}",
            createdAt = now,
            updatedAt = now,
            userId = currentUid(),
        )
        projectDao.upsert(entity)
        pushAsync(entity)
        return entity.toDomain()
    }

    suspend fun rename(id: String, name: String): Boolean {
        val existing = projectDao.getById(id) ?: return false
        if (existing.name == name) return false
        val updated = existing.copy(name = name, updatedAt = System.currentTimeMillis())
        projectDao.update(updated)
        pushAsync(updated)
        return true
    }

    suspend fun updateCanvas(id: String, backgroundType: String, canvasJson: String): Boolean {
        val existing = projectDao.getById(id) ?: return false
        if (existing.backgroundType == backgroundType && existing.canvasJson == canvasJson) return false
        val updated = existing.copy(
            backgroundType = backgroundType,
            canvasJson = canvasJson,
            updatedAt = System.currentTimeMillis(),
        )
        projectDao.update(updated)
        pushAsync(updated)
        return true
    }

    suspend fun delete(id: String) {
        projectDao.deleteById(id)
        val uid = currentUid() ?: return
        val s = sync ?: return
        appScope?.launch { s.deleteProject(uid, id) }
    }

    private fun pushAsync(entity: ProjectEntity) {
        val uid = currentUid() ?: return
        val s = sync ?: return
        appScope?.launch { s.pushProject(uid, entity.copy(userId = uid)) }
    }
}
