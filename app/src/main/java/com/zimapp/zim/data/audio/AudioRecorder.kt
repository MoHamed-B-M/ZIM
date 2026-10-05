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

    // Whether a clip is loaded at all, playing or paused. This is the state that
    // makes a pause button possible: isPlaying alone cannot tell "paused" from
    // "never started", and the transport needs to tell them apart to decide
    // whether to resume or to load.
    val isLoaded: Boolean get() = player != null

    val loadedPath: String? get() = current

    // Both of these are only meaningful while a clip is prepared, and 0 when it
    // is not. Callers should treat 0 as "unknown" rather than "zero length".
    val positionMs: Int get() = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
    val durationMs: Int get() = runCatching { player?.duration ?: 0 }.getOrDefault(0)

    // Loads and starts [file], replacing anything already loaded. Returns whether
    // audio is playing afterwards, so a refused playback (file removed
    // underneath us, codec unavailable) does not leave a stuck indicator.
    fun play(file: File): Boolean {
        stop()
        val started = runCatching {
            val p = MediaPlayer()
            p.setDataSource(file.absolutePath)
            p.setOnCompletionListener {
                stop()
                onFinished?.invoke()
            }
            p.prepare()
            p.start()
            player = p
            current = file.absolutePath
        }.isSuccess
        if (!started) stop()
        return started
    }

    // Suspends without releasing the decoder, so a later resume continues from
    // the same position. This is the whole reason [toggle]'s release-the-player
    // behaviour had to go: it made "pause" indistinguishable from "restart".
    fun pause(): Boolean = runCatching {
        if (player?.isPlaying == true) {
            player?.pause()
            true
        } else {
            false
        }
    }.getOrDefault(false)

    fun resume(): Boolean = runCatching {
        if (player != null && player?.isPlaying == false) {
            player?.start()
            true
        } else {
            false
        }
    }.getOrDefault(false)

    /**
     * Jumps [deltaMs] from the current position, clamped to the clip so a skip
     * past either end parks rather than throwing. Returns the resulting
     * position, or -1 when nothing is loaded.
     */
    fun seekBy(deltaMs: Int): Int {
        val p = player ?: return -1
        return runCatching {
            val target = (p.currentPosition + deltaMs).coerceIn(0, p.duration)
            p.seekTo(target)
            target
        }.getOrDefault(-1)
    }

    fun stop() {
        // IllegalStateException from stop() is expected here: the player may be
        // idle or already completed, and neither is worth propagating.
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        current = null
    }
}