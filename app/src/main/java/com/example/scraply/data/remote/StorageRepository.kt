package com.example.scraply.data.remote

import android.content.Context
import com.example.scraply.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Uploads / downloads image files to Supabase Storage via its REST API.
 *
 *   POST   /storage/v1/object/{bucket}/{path}               -- create
 *   PUT    /storage/v1/object/{bucket}/{path}               -- replace
 *   POST + `x-upsert: true`                                 -- upsert
 *   GET    /storage/v1/object/public/{bucket}/{path}        -- public download URL
 *
 * The anon key is used as both `apikey` and `Authorization: Bearer`. This is safe to ship in-app –
 * bucket policies + RLS enforce access control. For a stricter setup, you can add Supabase Auth
 * later and exchange Firebase's ID token for a Supabase JWT; the current app uses UUID-based paths
 * which give practical privacy via unguessable names.
 *
 * Expected buckets (create in Supabase dashboard):
 *   - `stamps`   (public download OK; each file lives under {uid}/{stampId}.png)
 *   - `posts`    (public; {uid}/{postId}.png)
 *   - `avatars`  (public; {uid}.png)
 */
class StorageRepository(private val appContext: Context) {

    private val baseUrl: String = appContext.getString(R.string.supabase_url).trimEnd('/')
    private val anonKey: String = appContext.getString(R.string.supabase_anon_key)

    private val configured: Boolean =
        !baseUrl.contains("REPLACE") && !anonKey.startsWith("REPLACE")

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadStampImage(uid: String, stampId: String, localPath: String): String =
        uploadAndPublicUrl(STAMPS_BUCKET, "$uid/$stampId.png", localPath)

    suspend fun uploadPostImage(uid: String, postId: String, localPath: String): String =
        uploadAndPublicUrl(POSTS_BUCKET, "$uid/$postId.png", localPath)

    suspend fun uploadAvatar(uid: String, localPath: String): String =
        uploadAndPublicUrl(AVATARS_BUCKET, "$uid.png", localPath)

    suspend fun downloadToFile(remoteUrl: String, dest: File): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(remoteUrl).build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("Download failed (${resp.code}): $remoteUrl")
            val body = resp.body ?: error("Download returned empty body: $remoteUrl")
            dest.parentFile?.mkdirs()
            dest.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        dest
    }

    private suspend fun uploadAndPublicUrl(
        bucket: String,
        path: String,
        localPath: String,
    ): String = withContext(Dispatchers.IO) {
        require(configured) {
            "Supabase is not configured. Set supabase_url and supabase_anon_key in res/values/firebase_strings.xml."
        }
        val file = File(localPath)
        require(file.exists()) { "File to upload not found: $localPath" }

        val mediaType = guessMediaType(file.extension)
        val url = "$baseUrl/storage/v1/object/$bucket/$path"
        val request = Request.Builder()
            .url(url)
            .header("apikey", anonKey)
            .header("Authorization", "Bearer $anonKey")
            .header("Content-Type", mediaType)
            .header("x-upsert", "true")
            .header("cache-control", "3600")
            .post(file.asRequestBody(mediaType.toMediaType()))
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                error("Supabase upload failed (${resp.code}): $body")
            }
        }
        "$baseUrl/storage/v1/object/public/$bucket/$path"
    }

    private fun guessMediaType(extension: String): String = when (extension.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "image/png"
    }

    companion object {
        const val STAMPS_BUCKET = "stamps"
        const val POSTS_BUCKET = "posts"
        const val AVATARS_BUCKET = "avatars"
    }
}
