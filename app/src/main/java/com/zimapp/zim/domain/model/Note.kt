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

    // Retained for existing installs only: the voice-note feature was removed,
    // so this is always null on new rows and never read. Dropping the column
    // would need a table-recreate migration, which is risk without runtime
    // verification — keeping a nullable dead column is the safe option.
    @ColumnInfo(name = "audio_path")
    val audioPath: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() // Default value is the current timestamp
)