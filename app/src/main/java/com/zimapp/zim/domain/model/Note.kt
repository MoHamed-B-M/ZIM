package com.zimapp.zim.domain.model

import kotlinx.serialization.Serializable

enum class SyncStatus { SYNCED, PENDING, CONFLICT }

enum class NoteFormat { RICH, MARKDOWN }

@Serializable
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val tags: List<String> = emptyList(),
    val colorToken: Int = 0, // 0 = default surface, 1..5 = expressive tonal seeds
    // RICH = formatted HTML, MARKDOWN = raw markdown. New notes default RICH.
    val format: NoteFormat = NoteFormat.RICH,
)
