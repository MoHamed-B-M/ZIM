package com.zimapp.zim.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.zimapp.zim.domain.model.NoteFormat
import com.zimapp.zim.domain.model.SyncStatus

@Entity(tableName = "notes")
@TypeConverters(Converters::class)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val tagsCsv: String = "", // comma-separated; keeps table narrow, zero join overhead
    val colorToken: Int = 0,
    // RICH/MARKDOWN as String (existing rows backfilled MARKDOWN by migration).
    val format: String = "MARKDOWN",
)

class Converters {
    @TypeConverter fun syncToString(v: SyncStatus): String = v.name
    @TypeConverter fun stringToSync(v: String): SyncStatus =
        runCatching { SyncStatus.valueOf(v) }.getOrDefault(SyncStatus.PENDING)
}

fun String.toNoteFormat(): NoteFormat =
    runCatching { NoteFormat.valueOf(this) }.getOrDefault(NoteFormat.MARKDOWN)
