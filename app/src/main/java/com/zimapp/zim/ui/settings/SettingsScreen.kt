package com.zimapp.zim.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zimapp.zim.R
import com.zimapp.zim.domain.repository.NoteRepository
import com.zimapp.zim.ui.navigation.BottomDockWithFab
import com.zimapp.zim.ui.theme.DarkExpressive
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val TileWhite = Color.White
private val TileInk = Color(0xFF1C274C)

// White circular tile with a dark glyph — the signature look of every row.
@Composable
private fun SettingTile(icon: ImageVector, description: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(TileWhite),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = TileInk)
    }
}

// Segmented list row: tile + bold title + muted single-line description.
@Composable
private fun SegmentedListItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { SettingTile(icon, title) },
        trailingContent = trailing ?: onClick?.let {
            { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

// Outer group container: rounded card; place SettingsDivider() between rows.
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.75.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTab: String = "settings",
    onTab: (String) -> Unit = {},
    onBack: () -> Unit,
    onCloudConfig: () -> Unit,
    repo: NoteRepository = koinInject(),
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }
    val currentIcon by remember(ctx) { appIconFlow(ctx) }.collectAsState(initial = AppIcon.DEFAULT)
    val appVersion = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0.0"
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val json = repo.exportJson()
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            msg = "Backup exported"
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: return@launch
            val n = repo.importJson(text)
            msg = "Imported $n notes"
        }
    }
    fun openUrl(url: String) {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // Dark grouped look regardless of system theme.
    MaterialTheme(
        colorScheme = DarkExpressive,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                )
            },
            bottomBar = {
                BottomDockWithFab(currentRoute = currentTab, onTab = onTab)
            },
        ) { p ->
            Column(
                Modifier.fillMaxSize().padding(p).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SettingsGroup("Appearance") {
                    SegmentedListItem(
                        icon = Icons.Filled.Palette,
                        title = "App icon",
                        description = "Currently: ${currentIcon.label}",
                    )
                    SettingsDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppIcon.entries.forEach { option ->
                            val selected = option == currentIcon
                            Card(
                                modifier = Modifier.weight(1f).clickable {
                                    scope.launch {
                                        setAppIcon(ctx, option)
                                        msg = "Icon: ${option.label}"
                                    }
                                },
                                colors = if (selected) CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ) else CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            ) {
                                Column(
                                    Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Image(
                                        painter = painterResource(option.previewRes),
                                        contentDescription = option.label,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Text(option.label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                SettingsGroup("Backup & sync") {
                    SegmentedListItem(
                        icon = Icons.Filled.CloudUpload,
                        title = "Cloud sync",
                        description = "WebDAV / REST, opt-in only",
                        onClick = onCloudConfig,
                    )
                    SettingsDivider()
                    SegmentedListItem(
                        icon = Icons.Filled.Download,
                        title = "Export backup",
                        description = "Save notes as JSON",
                        onClick = { exportLauncher.launch("zim-backup.json") },
                    )
                    SettingsDivider()
                    SegmentedListItem(
                        icon = Icons.Filled.Upload,
                        title = "Import backup",
                        description = "Restore from a JSON file",
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                    )
                }

                SettingsGroup("App updates") {
                    Column(Modifier.padding(16.dp)) {
                        AppUpdateSection()
                    }
                }

                SettingsGroup("About") {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.preview_icon_default),
                            contentDescription = "ZIM app icon",
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("ZIM", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Version $appVersion",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Fast, local-first notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    SettingsDivider()
                    SegmentedListItem(
                        icon = Icons.Filled.Code,
                        title = "Source code",
                        description = "github.com/MoHamed-B-M/ZIM",
                        onClick = { openUrl("https://github.com/MoHamed-B-M/ZIM") },
                    )
                    SettingsDivider()
                    SegmentedListItem(
                        icon = Icons.Filled.Info,
                        title = "Open source",
                        description = "Community-driven, no lock-in",
                        onClick = { openUrl("https://github.com/MoHamed-B-M/ZIM") },
                    )
                }

                msg?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Local-first: everything works offline. Trash auto-purges after 30 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
