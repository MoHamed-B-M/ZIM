package com.example.expressivenotes.data.remote

import com.example.expressivenotes.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// Custom REST / Webhook provider: POST /push, GET /pull?since=.
class CustomRestSyncProvider(
    private val endpoint: String,
    private val bearerToken: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CloudSyncProvider {
    override val id = "rest"
    override val displayName = "Custom REST"

    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(endpoint.trimEnd('/') + "/health").get()
                .header("Authorization", "Bearer $bearerToken").build()
            client.newCall(req).execute().use { if (!it.isSuccessful) error("HTTP ${it.code}") }
        }
    }

    override suspend fun push(notes: List<Note>): SyncResult = withContext(Dispatchers.IO) {
        runCatching {
            val body = json.encodeToString(notes).toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(endpoint.trimEnd('/') + "/push").post(body)
                .header("Authorization", "Bearer $bearerToken").build()
            client.newCall(req).execute().use { if (!it.isSuccessful) return@withContext SyncResult.Error("HTTP ${it.code}") }
            SyncResult.Success
        }.getOrElse { SyncResult.Error(it.message ?: "REST push failed") }
    }

    override suspend fun pull(sinceMillis: Long): Result<List<Note>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(endpoint.trimEnd('/') + "/pull?since=$sinceMillis").get()
                .header("Authorization", "Bearer $bearerToken").build()
            client.newCall(req).execute().use {
                if (!it.isSuccessful) error("HTTP ${it.code}")
                json.decodeFromString<List<Note>>(it.body!!.string())
            }
        }
    }

    override suspend fun sync(local: List<Note>): SyncResult {
        val remote = pull(0).getOrElse { return SyncResult.Error(it.message ?: "pull failed") }
        val merged = (local + remote).groupBy { it.id }.mapValues { (_, v) -> v.maxBy { it.updatedAt } }.values.toList()
        return push(merged)
    }
}
