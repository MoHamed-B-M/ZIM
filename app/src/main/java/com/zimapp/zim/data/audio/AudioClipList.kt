package com.zimapp.zim.data.audio

/**
 * Codec for the newline-separated clip list held in the note's single
 * `audio_path` column.
 *
 * Android absolute paths cannot contain a newline, so joining on `"\n"` is
 * unambiguous in both directions without a schema change. One clip therefore
 * encodes to exactly the bare path it has always been, which is what lets every
 * note recorded before multi-clip support keep playing with no migration at all.
 *
 * Trade-off, stated plainly: this trades the column's type safety for that. A
 * hand-edited or truncated value decodes to whatever survives rather than
 * failing loudly, and clips cannot be queried or joined in SQL. The alternative
 * is a real `audio_clips` table, which costs a migration — and Room validates
 * migrations at runtime, never at compile time, so nothing in CI would catch a
 * mismatch until it threw on every existing install's first launch.
 */
object AudioClipList {
    private const val SEPARATOR = "\n"

    fun encode(paths: List<String>): String = paths.joinToString(SEPARATOR)

    fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
}