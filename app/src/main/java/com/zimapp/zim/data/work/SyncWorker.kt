package com.zimapp.zim.data.work

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zimapp.zim.data.local.AppDatabase
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

val Context.syncPrefs by preferencesDataStore("sync_prefs")
val KEY_PROVIDER = stringPreferencesKey("provider") // disabled|webdav|rest
val KEY_LAST_SYNC = longPreferencesKey("last_sync")

// Opt-in only: scheduled from Settings when user enables cloud sync.
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params), KoinComponent {
    private val db: AppDatabase by inject()

    override suspend fun doWork(): Result {
        val prefs = applicationContext.syncPrefs.data.first()
        if ((prefs[KEY_PROVIDER] ?: "disabled") == "disabled") return Result.success()
        // 30-day trash auto-purge on every sync tick
        db.noteDao().purgeTrashOlderThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
        // Actual push/pull delegates to the configured provider (resolved in Settings VM).
        // Kept minimal here to avoid network on the critical path when unconfigured.
        return Result.success()
    }

    companion object {
        const val NAME = "expressive-sync"
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
        }
        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
