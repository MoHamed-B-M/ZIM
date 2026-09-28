package com.zimapp.zim.domain.repository

import com.zimapp.zim.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(includeArchived: Boolean = false): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
    fun observeTrash(): Flow<List<Note>>
    suspend fun getById(id: String): Note?
    suspend fun upsert(note: Note)
    suspend fun togglePin(id: String)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun moveToTrash(id: String)
    suspend fun restore(id: String)
    suspend fun deleteForever(id: String)
    suspend fun purgeTrashOlderThan(cutoffMillis: Long)
    suspend fun exportJson(): String
    suspend fun importJson(json: String): Int
}
