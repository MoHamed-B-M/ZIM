package com.zimapp.zim.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

const val UPDATE_OWNER = "MoHamed-B-M"
const val UPDATE_REPO = "ZIM"
// Rolling beta prerelease tag maintained by build.yaml.
const val BETA_TAG = "beta-latest"
// Must mirror the workflow's BETA_CODE_OFFSET so beta codes compare correctly.
const val BETA_CODE_OFFSET = 100000

data class RemoteUpdate(
    val beta: Boolean,
    val tag: String,
    val title: String,
    val versionCode: Int,
    val apkUrl: String,
    val apkName: String,
    val publishedAt: String,
    val notes: String,
)

fun installedVersionCode(context: Context): Long {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
    else @Suppress("DEPRECATION") info.versionCode.toLong()
}

fun installedVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

// Beta names look like "Flambo 1.6.5-dev(#12)" → 100000 + run number (see workflow).
private fun parseBetaCode(name: String): Int =
    Regex("""#(\d+)""").find(name)?.groupValues?.get(1)?.toIntOrNull()
        ?.let { BETA_CODE_OFFSET + it } ?: 0

// Stable tags look like "v1.6.5+8" → code after '+'.
private fun parseStableCode(tag: String): Int =
    Regex("""\+(\d+)\s*$""").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0

// Beta channel → rolling prerelease; stable channel → latest finished release.
suspend fun fetchUpdate(client: OkHttpClient, beta: Boolean): Result<RemoteUpdate> =
    withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$UPDATE_OWNER/$UPDATE_REPO/releases?per_page=20")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("GitHub API HTTP ${resp.code}")
                val arr = JSONArray(resp.body!!.string())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    if (o.optBoolean("draft", false)) continue
                    if (o.optBoolean("prerelease", false) != beta) continue
                    val assets = o.optJSONArray("assets") ?: JSONArray()
                    val apks = (0 until assets.length())
                        .map { assets.getJSONObject(it) }
                        .filter { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    if (apks.isEmpty()) continue
                    // Prefer the arm64 build (most devices), then universal.
                    val pick = apks.firstOrNull { "arm64" in it.optString("name").lowercase() }
                        ?: apks.firstOrNull { "universal" in it.optString("name").lowercase() }
                        ?: apks.first()
                    val tag = o.optString("tag_name")
                    val name = o.optString("name").ifBlank { tag }
                    return@runCatching RemoteUpdate(
                        beta = beta, tag = tag, title = name,
                        versionCode = if (beta) parseBetaCode(name) else parseStableCode(tag),
                        apkUrl = pick.getString("browser_download_url"),
                        apkName = pick.optString("name"),
                        publishedAt = o.optString("published_at").take(10),
                        notes = o.optString("body").lineSequence().take(8).joinToString("\n").take(600),
                    )
                }
                error(if (beta) "No beta prerelease published yet" else "No stable release published yet")
            }
        }
    }

fun enqueueDownload(context: Context, url: String, fileName: String): Long {
    val dm = context.getSystemService(DownloadManager::class.java)
    val req = DownloadManager.Request(Uri.parse(url))
        .setTitle(fileName)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        .setMimeType("application/vnd.android.package-archive")
    return dm.enqueue(req)
}

sealed interface DownloadState {
    data class Running(val downloaded: Long, val total: Long) : DownloadState
    data class Done(val file: File) : DownloadState
    data class Failed(val reason: String) : DownloadState
}

fun queryDownload(context: Context, id: Long): DownloadState {
    val dm = context.getSystemService(DownloadManager::class.java)
    dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
        if (!c.moveToFirst()) return DownloadState.Failed("Download not found")
        val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        return when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                val uri = Uri.parse(c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)))
                val file = File(uri.path!!)
                if (file.exists()) DownloadState.Done(file)
                else DownloadState.Failed("File missing after download")
            }
            DownloadManager.STATUS_FAILED -> {
                val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                DownloadState.Failed("Download failed (reason $reason)")
            }
            else -> {
                val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                DownloadState.Running(done, total)
            }
        }
    }
}

fun canInstallUnknown(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

fun openInstallPermission(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    )
}
