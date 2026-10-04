package com.zimapp.zim.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes-table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "note-name")
    val name: String,

    @ColumnInfo(name = "note-description")
    val description: String,

    @ColumnInfo(name = "pinned")
    val pinned: Boolean = false,

    @ColumnInfo(name = "encrypted")
    val encrypted: Boolean = false,

    // Absolute path to a recording in app-private storage, or null for a note
    // with no audio. Nullable so every existing row stays valid after the
    // column is added: Room reads a missing value as null rather than
    // demanding a default.
    @ColumnInfo(name = "audio_path")
    val audioPath: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() // Default value is the current timestamp
)