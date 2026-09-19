package com.shijian.md.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.shijian.md.md.Inline
import com.shijian.md.ui.theme.ThemeSpec

const val LINK_TAG = "shijian.link"

/** 把行内元素写成 AnnotatedString，链接以注释形式携带目标地址。 */
fun AnnotatedString.Builder.appendInlines(inlines: List<Inline>, spec: ThemeSpec) {
    val c = spec.colors
    inlines.forEach { node ->
        when (node) {
            is Inline.Text -> append(node.text)

            is Inline.Emph -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInlines(node.children, spec)
            }

            is Inline.Strong -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlines(node.children, spec)
            }

            is Inline.Strike -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = c.muted)) {
                appendInlines(node.children, spec)
            }

            is Inline.Highlight -> withStyle(
                SpanStyle(background = c.primarySoft, color = c.primaryDeep, fontWeight = FontWeight.Medium)
            ) {
                append("\u2009")
                appendInlines(node.children, spec)
                append("\u2009")
            }

            is Inline.Code -> withStyle(
                SpanStyle(
                    fontFamily = spec.typography.inlineCode.fontFamily,
                    fontSize = spec.typography.inlineCode.fontSize,
                    color = c.codeInlineFg,
                    background = c.codeInlineBg,
                )
            ) {
                append("\u2009")
                append(node.text)
                append("\u2009")
            }

            is Inline.Math -> withStyle(
                SpanStyle(
                    fontFamily = spec.typography.inlineCode.fontFamily,
                    fontSize = spec.typography.inlineCode.fontSize,
                    color = c.primaryDeep,
                )
            ) {
                append(node.text)
            }

            is Inline.Link -> {
                val start = length
                pushStyle(
                    SpanStyle(color = c.link, textDecoration = TextDecoration.Underline)
                )
                appendInlines(node.children, spec)
                pop()
                addStringAnnotation(LINK_TAG, node.url, start, length)
            }

            is Inline.ImageRef -> withStyle(SpanStyle(color = c.muted, fontStyle = FontStyle.Italic)) {
                append("[" + (node.alt.ifBlank { "图片" }) + "]")
            }

            is Inline.FootnoteRef -> withStyle(
                SpanStyle(color = c.primary, fontSize = spec.typography.caption.fontSize, fontWeight = FontWeight.Medium)
            ) {
                append("[")
                append(node.label)
                append("]")
            }

            is Inline.Break -> append(if (node.hard) "\n" else " ")
            is Inline.Raw -> Unit
        }
    }
}

@Composable
fun rememberInline(inlines: List<Inline>, spec: ThemeSpec): AnnotatedString =
    remember(inlines, spec) { buildAnnotatedString { appendInlines(inlines, spec) } }

/**
 * 正文文本：支持点击链接，长按可选中复制。
 */
@Composable
fun InlineText(
    inlines: List<Inline>,
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
    style: TextStyle = spec.typography.body,
    color: Color = spec.colors.onBackground,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    selectable: Boolean = false,
    onOpenLink: ((String) -> Unit)? = null,
) {
    val annotated = rememberInline(inlines, spec)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val tapModifier = if (onOpenLink == null) Modifier else Modifier.pointerInput(annotated) {
        detectTapGestures { pos ->
            val result = layout ?: return@detectTapGestures
            val offset = result.getOffsetForPosition(pos)
            annotated.getStringAnnotations(LINK_TAG, offset, offset).firstOrNull()
                ?.let { onOpenLink(it.item) }
        }
    }
    val text: @Composable () -> Unit = {
        Text(
            text = annotated,
            modifier = modifier.then(tapModifier),
            style = style,
            color = color,
            textAlign = textAlign,
            maxLines = maxLines,
            onTextLayout = { layout = it },
        )
    }
    if (selectable) SelectionContainer { text() } else text()
}
