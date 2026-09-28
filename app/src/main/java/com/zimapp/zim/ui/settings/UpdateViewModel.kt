package com.zimapp.zim.ui.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zimapp.zim.data.update.RemoteUpdate
import com.zimapp.zim.data.update.canInstallUnknown
import com.zimapp.zim.data.update.canUpdateOverInstalled
import com.zimapp.zim.data.update.downloadFast
import com.zimapp.zim.data.update.fetchUpdate
import com.zimapp.zim.data.update.installApk
import com.zimapp.zim.data.update.installedVersionCode
import com.zimapp.zim.data.update.installedVersionName
import com.zimapp.zim.data.update.openInstallPermission
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class UpdateUiState(
    val betaChannel: Boolean = true,
    val checking: Boolean = false,
    val remote: RemoteUpdate? = null,
    val updateAvailable: Boolean = false,
    val error: String? = null,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedFile: File? = null,
    val message: String? = null,
)

class UpdateViewModel(private val appContext: Context) : ViewModel() {
    private val client = OkHttpClient()
    private val _state = MutableStateFlow(
        UpdateUiState(message = "Installed: ${installedVersionName(appContext)}")
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun setChannel(beta: Boolean) {
        _state.update { it.copy(betaChannel = beta, remote = null, error = null, downloadedFile = null) }
    }

    fun check() = viewModelScope.launch {
        _state.update { it.copy(checking = true, error = null, message = null) }
        val res = fetchUpdate(client, _state.value.betaChannel)
        res.onSuccess { r ->
            val installed = installedVersionCode(appContext).toInt()
            // versionCode 0 = unparsable tag: offer download when names differ.
            val available = if (r.versionCode > 0) r.versionCode > installed
            else r.title != installedVersionName(appContext)
            _state.update {
                it.copy(checking = false, remote = r, updateAvailable = available,
                    message = if (available) "Update available" else "You're up to date")
            }
        }.onFailure { e ->
            _state.update { it.copy(checking = false, error = e.message ?: "Check failed") }
        }
    }

    fun download() = viewModelScope.launch {
        val r = _state.value.remote ?: return@launch
        _state.update { it.copy(downloading = true, progress = 0f, error = null, downloadedFile = null) }
        try {
            val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: error("Storage unavailable")
            val dest = File(dir, r.apkName)
            downloadFast(client, r.apkUrl, dest) { p ->
                _state.update { it.copy(progress = p) }
            }
            _state.update { it.copy(downloading = false, progress = 1f, downloadedFile = dest) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(downloading = false, error = e.message ?: "Download failed") }
        }
    }

    fun install() {
        val file = _state.value.downloadedFile ?: return
        if (!canUpdateOverInstalled(appContext, file)) {
            _state.update {
                it.copy(message = "Different signature — uninstall the current version first (Export your notes first)")
            }
            return
        }
        if (!canInstallUnknown(appContext)) {
            openInstallPermission(appContext)
            _state.update { it.copy(message = "Allow “Install unknown apps”, then tap Install again") }
            return
        }
        installApk(appContext, file)
    }
}
