package com.zimapp.zim.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zimapp.zim.core.constant.DatabaseConst
import com.zimapp.zim.data.local.dao.NoteDao
import com.zimapp.zim.domain.model.Note

@Database(
    entities = [Note::class],
    version = DatabaseConst.NOTES_DATABASE_VERSION,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
}