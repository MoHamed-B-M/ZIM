package com.zimapp.zim.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Unified bottom dock: expressive floating nav bar + scalloped FAB in one row.
// The FAB is owned by the screen (e.g. it toggles the FloatingActionButtonMenu
// rendered in the Scaffold FAB slot, where the menu gets proper overlay
// constraints). It appears only on tabs that pass onFabClick, scaling/fading
// in and out with spring physics so the bar recenters dynamically.
@Composable
fun BottomDockWithFab(
    currentRoute: String,
    onTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
    fabExpanded: Boolean = false,
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
        ExpressiveFloatingBottomBar(
            currentRoute = currentRoute,
            onNavigate = onTab,
        )

        AnimatedVisibility(
            visible = onFabClick != null,
            enter = fadeIn(spring()) + scaleIn(spring()),
            exit = fadeOut(spring()) + scaleOut(spring()),
        ) {
            FloatingActionButton(
                onClick = { onFabClick?.invoke() },
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
