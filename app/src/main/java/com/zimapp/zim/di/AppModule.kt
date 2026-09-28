package com.zimapp.zim.di

import androidx.room.Room
import com.zimapp.zim.data.local.AppDatabase
import com.zimapp.zim.data.repository.NoteRepositoryImpl
import com.zimapp.zim.domain.repository.NoteRepository
import com.zimapp.zim.ui.note_detail.NoteDetailViewModel
import com.zimapp.zim.ui.notes_list.NotesViewModel
import com.zimapp.zim.ui.settings.SyncSettingsViewModel
import com.zimapp.zim.ui.settings.UpdateViewModel
import com.zimapp.zim.ui.todo.TodoViewModel
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
    viewModelOf(::UpdateViewModel)
    viewModelOf(::TodoViewModel)
}
