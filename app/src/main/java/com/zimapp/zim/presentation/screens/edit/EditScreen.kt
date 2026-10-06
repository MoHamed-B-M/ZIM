package com.zimapp.zim.presentation.screens.edit

import android.Manifest
import android.icu.text.SimpleDateFormat
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.ContentInfoCompat
import androidx.core.view.ViewCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.util.UUID
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zimapp.zim.R
import com.zimapp.zim.presentation.components.MoreButton
import com.zimapp.zim.presentation.components.NavigationIcon
import com.zimapp.zim.presentation.components.shareNote
import com.zimapp.zim.presentation.components.material.MaterialScaffold
import com.zimapp.zim.presentation.components.RedoButton
import com.zimapp.zim.presentation.components.SaveButton
import com.zimapp.zim.presentation.components.UndoButton
import com.zimapp.zim.presentation.components.markdown.MarkdownText
import com.zimapp.zim.presentation.screens.edit.components.CustomIconButton
import com.zimapp.zim.presentation.screens.edit.components.CustomTextField
import com.zimapp.zim.presentation.screens.edit.components.TextFormattingToolbar
import com.zimapp.zim.presentation.screens.edit.model.EditViewModel
import com.zimapp.zim.presentation.screens.settings.model.SettingsViewModel
import com.zimapp.zim.presentation.screens.settings.settings.shapeManager
import com.zimapp.zim.presentation.screens.settings.widgets.ActionType
import com.zimapp.zim.presentation.screens.settings.widgets.SettingsBox
import com.zimapp.zim.presentation.theme.FontUtils
import com.zimapp.zim.presentation.screens.settings.widgets.copyToClipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditNoteView(
    id: Int,
    settingsViewModel: SettingsViewModel,
    encrypted: Boolean = false,
    kind: String = "",
    isWidget: Boolean = false,
    onClickBack: () -> Unit
) {
    val viewModel: EditViewModel = hiltViewModel<EditViewModel>()
    viewModel.updateIsEncrypted(encrypted)
    viewModel.setupNoteData(id)
    viewModel.applyTemplate(kind)
    ObserveLifecycleEvents(viewModel)

    val pagerState = rememberPagerState(initialPage = if (id == 0 || isWidget || settingsViewModel.settings.value.editMode) 0 else 1, pageCount = { 2 })


    val coroutineScope = rememberCoroutineScope()

    MaterialScaffold(
        topBar = { if (!settingsViewModel.settings.value.minimalisticMode) TopBar(pagerState, coroutineScope,onClickBack, viewModel) },
        content = { PagerContent(pagerState, viewModel, settingsViewModel, onClickBack) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopBarActions(pagerState: PagerState, onClickBack: () -> Unit, viewModel: EditViewModel) {
    @Composable
    fun menuItemColors() = MenuDefaults.itemColors(
        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
        leadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    val context = LocalContext.current

    when (pagerState.currentPage) {

        0 -> {
            Row {
                if (viewModel.isDescriptionInFocus.value) {
                    RedoButton { viewModel.redo() }
                }
                SaveButton { onClickBack() }
            }
        }
        1 -> {
            Row {
                MoreButton {
                    viewModel.toggleEditMenuVisibility(true)
                }
                DropdownMenu(
                    expanded = viewModel.isEditMenuVisible.value,
                    onDismissRequest = { viewModel.toggleEditMenuVisibility(false) },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    if (viewModel.noteId.value != 0) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = "Delete")},
                            colors = menuItemColors(),
                            onClick = {
                                viewModel.toggleEditMenuVisibility(false)
                                viewModel.deleteNote(viewModel.noteId.value)
                                onClickBack()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.pinned)) },
                        leadingIcon = { Icon(if (viewModel.isPinned.value) Icons.Rounded.PushPin else Icons.Outlined.PushPin, contentDescription = "Pin")},
                        colors = menuItemColors(),
                        onClick = { viewModel.toggleNotePin(!viewModel.isPinned.value) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.copy)) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy")},
                        colors = menuItemColors(),
                        onClick = {
                            copyToClipboard(context, viewModel.noteDescription.value.text)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.information)) },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = "Information")},
                        colors = menuItemColors(),
                        onClick = {
                            viewModel.toggleEditMenuVisibility(false)
                            viewModel.toggleNoteInfoVisibility(true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = "Share")},
                        colors = menuItemColors(),
                        onClick = {
                            // Dismissing first: the chooser is its own window,
                            // and leaving this menu open under it reads as two
                            // competing layers of UI.
                            viewModel.toggleEditMenuVisibility(false)
                            // Title and body, so a shared note is identifiable
                            // in the target app rather than an anonymous body.
                            val title = viewModel.noteName.value.text
                            val body = viewModel.noteDescription.value.text
                            shareNote(
                                context = context,
                                text = buildString {
                                    if (title.isNotBlank()) append(title)
                                    if (body.isNotBlank()) {
                                        if (isNotEmpty()) append("\n\n")
                                        append(body)
                                    }
                                },
                                audioPaths = viewModel.audioClips.value,
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerContent(pagerState: PagerState, viewModel: EditViewModel,settingsViewModel: SettingsViewModel, onClickBack: () -> Unit) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.imePadding(),
        userScrollEnabled = !settingsViewModel.settings.value.disableSwipeInEditMode
    ) { page ->
        when (page) {
            0 -> EditScreen(viewModel, settingsViewModel, pagerState, onClickBack)
            1 -> PreviewScreen(viewModel, settingsViewModel, pagerState, onClickBack)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopBar(pagerState: PagerState,coroutineScope: CoroutineScope, onClickBack: () -> Unit, viewModel: EditViewModel) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = { ModeButton(pagerState, coroutineScope) },
        navigationIcon = {
            Row {
                NavigationIcon(onClickBack)
                if (pagerState.currentPage == 0 && viewModel.isDescriptionInFocus.value) {
                    UndoButton { viewModel.undo() }
                }
            }
     },
        actions = { TopBarActions(pagerState,  onClickBack, viewModel) }
    )
}

// Transport card for a note's voice clips, sitting at the bottom of the
// description card. Deliberately not a gate: the note saves with or without a
// recording, so declining the microphone permission costs the user nothing but
// the audio.
@Composable
private fun AudioTransportCard(viewModel: EditViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clips = viewModel.audioClips.value
    val isRecording = viewModel.isRecording.value
    val isPlaying = viewModel.isPlayingAudio.value
    val activeClip = viewModel.activeClip.value

    // Asked on each composition rather than cached: the permission can be
    // revoked from system settings while the editor stays open, and
    // checkSelfPermission is a cheap binder read.
    val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Only start once granted, and never on a denial: the note is still a
        // perfectly good text note without audio.
        if (granted) viewModel.startRecording()
    }

    // Single source of truth with the description field's layout, so the card
    // and the space it needs can never disagree.
    if (!viewModel.showAudioControls) return

    val onRecordTap = {
        when {
            isRecording -> viewModel.stopRecording()
            micGranted -> viewModel.startRecording()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        Unit
    }

    // Rounder while the card is just a record button, softer once it has grown
    // into a transport. Animated rather than swapped so the change of role reads
    // as the card growing into it.
    val cornerRadius by animateDpAsState(
        targetValue = if (clips.isEmpty()) 28.dp else 20.dp,
        animationSpec = tween(durationMillis = 220),
        label = "audioCardCornerRadius",
    )
    // A slow pulse while recording: the shape alone cannot say "in progress".
    // animateFloat is an extension on InfiniteTransition, not a top-level
    // function, hence the explicit transition below.
    val infiniteTransition = rememberInfiniteTransition(label = "audioCardTransition")
    // animateFloat on an InfiniteTransition requires an InfiniteRepeatableSpec,
    // so this keeps ticking even when idle - there is no way to hand it a
    // one-shot spec. Idle cost is bounded: the value stays at 1f and the
    // graphicsLayer lambda only invalidates the layer, not the composition.
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "audioCardPulse",
    )

    Surface(
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.graphicsLayer { scaleX = pulse; scaleY = pulse },
    ) {
        Column(
            modifier = Modifier.padding(AUDIO_CARD_INSET),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AUDIO_ROW_GAP),
        ) {
            if (isRecording) {
                Text(
                    text = stringResource(R.string.audio_recording_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (clips.isEmpty()) {
                // Nothing to transport yet, so just the record button.
                RecordButton(
                    isRecording = isRecording,
                    recordNew = false,
                    onClick = onRecordTap,
                )
            } else {
                // One row per clip: each plays alone, with its own wavy progress
                // bar. The old FilterChip preview run is gone on purpose - it only
                // selected a clip for a shared transport, and every clip now
                // carries its own play control.
                //
                // Progress is polled, not pushed: MediaPlayer exposes position only
                // as a getter, so while a clip plays a ticker re-reads it into
                // state, which is what resubscribes this composition to the poll.
                var livePositionMs by remember(isPlaying, activeClip) {
                    mutableIntStateOf(viewModel.activeClipPositionMs())
                }
                LaunchedEffect(isPlaying, activeClip) {
                    if (isPlaying) {
                        while (true) {
                            delay(CLIP_PROGRESS_POLL_MS)
                            livePositionMs = viewModel.activeClipPositionMs()
                        }
                    } else {
                        livePositionMs = viewModel.activeClipPositionMs()
                    }
                }

                clips.indices.forEach { index ->
                    val isActive = index == activeClip
                    val duration = if (isActive) viewModel.activeClipDurationMs() else 0
                    val position = if (isActive) livePositionMs else 0
                    AudioClipRow(
                        index = index,
                        isPlaying = isPlaying && isActive,
                        progress = if (duration > 0) {
                            (position.toFloat() / duration).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                        positionMs = position,
                        durationMs = duration,
                        showTime = isActive && duration > 0,
                        enabled = !isRecording,
                        onPlayPause = { viewModel.toggleClipPlayback(index) },
                        onDelete = { viewModel.deleteClip(index) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Record sits alone at the end, centred, so a new take never
                // shifts the rows above it.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RecordButton(
                        isRecording = isRecording,
                        recordNew = true,
                        onClick = onRecordTap,
                    )
                }
            }
        }
    }
}

// 48dp is the Android minimum touch target. Every control here meets it, which
// the previous 38dp Surface did not - Surface applies no minimum of its own, so
// that size was whatever it was told to be.
private val AUDIO_TOUCH_TARGET = 48.dp
private val AUDIO_CARD_INSET = 8.dp
private val AUDIO_ROW_GAP = 4.dp

/** How often the wavy progress bar re-reads the player position, in milliseconds. */
private const val CLIP_PROGRESS_POLL_MS = 120L

/**
 * The bouncy spring behind the play button's push scale.
 *
 * Same values as the pasted push-row spec (DampingRatioMediumBouncy,
 * StiffnessLow): it overshoots past its target and settles back, which is what
 * makes a tap read as pushing rather than easing.
 */
private fun <T> pushSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
)

/**
 * One clip's row: elastic play/pause, wavy progress, per-clip delete.
 *
 * Each clip plays alone - tapping play on one stops whatever else is loaded
 * first (see EditViewModel.toggleClipPlayback), because MediaPlayer holds a
 * single stream.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioClipRow(
    index: Int,
    isPlaying: Boolean,
    progress: Float,
    positionMs: Int,
    durationMs: Int,
    showTime: Boolean,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ElasticPlayButton(
            isPlaying = isPlaying,
            enabled = enabled,
            onClick = onPlayPause,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.audio_clip_label, index + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (showTime) {
                    Text(
                        text = "${formatClipTime(positionMs)} / ${formatClipTime(durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Fixed 240dp wide by the library (LinearContainerWidth) - it cannot
            // fill the row, so it sits centred under the label. Flat until the
            // clip moves: the default amplitude is 0 below 10% progress.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                LinearWavyProgressIndicator(progress = { progress })
            }
        }
        TransportButton(
            icon = Icons.Rounded.Delete,
            contentDescription = stringResource(R.string.delete_clip),
            enabled = enabled,
            onClick = onDelete,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

private fun formatClipTime(ms: Int): String {
    val totalSeconds = (ms / 1_000).coerceAtLeast(0)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

/**
 * Play/pause with the pasted push-row's elastic feel, adapted to the real API.
 *
 * Two honest deviations from the pasted snippet, both forced by what actually
 * ships in material3 1.5.0-alpha13:
 *
 * 1. AbsoluteSmoothCornerShape does not exist in this version (no Smooth*
 *    shape anywhere in the artifact), so the corner morph is a plain
 *    RoundedCornerShape driven by animateDpAsState - same animation, without
 *    the smooth-corner look.
 * 2. Layout-weight morphing is vacuous with a single control: one weighted
 *    child fills the row whatever its weight is, so there is nothing to push
 *    against. The push is expressed as a scale bounce on the same spring
 *    instead, and the weight API is dropped rather than kept as decoration.
 *
 * What is kept verbatim from the snippet: the bouncy weight spring
 * (DampingRatioMediumBouncy + StiffnessLow) now drives the scale, and the
 * corner morph (22.dp while playing, 50.dp pill while idle) runs on
 * MotionScheme.expressive().defaultSpatialSpec() - both of which do exist.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ElasticPlayButton(
    isPlaying: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tapped by remember { mutableStateOf(false) }

    val pushScale by animateFloatAsState(
        targetValue = if (tapped) 1.12f else 1f,
        animationSpec = pushSpring(),
        label = "elasticPlayScale",
    )
    val playCornerRadius by animateDpAsState(
        // Morphs between squircle and pill, per the snippet.
        targetValue = if (isPlaying) 22.dp else 50.dp,
        animationSpec = MotionScheme.expressive().defaultSpatialSpec(),
        label = "elasticPlayCornerRadius",
    )

    Surface(
        onClick = {
            tapped = true
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(playCornerRadius),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .size(AUDIO_TOUCH_TARGET)
            .graphicsLayer {
                scaleX = pushScale
                scaleY = pushScale
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.pause_audio else R.string.play_audio
                ),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    recordNew: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(AUDIO_TOUCH_TARGET),
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
            contentDescription = stringResource(
                when {
                    isRecording -> R.string.stop_recording
                    recordNew -> R.string.record_new_audio
                    else -> R.string.record_audio
                }
            ),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(AUDIO_TOUCH_TARGET),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
    }
}

@Composable
fun ObserveLifecycleEvents(viewModel: EditViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Stop before saving: the note on disk has to describe a finished
            // recording, and onCleared() only runs once the screen is gone.
            if (event == Lifecycle.Event.ON_STOP) {
                if (viewModel.isRecording.value) viewModel.stopRecording()
                viewModel.saveNote(viewModel.noteId.value)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomModal(viewModel: EditViewModel, settingsViewModel: SettingsViewModel) {
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = { viewModel.toggleNoteInfoVisibility(false) }
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        Column(
            modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 20.dp)
        ) {
            SettingsBox(
                size = 8.dp,
                title = stringResource(R.string.created_time),
                icon = Icons.Rounded.Numbers,
                actionType = ActionType.TEXT,
                radius = shapeManager(isFirst = true, radius = settingsViewModel.settings.value.cornerRadius),
                customText = sdf.format(viewModel.noteCreatedTime.value).toString()
            )
            SettingsBox(
                size = 8.dp,
                title = stringResource(R.string.words),
                icon = Icons.Rounded.Numbers,
                radius = shapeManager(radius = settingsViewModel.settings.value.cornerRadius),
                actionType = ActionType.TEXT,
                customText = if (viewModel.noteDescription.value.text != "") viewModel.noteDescription.value.text.split("\\s+".toRegex()).size.toString() else "0"
            )
            SettingsBox(
                size = 8.dp,
                title = stringResource(R.string.characters),
                icon = Icons.Rounded.Numbers,
                actionType = ActionType.TEXT,
                radius = shapeManager(radius = settingsViewModel.settings.value.cornerRadius, isLast = true),
                customText = viewModel.noteDescription.value.text.length.toString()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MinimalisticMode(
    alignment : Alignment.Vertical = Alignment.CenterVertically,
    viewModel: EditViewModel,
    modifier: Modifier = Modifier,
    isEnabled: Boolean, pagerState: PagerState,
    isExtremeAmoled: Boolean,
    showOnlyDescription: Boolean = false,
    onClickBack: () -> Unit, content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        verticalAlignment = alignment,
        modifier = modifier
            .fillMaxWidth()
            .then(if (showOnlyDescription) Modifier.padding(top = 8.dp) else Modifier)
    ) {
        if (!showOnlyDescription) {
            if (isEnabled) NavigationIcon(onClickBack)
            if (isEnabled && viewModel.isDescriptionInFocus.value) UndoButton { viewModel.undo() }
            content()
            if (isEnabled) TopBarActions(pagerState,  onClickBack, viewModel)
            if (isEnabled) ModeButton(pagerState, coroutineScope, isMinimalistic = true, isExtremeAmoled = isExtremeAmoled) } else {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnabled) NavigationIcon(onClickBack)
                    Spacer(modifier = Modifier.weight(1f))
                    if (isEnabled) ModeButton(pagerState, coroutineScope, isMinimalistic = true, isExtremeAmoled = isExtremeAmoled)
                    if (isEnabled) TopBarActions(pagerState,  onClickBack, viewModel)
                }
                content()
            }
        }
    }
}



private fun attachDroppedClip(
    context: Context,
    viewModel: EditViewModel,
    clip: ClipData,
) {
    val resolver = context.contentResolver
    val cur = viewModel.noteDescription.value
    val sb = StringBuilder(cur.text)
    if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append('\n')
    for (i in 0 until clip.itemCount) {
        val item = clip.getItemAt(i)
        val uri: Uri? = item.uri
        if (uri == null) {
            item.coerceToText(context)?.let { if (it.isNotBlank()) sb.append(it).append('\n') }
            continue
        }
        runCatching {
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "file"
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                ?: name.substringAfterLast('.', "")
            val dir = File(context.filesDir, if (mime.startsWith("image/")) "images" else "files").apply { mkdirs() }
            val dest = File(dir, "${UUID.randomUUID()}.$ext")
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            if (mime.startsWith("image/")) sb.append("!(${dest.absolutePath})\n")
            else sb.append("[$name](${dest.absolutePath})\n")
        }
    }
    viewModel.updateNoteDescription(cur.copy(text = sb.toString()))
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun EditScreen(viewModel: EditViewModel,settingsViewModel: SettingsViewModel, pagerState: PagerState,onClickBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    // Cross-app drag & drop (floating windows, split-screen): while the body
    // field is focused, dropped images/files land in app storage and are
    // inserted at the end of the note. System grants read access on drop.
    // NOTE: "*/*" is rejected by setOnReceiveContentListener — list types.
    DisposableEffect(view, viewModel.isDescriptionInFocus.value) {
        if (viewModel.isDescriptionInFocus.value) {
            ViewCompat.setOnReceiveContentListener(
                view,
                arrayOf("image/*", "video/*", "audio/*", "text/*", "application/*"),
            ) { _, payload ->
                // Only files/images are intercepted (copied + referenced);
                // plain text flows through the normal paste/drop path untouched.
                val hasUris = (0 until payload.clip.itemCount).any { i ->
                    payload.clip.getItemAt(i).uri != null
                }
                if (!hasUris) {
                    payload
                } else {
                    attachDroppedClip(context, viewModel, payload.clip)
                    null
                }
            }
        }
        onDispose {
            ViewCompat.setOnReceiveContentListener(view, null, null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, 16.dp, 16.dp, if (viewModel.isDescriptionInFocus.value && settingsViewModel.settings.value.isMarkdownEnabled) 2.dp else 16.dp)
    ) {
        MarkdownBox(
            isExtremeAmoled = settingsViewModel.settings.value.extremeAmoledMode,
            shape = shapeManager(radius = settingsViewModel.settings.value.cornerRadius, isFirst = true),
            content = {
                MinimalisticMode(
                    viewModel = viewModel,
                    modifier = Modifier.padding(top = 2.dp),
                    isEnabled = settingsViewModel.settings.value.minimalisticMode,
                    pagerState = pagerState,
                    isExtremeAmoled = settingsViewModel.settings.value.extremeAmoledMode,
                    onClickBack = { onClickBack() }
                ) {
                    CustomTextField(
                        value = viewModel.noteName.value,
                        modifier = Modifier.weight(1f),
                        onValueChange = { viewModel.updateNoteName(it) },
                        placeholder = stringResource(R.string.name),
                        useMonoSpaceFont = settingsViewModel.settings.value.useMonoSpaceFont
                    )
                }
            }
        )
        MarkdownBox(
            isExtremeAmoled = settingsViewModel.settings.value.extremeAmoledMode,
            shape = shapeManager(radius = settingsViewModel.settings.value.cornerRadius, isLast = true),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { viewModel.toggleIsDescriptionInFocus(it.isFocused) },
            content = {
                // A Column rather than an overlay: the transport card takes its
                // own height off the bottom instead of floating over the text.
                // Overlaying would mean the field had to reserve a guessed
                // height, and any mismatch between that guess and what the card
                // actually draws either hides the last line or leaves a gap.
                Column(modifier = Modifier.fillMaxSize()) {
                    AudioTransportCard(
                        viewModel = viewModel,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp),
                    )
                    CustomTextField(
                        value = viewModel.noteDescription.value,
                        onValueChange = { viewModel.updateNoteDescription(it) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        placeholder = stringResource(R.string.description),
                        useMonoSpaceFont = settingsViewModel.settings.value.useMonoSpaceFont
                    )
                }
            }
        )
        AnimatedVisibility(
            visible = viewModel.isDescriptionInFocus.value && settingsViewModel.settings.value.isMarkdownEnabled,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            TextFormattingToolbar(
                viewModel = viewModel,
                expanded = true,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewScreen(viewModel: EditViewModel, settingsViewModel: SettingsViewModel, pagerState: PagerState, onClickBack: () -> Unit) {
    if (viewModel.isNoteInfoVisible.value) BottomModal(viewModel, settingsViewModel)

    val focusManager = LocalFocusManager.current
    focusManager.clearFocus()
    val showOnlyDescription = viewModel.noteName.value.text.isNotBlank()

    Column(
        modifier = Modifier.padding(16.dp),
    ) {
        if (showOnlyDescription) {
            MarkdownBox(
                isExtremeAmoled = settingsViewModel.settings.value.extremeAmoledMode,
                shape = shapeManager(radius = settingsViewModel.settings.value.cornerRadius, isFirst = true),
                content = {
                    MinimalisticMode(
                        viewModel = viewModel,
                        isEnabled = settingsViewModel.settings.value.minimalisticMode,
                        pagerState = pagerState,
                        isExtremeAmoled = settingsViewModel.settings.value.extremeAmoledMode,
                        onClickBack = { onClickBack() }
                    ) {
                        MarkdownText(
                            markdown = viewModel.noteName.value.text,
                            isEnabled = settingsViewModel.settings.value.isMarkdownEnabled,
                            weight = FontWeight.Bold,
                            fontSize = FontUtils.getTitleFontSize(settingsViewModel),
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally),
                            onContentChange = { viewModel.updateNoteName(TextFieldValue(text = it)) },
                            radius = settingsViewModel.settings.value.cornerRadius,
                            settingsViewModel = settingsViewModel
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            )
        }
        MarkdownBox(
            isExtremeAmoled = settingsViewModel.settings.value.extremeAmoledMode,
            shape = shapeManager(radius = settingsViewModel.settings.value.cornerRadius, isLast = (showOnlyDescription), isBoth = (!showOnlyDescription)),
            modifier = Modifier.fillMaxSize(),
            content = {
                MinimalisticMode(
                    alignment = Alignment.Top,
                    viewModel = viewModel,
                    isExtremeAmoled = settingsViewModel.settings.value.extremeAmoledMode,
                    isEnabled = settingsViewModel.settings.value.minimalisticMode && !showOnlyDescription,
                    pagerState = pagerState,
                    showOnlyDescription = !showOnlyDescription,
                    onClickBack = { onClickBack() },
                ) {
                    MarkdownText(
                        radius = settingsViewModel.settings.value.cornerRadius,
                        markdown = viewModel.noteDescription.value.text,
                        isEnabled = settingsViewModel.settings.value.isMarkdownEnabled,
                        fontSize = FontUtils.getBodyFontSize(settingsViewModel),
                        modifier = Modifier
                            .padding(
                                16.dp,
                                top = if (showOnlyDescription) 16.dp else 6.dp,
                                16.dp,
                                16.dp
                            )
                            .weight(1f),
                        onContentChange = { viewModel.updateNoteDescription(TextFieldValue(text = it)) },
                        settingsViewModel = settingsViewModel)
                    }
            }
        )
    }
}

@Composable
fun MarkdownBox(
    isExtremeAmoled: Boolean,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(0.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .heightIn(max = 128.dp, min = 42.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = shape
            )
            .then(
                if (isExtremeAmoled) {
                    Modifier.border(
                        1.5.dp,
                        shape = shape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                } else Modifier
            )
    ) {
        content()
    }
    Spacer(modifier = Modifier.height(3.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModeButton(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    isMinimalistic: Boolean = false,
    isExtremeAmoled: Boolean = false,
) {
    Row {
        if (!isMinimalistic) {
            RenderButton(
                pagerState,
                coroutineScope,
                0,
                Icons.Rounded.Edit,
                false,
                isExtremeAmoled
            )
            RenderButton(
                    pagerState,
            coroutineScope,
            1,
            Icons.Rounded.RemoveRedEye,
            false,
            isExtremeAmoled
            )
        } else {
            val currentPage = pagerState.currentPage
            val icon = if (currentPage == 1) Icons.Rounded.Edit else Icons.Rounded.RemoveRedEye
            RenderButton(pagerState, coroutineScope, if (currentPage == 1) 0 else 1, icon, true, isExtremeAmoled)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RenderButton(
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    pageIndex: Int,
    icon: ImageVector,
    isMinimalistic: Boolean,
    isExtremeAmoled: Boolean
) {
    CustomIconButton(
        shape = if (isMinimalistic) RoundedCornerShape(100) else if (pageIndex == 0) RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp) else RoundedCornerShape(bottomEnd = 32.dp, topEnd = 32.dp),
        onClick = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pageIndex)
            }
        },
        icon = icon,
        isSelected = pagerState.currentPage == pageIndex,
        elevation = 0.dp
    )
}
