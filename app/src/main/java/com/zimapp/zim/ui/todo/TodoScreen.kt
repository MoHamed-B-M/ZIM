package com.zimapp.zim.ui.todo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zimapp.zim.ui.navigation.BottomDockWithFab
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    currentTab: String,
    onTab: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onNewChecklist: () -> Unit,
    vm: TodoViewModel = koinViewModel(),
) {
    val notes by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("To-Do") }) },
        bottomBar = {
            BottomDockWithFab(
                currentRoute = currentTab,
                onTab = onTab,
                onFabClick = onNewChecklist,
                fabContentDescription = "New checklist",
            )
        },
    ) { padding ->
        if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No checklist tasks — create one from +",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    start = 12.dp, end = 12.dp, bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notes, key = { it.note.id }) { item ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenNote(item.note.id) },
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = item.note.title.ifBlank { "(untitled)" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            item.tasks.forEach { task ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        vm.toggle(item.note.id, task.lineIndex)
                                    },
                                ) {
                                    Checkbox(
                                        checked = task.done,
                                        onCheckedChange = { vm.toggle(item.note.id, task.lineIndex) },
                                    )
                                    Text(
                                        text = task.text.ifBlank { "(empty)" },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (task.done) TextDecoration.LineThrough else null,
                                        ),
                                        color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
