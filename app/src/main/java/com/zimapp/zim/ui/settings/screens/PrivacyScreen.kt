package com.zimapp.zim.ui.settings.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.rounded.DoDisturbAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.domain.model.LockType
import com.zimapp.zim.ui.settings.AppSettingsViewModel
import com.zimapp.zim.ui.settings.easy.ActionType
import com.zimapp.zim.ui.settings.easy.CustomListDialog
import com.zimapp.zim.ui.settings.easy.SettingsBox
import com.zimapp.zim.ui.settings.easy.SettingsScaffold
import com.zimapp.zim.ui.settings.easy.shapeManager
import org.koin.androidx.compose.koinViewModel

// Privacy hub: screen protection + app lock setup (passcode / fingerprint /
// pattern). Lock enforcement happens at app start; per-resume locking dropped.
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onLockSetup: (LockType) -> Unit,
    vm: AppSettingsViewModel = koinViewModel(),
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        CustomListDialog(text = "App lock", onExit = { showPicker = false }) {
            item {
                LockOptionRow(
                    title = "Passcode",
                    description = if (s.lockType == LockType.PASSCODE) "Tap to remove" else "6-digit code",
                    active = s.lockType == LockType.PASSCODE,
                    radius = s.cornerRadius,
                    onClick = {
                        showPicker = false
                        if (s.lockType == LockType.PASSCODE) vm.clearLock()
                        else onLockSetup(LockType.PASSCODE)
                    },
                )
            }
            item {
                LockOptionRow(
                    title = "Fingerprint",
                    description = if (s.lockType == LockType.FINGERPRINT) "Tap to remove" else "Biometric unlock",
                    active = s.lockType == LockType.FINGERPRINT,
                    radius = s.cornerRadius,
                    onClick = {
                        showPicker = false
                        if (s.lockType == LockType.FINGERPRINT) vm.clearLock()
                        else onLockSetup(LockType.FINGERPRINT)
                    },
                )
            }
            item {
                LockOptionRow(
                    title = "Pattern",
                    description = if (s.lockType == LockType.PATTERN) "Tap to remove" else "Draw pattern",
                    active = s.lockType == LockType.PATTERN,
                    radius = s.cornerRadius,
                    onClick = {
                        showPicker = false
                        if (s.lockType == LockType.PATTERN) vm.clearLock()
                        else onLockSetup(LockType.PATTERN)
                    },
                )
            }
        }
    }

    SettingsScaffold(title = "Privacy & Lock", onBack = onBack) {
        LazyColumn {
            item {
                SettingsBox(
                    title = "Screen protection",
                    description = "Block screenshots and recents preview",
                    icon = Icons.Filled.RemoveRedEye,
                    radius = shapeManager(isBoth = true, radius = s.cornerRadius),
                    actionType = ActionType.SWITCH,
                    variable = s.screenProtection,
                    switchEnabled = { checked -> vm.update { it.copy(screenProtection = checked) } },
                )
            }
            item {
                SettingsBox(
                    title = "App lock",
                    description = when (s.lockType) {
                        LockType.PASSCODE -> "Passcode set"
                        LockType.PATTERN -> "Pattern set"
                        LockType.FINGERPRINT -> "Fingerprint set"
                        LockType.NONE -> "Off — passcode, fingerprint or pattern"
                    },
                    icon = Icons.Filled.Lock,
                    radius = shapeManager(isBoth = true, radius = s.cornerRadius),
                    actionType = ActionType.CUSTOM,
                    customAction = { showPicker = true },
                )
            }
            item {
                SettingsBox(
                    title = "Lock status",
                    description = if (s.lockType == LockType.NONE) "Unlocked" else "Locked on app start",
                    icon = Icons.Rounded.Security,
                    radius = shapeManager(isBoth = true, radius = s.cornerRadius),
                    actionType = ActionType.TEXT,
                    customText = if (s.lockType == LockType.NONE) "Off" else "On",
                )
            }
        }
    }
}

@Composable
private fun LockOptionRow(
    title: String,
    description: String,
    active: Boolean,
    radius: Int,
    onClick: () -> Unit,
) {
    SettingsBox(
        title = title,
        description = description,
        radius = shapeManager(isBoth = true, radius = radius),
        actionType = ActionType.CUSTOM,
        customAction = { onClick() },
        customButton = {
            Icon(
                imageVector = if (active) Icons.Rounded.DoDisturbAlt
                else Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.scale(0.75f),
            )
        },
    )
}
