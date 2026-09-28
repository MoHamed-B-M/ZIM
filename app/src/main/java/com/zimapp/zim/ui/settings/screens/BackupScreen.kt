package com.zimapp.zim.ui.settings.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.ImportExport
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.data.backup.EncryptedBackup
import com.zimapp.zim.domain.repository.NoteRepository
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import com.zimapp.zim.ui.settings.easy.ActionType
import com.zimapp.zim.ui.settings.easy.SettingsBox
import com.zimapp.zim.ui.settings.easy.SettingsScaffold
import com.zimapp.zim.ui.settings.easy.shapeManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.BadPaddingException
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// Local encrypted backup (password AES, adapted from EasyNotes) + plain JSON
// export/import + entry point to the WebDAV/REST cloud setup.
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onCloudConfig: () -> Unit,
    vm: AppSettingsViewModel = koinViewModel(),
    repo: NoteRepository = koinInject(),
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var passwordDialog by remember { mutableStateOf<BackupAction?>(null) }
    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()

    val encryptedExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val plain = repo.exportJson().toByteArray()
                val blob = vm.backupPassword?.let { EncryptedBackup.encrypt(plain, it) } ?: plain
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(blob) }
            }.onSuccess { toast("Backup exported") }.onFailure { toast("Export failed: ${it.message}") }
        }
    }
    val encryptedImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val blob = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Cannot read file")
                val plain = vm.backupPassword?.let {
                    try {
                        EncryptedBackup.decrypt(blob, it)
                    } catch (e: BadPaddingException) {
                        error("Wrong password")
                    }
                } ?: blob
                repo.importJson(plain.decodeToString())
            }.onSuccess { toast("Imported $it notes") }.onFailure { toast("Import failed: ${it.message}") }
        }
    }
    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(repo.exportJson().toByteArray()) }
            toast("Exported")
        }
    }
    val jsonImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return@launch
            toast("Imported ${repo.importJson(text)} notes")
        }
    }

    passwordDialog?.let { action ->
        PasswordDialog(
            title = if (action == BackupAction.EXPORT) "Backup password" else "Restore password",
            onDismiss = { passwordDialog = null },
            onConfirm = { pw ->
                vm.backupPassword = pw.ifBlank { null }
                passwordDialog = null
                if (action == BackupAction.EXPORT) encryptedExport.launch("zim-backup-${timestamp()}.zimbackup")
                else encryptedImport.launch(arrayOf("application/octet-stream"))
            },
        )
    }

    SettingsScaffold(title = "Backup & Sync", onBack = onBack) {
        LazyColumn {
            item {
                SettingsBox(
                    title = "Encrypt backup",
                    description = "Password-protect .zimbackup files",
                    icon = Icons.Rounded.EnhancedEncryption,
                    radius = shapeManager(isBoth = true, radius = s.cornerRadius),
                    actionType = ActionType.SWITCH,
                    variable = s.encryptBackup,
                    switchEnabled = { checked -> vm.update { it.copy(encryptBackup = checked) } },
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            item {
                SettingsBox(
                    title = "Backup now",
                    description = if (s.encryptBackup) "Password-encrypted file" else "Plain JSON file",
                    icon = Icons.Rounded.Backup,
                    radius = shapeManager(isFirst = true, radius = s.cornerRadius),
                    actionType = ActionType.CUSTOM,
                    customAction = {
                        if (s.encryptBackup) passwordDialog = BackupAction.EXPORT
                        else jsonExport.launch("zim-backup-${timestamp()}.json")
                    },
                )
            }
            item {
                SettingsBox(
                    title = "Restore",
                    description = "From .zimbackup or .json",
                    icon = Icons.Rounded.ImportExport,
                    radius = shapeManager(radius = s.cornerRadius),
                    actionType = ActionType.CUSTOM,
                    customAction = {
                        if (s.encryptBackup) passwordDialog = BackupAction.IMPORT
                        else jsonImport.launch(arrayOf("application/json"))
                    },
                )
            }
            item {
                SettingsBox(
                    title = "Import text file",
                    description = "Each file becomes a note",
                    icon = Icons.Rounded.FileOpen,
                    radius = shapeManager(radius = s.cornerRadius, isLast = true),
                    actionType = ActionType.CUSTOM,
                    customAction = {
                        // Reuses the JSON picker path with text mime; importJson handles objects,
                        // plain text falls back to a single note below.
                        jsonImport.launch(arrayOf("text/*"))
                    },
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            item {
                SettingsBox(
                    title = "Cloud sync",
                    description = "WebDAV / REST, opt-in only",
                    icon = Icons.Rounded.CloudUpload,
                    radius = shapeManager(isBoth = true, radius = s.cornerRadius),
                    actionType = ActionType.CUSTOM,
                    customAction = { onCloudConfig() },
                )
            }
        }
    }
}

private enum class BackupAction { EXPORT, IMPORT }

private fun timestamp(): String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-HH-mm"))

@Composable
private fun PasswordDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { onConfirm(password) }, modifier = Modifier.align(Alignment.End)) {
                Text("Confirm")
            }
        }
    }
}
