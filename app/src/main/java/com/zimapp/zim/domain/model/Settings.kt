package com.zimapp.zim.domain.model

import com.zimapp.zim.presentation.navigation.NavRoutes

data class Settings(
    var defaultRouteType: String = NavRoutes.Home.route,
    var passcode: String? = null,
    var fingerprint: Boolean = false,
    var pattern: String? = null,
    val viewMode: Boolean = true,
    val automaticTheme: Boolean = true,
    val darkTheme: Boolean = false,
    var dynamicTheme: Boolean = false,
    var amoledTheme: Boolean = false,
    var customColor: Int = -7896468, // Default value that triggers system dynamic colors
    var minimalisticMode: Boolean = false,
    var extremeAmoledMode: Boolean = false,
    var isMarkdownEnabled: Boolean = true,
    var screenProtection: Boolean = false,
    var encryptBackup: Boolean = false,
    var sortDescending: Boolean = true,
    var vaultSettingEnabled: Boolean = false,
    var vaultEnabled: Boolean = false,
    var editMode: Boolean = false,
    var gallerySync: Boolean = true,
    var showOnlyTitle: Boolean = false,
    var termsOfService: Boolean = false,
    // First-run walkthrough. Covers what the in-app updater is and why it may
    // need “Install unknown apps”, so that grant is never a surprise later.
    var onboardingComplete: Boolean = false,
    var useMonoSpaceFont: Boolean = false,
    var lockImmediately: Boolean = true,
    var cornerRadius: Int = 32,
    var disableSwipeInEditMode: Boolean = false,
    var makeSearchBarLonger: Boolean = false,
    var fontSize: Int = 16
)

