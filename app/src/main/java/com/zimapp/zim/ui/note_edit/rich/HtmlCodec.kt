package com.zimapp.zim.ui.note_edit.rich

import androidx.compose.ui.text.ExperimentalTextApi

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Minimal HTML subset for rich notes. Plain lines (incl. "- [ ]" checklist and
// "![](...)" image lines) pass through verbatim so list screens keep working.
@OptIn(ExperimentalTextApi::class)
object HtmlCodec {
    private val ImgLine = Regex("""^!\(.+\)$""")

    fun toHtml(doc: AnnotatedString): String {
        val text = doc.text
        if (text.isEmpty()) return ""
        // Line boundaries to keep block lines verbatim.
        val lineStarts = mutableListOf(0)
        text.forEachIndexed { i, c -> if (c == '\n') lineStarts.add(i + 1) }
        val out = StringBuilder()
        lineStarts.forEachIndexed { li, start ->
            val end = if (li + 1 < lineStarts.size) lineStarts[li + 1] - 1 else text.length
            val line = text.substring(start, end)
            if (line.matches(ImgLine)) {
                val src = line.removePrefix("!(").removeSuffix(")")
                out.append("<img src=\"${esc(src)}\"/>")
            } else {
                // Slice spans intersecting this line.
                val bounds = sortedSetOf(start, end)
                doc.spanStyles.forEach {
                    if (it.start < end && it.end > start) {
                        bounds.add(it.start.coerceAtLeast(start))
                        bounds.add(it.end.coerceAtMost(end))
                    }
                }
                val pts = bounds.toList()
                // Paragraph alignment for the line.
                val align = doc.paragraphStyles
                    .firstOrNull { it.start < end && it.end > start }
                    ?.item?.textAlign ?: TextAlign.Unspecified
                val alignName = when (align) {
                    TextAlign.Center -> "center"
                    TextAlign.End -> "end"
                    TextAlign.Justify -> "justify"
                    TextAlign.Left, TextAlign.Start -> "left"
                    else -> null
                }
                if (alignName != null) {
                    out.append("<p align=\"$alignName\">")
                }
                for (i in 0 until pts.size - 1) {
                    val s = pts[i]
                    val e = pts[i + 1]
                    if (s >= e) continue
                    val styles = doc.spanStyles.filter { it.start <= s && it.end >= e }.map { it.item }
                    val links = doc.getStringAnnotations(s, e).filter { it.tag == "URL" }
                    var chunk = esc(text.substring(s, e))
                    styles.forEach { chunk = wrap(chunk, it) }
                    links.forEach { chunk = "<a href=\"${esc(it.item)}\">$chunk</a>" }
                    out.append(chunk)
                }
                if (alignName != null) out.append("</p>")
            }
            if (li + 1 < lineStarts.size) out.append("\n")
        }
        return out.toString()
    }

    private fun wrap(chunk: String, s: SpanStyle): String {
        var c = chunk
        if (s.fontWeight == FontWeight.Bold) c = "<b>$c</b>"
        if (s.fontStyle == FontStyle.Italic) c = "<i>$c</i>"
        if (s.textDecoration?.contains(TextDecoration.Underline) == true) c = "<u>$c</u>"
        if (s.textDecoration?.contains(TextDecoration.LineThrough) == true) c = "<s>$c</s>"
        if (s.fontSize != TextUnit.Unspecified) c = "<font size=\"${s.fontSize.value.toInt()}\">$c</font>"
        if (s.fontFamily == FontFamily.Serif) c = "<font face=\"serif\">$c</font>"
        if (s.fontFamily == FontFamily.Monospace) c = "<font face=\"monospace\">$c</font>"
        if (s.color != Color.Unspecified) c = "<color value=\"#${s.color.value.toUInt().toString(16).uppercase().padStart(8, '0')}\">$c</color>"
        if (s.background != Color.Unspecified) c = "<hl value=\"#${s.background.value.toUInt().toString(16).uppercase().padStart(8, '0')}\">$c</hl>"
        return c
    }

    fun fromHtml(html: String): AnnotatedString = buildAnnotatedString {
        val openPositions = mutableListOf<Int>()
        val pendingAlign = mutableListOf<TextAlign?>()
        var i = 0
        val open = ArrayDeque<Tag>()
        fun curStyle(): SpanStyle {
            var st = SpanStyle()
            open.forEach {
                st = when (it.name) {
                    "b" -> st.merge(SpanStyle(fontWeight = FontWeight.Bold))
                    "i" -> st.merge(SpanStyle(fontStyle = FontStyle.Italic))
                    "u" -> st.merge(SpanStyle(textDecoration = TextDecoration.Underline))
                    "s" -> st.merge(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    "font" -> {
                        var x = st
                        it.attrs["size"]?.toIntOrNull()?.let { sz -> x = x.merge(SpanStyle(fontSize = sz.sp)) }
                        when (it.attrs["face"]) {
                            "serif" -> x = x.merge(SpanStyle(fontFamily = FontFamily.Serif))
                            "monospace" -> x = x.merge(SpanStyle(fontFamily = FontFamily.Monospace))
                        }
                        x
                    }
                    "color" -> it.attrs["value"]?.let { v -> st.merge(SpanStyle(color = parseColor(v))) } ?: st
                    "hl" -> it.attrs["value"]?.let { v -> st.merge(SpanStyle(background = parseColor(v))) } ?: st
                    else -> st
                }
            }
            return st
        }
        while (i < html.length) {
            if (html[i] == '<') {
                val j = html.indexOf('>', i)
                if (j < 0) {
                    appendWithStyle(html.substring(i), curStyle(), open)
                    break
                }
                val raw = html.substring(i + 1, j).trim()
                when {
                    raw.startsWith("/") -> {
                        val name = raw.drop(1).trim().lowercase()
                        if (name == "p") {
                            val start = openPositions.removeLastOrNull() ?: length
                            val align = pendingAlign.removeLastOrNull()
                            if (align != null) addStyle(ParagraphStyle(textAlign = align), start, length)
                        }
                        while (open.isNotEmpty() && open.last().name != name) open.removeLast()
                        if (open.isNotEmpty()) open.removeLast()
                    }
                    raw.startsWith("img") -> {
                        val src = Regex("""src="([^"]*)"""").find(raw)?.groupValues?.get(1) ?: ""
                        appendWithStyle("!($src)", curStyle(), open)
                    }
                    raw.startsWith("a ") || raw == "a" -> {
                        val href = Regex("""href="([^"]*)"""").find(raw)?.groupValues?.get(1) ?: ""
                        open.add(Tag("a", mapOf("href" to href)))
                    }
                    else -> {
                        val name = raw.split(" ", "\t").first().lowercase()
                        val attrs = Regex("""(\w+)="([^"]*)"""").findAll(raw)
                            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                        if (name == "p") {
                            openPositions.add(length)
                            pendingAlign.add(
                                when (attrs["align"]?.lowercase()) {
                                    "center" -> TextAlign.Center
                                    "right", "end" -> TextAlign.End
                                    "justify" -> TextAlign.Justify
                                    "left", "start" -> TextAlign.Start
                                    else -> null
                                },
                            )
                        }
                        if (name in setOf("b", "i", "u", "s", "font", "color", "hl", "p")) {
                            open.add(Tag(name, attrs))
                        }
                    }
                }
                i = j + 1
            } else {
                val j = html.indexOf('<', i).let { if (it < 0) html.length else it }
                appendWithStyle(unesc(html.substring(i, j)), curStyle(), open)
                i = j
            }
        }
    }

    private fun AnnotatedString.Builder.appendWithStyle(text: String, style: SpanStyle, open: ArrayDeque<Tag>) {
        val a = open.firstOrNull { it.name == "a" }
        if (a != null) {
            pushLink(LinkAnnotation.Url(a.attrs["href"] ?: "", TextLinkStyles(style)))
        }
        pushStyle(style)
        append(text)
        pop()
        if (a != null) pop()
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun unesc(s: String): String = s
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&")

    private fun parseColor(v: String): Color = runCatching {
        val hex = v.removePrefix("#")
        val argb = when (hex.length) {
            6 -> "FF$hex"
            8 -> hex
            else -> return Color.Unspecified
        }
        Color(argb.toULong(16))
    }.getOrDefault(Color.Unspecified)

    private data class Tag(val name: String, val attrs: Map<String, String> = emptyMap())

    // Strip all tags → plain text (list cards, checklist parsing, format switch).
    fun htmlToPlain(html: String): String {
        var t = html.replace(Regex("""<img\s+src="([^"]*)"\s*/>""")) { "!(${it.groupValues[1]})" }
        t = t.replace(Regex("""</?p[^>]*>"""), "")
        t = t.replace(Regex("""<[^>]+>"""), "")
        return unesc(t)
    }
}
