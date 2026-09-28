package com.zimapp.zim.data.repository

import com.zimapp.zim.data.local.NoteDao
import com.zimapp.zim.data.local.NoteEntity
import com.zimapp.zim.domain.model.Note
import com.zimapp.zim.domain.model.SyncStatus
import com.zimapp.zim.domain.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun NoteEntity.toDomain() = Note(
    id = id, title = title, content = content, createdAt = createdAt, updatedAt = updatedAt,
    isPinned = isPinned, isArchived = isArchived, isDeleted = isDeleted, deletedAt = deletedAt,
    syncStatus = syncStatus, tags = tagsCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    colorToken = colorToken,
)

private fun Note.toEntity() = NoteEntity(
    id = id, title = title, content = content, createdAt = createdAt, updatedAt = updatedAt,
    isPinned = isPinned, isArchived = isArchived, isDeleted = isDeleted, deletedAt = deletedAt,
    syncStatus = if (syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING else syncStatus,
    tagsCsv = tags.joinToString(","),
    colorToken = colorToken,
)

// FTS MATCH escaping: quote + wildcard for prefix search.
fun ftsMatchQuery(raw: String): String {
    val clean = raw.trim().replace('"', ' ').split(Regex("\\s+")).filter { it.isNotEmpty() }.take(10)
    if (clean.isEmpty()) return ""
    return clean.joinToString(" ") { "\"$it\"*" }
}

class NoteRepositoryImpl(
    private val dao: NoteDao,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true },
) : NoteRepository {
    override fun observeNotes(includeArchived: Boolean): Flow<List<Note>> =
        dao.observeNotes(includeArchived).map { list -> list.map { it.toDomain() } }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun searchNotes(query: String): Flow<List<Note>> =
        flowOf(query).debounce(150).distinctUntilChanged().flatMapLatest { q ->
            if (q.isBlank()) dao.observeNotes(false)
            else dao.searchFts(ftsMatchQuery(q))
        }.map { list -> list.map { it.toDomain() } }

    override fun observeTrash(): Flow<List<Note>> =
        dao.observeTrash().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Note? = dao.getById(id)?.toDomain()
    override suspend fun upsert(note: Note) = dao.upsert(note.toEntity())
    override suspend fun togglePin(id: String) = dao.togglePin(id)
    override suspend fun setArchived(id: String, archived: Boolean) = dao.setArchived(id, archived)
    override suspend fun moveToTrash(id: String) = dao.moveToTrash(id)
    override suspend fun restore(id: String) = dao.restore(id)
    override suspend fun deleteForever(id: String) = dao.deleteForever(id)
    override suspend fun purgeTrashOlderThan(cutoffMillis: Long) = dao.purgeTrashOlderThan(cutoffMillis)

    override suspend fun exportJson(): String = json.encodeToString(dao.getAllOnce().map { it.toDomain() })
    override suspend fun importJson(json: String): Int {
        val notes = this.json.decodeFromString<List<Note>>(json)
        notes.forEach { dao.upsert(it.copy(syncStatus = SyncStatus.PENDING).toEntity()) }
        return notes.size
    }
}
