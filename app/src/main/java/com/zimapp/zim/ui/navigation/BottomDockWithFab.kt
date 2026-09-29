package com.zimapp.zim.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Unified bottom dock: expressive floating nav bar + scalloped FAB in one row.
// Opening the menu swaps the bar for inline Note/Checklist pills (crossfade +
// size morph, single Row slot) — an overlay menu can't align with the centered
// dock and overlapped it. FAB hidden when onFabClick is null (Settings).
@Composable
fun BottomDockWithFab(
    currentRoute: String,
    onTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
    fabExpanded: Boolean = false,
    onPickNote: (() -> Unit)? = null,
    onPickChecklist: (() -> Unit)? = null,
    fabContentDescription: String = "Create",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = fabExpanded,
            transitionSpec = {
                (fadeIn(spring()) + scaleIn(spring())) togetherWith
                    (fadeOut(spring()) + scaleOut(spring())) using
                    SizeTransform(clip = false)
            },
            label = "dock-swap",
        ) { expanded ->
            if (expanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = { onPickNote?.invoke() },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Note")
                    }
                    FilledTonalButton(
                        onClick = { onPickChecklist?.invoke() },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Filled.Checklist, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Checklist")
                    }
                }
            } else {
                ExpressiveFloatingBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = onTab,
                )
            }
        }

        if (onFabClick != null) {
            FloatingActionButton(
                onClick = onFabClick,
                shape = RoundedCornerShape(
                    topStart = CornerSize(50),
                    topEnd = CornerSize(20),
                    bottomEnd = CornerSize(50),
                    bottomStart = CornerSize(20),
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = fabContentDescription,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}
