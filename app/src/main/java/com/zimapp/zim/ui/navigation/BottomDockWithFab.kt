package com.zimapp.zim.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class DockFabMenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

// Unified bottom dock: expressive floating nav bar + scalloped FAB in one row.
// FAB hidden when neither onFabClick nor fabMenuItems is given (e.g. Settings).
// With menu items, a tap opens a dropdown; otherwise onFabClick runs directly.
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
            Box {
                FloatingActionButton(
                    onClick = {
                        if (fabMenuItems.isNotEmpty()) menuOpen = true
                        else onFabClick?.invoke()
                    },
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
                        imageVector = if (menuOpen) Icons.Rounded.Close else Icons.Rounded.Add,
                        contentDescription = fabContentDescription,
                        modifier = Modifier.size(26.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    fabMenuItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label) },
                            leadingIcon = { Icon(item.icon, contentDescription = null) },
                            onClick = { menuOpen = false; item.onClick() },
                        )
                    }
                }
            }
        }
    }
}
