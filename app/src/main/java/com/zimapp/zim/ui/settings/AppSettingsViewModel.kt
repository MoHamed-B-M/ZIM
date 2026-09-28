package com.zimapp.zim.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zimapp.zim.domain.model.AppSettings
import com.zimapp.zim.domain.model.LockType
import com.zimapp.zim.domain.model.ThemeMode
import com.zimapp.zim.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Koin counterpart to EasyNotes' Hilt SettingsViewModel (trimmed to ZIM's fields).
// Backup passwords stay in memory only — never persisted.
class AppSettingsViewModel(private val repo: AppSettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    var backupPassword: String? = null

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        repo.update(transform)
    }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setSeedColor(argb: Int?) = update { it.copy(seedColor = argb) }
    fun clearLock() = update { it.copy(lockType = LockType.NONE, lockSecret = null) }
}
