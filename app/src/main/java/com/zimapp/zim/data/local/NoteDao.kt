package com.zimapp.zim.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NoteEntity)

    @Update suspend fun update(entity: NoteEntity)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query(
        """SELECT * FROM notes WHERE isDeleted = 0 AND (:includeArchived OR isArchived = 0)
           ORDER BY isPinned DESC, updatedAt DESC"""
    )
    fun observeNotes(includeArchived: Boolean): Flow<List<NoteEntity>>

    // FTS: sub-ms prefix search. `notes_fts MATCH :q` requires `q` like '"term"*'.
    // Highlighting is done in UI with the raw query string (no snippet() cursor overhead).
    @Query(
        """SELECT n.* FROM notes n JOIN notes_fts f ON n.rowid = f.rowid
           WHERE f.notes_fts MATCH :matchQuery AND n.isDeleted = 0
           ORDER BY n.isPinned DESC, n.updatedAt DESC LIMIT 200"""
    )
    fun searchFts(matchQuery: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET isPinned = NOT isPinned, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun togglePin(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isArchived = :archived, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :now, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun moveToTrash(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun restore(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrashOlderThan(cutoff: Long)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<NoteEntity>
}
