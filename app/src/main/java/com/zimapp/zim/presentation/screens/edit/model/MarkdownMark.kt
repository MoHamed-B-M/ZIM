package com.zimapp.zim.presentation.screens.edit.model

/**
 * Inline markdown marks, with the delimiters the renderer actually understands.
 *
 * These must stay in sync with the `TextStyleSegment` list in
 * `presentation/components/markdown/Builder.kt`, which is what parses the note
 * body. The toolbar used to insert doubled delimiters (`****` for bold, `**`
 * for italic, `~~` etc.) which mostly worked only by accident: the parser
 * alternates `indexOf` hits, so `****x****` happens to pair up into one bold
 * segment. The exception was italic, which inserted `**` and therefore rendered
 * **bold**. Using the real delimiters fixes that, and existing notes keep
 * rendering because the accidental pairing still holds.
 *
 * Note the parser is a flat alternating scan with no notion of nesting, so
 * wrapping text that is already inside a `**bold**` run with, say, `_underline_`
 * will visually break the bold run. That is a property of the renderer, not
 * something a toolbar can work around.
 */
enum class MarkdownMark(val open: String, val close: String = open) {
    BOLD("**"),
    ITALIC("*"),
    UNDERLINE("_"),
    STRIKETHROUGH("~~"),
    HIGHLIGHT("=="),
    CODE("`"),
}
