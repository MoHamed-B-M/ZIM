package com.zimapp.zim.ui.settings.lock

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

// Ported from EasyNotes FingerprintLock; AppCompatActivity base supplied by ZIM's MainActivity.
@Composable
fun FingerprintLock(
    hasLock: Boolean,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current

    BackHandler {
        if (hasLock) (context as? ComponentActivity)?.finish() else onCancel()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.1f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.scale(2f),
        )
        LaunchedEffect(Unit) {
            showZimBiometricPrompt(
                context = context,
                onSuccess = onSuccess,
                onRetry = { showZimBiometricPrompt(context, onSuccess, {}) },
            )
        }
        Text("Touch the sensor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

fun showZimBiometricPrompt(
    context: Context,
    onSuccess: () -> Unit,
    onRetry: () -> Unit,
) {
    val activity = context as? AppCompatActivity ?: return
    val executor: Executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onRetry()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onRetry()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock ZIM")
        .setSubtitle("ZIM")
        .setNegativeButtonText("Cancel")
        .setConfirmationRequired(true)
        .build()
    prompt.authenticate(info)
}
