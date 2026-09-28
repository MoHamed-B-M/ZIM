package com.example.expressivenotes.di

import androidx.room.Room
import com.example.expressivenotes.data.local.AppDatabase
import com.example.expressivenotes.data.repository.NoteRepositoryImpl
import com.example.expressivenotes.domain.repository.NoteRepository
import com.example.expressivenotes.ui.note_detail.NoteDetailViewModel
import com.example.expressivenotes.ui.notes_list.NotesViewModel
import com.example.expressivenotes.ui.settings.SyncSettingsViewModel
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val AppModule = module {
    single { Json { ignoreUnknownKeys = true; prettyPrint = true } }
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "expressive-notes.db").build()
    }
    single { get<AppDatabase>().noteDao() }
    single<NoteRepository> { NoteRepositoryImpl(get(), get()) }
    viewModelOf(::NotesViewModel)
    viewModelOf(::NoteDetailViewModel)
    viewModelOf(::SyncSettingsViewModel)
}
