package com.zimapp.zim.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Existing notes were written as markdown; new notes default RICH.
        db.execSQL("ALTER TABLE notes ADD COLUMN format TEXT NOT NULL DEFAULT 'MARKDOWN'")
    }
}

@Database(entities = [NoteEntity::class, NoteFtsEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
