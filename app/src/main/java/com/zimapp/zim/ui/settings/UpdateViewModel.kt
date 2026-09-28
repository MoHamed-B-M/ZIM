package com.zimapp.zim.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zimapp.zim.data.update.DownloadState
import com.zimapp.zim.data.update.RemoteUpdate
import com.zimapp.zim.data.update.canInstallUnknown
import com.zimapp.zim.data.update.enqueueDownload
import com.zimapp.zim.data.update.fetchUpdate
import com.zimapp.zim.data.update.installApk
import com.zimapp.zim.data.update.installedVersionCode
import com.zimapp.zim.data.update.installedVersionName
import com.zimapp.zim.data.update.openInstallPermission
import com.zimapp.zim.data.update.queryDownload
import java.io.File
import kotlinx.coroutines.delay
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
        val id = try {
            enqueueDownload(appContext, r.apkUrl, r.apkName)
        } catch (e: Exception) {
            _state.update { it.copy(downloading = false, error = e.message ?: "Download failed") }
            return@launch
        }
        while (true) {
            when (val s = queryDownload(appContext, id)) {
                is DownloadState.Running -> {
                    val p = if (s.total > 0) (s.downloaded.toFloat() / s.total).coerceIn(0f, 1f) else 0f
                    _state.update { it.copy(progress = p) }
                    delay(500)
                }
                is DownloadState.Done -> {
                    _state.update { it.copy(downloading = false, progress = 1f, downloadedFile = s.file) }
                    return@launch
                }
                is DownloadState.Failed -> {
                    _state.update { it.copy(downloading = false, error = s.reason) }
                    return@launch
                }
            }
        }
    }

    fun install() {
        val file = _state.value.downloadedFile ?: return
        if (!canInstallUnknown(appContext)) {
            openInstallPermission(appContext)
            _state.update { it.copy(message = "Allow “Install unknown apps”, then tap Install again") }
            return
        }
        installApk(appContext, file)
    }
}
