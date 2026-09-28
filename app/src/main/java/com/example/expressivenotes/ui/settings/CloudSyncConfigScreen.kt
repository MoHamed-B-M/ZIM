package com.example.expressivenotes.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

// Self-add cloud panel: Disabled / WebDAV / REST. No account forced; test + history inline.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CloudSyncConfigScreen(onBack: () -> Unit, vm: SyncSettingsViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            title = { Text("Cloud sync (opt-in)") })
    }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Provider", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("disabled" to "Off", "webdav" to "WebDAV", "rest" to "REST").forEach { (id, label) ->
                        FilterChip(selected = s.provider == id, onClick = { vm.setProvider(id) }, label = { Text(label) })
                    }
                }
            }
            if (s.provider == "webdav") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(s.webdavUrl, { vm.editField { copy(webdavUrl = it) } },
                            label = { Text("Server URL (https://…/remote.php/dav)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(s.webdavUser, { vm.editField { copy(webdavUser = it) } },
                            label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(s.webdavPass, { vm.editField { copy(webdavPass = it) } },
                            label = { Text("Password / app token") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = PasswordVisualTransformation())
                    }
                }
            }
            if (s.provider == "rest") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(s.restUrl, { vm.editField { copy(restUrl = it) } },
                            label = { Text("Endpoint base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(s.restToken, { vm.editField { copy(restToken = it) } },
                            label = { Text("Bearer token") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = PasswordVisualTransformation())
                    }
                }
            }
            if (s.provider != "disabled") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (s.testing || s.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
                        OutlinedButton(onClick = vm::persistCredentials, modifier = Modifier.fillMaxWidth()) { Text("Save credentials (device only)") }
                        Button(onClick = vm::testConnection, enabled = !s.testing, modifier = Modifier.fillMaxWidth()) {
                            Text(if (s.testing) "Testing…" else "Test connection")
                        }
                        Button(onClick = vm::syncNow, enabled = !s.syncing, modifier = Modifier.fillMaxWidth()) {
                            Text(if (s.syncing) "Syncing…" else "Sync now")
                        }
                        s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                        if (s.lastSync > 0) Text("Last sync: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(s.lastSync)),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                item { Text("History", style = MaterialTheme.typography.labelLarge) }
                items(s.history) { h ->
                    ListItem(headlineContent = { Text(h.message) },
                        supportingContent = { Text("${h.provider} · " + SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(h.timestamp))) },
                        trailingContent = { Text(if (h.success) "OK" else "FAIL") })
                }
            } else {
                item { Text("Sync is off. Notes stay 100% on-device.", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
