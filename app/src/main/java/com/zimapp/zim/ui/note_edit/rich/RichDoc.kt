@file:OptIn(ExperimentalTextApi::class)
package com.zimapp.zim.ui.note_edit.rich

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

// Pure selection/style operations for the rich editor. Toggle semantics:
// if every char in range already carries the attribute → remove, else add.
private fun TextFieldValue.rangeOrWord(): IntRange {
    if (!selection.collapsed) return selection.min..selection.max
    val t = text
    if (t.isEmpty()) return 0..0
    var s = selection.start.coerceIn(0, t.length)
    var e = s
    while (s > 0 && t[s - 1].isLetterOrDigit()) s--
    while (e < t.length && t[e].isLetterOrDigit()) e++
    return s..e
}

private fun AnnotatedString.hasAll(range: IntRange, test: (SpanStyle) -> Boolean): Boolean {
    if (range.first >= range.last) return false
    for (i in range.first until range.last.coerceAtMost(text.length)) {
        if (spanStyles.none { it.start <= i && it.end > i && test(it.item) }) return false
    }
    return true
}

private fun AnnotatedString.without(range: IntRange, test: (SpanStyle) -> Boolean): AnnotatedString {
    val src = this
    return buildAnnotatedString {
        append(src.text)
        src.spanStyles.forEach { span ->
            if (!test(span.item) || span.end <= range.first || span.start >= range.last) {
                addStyle(span.item, span.start, span.end)
            } else {
                if (span.start < range.first) addStyle(span.item, span.start, range.first)
                if (span.end > range.last) addStyle(span.item, range.last, span.end)
            }
        }
        src.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
        src.getStringAnnotations(0, src.text.length).forEach { a ->
            addStringAnnotation(a.tag, a.item, a.start, a.end)
        }
        src.getUrlAnnotations(0, src.text.length).forEach { a ->
            (a.item as? LinkAnnotation.Url)?.let { addLink(it, a.start, a.end) }
        }
    }
}

private fun AnnotatedString.Builder.copyAnnotationsFrom(src: AnnotatedString) {
    src.getStringAnnotations(0, src.text.length).forEach { a ->
        addStringAnnotation(a.tag, a.item, a.start, a.end)
    }
    src.getUrlAnnotations(0, src.text.length).forEach { a ->
        (a.item as? LinkAnnotation.Url)?.let { addLink(it, a.start, a.end) }
    }
}

private fun TextFieldValue.toggleSpan(add: SpanStyle, test: (SpanStyle) -> Boolean): TextFieldValue {
    val range = rangeOrWord().let { it.first.coerceAtLeast(0)..it.last.coerceAtMost(text.length) }
    if (range.first >= range.last) return this
    val next = if (annotatedString.hasAll(range, test)) {
        annotatedString.without(range, test)
    } else {
        buildAnnotatedString {
            append(annotatedString.text)
            annotatedString.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            annotatedString.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
            copyAnnotationsFrom(annotatedString)
            addStyle(add, range.first, range.last)
        }
    }
    return copy(annotatedString = next)
}

fun TextFieldValue.toggleBold() = toggleSpan(SpanStyle(fontWeight = FontWeight.Bold)) {
    it.fontWeight == FontWeight.Bold
}

fun TextFieldValue.toggleItalic() = toggleSpan(SpanStyle(fontStyle = FontStyle.Italic)) {
    it.fontStyle == FontStyle.Italic
}

fun TextFieldValue.toggleUnderline() = toggleSpan(SpanStyle(textDecoration = TextDecoration.Underline)) {
    it.textDecoration?.contains(TextDecoration.Underline) == true
}

fun TextFieldValue.toggleStrike() = toggleSpan(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
    it.textDecoration?.contains(TextDecoration.LineThrough) == true
}

fun TextFieldValue.effectiveSize(fallback: Int = 16): Int {
    val i = selection.start.coerceIn(0, (text.length - 1).coerceAtLeast(0))
    if (text.isEmpty()) return fallback
    return annotatedString.spanStyles
        .filter { it.start <= i && it.end > i && it.item.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified }
        .maxOfOrNull { it.item.fontSize.value.toInt() } ?: fallback
}

fun TextFieldValue.adjustSize(delta: Int): TextFieldValue {
    val range = rangeOrWord()
    if (range.first >= range.last) return this
    val size = (effectiveSize() + delta).coerceIn(10, 48)
    val stripped = annotatedString.without(range) { it.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified }
    return copy(
        annotatedString = buildAnnotatedString {
            append(stripped.text)
            stripped.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            stripped.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
            copyAnnotationsFrom(stripped)
            addStyle(SpanStyle(fontSize = size.sp), range.first, range.last)
        },
    )
}

fun TextFieldValue.setFontFamily(family: FontFamily?): TextFieldValue {
    val range = rangeOrWord()
    if (range.first >= range.last) return this
    val stripped = annotatedString.without(range) { it.fontFamily != null }
    if (family == null) return copy(annotatedString = stripped)
    return copy(
        annotatedString = buildAnnotatedString {
            append(stripped.text)
            stripped.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            stripped.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
            copyAnnotationsFrom(stripped)
            addStyle(SpanStyle(fontFamily = family), range.first, range.last)
        },
    )
}

fun TextFieldValue.setTextColor(color: Color?): TextFieldValue {
    val range = rangeOrWord()
    if (range.first >= range.last) return this
    val stripped = annotatedString.without(range) { it.color != Color.Unspecified }
    if (color == null || color == Color.Unspecified) return copy(annotatedString = stripped)
    return copy(
        annotatedString = buildAnnotatedString {
            append(stripped.text)
            stripped.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            stripped.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
            copyAnnotationsFrom(stripped)
            addStyle(SpanStyle(color = color), range.first, range.last)
        },
    )
}

fun TextFieldValue.setHighlight(color: Color?): TextFieldValue {
    val range = rangeOrWord()
    if (range.first >= range.last) return this
    val stripped = annotatedString.without(range) { it.background != Color.Unspecified }
    if (color == null || color == Color.Unspecified) return copy(annotatedString = stripped)
    return copy(
        annotatedString = buildAnnotatedString {
            append(stripped.text)
            stripped.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            stripped.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
            copyAnnotationsFrom(stripped)
            addStyle(SpanStyle(background = color), range.first, range.last)
        },
    )
}

fun TextFieldValue.setAlignment(align: TextAlign): TextFieldValue {
    val t = text
    if (t.isEmpty()) return this
    val s = selection.min
    val lineStart = t.lastIndexOf('\n', (s - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val lineEnd = t.indexOf('\n', s).let { if (it < 0) t.length else it + 1 }
    return copy(
        annotatedString = buildAnnotatedString {
            append(t)
            annotatedString.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            annotatedString.paragraphStyles.forEach {
                if (it.end <= lineStart || it.start >= lineEnd) addStyle(it.item, it.start, it.end)
            }
            copyAnnotationsFrom(annotatedString)
            addStyle(ParagraphStyle(textAlign = align), lineStart, lineEnd)
        },
        selection = selection,
    )
}

// Toggle a line prefix ("• " bullets / "- [ ] " checklist) on touched lines.
fun TextFieldValue.toggleLinePrefix(prefix: String): TextFieldValue {
    val t = text
    if (t.isEmpty()) return copy(text = prefix, selection = TextRange(prefix.length))
    val s = selection.min.coerceIn(0, t.length)
    val e = selection.max.coerceIn(0, t.length)
    val firstLine = t.lastIndexOf('\n', (s - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val lastLineExcl = t.indexOf('\n', e).let { if (it < 0) t.length else it }
    val lines = t.substring(firstLine, lastLineExcl).split('\n')
    val allHave = lines.all { it.startsWith(prefix) }
    val mapped = lines.map { if (allHave) it.removePrefix(prefix) else prefix + it }
    val replacement = mapped.joinToString("\n")
    // Spans are dropped across prefix edits (body text is preserved).
    val newText = t.substring(0, firstLine) + replacement + t.substring(lastLineExcl)
    return copy(
        annotatedString = AnnotatedString(newText),
        selection = TextRange((firstLine + replacement.length).coerceAtMost(newText.length)),
    )
}

fun TextFieldValue.insertLink(url: String): TextFieldValue {
    val range = if (selection.collapsed) {
        val t = text
        val at = selection.start.coerceIn(0, t.length)
        return copy(
            annotatedString = buildAnnotatedString {
                append(t.substring(0, at))
                val linkStart = length
                append(url)
                addLink(LinkAnnotation.Url(url), linkStart, length)
                append(t.substring(at))
                annotatedString.spanStyles.forEach { s ->
                    val na = if (s.start >= at) s.start + url.length else s.start
                    val nb = if (s.end >= at) s.end + url.length else s.end
                    addStyle(s.item, na, nb)
                }
                annotatedString.paragraphStyles.forEach { s ->
                    val na = if (s.start >= at) s.start + url.length else s.start
                    val nb = if (s.end >= at) s.end + url.length else s.end
                    addStyle(s.item, na, nb)
                }
            },
            selection = TextRange(at + url.length),
        )
    } else {
        selection.min..selection.max
    }
    return copy(
        annotatedString = buildAnnotatedString {
            append(annotatedString.text)
            annotatedString.spanStyles.forEach { addStyle(it.item, it.start, it.end) }
            addLink(LinkAnnotation.Url(url), range.first, range.last)
        },
    )
}

fun TextFieldValue.insertImageBlock(path: String): TextFieldValue {
    val t = text
    val at = selection.min.coerceIn(0, t.length)
    val block = if (at == 0 || t[at - 1] == '\n') "!($path)\n" else "\n!($path)\n"
    val newText = t.substring(0, at) + block + t.substring(at)
    return copy(
        annotatedString = AnnotatedString(newText),
        selection = TextRange(at + block.length),
    )
}
