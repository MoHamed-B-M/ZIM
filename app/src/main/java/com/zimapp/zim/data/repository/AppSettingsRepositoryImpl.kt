package com.zimapp.zim.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zimapp.zim.domain.model.AppSettings
import com.zimapp.zim.domain.model.LockType
import com.zimapp.zim.domain.model.ThemeMode
import com.zimapp.zim.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.appSettingsStore by preferencesDataStore("app_settings")

private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC = booleanPreferencesKey("dynamic_color")
    val SEED = intPreferencesKey("seed_color") // -1 = null
    val FONT = intPreferencesKey("font_size")
    val RADIUS = intPreferencesKey("corner_radius")
    val MARKDOWN = booleanPreferencesKey("markdown")
    val MONO = booleanPreferencesKey("monospace")
    val SCREEN = booleanPreferencesKey("screen_protection")
    val LOCK = stringPreferencesKey("lock_type")
    val SECRET = stringPreferencesKey("lock_secret")
    val ENCRYPT = booleanPreferencesKey("encrypt_backup")
}

class AppSettingsRepositoryImpl(private val context: Context) : AppSettingsRepository {
    override val settings: Flow<AppSettings> = context.appSettingsStore.data.map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(context.appSettingsStore.data.first().toSettings())
        context.appSettingsStore.edit {
            it[Keys.THEME_MODE] = next.themeMode.name
            it[Keys.DYNAMIC] = next.dynamicColor
            it[Keys.SEED] = next.seedColor ?: -1
            it[Keys.FONT] = next.fontSize
            it[Keys.RADIUS] = next.cornerRadius
            it[Keys.MARKDOWN] = next.markdownEnabled
            it[Keys.MONO] = next.monospaceFont
            it[Keys.SCREEN] = next.screenProtection
            it[Keys.LOCK] = next.lockType.name
            if (next.lockSecret == null) it.remove(Keys.SECRET) else it[Keys.SECRET] = next.lockSecret
            it[Keys.ENCRYPT] = next.encryptBackup
        }
    }

    private fun androidx.datastore.preferences.core.Preferences.toSettings() = AppSettings(
        themeMode = runCatching { ThemeMode.valueOf(this[Keys.THEME_MODE] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = this[Keys.DYNAMIC] ?: true,
        seedColor = (this[Keys.SEED] ?: -1).takeIf { it != -1 },
        fontSize = this[Keys.FONT] ?: 16,
        cornerRadius = this[Keys.RADIUS] ?: 28,
        markdownEnabled = this[Keys.MARKDOWN] ?: true,
        monospaceFont = this[Keys.MONO] ?: false,
        screenProtection = this[Keys.SCREEN] ?: false,
        lockType = runCatching { LockType.valueOf(this[Keys.LOCK] ?: "NONE") }.getOrDefault(LockType.NONE),
        lockSecret = this[Keys.SECRET],
        encryptBackup = this[Keys.ENCRYPT] ?: false,
    )
}
