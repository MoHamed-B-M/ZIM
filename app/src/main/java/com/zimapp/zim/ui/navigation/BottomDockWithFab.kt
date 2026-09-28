package com.zimapp.zim.ui.navigation

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

data class DockFabMenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

// Unified bottom dock: expressive floating nav bar + expandable FAB menu in one row.
// With menu items the + button is a FloatingActionButtonMenu toggle (items stack
// above it); with only onFabClick it's a plain scalloped FAB; with neither the
// bar stands alone (e.g. Settings).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomDockWithFab(
    currentRoute: String,
    onTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
    fabMenuItems: List<DockFabMenuItem> = emptyList(),
    fabContentDescription: String = "Create",
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(menuOpen) { menuOpen = false }
    val showFab = onFabClick != null || fabMenuItems.isNotEmpty()

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

        if (showFab) {
            if (fabMenuItems.isNotEmpty()) {
                FloatingActionButtonMenu(
                    expanded = menuOpen,
                    button = {
                        ToggleFloatingActionButton(
                            checked = menuOpen,
                            onCheckedChange = { menuOpen = !menuOpen },
                        ) {
                            val imageVector by remember {
                                derivedStateOf {
                                    if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                                }
                            }
                            Icon(
                                painter = rememberVectorPainter(imageVector),
                                contentDescription = fabContentDescription,
                                modifier = Modifier.animateIcon({ checkedProgress }),
                            )
                        }
                    },
                ) {
                    fabMenuItems.forEach { item ->
                        FloatingActionButtonMenuItem(
                            onClick = { menuOpen = false; item.onClick() },
                            icon = { Icon(item.icon, contentDescription = null) },
                            text = { Text(item.label) },
                        )
                    }
                }
            } else {
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
                        imageVector = Icons.Filled.Add,
                        contentDescription = fabContentDescription,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
