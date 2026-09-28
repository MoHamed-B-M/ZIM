package com.example.expressivenotes.ui.navigation

import kotlinx.serialization.Serializable

@Serializable sealed interface Route {
    @Serializable data object List : Route
    @Serializable data class Detail(val id: String? = null) : Route // null = new note
    @Serializable data object Settings : Route
    @Serializable data object CloudConfig : Route
}
