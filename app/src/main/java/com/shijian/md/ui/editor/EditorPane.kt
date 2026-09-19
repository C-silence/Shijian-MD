package com.shijian.md.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shijian.md.ui.theme.LocalMdTheme
import com.shijian.md.ui.theme.ThemeSpec

@Composable
fun EditorPane(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    showToolbar: Boolean = true,
    readOnly: Boolean = false,
    verticalPadding: Dp = 8.dp,
) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val transformation = remember(spec) {
        VisualTransformation { text ->
            TransformedText(MarkdownHighlight.highlight(text.text, spec), OffsetMapping.Identity)
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                modifier = Modifier.fillMaxSize(),
                textStyle = spec.typography.code.copy(
                    color = c.onBackground,
                    lineHeight = spec.typography.code.fontSize * 1.65f,
                ),
                cursorBrush = SolidColor(c.primary),
                visualTransformation = transformation,
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                    ) { inner() }
                },
            )
        }
        if (showToolbar && !readOnly) {
            EditorToolbar(
                spec = spec,
                onAction = { action -> onValueChange(value.apply(action)) },
            )
        }
    }
}

/* ------------------------------------------------------------------ 工具条 */

/** 插入工具条高度：Chip 最小 40dp + 上下各 8dp 内边距（不含 imePadding 与 1dp 顶部分隔线）。 */
val EditorToolbarHeight: Dp = 56.dp

private enum class EditAction { H1, H2, H3, Bold, Italic, Strike, Highlight, Code, Block, List, Task, Quote, Link, Table, Rule, Image }

@Composable
private fun EditorToolbar(spec: ThemeSpec, onAction: (EditAction) -> Unit) {
    val c = spec.colors
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .background(c.surface)
            .border(1.dp, c.outline.copy(alpha = 0.6f))
            .horizontalScroll(scroll)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip("H1", spec) { onAction(EditAction.H1) }
        Chip("H2", spec) { onAction(EditAction.H2) }
        Chip("H3", spec) { onAction(EditAction.H3) }
        Chip("粗", spec) { onAction(EditAction.Bold) }
        Chip("斜", spec) { onAction(EditAction.Italic) }
        Chip("删", spec) { onAction(EditAction.Strike) }
        Chip("高", spec) { onAction(EditAction.Highlight) }
        Chip("码", spec) { onAction(EditAction.Code) }
        Chip("块", spec) { onAction(EditAction.Block) }
        Chip("列", spec) { onAction(EditAction.List) }
        Chip("任", spec) { onAction(EditAction.Task) }
        Chip("引", spec) { onAction(EditAction.Quote) }
        Chip("链", spec) { onAction(EditAction.Link) }
        Chip("表", spec) { onAction(EditAction.Table) }
        Chip("线", spec) { onAction(EditAction.Rule) }
        Chip("图", spec) { onAction(EditAction.Image) }
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun Chip(label: String, spec: ThemeSpec, onClick: () -> Unit) {
    val c = spec.colors
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(c.surfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, c.outline.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = spec.typography.label,
            color = c.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

/* ------------------------------------------------------------------ 插入逻辑 */

private fun TextFieldValue.apply(action: EditAction): TextFieldValue = when (action) {
    EditAction.H1 -> prefixLines("# ")
    EditAction.H2 -> prefixLines("## ")
    EditAction.H3 -> prefixLines("### ")
    EditAction.Bold -> wrap("**")
    EditAction.Italic -> wrap("*")
    EditAction.Strike -> wrap("~~")
    EditAction.Highlight -> wrap("==")
    EditAction.Code -> wrap("`")
    EditAction.Block -> insertSnippet("\n```\n\n```\n", 5)
    EditAction.List -> prefixLines("- ")
    EditAction.Task -> prefixLines("- [ ] ")
    EditAction.Quote -> prefixLines("> ")
    EditAction.Link -> insertSnippet("[链接](https://)", 11, 13)
    EditAction.Table -> insertSnippet(
        "\n| 列 1 | 列 2 |\n| --- | --- |\n| 内容 | 内容 |\n",
        0, 0,
    )
    EditAction.Rule -> insertSnippet("\n---\n", 5)
    EditAction.Image -> insertSnippet("![描述](图片路径)", 2, 2)
}

private fun TextFieldValue.wrap(marker: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val replaced = marker + selected + marker
    val caret = if (selected.isEmpty()) start + marker.length else start + replaced.length
    return copy(
        text = text.replaceRange(start, end, replaced),
        selection = TextRange(caret.coerceAtMost(text.length + replaced.length)),
    )
}

private fun TextFieldValue.prefixLines(prefix: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }
    val block = text.substring(lineStart, lineEnd)
    val replaced = block.split("\n").joinToString("\n") { if (it.isEmpty()) prefix.trimEnd() else prefix + it }
    return copy(
        text = text.replaceRange(lineStart, lineEnd, replaced),
        selection = TextRange(lineStart + replaced.length),
    )
}

private fun TextFieldValue.insertSnippet(snippet: String, caretBack: Int, selectBack: Int = caretBack): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val newText = text.replaceRange(start, end, snippet)
    val caret = (start + snippet.length - caretBack).coerceIn(0, newText.length)
    val selStart = (start + snippet.length - selectBack).coerceIn(0, newText.length)
    return copy(text = newText, selection = TextRange(selStart, caret))
}
