package com.zimapp.zim.presentation.screens.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zimapp.zim.R
import com.zimapp.zim.data.update.canInstallUnknown
import com.zimapp.zim.data.update.openInstallPermission
import com.zimapp.zim.presentation.components.material.MaterialScaffold
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import kotlinx.coroutines.launch

// First-run walkthrough. Its single job is to make the "Install unknown apps"
// grant understandable before it is ever needed: the updater asks for it again
// at the moment of install, and a permission the user has already been walked
// through is far less alarming than a sudden jump to system settings.
//
// Nothing here is a gate. Every step can be skipped, and finishing is always
// available — the permission is genuinely optional, since updates also work by
// downloading the APK from the releases page.
@Composable
fun OnboardingScreen(
    settingsViewModel: SettingsViewModel,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val steps = onboardingSteps()
    // Remembered rather than saveable: a half-read walkthrough is not worth
    // restoring across process death, and re-entering from Settings should
    // always start at step one rather than wherever the user left off.
    val pagerState = rememberPagerState(pageCount = { steps.size })
    var permissionGranted by remember { mutableStateOf(canInstallUnknown(context)) }

    // The system settings screen is not a contract result, so the only reliable
    // signal that the user flipped the switch is coming back to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = canInstallUnknown(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun finish() {
        settingsViewModel.update(
            settingsViewModel.settings.value.copy(onboardingComplete = true)
        )
        onFinished()
    }

    // PagerState has no nextPage()/previousPage(); animating to the target
    // index is the supported way to move programmatically.
    val pagerScope = rememberCoroutineScope()
    fun goToPage(page: Int) {
        pagerScope.launch { pagerState.animateScrollToPage(page) }
    }

    MaterialScaffold(
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                OnboardingPager(
                    steps = steps,
                    pagerState = pagerState,
                    permissionGranted = permissionGranted,
                    // weight() only exists in a Column scope, so the caller
                    // supplies it rather than this function guessing.
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.height(24.dp))
                OnboardingDots(count = steps.size, current = pagerState.currentPage)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(onClick = { goToPage(pagerState.currentPage - 1) }) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    val isLastPage = pagerState.currentPage == steps.lastIndex
                    if (isLastPage) {
                        // Two separate actions on the final step: granting the
                        // permission sends the user to system settings, while
                        // finishing never depends on that succeeding.
                        if (!permissionGranted) {
                            Button(
                                onClick = {
                                    runCatching { openInstallPermission(context) }
                                        .onFailure { permissionGranted = canInstallUnknown(context) }
                                },
                            ) {
                                Text(stringResource(R.string.onboarding_allow))
                            }
                        }
                        TextButton(onClick = { finish() }) {
                            Text(
                                stringResource(
                                    if (permissionGranted) R.string.onboarding_get_started
                                    else R.string.onboarding_not_now
                                )
                            )
                        }
                    } else {
                        Button(onClick = { goToPage(pagerState.currentPage + 1) }) {
                            Text(stringResource(R.string.onboarding_next))
                        }
                        TextButton(onClick = { finish() }) {
                            Text(stringResource(R.string.onboarding_skip))
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun OnboardingPager(
    steps: List<OnboardingStep>,
    pagerState: PagerState,
    permissionGranted: Boolean,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
    ) { page ->
        val step = steps[page]
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(step.title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(step.body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )
            Spacer(Modifier.height(24.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                step.bullets.forEachIndexed { index, bullet ->
                    val text = stringResource(bullet)
                    val highlighted = step.permissionBullets[index] == true
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (highlighted && permissionGranted) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.Lock
                            },
                            contentDescription = null,
                            tint = if (highlighted && permissionGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highlighted && permissionGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 10.dp else 8.dp)
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape,
                    )
            )
        }
    }
}

private data class OnboardingStep(
    val icon: ImageVector,
    val title: Int,
    val body: Int,
    val bullets: List<Int>,
    // Bullets that depend on the install permission get a live checkmark once
    // the user has granted it, so the last page reflects reality.
    val permissionBullets: Map<Int, Boolean> = emptyMap(),
)

private fun onboardingSteps(): List<OnboardingStep> = listOf(
    OnboardingStep(
        icon = Icons.Rounded.Lock,
        title = R.string.onboarding_welcome_title,
        body = R.string.onboarding_welcome_body,
        bullets = listOf(
            R.string.onboarding_welcome_bullet_local,
            R.string.onboarding_welcome_bullet_markdown,
            R.string.onboarding_welcome_bullet_widget,
        ),
    ),
    OnboardingStep(
        icon = Icons.Rounded.SystemUpdate,
        title = R.string.onboarding_updates_title,
        body = R.string.onboarding_updates_body,
        bullets = listOf(
            R.string.onboarding_updates_bullet_signed,
            R.string.onboarding_updates_bullet_scope,
            R.string.onboarding_updates_bullet_optional,
        ),
    ),
    OnboardingStep(
        icon = Icons.Rounded.SystemUpdate,
        title = R.string.onboarding_permission_title,
        body = R.string.onboarding_permission_body,
        bullets = listOf(
            R.string.onboarding_permission_bullet_enable,
            R.string.onboarding_permission_bullet_usage,
            R.string.onboarding_permission_bullet_revoke,
        ),
        permissionBullets = mapOf(0 to true, 1 to true),
    ),
)