package com.example.scraply.data.remote

import android.content.Context
import android.util.Log
import com.example.scraply.data.local.CollectionEntity
import com.example.scraply.data.local.CollectionStampEntity
import com.example.scraply.data.local.ProjectEntity
import com.example.scraply.data.local.ScraplyDatabase
import com.example.scraply.data.local.StampEntity
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Dual-storage sync layer (REQUIREMENTS §9): Room is the offline source of truth for the current
 * session; Firestore is the durable cross-device source of truth once signed in.
 *
 * Writes are local-first: callers still hit Room via the existing repositories; [pushStamp],
 * [pushCollection], [pushProject] mirror to Firestore and should be invoked from the repositories
 * after a successful local write when a user is signed in.
 *
 * On sign-in / fresh install the UI should call [pullAllForUser] once to hydrate Room from
 * Firestore; subsequent live updates arrive through Firestore offline persistence + snapshot
 * listeners registered by the repositories or ViewModels as needed.
 */
class FirestoreSyncRepository(
    private val appContext: Context,
    private val db: ScraplyDatabase,
    private val storage: StorageRepository,
) {
    private val firestore = Firebase.firestore

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)
    private fun stampsCol(uid: String) = userDoc(uid).collection("stamps")
    private fun collectionsCol(uid: String) = userDoc(uid).collection("collections")
    private fun projectsCol(uid: String) = userDoc(uid).collection("projects")

    suspend fun pushStamp(uid: String, stamp: StampEntity) = runCatching {
        val remoteUrl = stamp.imageUrl ?: run {
            if (stamp.imageUri.startsWith("http")) stamp.imageUri
            else storage.uploadStampImage(uid, stamp.id, stamp.imageUri)
        }
        val data = mapOf(
            "id" to stamp.id,
            "imageUrl" to remoteUrl,
            "frameType" to stamp.frameType,
            "title" to stamp.title,
            "caption" to stamp.caption,
            "createdAt" to stamp.createdAt,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        stampsCol(uid).document(stamp.id).set(data).await()
        if (stamp.imageUrl != remoteUrl) {
            db.stampDao().upsert(stamp.copy(imageUrl = remoteUrl, userId = uid))
        }
    }.onFailure { Log.w(TAG, "pushStamp failed: ${it.message}") }

    suspend fun deleteStamp(uid: String, stampId: String) = runCatching {
        stampsCol(uid).document(stampId).delete().await()
    }.onFailure { Log.w(TAG, "deleteStamp failed: ${it.message}") }

    suspend fun pushCollection(uid: String, c: CollectionEntity, stampIds: List<String>) = runCatching {
        val data = mapOf(
            "id" to c.id,
            "name" to c.name,
            "isDefault" to c.isDefault,
            "createdAt" to c.createdAt,
            "stampIds" to stampIds,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        collectionsCol(uid).document(c.id).set(data).await()
    }.onFailure { Log.w(TAG, "pushCollection failed: ${it.message}") }

    suspend fun deleteCollection(uid: String, collectionId: String) = runCatching {
        collectionsCol(uid).document(collectionId).delete().await()
    }.onFailure { Log.w(TAG, "deleteCollection failed: ${it.message}") }

    suspend fun pushProject(uid: String, p: ProjectEntity) = runCatching {
        val data = mapOf(
            "id" to p.id,
            "name" to p.name,
            "backgroundType" to p.backgroundType,
            "canvasJson" to p.canvasJson,
            "createdAt" to p.createdAt,
            "updatedAt" to p.updatedAt,
            "serverUpdatedAt" to FieldValue.serverTimestamp(),
        )
        projectsCol(uid).document(p.id).set(data).await()
    }.onFailure { Log.w(TAG, "pushProject failed: ${it.message}") }

    suspend fun deleteProject(uid: String, projectId: String) = runCatching {
        projectsCol(uid).document(projectId).delete().await()
    }.onFailure { Log.w(TAG, "deleteProject failed: ${it.message}") }

    /**
     * Associate any stamps/collections/projects that were created offline with this user and push
     * them to Firestore (one-time local→cloud migration, REQUIREMENTS §9 row "Sign-in after offline
     * use").
     */
    suspend fun migrateLocalToCloud(uid: String) = runCatching {
        val stamps = db.stampDao().getAllSync().map { if (it.userId == null) it.copy(userId = uid) else it }
        stamps.forEach { db.stampDao().upsert(it); pushStamp(uid, it) }

        val collections = db.collectionDao().getAllSync().map { if (it.userId == null) it.copy(userId = uid) else it }
        collections.forEach { c ->
            db.collectionDao().upsert(c)
            val ids = db.collectionStampDao().stampIdsIn(c.id)
            pushCollection(uid, c, ids)
        }

        val projects = db.projectDao().getAllSync().map { if (it.userId == null) it.copy(userId = uid) else it }
        projects.forEach { db.projectDao().upsert(it); pushProject(uid, it) }
    }.onFailure { Log.w(TAG, "migrateLocalToCloud failed: ${it.message}") }

    /** Hydrate Room from Firestore after sign-in on a new device. */
    suspend fun pullAllForUser(uid: String) = runCatching {
        val stampDocs = stampsCol(uid).get().await().documents
        stampDocs.forEach { doc -> doc.toStamp(uid)?.let { (entity, remoteUrl) ->
            val local = downloadAndCacheStamp(uid, entity, remoteUrl)
            db.stampDao().upsert(local)
        } }

        val collectionDocs = collectionsCol(uid).get().await().documents
        collectionDocs.forEach { doc ->
            val c = doc.toCollection(uid) ?: return@forEach
            db.collectionDao().upsert(c)
            val stampIds = (doc.get("stampIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            db.collectionStampDao().removeAllInCollection(c.id)
            stampIds.forEach { sid -> db.collectionStampDao().insert(CollectionStampEntity(c.id, sid)) }
        }

        val projectDocs = projectsCol(uid).get().await().documents
        projectDocs.forEach { doc ->
            val p = doc.toProject(uid) ?: return@forEach
            db.projectDao().upsert(p)
        }
    }.onFailure { Log.w(TAG, "pullAllForUser failed: ${it.message}") }

    private suspend fun downloadAndCacheStamp(uid: String, entity: StampEntity, remoteUrl: String): StampEntity {
        val cached = File(appContext.filesDir, "stamps/${entity.id}.png")
        if (!cached.exists()) {
            cached.parentFile?.mkdirs()
            try { storage.downloadToFile(remoteUrl, cached) }
            catch (t: Throwable) { Log.w(TAG, "download stamp ${entity.id} failed: ${t.message}") }
        }
        return entity.copy(imageUri = cached.absolutePath, imageUrl = remoteUrl, userId = uid)
    }

    private fun DocumentSnapshot.toStamp(uid: String): Pair<StampEntity, String>? {
        val id = getString("id") ?: this.id
        val url = getString("imageUrl") ?: return null
        return StampEntity(
            id = id,
            imageUri = url,
            imageUrl = url,
            frameType = getString("frameType") ?: "postage_stamp",
            title = getString("title"),
            caption = getString("caption"),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            userId = uid,
        ) to url
    }

    private fun DocumentSnapshot.toCollection(uid: String): CollectionEntity? {
        val id = getString("id") ?: this.id
        val name = getString("name") ?: return null
        return CollectionEntity(
            id = id,
            name = name,
            isDefault = getBoolean("isDefault") ?: false,
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            userId = uid,
        )
    }

    private fun DocumentSnapshot.toProject(uid: String): ProjectEntity? {
        val id = getString("id") ?: this.id
        val name = getString("name") ?: return null
        val now = System.currentTimeMillis()
        return ProjectEntity(
            id = id,
            name = name,
            backgroundType = getString("backgroundType") ?: "paper",
            canvasJson = getString("canvasJson") ?: "{\"elements\":[]}",
            createdAt = getLong("createdAt") ?: now,
            updatedAt = getLong("updatedAt") ?: now,
            userId = uid,
        )
    }

    companion object { private const val TAG = "FirestoreSync" }
}
