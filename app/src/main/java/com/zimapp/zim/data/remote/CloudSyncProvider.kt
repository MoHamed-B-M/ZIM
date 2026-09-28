package com.zimapp.zim.data.remote

import com.zimapp.zim.domain.model.Note

sealed interface SyncResult {
    data object Success : SyncResult
    data class Conflict(val local: Note, val remoteUpdatedAt: Long) : SyncResult
    data class Error(val message: String) : SyncResult
}

data class SyncHistoryEntry(
    val timestamp: Long,
    val provider: String,
    val message: String,
    val success: Boolean,
)

interface CloudSyncProvider {
    val id: String // "webdav" | "rest" | "disabled"
    val displayName: String
    suspend fun testConnection(): Result<Unit>
    suspend fun push(notes: List<Note>): SyncResult
    suspend fun pull(sinceMillis: Long): Result<List<Note>>
    suspend fun sync(local: List<Note>): SyncResult
}
