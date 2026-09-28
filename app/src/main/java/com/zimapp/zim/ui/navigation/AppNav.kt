package com.zimapp.zim.ui.navigation

import kotlinx.serialization.Serializable

@Serializable sealed interface Route {
    @Serializable data object List : Route
    @Serializable data object Todo : Route
    @Serializable data class Detail(val id: String? = null, val isChecklist: Boolean = false) : Route
    @Serializable data object Settings : Route
    @Serializable data object CloudConfig : Route
    @Serializable data object Colors : Route
    @Serializable data object Backup : Route
    @Serializable data object Privacy : Route
    @Serializable data object Language : Route
    @Serializable data object Tools : Route
    @Serializable data object About : Route
    @Serializable data object Updates : Route
    @Serializable data class LockSetup(val type: String) : Route
}
