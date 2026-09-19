package com.shijian.md.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.shijian.md.ui.theme.ThemeSpec

/** 源码模式的轻量着色，帮助在手机上快速辨认结构。 */
object MarkdownHighlight {

    private val FENCE = Regex("^\\s*(```|~~~).*$")
    private val HEADING = Regex("^(#{1,6})\\s")
    private val HR = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
    private val QUOTE = Regex("^\\s*>")
    private val LIST_MARK = Regex("^(\\s*)([-*+]|\\d{1,3}[.)])(\\s+)")
    private val CHECKBOX = Regex("\\[([ xX])\\]")
    private val FRONT_DELIM = Regex("^---\\s*$")
    private val INLINE = listOf(
        Regex("!?\\[[^\\]\\n]*\\]\\([^)\\n]*\\)"),
        Regex("`[^`\\n]*`"),
        Regex("\\*\\*[^*\\n]+\\*\\*"),
        Regex("__[^_\\n]+__"),
        Regex("~~[^~\\n]+~~"),
        Regex("==[^=\\n]+=="),
        Regex("(?<![*_])\\*[^*\\n]+\\*(?![*_])"),
        Regex("(?<![*_])_[^_\\n]+_(?![*_])"),
    )

    fun highlight(text: String, spec: ThemeSpec): AnnotatedString {
        val c = spec.colors
        val mono = spec.typography.code.fontFamily
        return buildAnnotatedString {
            var index = 0
            var inFence = false
            var inFront = false
            var first = true
            while (index < text.length) {
                val nl = text.indexOf('\n', index)
                val end = if (nl < 0) text.length else nl
                val line = text.substring(index, end)

                if (FRONT_DELIM.matches(line)) {
                    if (first || inFront) inFront = !inFront
                }
                val fenceLine = FENCE.matches(line)
                val inCode = inFence && !fenceLine

                val base: SpanStyle? = when {
                    fenceLine -> SpanStyle(color = c.muted, fontFamily = mono)
                    inCode -> SpanStyle(color = c.codeFg, fontFamily = mono)
                    inFront -> SpanStyle(color = c.muted, fontStyle = FontStyle.Italic)
                    HEADING.containsMatchIn(line) -> SpanStyle(color = c.primaryDeep, fontWeight = FontWeight.Bold)
                    HR.matches(line) -> SpanStyle(color = c.muted)
                    QUOTE.containsMatchIn(line) -> SpanStyle(color = c.onSurfaceVariant, fontStyle = FontStyle.Italic)
                    else -> null
                }

                val start = length
                if (base != null) withStyle(base) { append(line) } else append(line)

                if (!inCode && !inFront) {
                    val spans = ArrayList<Triple<Int, Int, SpanStyle>>()
                    HEADING.find(line)?.let {
                        spans += Triple(it.range.first, it.range.last + 1, SpanStyle(color = c.primary))
                    }
                    LIST_MARK.find(line)?.let {
                        spans += Triple(it.range.first + it.groupValues[1].length, it.range.last + 1, SpanStyle(color = c.primary, fontWeight = FontWeight.Medium))
                    }
                    if (QUOTE.containsMatchIn(line)) {
                        spans += Triple(0, line.indexOf('>') + 1, SpanStyle(color = c.primary.copy(alpha = 0.85f)))
                    }
                    CHECKBOX.findAll(line).forEach {
                        spans += Triple(
                            it.range.first,
                            it.range.last + 1,
                            SpanStyle(color = c.primary, fontWeight = FontWeight.Bold),
                        )
                    }
                    INLINE.forEachIndexed { order, regex ->
                        regex.findAll(line).forEach { m ->
                            spans += Triple(m.range.first, m.range.last + 1, inlineStyle(order, c))
                        }
                    }
                    spans.sortBy { it.first }
                    var lastEnd = -1
                    spans.forEach { (s, e, style) ->
                        if (s >= lastEnd && e <= line.length) {
                            addStyle(style, start + s, start + e)
                            lastEnd = e
                        }
                    }
                }

                if (fenceLine) inFence = !inFence
                if (nl >= 0) append("\n")
                index = if (nl < 0) text.length else nl + 1
                first = false
            }
        }
    }

    private fun inlineStyle(order: Int, c: com.shijian.md.ui.theme.MdColors): SpanStyle = when (order) {
        0 -> SpanStyle(color = c.link, textDecoration = TextDecoration.Underline)
        1 -> SpanStyle(color = c.codeInlineFg, background = c.codeInlineBg)
        2, 3 -> SpanStyle(fontWeight = FontWeight.Bold)
        4 -> SpanStyle(textDecoration = TextDecoration.LineThrough, color = c.muted)
        5 -> SpanStyle(color = c.primaryDeep, background = c.primarySoft)
        else -> SpanStyle(fontStyle = FontStyle.Italic)
    }
}
