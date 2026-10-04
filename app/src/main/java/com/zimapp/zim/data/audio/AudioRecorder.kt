package com.zimapp.zim.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

// Voice notes are stored under filesDir rather than shared media: no storage
// permission is involved, and nothing lands where another app can enumerate it.
// AAC in an MPEG-4 container is the format every Android release can both write
// and play back.
private const val AUDIO_DIR = "audio"
private const val AUDIO_EXT = ".m4a"

fun newAudioFile(context: Context): File {
    val dir = File(context.filesDir, AUDIO_DIR).apply { mkdirs() }
    return File(dir, "${UUID.randomUUID()}$AUDIO_EXT")
}

// Recording files are ours alone, so removal never needs to be careful about
// another app holding one open — except while a share is in flight, which is
// why this only ever runs on a file no live share references.
fun deleteAudioFile(path: String?) {
    val file = path?.let(::File) ?: return
    if (file.parentFile?.name == AUDIO_DIR) runCatching { file.delete() }
}

// MediaRecorder throws on nearly every misstep (mic busy, permission revoked
// between the check and the call, storage gone) and cannot be reused after
// stop(). So every call is guarded and a failure leaves the caller with no
// audio at all rather than a half-written file it would later try to play.
class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var pending: File? = null

    fun start(): Boolean = runCatching {
        val file = newAudioFile(context)
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        pending = file
        true
    }.getOrElse {
        release()
        pending = null
        false
    }

    // Returns the finished file, or null when there is nothing usable. A tap
    // that releases too early produces a zero-length file, which would
    // otherwise surface as a silent, unplayable attachment.
    fun stop(): File? {
        val r = recorder ?: return null
        val file = pending
        val stopped = runCatching { r.stop() }.isSuccess
        release()
        pending = null
        if (!stopped || file == null || !file.isFile || file.length() == 0L) {
            file?.delete()
            return null
        }
        return file
    }

    fun cancel() {
        val file = pending
        runCatching { recorder?.stop() }
        release()
        pending = null
        file?.delete()
    }

    private fun release() {
        runCatching { recorder?.release() }
        recorder = null
    }
}

class AudioPlayer {
    private var player: MediaPlayer? = null
    private var current: String? = null

    // Fires when a clip reaches its end on its own, so the caller can drop its
    // "playing" flag without polling. Set by the owner, which owns that state.
    var onFinished: (() -> Unit)? = null

    val isPlaying: Boolean get() = runCatching { player?.isPlaying == true }.getOrDefault(false)

    // Starts the clip, or stops it when it is already the one playing. Returns
    // whether audio is playing afterwards, so a refused playback (file removed
    // underneath us, codec unavailable) does not leave a stuck indicator.
    fun toggle(file: File): Boolean {
        val path = file.absolutePath
        if (current == path && player != null) {
            stop()
            return false
        }
        stop()
        val started = runCatching {
            val p = MediaPlayer()
            p.setDataSource(path)
            p.setOnCompletionListener {
                stop()
                onFinished?.invoke()
            }
            p.prepare()
            p.start()
            player = p
            current = path
        }.isSuccess
        if (!started) stop()
        return started
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        current = null
    }
}