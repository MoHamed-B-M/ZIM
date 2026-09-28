package com.zimapp.zim.ui.navigation

import kotlinx.serialization.Serializable

@Serializable sealed interface Route {
    @Serializable data object List : Route
    @Serializable data object Todo : Route
    @Serializable data class Detail(val id: String? = null, val isChecklist: Boolean = false) : Route
    @Serializable data object Settings : Route
    @Serializable data object CloudConfig : Route
}
