package com.example.expressivenotes.data.remote

import com.example.expressivenotes.domain.model.Note
import com.example.expressivenotes.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// Opt-in WebDAV / Nextcloud provider. LWW: remote wins only if remote.updatedAt > local.
class WebDavSyncProvider(
    private val serverUrl: String,
    private val username: String,
    private val password: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CloudSyncProvider {
    override val id = "webdav"
    override val displayName = "WebDAV / Nextcloud"

    private fun fileUrl(): String = serverUrl.trimEnd('/') + "/ExpressiveNotes/notes.json"

    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(fileUrl()).head()
                .header("Authorization", okhttp3.Credentials.basic(username, password))
                .build()
            client.newCall(req).execute().use { resp ->
                // 404 = reachable but no backup yet → treat as OK
                if (resp.code !in 200..299 && resp.code != 404) error("HTTP ${resp.code}")
            }
        }
    }

    override suspend fun push(notes: List<Note>): SyncResult = withContext(Dispatchers.IO) {
        runCatching {
            val body = json.encodeToString(notes).toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(fileUrl()).put(body)
                .header("Authorization", okhttp3.Credentials.basic(username, password))
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext SyncResult.Error("PUT ${resp.code}")
            }
            SyncResult.Success
        }.getOrElse { SyncResult.Error(it.message ?: "WebDAV push failed") }
    }

    override suspend fun pull(sinceMillis: Long): Result<List<Note>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(fileUrl()).get()
                .header("Authorization", okhttp3.Credentials.basic(username, password))
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@runCatching emptyList()
                if (!resp.isSuccessful) error("GET ${resp.code}")
                json.decodeFromString<List<Note>>(resp.body!!.string()).filter { it.updatedAt > sinceMillis }
            }
        }
    }

    override suspend fun sync(local: List<Note>): SyncResult {
        val remote = pull(0).getOrElse { return SyncResult.Error(it.message ?: "pull failed") }
        val remoteById = remote.associateBy { it.id }
        val conflict = local.firstOrNull { l ->
            val r = remoteById[l.id]
            r != null && r.updatedAt > l.updatedAt && r.content != l.content && l.syncStatus != SyncStatus.SYNCED
        }
        if (conflict != null) return SyncResult.Conflict(conflict, remoteById[conflict.id]!!.updatedAt)
        // LWW merge: newest per id wins, then push merged set
        val merged = (local + remote).groupBy { it.id }.mapValues { (_, v) -> v.maxBy { it.updatedAt } }.values.toList()
        return push(merged)
    }
}
