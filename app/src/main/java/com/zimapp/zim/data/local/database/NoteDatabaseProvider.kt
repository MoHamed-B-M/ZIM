package com.zimapp.zim.data.local.database


import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zimapp.zim.core.constant.DatabaseConst
import com.zimapp.zim.data.local.dao.NoteDao

class NoteDatabaseProvider(private val application: Application) {

    @Volatile
    private var database: NoteDatabase? = null

    @Synchronized
    fun instance(): NoteDatabase {
        return database ?: synchronized(this) {
            database ?: buildDatabase().also { database = it }
        }
    }

    private fun buildDatabase(): NoteDatabase {
        return Room.databaseBuilder(application.applicationContext,
            NoteDatabase::class.java,
            DatabaseConst.NOTES_DATABASE_FILE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_2_4, MIGRATION_4_5)
            .build()
    }

    @Synchronized
    fun close() {
        database?.close()
        database = null
    }

    fun noteDao(): NoteDao {
        return instance().noteDao()
    }
}



private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notes-table` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notes-table` ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notes-table` ADD COLUMN `encrypted` INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_4 = object : Migration(2, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notes-table` ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `notes-table` ADD COLUMN `encrypted` INTEGER NOT NULL DEFAULT 0")
    }
}

// Nullable with no default, which is what Room expects for audioPath: rows that
// predate voice notes come back as null instead of as an empty string that
// would look like a broken recording.
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notes-table` ADD COLUMN `audio_path` TEXT")
    }
}
