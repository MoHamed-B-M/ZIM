package com.example.expressivenotes.ui.notes_list.components

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

// Thin wrapper over M3 Expressive SearchBar input (catalog: SearchBarSamples.kt).
// Voice/mic intentionally omitted to keep INTERNET-free default.
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ExpressiveSearchInput(query: String, onQuery: (String) -> Unit) {
    val barState = rememberSearchBarState()
    val fieldState = rememberTextFieldState(query)
    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }.debounce(150).collect(onQuery)
    }
    SearchBarDefaults.InputField(
        textFieldState = fieldState,
        searchBarState = barState,
        onSearch = {},
        placeholder = { Text("Search notes") },
        leadingIcon = {
            if (barState.currentValue == androidx.compose.material3.SearchBarValue.Expanded)
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            else Icon(Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Clear")
            }
        },
    )
}
