package com.zimapp.zim.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
// Rendered by raw.githubusercontent; the updater reads its top section.
const val CHANGELOG_URL = "https://raw.githubusercontent.com/MoHamed-B-M/ZIM/beta/changelogs.md"

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

// Beta names look like "ZIM 1.0.0-dev(#18)" → 100000 + run number (see workflow).
private fun parseBetaCode(name: String): Int =
    Regex("""#(\d+)""").find(name)?.groupValues?.get(1)?.toIntOrNull()
        ?.let { BETA_CODE_OFFSET + it } ?: 0

// Stable tags look like "v1.0.0+1" → code after '+'.
private fun parseStableCode(tag: String): Int =
    Regex("""\+(\d+)\s*$""").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: 0

// "What's new" comes from the top section of changelogs.md (simple bullets).
// Falls back to "" when offline — the UI hides empty notes.
private fun changelogBullets(client: OkHttpClient): String {
    return runCatching {
        val req = Request.Builder().url(CHANGELOG_URL).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return ""
            val lines = (resp.body ?: return "").string().lineSequence()
                .map { it.trim() }
                .dropWhile { !it.startsWith("## [") }
                .drop(1)
                .takeWhile { !it.startsWith("## [") }
                .map { it.trim() }
                .filter { it.startsWith("- ") }
                .take(8)
                .map { line ->
                    "• " + line.removePrefix("- ").trim()
                        .replace("**", "")
                        .replace(Regex("""\[(.*?)]\(.*?\)"""), "$1")
                }
                .joinToString("\n")
                .take(600)
            lines
        }
    }.getOrDefault("")
}

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
                    // Prefer the fdroid flavor when present (no Play billing
                    // assumptions when sideloading), then arm64, then universal.
                    val pick = apks.firstOrNull { "fdroid" in it.optString("name").lowercase() }
                        ?: apks.firstOrNull { "arm64" in it.optString("name").lowercase() }
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
                        notes = changelogBullets(client),
                    )
                }
                error(if (beta) "No beta prerelease published yet" else "No stable release published yet")
            }
        }
    }

// Multi-connection ranged download (GitHub release assets support HTTP
// ranges): splits the file into parts fetched in parallel, then assembles.
// Falls back to a single stream for small files or servers ignoring ranges.
private class RangeNotSupported : Exception()

suspend fun downloadFast(
    client: OkHttpClient,
    url: String,
    dest: File,
    parts: Int = 4,
    onProgress: (Float) -> Unit,
): File = withContext(Dispatchers.IO) {
    val total = contentLength(client, url)
    if (total == null || total < 1_048_576L || parts < 2) {
        streamWhole(client, url, dest) { done, _ ->
            onProgress(if (total != null && total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f)
        }
        return@withContext dest
    }
    try {
        val done = AtomicLong(0)
        val chunk = total / parts
        coroutineScope {
            (0 until parts).map { i ->
                async {
                    val start = i * chunk
                    val end = if (i == parts - 1) total - 1 else start + chunk - 1
                    val part = File(dest.parent, "${dest.name}.part$i")
                    streamRange(client, url, part, start, end, expected = end - start + 1) {
                        onProgress((done.addAndGet(it).toFloat() / total).coerceIn(0f, 1f))
                    }
                    part
                }
            }.awaitAll().forEachIndexed { _, part ->
                dest.appendBytes(part.readBytes())
                part.delete()
            }
        }
        onProgress(1f)
        dest
    } catch (e: RangeNotSupported) {
        streamWhole(client, url, dest) { done, _ ->
            onProgress((done.toFloat() / total).coerceIn(0f, 1f))
        }
        dest
    }
}

private fun contentLength(client: OkHttpClient, url: String): Long? {
    val req = Request.Builder().url(url).head().build()
    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) return null
        return resp.header("Content-Length")?.toLongOrNull()
    }
}

private suspend fun streamWhole(
    client: OkHttpClient,
    url: String,
    dest: File,
    onChunk: (done: Long, total: Long) -> Unit,
) {
    if (dest.exists()) dest.delete()
    val req = Request.Builder().url(url).get().build()
    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("Download HTTP ${resp.code}")
        val body = resp.body ?: error("Empty response")
        var done = 0L
        dest.outputStream().use { out ->
            body.byteStream().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    onChunk(done, body.contentLength())
                }
            }
        }
    }
}

private suspend fun streamRange(
    client: OkHttpClient,
    url: String,
    dest: File,
    start: Long,
    end: Long,
    expected: Long,
    onChunk: (bytes: Long) -> Unit,
) {
    if (dest.exists()) dest.delete()
    val req = Request.Builder().url(url).header("Range", "bytes=$start-$end").get().build()
    client.newCall(req).execute().use { resp ->
        // 206 = partial content honored; anything else means ranges unsupported.
        if (resp.code != 206) throw RangeNotSupported()
        val body = resp.body ?: throw RangeNotSupported()
        dest.outputStream().use { out ->
            body.byteStream().use { input ->
                val buf = ByteArray(64 * 1024)
                var written = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    written += n
                    onChunk(n.toLong())
                }
                if (written != expected) throw RangeNotSupported()
            }
        }
    }
}

fun canInstallUnknown(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

// SHA-256 of the signing certs, or null when unreadable (API < 28).
private fun signerDigests(context: Context, archivePath: String? = null): List<String>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    val info = if (archivePath == null) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
        context.packageManager.getPackageArchiveInfo(archivePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: return null
    }
    val signers = info.signingInfo?.apkContentsSigners ?: return null
    val md = MessageDigest.getInstance("SHA-256")
    return signers.map { md.digest(it.toByteArray()).joinToString(":") { b -> "%02X".format(b) } }
}

// True when the APK can install over the current app. Catches the
// INSTALL_FAILED_UPDATE_INCOMPATIBLE case before the system installer fails.
fun canUpdateOverInstalled(context: Context, apkFile: File): Boolean {
    val current = signerDigests(context) ?: return true
    val next = signerDigests(context, apkFile.absolutePath) ?: return true
    return current.toSet() == next.toSet()
}

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
