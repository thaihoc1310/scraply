package com.example.scraply.data.remote

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

data class FeedPost(
    val id: String,
    val projectId: String,
    val imageUrl: String,
    val canvasJson: String,
    val userId: String,
    val username: String?,
    val avatarUrl: String?,
    val likeCount: Long,
    val commentCount: Long,
    val createdAt: Long,
    val title: String? = null,
    val description: String? = null,
    val likedByMe: Boolean = false,
    val savedByMe: Boolean = false,
    val previewComments: List<FeedComment> = emptyList(),
)

data class FeedComment(
    val id: String,
    val userId: String,
    val username: String?,
    val avatarUrl: String?,
    val text: String,
    val createdAt: Long,
)

data class FeedLikeUser(
    val userId: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val likedAt: Long,
)

/**
 * Firestore-backed social layer (REQUIREMENTS §8, §11.3).
 *
 *   published_scrapbooks/{id}            -- posts
 *   published_scrapbooks/{id}/comments   -- subcollection
 *   published_scrapbooks/{id}/likes/{uid}
 *   published_scrapbooks/{id}/saves/{uid}
 *   follows/{followerId_followeeId}
 */
class SocialRepository(
    private val appContext: Context,
    private val storage: StorageRepository,
    private val notifications: NotificationsRepository = NotificationsRepository(),
) {
    private val firestore = Firebase.firestore

    private val postsRef = firestore.collection("published_scrapbooks")
    private val followsRef = firestore.collection("follows")
    private val usersRef = firestore.collection("users")
    private val userSummaryLock = Mutex()
    private val userSummaryCache = mutableMapOf<String, UserSummary>()

    private data class UserSummary(
        val username: String?,
        val displayName: String?,
        val avatarUrl: String?,
    )

    private suspend fun actorProfile(uid: String): Triple<String?, String?, String?> {
        val summary = userSummaries(listOf(uid))[uid] ?: return Triple(uid, null, null)
        val name = summary.displayName ?: summary.username
        return Triple(uid, name, summary.avatarUrl)
    }

    private suspend fun postSummary(postId: String): Pair<String?, String?> {
        val snap = runCatching { postsRef.document(postId).get().await() }.getOrNull() ?: return null to null
        val owner = snap.getString("userId")
        val image = snap.getString("imageUrl")
        return owner to image
    }

    /**
     * Publishes a rendered scrapbook image + canvas data to the feed.
     * @param localImagePath path to the exported PNG on disk.
     */
    suspend fun publishScrapbook(
        uid: String,
        projectId: String,
        localImagePath: String,
        canvasJson: String,
        title: String? = null,
        description: String? = null,
    ): String {
        val docRef = postsRef.document()
        val postId = docRef.id
        val imageUrl = storage.uploadPostImage(uid, postId, localImagePath)
        val (_, actorName, actorAvatar) = actorProfile(uid)
        val data = mutableMapOf<String, Any?>(
            "id" to postId,
            "projectId" to projectId,
            "imageUrl" to imageUrl,
            "canvasJson" to canvasJson,
            "userId" to uid,
            "username" to actorName,
            "avatarUrl" to actorAvatar,
            "likeCount" to 0L,
            "commentCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        if (!title.isNullOrBlank()) data["title"] = title.take(1000)
        if (!description.isNullOrBlank()) data["description"] = description.take(1000)
        docRef.set(data).await()
        return postId
    }

    /**
     * Posts authored by the given uid, newest first. Drives the profile tab grid.
     */
    fun observeMyPosts(uid: String): Flow<List<FeedPost>> = callbackFlow {
        val q = postsRef
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(60)
        val reg = q.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            trySend(
                snap.documents.mapNotNull { d ->
                    FeedPost(
                        id = d.getString("id") ?: d.id,
                        projectId = d.getString("projectId") ?: return@mapNotNull null,
                        imageUrl = d.getString("imageUrl") ?: return@mapNotNull null,
                        canvasJson = d.getString("canvasJson") ?: "{\"elements\":[]}",
                        userId = d.getString("userId") ?: return@mapNotNull null,
                        username = d.getString("username"),
                        avatarUrl = d.getString("avatarUrl"),
                        likeCount = d.getLong("likeCount") ?: 0,
                        commentCount = d.getLong("commentCount") ?: 0,
                        createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        title = d.getString("title"),
                        description = d.getString("description"),
                    )
                }
            )
        }
        awaitClose { reg.remove() }
    }

    suspend fun deletePost(postId: String, uid: String) {
        val doc = postsRef.document(postId).get().await()
        require(doc.getString("userId") == uid) { "You can only delete your own posts." }
        postsRef.document(postId).delete().await()
    }

    suspend fun updatePostDetails(postId: String, uid: String, title: String, description: String) {
        val doc = postsRef.document(postId).get().await()
        require(doc.getString("userId") == uid) { "You can only edit your own posts." }

        val cleanTitle = title.trim().take(1000)
        val cleanDescription = description.trim().take(1000)
        val patch = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        patch["title"] = if (cleanTitle.isBlank()) FieldValue.delete() else cleanTitle
        patch["description"] = if (cleanDescription.isBlank()) FieldValue.delete() else cleanDescription
        postsRef.document(postId).update(patch).await()
    }

    /**
     * Returns a real-time feed. Sorted by createdAt desc; followed-users-first ordering is applied
     * client-side by pulling the follow set and reshuffling.
     */
    fun observeFeed(currentUid: String?): Flow<List<FeedPost>> = callbackFlow {
        val q = postsRef.orderBy("createdAt", Query.Direction.DESCENDING).limit(50)
        val registration = q.addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            val posts = snap.documents.mapNotNull { d ->
                FeedPost(
                    id = d.getString("id") ?: d.id,
                    projectId = d.getString("projectId") ?: return@mapNotNull null,
                    imageUrl = d.getString("imageUrl") ?: return@mapNotNull null,
                    canvasJson = d.getString("canvasJson") ?: "{\"elements\":[]}",
                    userId = d.getString("userId") ?: return@mapNotNull null,
                    username = d.getString("username"),
                    avatarUrl = d.getString("avatarUrl"),
                    likeCount = d.getLong("likeCount") ?: 0,
                    commentCount = d.getLong("commentCount") ?: 0,
                    createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    title = d.getString("title"),
                    description = d.getString("description"),
                )
            }
            trySend(posts)
        }
        awaitClose { registration.remove() }
    }

    private suspend fun userSummaries(uids: Collection<String>): Map<String, UserSummary> = coroutineScope {
        val cleanUids = uids.filter { it.isNotBlank() }.distinct()
        if (cleanUids.isEmpty()) return@coroutineScope emptyMap()

        val cached = userSummaryLock.withLock {
            cleanUids.mapNotNull { uid -> userSummaryCache[uid]?.let { uid to it } }.toMap()
        }
        val missing = cleanUids.filterNot { it in cached }
        if (missing.isNotEmpty()) {
            val loaded = missing.map { uid ->
                async {
                    val snap = runCatching { usersRef.document(uid).get().await() }.getOrNull()
                    uid to snap?.let {
                        UserSummary(
                            username = it.getString("username"),
                            displayName = it.getString("displayName"),
                            avatarUrl = it.getString("avatarUrl"),
                        )
                    }
                }
            }.awaitAll()
                .mapNotNull { (uid, summary) -> summary?.let { uid to it } }

            if (loaded.isNotEmpty()) {
                userSummaryLock.withLock {
                    loaded.forEach { (uid, summary) -> userSummaryCache[uid] = summary }
                }
            }
        }

        userSummaryLock.withLock {
            cleanUids.mapNotNull { uid -> userSummaryCache[uid]?.let { uid to it } }.toMap()
        }
    }

    suspend fun hydratePostAuthors(posts: List<FeedPost>): List<FeedPost> {
        val uids = posts
            .filter { it.username.isNullOrBlank() || it.avatarUrl.isNullOrBlank() }
            .map { it.userId }
            .distinct()
        val summaries = userSummaries(uids)
        return posts.map { post ->
            val summary = summaries[post.userId]
            post.copy(
                username = summary?.username ?: summary?.displayName ?: post.username,
                avatarUrl = summary?.avatarUrl ?: post.avatarUrl,
            )
        }
    }

    suspend fun hydrateCommentAuthors(comments: List<FeedComment>): List<FeedComment> {
        val uids = comments
            .filter { it.username.isNullOrBlank() || it.avatarUrl.isNullOrBlank() }
            .mapNotNull { it.userId.takeIf { id -> id.isNotBlank() } }
            .distinct()
        val summaries = userSummaries(uids)
        return comments.map { comment ->
            val summary = summaries[comment.userId]
            comment.copy(
                username = summary?.username ?: summary?.displayName ?: comment.username,
                avatarUrl = summary?.avatarUrl ?: comment.avatarUrl,
            )
        }
    }

    suspend fun hydrateLikeUsers(likes: List<FeedLikeUser>): List<FeedLikeUser> {
        val uids = likes
            .filter { it.username.isNullOrBlank() || it.displayName.isNullOrBlank() || it.avatarUrl.isNullOrBlank() }
            .mapNotNull { it.userId.takeIf { id -> id.isNotBlank() } }
            .distinct()
        val summaries = userSummaries(uids)
        return likes.map { like ->
            val summary = summaries[like.userId]
            like.copy(
                username = summary?.username ?: like.username,
                displayName = summary?.displayName ?: like.displayName,
                avatarUrl = summary?.avatarUrl ?: like.avatarUrl,
            )
        }
    }

    suspend fun fetchLatestComments(postIds: List<String>, limit: Long = 2): Map<String, List<FeedComment>> = coroutineScope {
        if (postIds.isEmpty()) return@coroutineScope emptyMap()
        postIds.distinct().map { postId ->
            async {
                val snap = postsRef.document(postId).collection("comments")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .await()
                val raw = snap.documents.map { d ->
                    FeedComment(
                        id = d.id,
                        userId = d.getString("userId") ?: "",
                        username = d.getString("username"),
                        avatarUrl = d.getString("avatarUrl"),
                        text = d.getString("text") ?: "",
                        createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                }
                val hydrated = runCatching { hydrateCommentAuthors(raw) }.getOrDefault(raw)
                postId to hydrated.reversed()
            }
        }.awaitAll().toMap()
    }

    suspend fun hydratePostCommentPreviews(posts: List<FeedPost>, limit: Long = 2): List<FeedPost> {
        val previews = fetchLatestComments(posts.map { it.id }, limit)
        return posts.map { post ->
            post.copy(previewComments = previews[post.id].orEmpty())
        }
    }

    suspend fun hydratePostEngagement(currentUid: String, posts: List<FeedPost>): List<FeedPost> = coroutineScope {
        if (posts.isEmpty()) return@coroutineScope posts
        posts.map { post ->
            async {
                val likeRef = postsRef.document(post.id).collection("likes").document(currentUid)
                val saveRef = postsRef.document(post.id).collection("saves").document(currentUid)
                val likedByMe = runCatching { likeRef.get().await().exists() }.getOrDefault(false)
                val savedByMe = runCatching { saveRef.get().await().exists() }.getOrDefault(false)
                post.copy(likedByMe = likedByMe, savedByMe = savedByMe)
            }
        }.awaitAll()
    }

    suspend fun hydrateFeedSnapshot(currentUid: String, posts: List<FeedPost>, rank: Boolean): List<FeedPost> = coroutineScope {
        if (posts.isEmpty()) return@coroutineScope posts

        val authors = async { runCatching { hydratePostAuthors(posts) }.getOrDefault(posts) }
        val engagement = async { runCatching { hydratePostEngagement(currentUid, posts) }.getOrDefault(posts) }
        val previews = async { runCatching { hydratePostCommentPreviews(posts) }.getOrDefault(posts) }
        val followed: Deferred<Set<String>>? =
            if (rank) async { runCatching { followedUserIds(currentUid) }.getOrDefault(emptySet()) } else null

        val engagementById = engagement.await().associateBy { it.id }
        val previewsById = previews.await().associateBy { it.id }
        val merged = authors.await().map { post ->
            val engagementPost = engagementById[post.id]
            val previewPost = previewsById[post.id]
            post.copy(
                likedByMe = engagementPost?.likedByMe ?: post.likedByMe,
                savedByMe = engagementPost?.savedByMe ?: post.savedByMe,
                previewComments = previewPost?.previewComments ?: post.previewComments,
            )
        }

        val followedIds = followed?.await() ?: return@coroutineScope merged
        merged.sortedWith(
            compareByDescending<FeedPost> { it.userId in followedIds }.thenByDescending { it.createdAt }
        )
    }

    private suspend fun followedUserIds(currentUid: String): Set<String> =
        followsRef.whereEqualTo("followerId", currentUid).get().await()
            .documents.mapNotNull { it.getString("followeeId") }.toSet()

    /** Followed-users-first ordering (REQUIREMENTS AC-8.3). */
    suspend fun rankByFollows(currentUid: String, posts: List<FeedPost>): List<FeedPost> {
        val followed = followedUserIds(currentUid)
        return posts.sortedWith(
            compareByDescending<FeedPost> { it.userId in followed }.thenByDescending { it.createdAt }
        )
    }

    suspend fun toggleLike(postId: String, uid: String): Boolean {
        val likeDoc = postsRef.document(postId).collection("likes").document(uid)
        val snap = likeDoc.get().await()
        val postDoc = postsRef.document(postId)
        return if (snap.exists()) {
            likeDoc.delete().await()
            postDoc.update("likeCount", FieldValue.increment(-1)).await()
            false
        } else {
            likeDoc.set(
                mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            postDoc.update("likeCount", FieldValue.increment(1)).await()
            runCatching {
                val (ownerUid, postImage) = postSummary(postId)
                if (ownerUid != null) {
                    val (actorId, actorName, actorAvatar) = actorProfile(uid)
                    notifications.create(
                        targetUid = ownerUid,
                        type = NotificationType.LIKE,
                        actorId = actorId,
                        actorName = actorName,
                        actorAvatar = actorAvatar,
                        postId = postId,
                        postImageUrl = postImage,
                        text = "liked your scrapbook",
                        dedupeKey = "like_${postId}_$uid",
                    )
                }
            }
            true
        }
    }

    fun observeLikes(postId: String): Flow<List<FeedLikeUser>> = callbackFlow {
        val reg = postsRef.document(postId).collection("likes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(
                    snap.documents.map { d ->
                        FeedLikeUser(
                            userId = d.getString("userId") ?: d.id,
                            username = null,
                            displayName = null,
                            avatarUrl = null,
                            likedAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        )
                    }
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun toggleSave(postId: String, uid: String): Boolean {
        val doc = postsRef.document(postId).collection("saves").document(uid)
        val snap = doc.get().await()
        return if (snap.exists()) {
            doc.delete().await(); false
        } else {
            doc.set(mapOf("createdAt" to FieldValue.serverTimestamp())).await(); true
        }
    }

    suspend fun addComment(postId: String, uid: String, text: String) {
        val col = postsRef.document(postId).collection("comments")
        val (_, actorName, actorAvatar) = actorProfile(uid)
        val added = col.add(
            mapOf(
                "userId" to uid,
                "username" to actorName,
                "avatarUrl" to actorAvatar,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        postsRef.document(postId).update("commentCount", FieldValue.increment(1)).await()
        runCatching {
            val (ownerUid, postImage) = postSummary(postId)
            if (ownerUid != null) {
                val (actorId, actorName, actorAvatar) = actorProfile(uid)
                notifications.create(
                    targetUid = ownerUid,
                    type = NotificationType.COMMENT,
                    actorId = actorId,
                    actorName = actorName,
                    actorAvatar = actorAvatar,
                    postId = postId,
                    postImageUrl = postImage,
                    text = "commented: ${text.take(80)}",
                    dedupeKey = "comment_${postId}_${added.id}",
                )
            }
        }
    }

    fun observeComments(postId: String): Flow<List<FeedComment>> = callbackFlow {
        val reg = postsRef.document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                trySend(
                    snap.documents.map { d ->
                        FeedComment(
                            id = d.id,
                            userId = d.getString("userId") ?: "",
                            username = d.getString("username"),
                            avatarUrl = d.getString("avatarUrl"),
                            text = d.getString("text") ?: "",
                            createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        )
                    }
                )
            }
        awaitClose { reg.remove() }
    }

    suspend fun toggleFollow(followerId: String, followeeId: String): Boolean {
        val id = "${followerId}_${followeeId}"
        val ref = followsRef.document(id)
        val existing = ref.get().await()
        return if (existing.exists()) {
            ref.delete().await()
            usersRef.document(followerId).update("followingCount", FieldValue.increment(-1)).await()
            usersRef.document(followeeId).update("followerCount", FieldValue.increment(-1)).await()
            false
        } else {
            ref.set(
                mapOf(
                    "followerId" to followerId,
                    "followeeId" to followeeId,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            usersRef.document(followerId).update("followingCount", FieldValue.increment(1)).await()
            usersRef.document(followeeId).update("followerCount", FieldValue.increment(1)).await()
            runCatching {
                val (actorId, actorName, actorAvatar) = actorProfile(followerId)
                notifications.create(
                    targetUid = followeeId,
                    type = NotificationType.FOLLOW,
                    actorId = actorId,
                    actorName = actorName,
                    actorAvatar = actorAvatar,
                    text = "started following you",
                    dedupeKey = "follow_${followerId}_$followeeId",
                )
            }
            true
        }
    }
}
