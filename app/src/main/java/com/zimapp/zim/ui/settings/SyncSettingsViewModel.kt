package com.zimapp.zim.ui.settings

import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zimapp.zim.data.remote.CloudSyncProvider
import com.zimapp.zim.data.remote.CustomRestSyncProvider
import com.zimapp.zim.data.remote.SyncHistoryEntry
import com.zimapp.zim.data.remote.SyncResult
import com.zimapp.zim.data.remote.WebDavSyncProvider
import com.zimapp.zim.data.work.KEY_LAST_SYNC
import com.zimapp.zim.data.work.KEY_PROVIDER
import com.zimapp.zim.data.work.SyncWorker
import com.zimapp.zim.data.work.syncPrefs
import com.zimapp.zim.domain.repository.NoteRepository
import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val KEY_WEBDAV_URL = stringPreferencesKey("webdav_url")
val KEY_WEBDAV_USER = stringPreferencesKey("webdav_user")
val KEY_WEBDAV_PASS = stringPreferencesKey("webdav_pass")
val KEY_REST_URL = stringPreferencesKey("rest_url")
val KEY_REST_TOKEN = stringPreferencesKey("rest_token")

data class SyncUiState(
    val provider: String = "disabled",
    val webdavUrl: String = "",
    val webdavUser: String = "",
    val webdavPass: String = "",
    val restUrl: String = "",
    val restToken: String = "",
    val lastSync: Long = 0L,
    val testing: Boolean = false,
    val syncing: Boolean = false,
    val message: String? = null,
    val history: List<SyncHistoryEntry> = emptyList(),
)

class SyncSettingsViewModel(
    private val repo: NoteRepository,
    private val appContext: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val p = appContext.syncPrefs.data.first()
            _state.update {
                it.copy(
                    provider = p[KEY_PROVIDER] ?: "disabled",
                    webdavUrl = p[KEY_WEBDAV_URL] ?: "", webdavUser = p[KEY_WEBDAV_USER] ?: "",
                    webdavPass = p[KEY_WEBDAV_PASS] ?: "", restUrl = p[KEY_REST_URL] ?: "",
                    restToken = p[KEY_REST_TOKEN] ?: "", lastSync = p[KEY_LAST_SYNC] ?: 0L,
                )
            }
        }
    }

    fun setProvider(id: String) = viewModelScope.launch {
        appContext.syncPrefs.edit { it[KEY_PROVIDER] = id }
        _state.update { it.copy(provider = id, message = null) }
        if (id == "disabled") SyncWorker.cancel(appContext) else SyncWorker.schedule(appContext)
    }

    fun editField(update: SyncUiState.() -> SyncUiState) = _state.update(update)

    fun persistCredentials() = viewModelScope.launch {
        val s = _state.value
        appContext.syncPrefs.edit {
            it[KEY_WEBDAV_URL] = s.webdavUrl; it[KEY_WEBDAV_USER] = s.webdavUser
            it[KEY_WEBDAV_PASS] = s.webdavPass; it[KEY_REST_URL] = s.restUrl
            it[KEY_REST_TOKEN] = s.restToken
        }
        _state.update { it.copy(message = "Credentials saved locally") }
    }

    private fun buildProvider(s: SyncUiState): CloudSyncProvider? = when (s.provider) {
        "webdav" -> WebDavSyncProvider(s.webdavUrl, s.webdavUser, s.webdavPass)
        "rest" -> CustomRestSyncProvider(s.restUrl, s.restToken)
        else -> null
    }

    fun testConnection() = viewModelScope.launch {
        val p = buildProvider(_state.value) ?: run {
            _state.update { it.copy(message = "Select a provider first") }; return@launch
        }
        _state.update { it.copy(testing = true, message = null) }
        val r = p.testConnection()
        _state.update { it.copy(testing = false, message = if (r.isSuccess) "Connection OK" else "Failed: ${r.exceptionOrNull()?.message}") }
    }

    fun syncNow() = viewModelScope.launch {
        val p = buildProvider(_state.value) ?: run {
            _state.update { it.copy(message = "Select a provider first") }; return@launch
        }
        _state.update { it.copy(syncing = true, message = null) }
        val local = repo.observeNotes(true).map { it }.first()
        val result = p.sync(local)
        val now = System.currentTimeMillis()
        val entry = when (result) {
            is SyncResult.Success -> { appContext.syncPrefs.edit { it[KEY_LAST_SYNC] = now }; SyncHistoryEntry(now, p.displayName, "Sync OK", true) }
            is SyncResult.Conflict -> SyncHistoryEntry(now, p.displayName, "Conflict on ${result.local.id}", false)
            is SyncResult.Error -> SyncHistoryEntry(now, p.displayName, result.message, false)
        }
        _state.update {
            it.copy(syncing = false, lastSync = if (entry.success) now else it.lastSync,
                message = entry.message, history = (listOf(entry) + it.history).take(20))
        }
    }
}
