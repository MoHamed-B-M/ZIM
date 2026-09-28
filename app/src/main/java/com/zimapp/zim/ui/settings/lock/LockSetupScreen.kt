package com.zimapp.zim.ui.settings.lock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zimapp.zim.domain.model.LockType

// Setup flow (expected == null → persist new secret) and unlock gate share this
// screen; MainActivity shows it fullscreen while locked, Privacy for setup.
@Composable
fun LockSetupScreen(
    type: LockType,
    expected: String?,
    hasLock: Boolean,
    onPinEntered: (String) -> Unit,
    onPatternEntered: (String) -> Unit,
    onUnlock: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.padding(vertical = 24.dp)) {
            when (type) {
                LockType.PASSCODE -> PasscodeLock(
                    expected = expected,
                    onPinEntered = onPinEntered,
                    onUnlock = onUnlock,
                    onCancel = onCancel,
                )
                LockType.PATTERN -> PatternLock(
                    expected = expected,
                    onPatternEntered = onPatternEntered,
                    onUnlock = onUnlock,
                    onCancel = onCancel,
                )
                LockType.FINGERPRINT -> FingerprintLock(
                    hasLock = hasLock,
                    onSuccess = onUnlock,
                    onCancel = onCancel,
                )
                LockType.NONE -> onCancel()
            }
        }
    }
}
