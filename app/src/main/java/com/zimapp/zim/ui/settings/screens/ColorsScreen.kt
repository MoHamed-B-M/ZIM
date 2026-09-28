package com.zimapp.zim.ui.settings.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.HdrAuto
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.domain.model.ThemeMode
import com.zimapp.zim.ui.settings.AppIcon
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import com.zimapp.zim.ui.settings.appIconFlow
import com.zimapp.zim.ui.settings.easy.ActionType
import com.zimapp.zim.ui.settings.easy.ListDialog
import com.zimapp.zim.ui.settings.easy.SettingsBox
import com.zimapp.zim.ui.settings.easy.SettingsScaffold
import com.zimapp.zim.ui.settings.easy.shapeManager
import com.zimapp.zim.ui.settings.setAppIcon
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// 12 accents ported from EasyNotes PALETTE_COLORS.
val SeedAccents = listOf(
    Color(0xFFFBD9D4), Color(0xFFFCE38A), Color(0xFFFBFFB1),
    Color(0xFFF08A5D), Color(0xFFEA5455), Color(0xFFFF2E63),
    Color(0xFF609966), Color(0xFF557153), Color(0xFF00ADB5),
    Color(0xFF3F72AF), Color(0xFF950101), Color(0xFF222831),
)
const val SYSTEM_SEED = -7896468 // sentinel: follow system dynamic / fallback

@Composable
fun ColorsScreen(onBack: () -> Unit, vm: AppSettingsViewModel = koinViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    var showModePicker by remember { mutableStateOf(false) }
    var showSeedPicker by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    var showRadiusPicker by remember { mutableStateOf(false) }

    if (showModePicker) {
        ListDialog(
            text = "Theme mode",
            list = ThemeMode.entries,
            initialItem = s.themeMode,
            onExit = { showModePicker = false },
            extractDisplayData = { it.name.lowercase().replaceFirstChar(Char::titlecase) to "" },
            setting = { (name, _) ->
                SettingsBox(
                    title = name,
                    actionType = ActionType.RADIOBUTTON,
                    variable = s.themeMode.name == name.uppercase(),
                    switchEnabled = {
                        vm.setThemeMode(ThemeMode.valueOf(name.uppercase()))
                        showModePicker = false
                    },
                )
            },
        )
    }
    if (showSeedPicker) SeedPickerDialog(s.seedColor, onPick = { vm.setSeedColor(it) }, onExit = { showSeedPicker = false })
    if (showFontPicker) FontPickerDialog(s.fontSize, onPick = { size -> vm.update { it.copy(fontSize = size) } }, onExit = { showFontPicker = false })
    if (showRadiusPicker) RadiusPickerDialog(s.cornerRadius, onPick = { radius -> vm.update { it.copy(cornerRadius = radius) } }, onExit = { showRadiusPicker = false })

    SettingsScaffold(title = "Colors & Styles", onBack = onBack) {
        LazyColumn {
            item {
                SettingsBox(
                    title = "Theme mode",
                    description = s.themeMode.name.lowercase().replaceFirstChar(Char::titlecase),
                    icon = Icons.Rounded.HdrAuto,
                    radius = shapeManager(isFirst = true, radius = s.cornerRadius),
                    actionType = ActionType.CUSTOM,
                    customAction = { showModePicker = true },
                )
            }
            item {
                SettingsBox(
                    title = "Dynamic color",
                    description = "Follow system wallpaper (Android 12+)",
                    icon = Icons.Rounded.Palette,
                    radius = shapeManager(radius = s.cornerRadius),
                    actionType = ActionType.SWITCH,
                    variable = s.dynamicColor,
                    switchEnabled = { vm.update { it.copy(dynamicColor = it) } },
                )
            }
            item {
                SettingsBox(
                    title = "Accent color",
                    description = "Custom seed (overrides dynamic)",
                    icon = Icons.Rounded.Palette,
                    radius = shapeManager(radius = s.cornerRadius),
                    actionType = ActionType.CUSTOM,
                    customAction = { showSeedPicker = true },
                )
            }
            item {
                SettingsBox(
                    title = "Dark theme",
                    description = "Extra-dark backgrounds",
                    icon = Icons.Rounded.DarkMode,
                    radius = shapeManager(radius = s.cornerRadius, isLast = true),
                    isEnabled = s.themeMode != ThemeMode.LIGHT,
                    actionType = ActionType.SWITCH,
                    variable = s.themeMode == ThemeMode.DARK,
                    switchEnabled = { vm.setThemeMode(if (it) ThemeMode.DARK else ThemeMode.SYSTEM) },
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            item {
                SettingsBox(
                    title = "Font size",
                    description = "${s.fontSize} sp",
                    icon = Icons.Rounded.FormatSize,
                    radius = shapeManager(isFirst = true, radius = s.cornerRadius),
                    actionType = ActionType.CUSTOM,
                    customAction = { showFontPicker = true },
                )
            }
            item {
                SettingsBox(
                    title = "Corner radius",
                    description = "${s.cornerRadius} dp",
                    icon = Icons.Rounded.RoundedCorner,
                    radius = shapeManager(radius = s.cornerRadius, isLast = true),
                    actionType = ActionType.CUSTOM,
                    customAction = { showRadiusPicker = true },
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            item {
                AppIconPicker(
                    radius = s.cornerRadius,
                    onMessage = {},
                )
            }
        }
    }
}

@Composable
private fun AppIconPicker(radius: Int, onMessage: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentIcon by remember(ctx) { appIconFlow(ctx) }.collectAsState(initial = com.zimapp.zim.ui.settings.AppIcon.DEFAULT)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("App icon", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.zimapp.zim.ui.settings.AppIcon.entries.forEach { option ->
                val selected = option == currentIcon
                Card(
                    modifier = Modifier.weight(1f).clickable {
                        scope.launch {
                            setAppIcon(ctx, option)
                            onMessage("Icon: ${option.label}")
                        }
                    },
                    shape = shapeManager(isBoth = true, radius = radius),
                    colors = if (selected) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
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
}

@Composable
private fun SeedPickerDialog(current: Int?, onPick: (Int?) -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = onExit) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Accent color", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
                val usingSystem = current == null
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (usingSystem) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onPick(null); onExit() }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "System colors",
                        fontWeight = if (usingSystem) FontWeight.Bold else FontWeight.Normal,
                        color = if (usingSystem) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
                }
                SeedAccents.chunked(3).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { color ->
                            val selected = current == color.toArgb()
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(color)
                                    .clickable { onPick(color.toArgb()); onExit() }
                                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontPickerDialog(current: Int, onPick: (Int) -> Unit, onExit: () -> Unit) {
    var slider by remember(current) { mutableFloatStateOf(((current - 12).toFloat() / 8).coerceIn(0f, 1f)) }
    val size = 12 + (8 * slider).toInt()
    Dialog(onDismissRequest = { onExit() }) {
        Column(
            Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .fillMaxWidth().padding(24.dp),
        ) {
            Text("Font size: $size sp", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("The quick brown fox", fontSize = size.sp, modifier = Modifier.padding(vertical = 16.dp))
            Slider(value = slider, onValueChange = { slider = it }, modifier = Modifier.fillMaxWidth())
            Box(
                Modifier.fillMaxWidth().padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                    .clickable { onPick(size); onExit() }.padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Apply", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RadiusPickerDialog(current: Int, onPick: (Int) -> Unit, onExit: () -> Unit) {
    var slider by remember(current) { mutableFloatStateOf(((current - 4).toFloat() / 28).coerceIn(0f, 1f)) }
    val radius = 4 + (28 * slider).toInt()
    Dialog(onDismissRequest = { onExit() }) {
        Column(
            Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .fillMaxWidth().padding(24.dp),
        ) {
            Text("Corner radius: $radius dp", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Box(
                Modifier.fillMaxWidth().height(62.dp).padding(vertical = 12.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(radius.dp)),
            )
            Slider(
                value = slider, onValueChange = { slider = it },
                colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                    .clickable { onPick(radius); onExit() }.padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Apply", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
