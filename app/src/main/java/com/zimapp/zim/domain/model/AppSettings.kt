package com.zimapp.zim.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class LockType { NONE, PASSCODE, PATTERN, FINGERPRINT }

// Persisted UI/behavior settings (ported from EasyNotes Settings, trimmed to
// what ZIM wires: theme + lock + backup flags; sync/icon keys stay in syncPrefs).
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val seedColor: Int? = null, // null = system dynamic / fallback scheme
    val fontSize: Int = 16,
    val cornerRadius: Int = 28,
    val markdownEnabled: Boolean = true,
    val monospaceFont: Boolean = false,
    val screenProtection: Boolean = false,
    val lockType: LockType = LockType.NONE,
    val lockSecret: String? = null, // passcode or pattern (memory + DataStore)
    val encryptBackup: Boolean = false,
)
