package com.zimapp.zim.presentation.screens.settings.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// In-app updater: beta channel reads the beta-latest prerelease, stable reads
// the latest finished release. Multi-connection download, install via
// FileProvider (needs “Install unknown apps” once).
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppUpdateSection(vm: UpdateViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("App updates", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !s.betaChannel, onClick = { vm.setChannel(false) }, label = { Text("Stable") })
            FilterChip(selected = s.betaChannel, onClick = { vm.setChannel(true) }, label = { Text("Beta") })
        }
        s.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        // The wave lives inside the button rather than below it, so the label
        // and the progress share one control and the row cannot jump height
        // when the check starts. The button is disabled throughout, which is
        // what keeps a second check from being fired mid-request.
        OutlinedButton(onClick = vm::check, enabled = !s.checking, modifier = Modifier.fillMaxWidth()) {
            if (s.checking) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Text("Check for updates")
            }
        }
        s.remote?.let { r ->
            Text("${r.title} · ${r.publishedAt}", style = MaterialTheme.typography.bodyMedium)
            if (r.notes.isNotBlank()) {
                Text(r.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (s.downloading) Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator(progress = { s.progress.coerceIn(0f, 1f) })
            }
            when {
                s.downloadedFile != null -> Button(onClick = vm::install, modifier = Modifier.fillMaxWidth()) {
                    Text("Install update")
                }
                s.updateAvailable -> Button(onClick = vm::download,
                    enabled = !s.downloading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (s.downloading) "Downloading ${(s.progress * 100).toInt()}%" else "Download update")
                }
            }
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
