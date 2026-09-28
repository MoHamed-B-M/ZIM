package com.example.expressivenotes.data.local

import androidx.room.Entity
import androidx.room.Fts4

// Room ships @Fts4 (FTS3/4). On devices with SQLite FTS5 we upgrade the
// shadow table in AppDatabase.Callback to `USING fts5`; queries below work on both.
@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    val title: String,
    val content: String,
)
