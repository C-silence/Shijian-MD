package com.shijian.md.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.shijian.md.md.MdBlock
import com.shijian.md.ui.theme.LocalMdTheme
import com.shijian.md.ui.theme.ThemeSpec
import com.shijian.md.util.copyPlainText
import kotlinx.coroutines.delay
import java.io.File

private fun indentOf(depth: Int, step: Dp = 18.dp): Dp =
    step * (depth - 1).coerceIn(0, 6)

/* ------------------------------------------------------------------ 分发 */

@Composable
fun MdBlockView(
    block: MdBlock,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
    imageBase: File? = null,
    onOpenLink: ((String) -> Unit)? = null,
    onToggleTask: ((MdBlock.Task, Boolean) -> Unit)? = null,
) {
    val spec = LocalMdTheme.current
    when (block) {
        is MdBlock.Heading -> HeadingBlock(block, spec, modifier, isFirst, onOpenLink)
        is MdBlock.Para -> InlineText(
            inlines = block.content,
            spec = spec,
            modifier = modifier.fillMaxWidth().padding(vertical = spec.spacing.paragraph / 2),
            style = spec.typography.body,
            selectable = true,
            onOpenLink = onOpenLink,
        )
        is MdBlock.Formula -> FormulaBlock(block, spec, modifier)
        is MdBlock.Bullet -> BulletBlock(block, spec, modifier, onOpenLink)
        is MdBlock.Ordered -> OrderedBlock(block, spec, modifier, onOpenLink)
        is MdBlock.Task -> TaskBlock(block, spec, modifier, onToggleTask)
        is MdBlock.Quote -> QuoteBlock(block, spec, modifier, onOpenLink)
        is MdBlock.Code -> CodeBlockView(block, spec, modifier)
        is MdBlock.Table -> TableBlockView(block, spec, modifier, onOpenLink)
        is MdBlock.Divider -> DividerBlock(spec, modifier)
        is MdBlock.Image -> ImageBlock(block, spec, modifier, imageBase)
        is MdBlock.FrontMatter -> FrontMatterView(block, spec, modifier)
        is MdBlock.Html -> HtmlView(block, spec, modifier)
    }
}

/* ------------------------------------------------------------------ 标题 */

@Composable
private fun HeadingBlock(
    block: MdBlock.Heading,
    spec: ThemeSpec,
    modifier: Modifier,
    isFirst: Boolean,
    onOpenLink: ((String) -> Unit)?,
) {
    val c = spec.colors
    val top = if (isFirst) 6.dp else when (block.level) {
        1 -> 30.dp
        2 -> 24.dp
        else -> 18.dp
    }
    when (block.level) {
        1 -> Column(
            modifier = modifier.fillMaxWidth().padding(top = top, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InlineText(
                inlines = block.content,
                spec = spec,
                modifier = Modifier.fillMaxWidth(),
                style = spec.typography.h1,
                color = c.onBackground,
                textAlign = TextAlign.Center,
                onOpenLink = onOpenLink,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.primary)
            )
        }

        2 -> Row(modifier.fillMaxWidth().padding(top = top, bottom = 10.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.primarySoft)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InlineText(
                    inlines = block.content,
                    spec = spec,
                    style = spec.typography.h2,
                    color = c.primaryDeep,
                    onOpenLink = onOpenLink,
                )
            }
        }

        3 -> Row(
            modifier = modifier.fillMaxWidth().padding(top = top, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.primary)
            )
            Spacer(Modifier.width(10.dp))
            InlineText(
                inlines = block.content,
                spec = spec,
                style = spec.typography.h3,
                color = c.onBackground,
                onOpenLink = onOpenLink,
            )
        }

        else -> Row(
            modifier = modifier.fillMaxWidth().padding(top = top, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(16.dp), contentAlignment = Alignment.Center) {
                when (block.level) {
                    4 -> Box(
                        Modifier.size(8.dp).clip(CircleShape).background(c.primary)
                    )
                    5 -> Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, c.primary, CircleShape)
                    )
                    else -> Box(
                        Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(c.primary)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            InlineText(
                inlines = block.content,
                spec = spec,
                style = spec.typography.h4,
                color = if (block.level >= 6) c.muted else c.onBackground,
                onOpenLink = onOpenLink,
            )
        }
    }
}

/* ------------------------------------------------------------------ 列表 */

@Composable
private fun BulletBlock(
    block: MdBlock.Bullet,
    spec: ThemeSpec,
    modifier: Modifier,
    onOpenLink: ((String) -> Unit)?,
) {
    val c = spec.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentOf(block.depth), top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.width(22.dp).padding(top = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (block.depth.coerceAtMost(3)) {
                1 -> Box(Modifier.size(6.dp).clip(CircleShape).background(c.primary))
                2 -> Box(Modifier.size(6.dp).clip(CircleShape).border(1.4.dp, c.primary, CircleShape))
                else -> Box(
                    Modifier.size(4.dp).clip(RoundedCornerShape(1.dp)).background(c.muted)
                )
            }
        }
        InlineText(
            inlines = block.content,
            spec = spec,
            modifier = Modifier.weight(1f),
            style = spec.typography.body,
            selectable = true,
            onOpenLink = onOpenLink,
        )
    }
}

@Composable
private fun OrderedBlock(
    block: MdBlock.Ordered,
    spec: ThemeSpec,
    modifier: Modifier,
    onOpenLink: ((String) -> Unit)?,
) {
    val c = spec.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentOf(block.depth), top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "${block.number}.",
            modifier = Modifier.width(30.dp).padding(top = 1.dp),
            style = spec.typography.body,
            color = c.primary,
            textAlign = TextAlign.End,
        )
        Spacer(Modifier.width(8.dp))
        InlineText(
            inlines = block.content,
            spec = spec,
            modifier = Modifier.weight(1f),
            style = spec.typography.body,
            selectable = true,
            onOpenLink = onOpenLink,
        )
    }
}

@Composable
fun CheckMark(
    checked: Boolean,
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
    box: Dp = 20.dp,
) {
    val c = spec.colors
    androidx.compose.foundation.Canvas(modifier.size(box)) {
        val radius = CornerRadius(box.toPx() * 0.28f)
        if (checked) {
            drawRoundRect(color = c.primary, cornerRadius = radius, size = size)
            val path = Path().apply {
                moveTo(size.width * 0.26f, size.height * 0.52f)
                lineTo(size.width * 0.43f, size.height * 0.69f)
                lineTo(size.width * 0.76f, size.height * 0.31f)
            }
            drawPath(
                path = path,
                color = c.onPrimary,
                style = Stroke(
                    width = size.width * 0.11f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        } else {
            drawRoundRect(
                color = c.outline,
                cornerRadius = radius,
                size = size,
                style = Stroke(width = box.toPx() * 0.09f),
            )
        }
    }
}

@Composable
private fun TaskBlock(
    block: MdBlock.Task,
    spec: ThemeSpec,
    modifier: Modifier,
    onToggleTask: ((MdBlock.Task, Boolean) -> Unit)?,
) {
    val c = spec.colors
    val enabled = onToggleTask != null
    val annotated = remember(block.content, block.checked, spec) {
        buildAnnotatedString {
            if (block.checked) {
                withStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough, color = c.muted)
                ) { appendInlines(block.content, spec) }
            } else {
                appendInlines(block.content, spec)
            }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentOf(block.depth), top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onToggleTask?.invoke(block, !block.checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CheckMark(checked = block.checked, spec = spec, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = annotated,
            modifier = Modifier.weight(1f),
            style = spec.typography.body,
            color = if (block.checked) c.muted else c.onBackground,
        )
    }
}

/* ------------------------------------------------------------------ 引用 / 分隔 / 公式 / HTML */

@Composable
private fun QuoteBlock(
    block: MdBlock.Quote,
    spec: ThemeSpec,
    modifier: Modifier,
    onOpenLink: ((String) -> Unit)?,
) {
    val c = spec.colors
    val nested = block.depth > 1
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = indentOf(block.depth, 14.dp),
                top = 6.dp,
                bottom = 6.dp,
            )
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(if (nested) c.surfaceVariant else c.primaryWhisper),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (nested) c.primary.copy(alpha = 0.45f) else c.primary)
        )
        InlineText(
            inlines = block.content,
            spec = spec,
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
            style = spec.typography.body,
            color = c.onSurfaceVariant,
            selectable = true,
            onOpenLink = onOpenLink,
        )
    }
}

@Composable
private fun DividerBlock(spec: ThemeSpec, modifier: Modifier) {
    val c = spec.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(c.outline))
        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .size(6.dp)
                .rotate(45f)
                .background(c.primary.copy(alpha = 0.6f))
        )
        Box(Modifier.weight(1f).height(1.dp).background(c.outline))
    }
}

@Composable
private fun FormulaBlock(block: MdBlock.Formula, spec: ThemeSpec, modifier: Modifier) {
    val c = spec.colors
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(shape)
            .background(c.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, c.outline.copy(alpha = 0.7f), shape)
            .padding(14.dp),
    ) {
        Text(
            text = "公式",
            style = spec.typography.caption,
            color = c.primary,
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Text(
                text = block.text,
                style = spec.typography.code,
                color = c.onBackground,
            )
        }
    }
}

@Composable
private fun HtmlView(block: MdBlock.Html, spec: ThemeSpec, modifier: Modifier) {
    val c = spec.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).height(IntrinsicSize.Min),
    ) {
        Box(Modifier.width(2.dp).fillMaxHeight().background(c.outline))
        Text(
            text = block.raw,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = spec.typography.small,
            color = c.muted,
        )
    }
}

/* ------------------------------------------------------------------ 代码 */

@Composable
private fun CodeBlockView(block: MdBlock.Code, spec: ThemeSpec, modifier: Modifier) {
    val c = spec.colors
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(shape)
            .background(c.codeBlockBg)
            .border(1.dp, c.outline.copy(alpha = 0.55f), shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.codeHeaderBg)
                .padding(start = 14.dp, end = 4.dp)
                .heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = block.language.ifBlank { "代码" },
                style = spec.typography.caption,
                color = c.muted,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    copyPlainText(context, block.code)
                    copied = true
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (copied) ShijianIcons.Check else ShijianIcons.Copy,
                    contentDescription = "复制代码",
                    tint = if (copied) c.primary else c.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Box(Modifier.horizontalScroll(rememberScrollState()).padding(14.dp)) {
            Text(
                text = remember(block.code, block.language, spec) {
                    CodeHighlight.highlight(block.code, block.language, spec)
                },
                style = spec.typography.code,
                color = c.codeFg,
            )
        }
    }
}

/* ------------------------------------------------------------------ 表格 */

@Composable
private fun TableBlockView(
    block: MdBlock.Table,
    spec: ThemeSpec,
    modifier: Modifier,
    onOpenLink: ((String) -> Unit)?,
) {
    val c = spec.colors
    val shape = RoundedCornerShape(10.dp)
    val columns = maxOf(
        block.header.size,
        block.rows.maxOfOrNull { it.size } ?: 0,
    ).coerceAtLeast(1)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(shape)
            .border(1.dp, c.outline, shape),
    ) {
        if (block.header.isNotEmpty()) {
            TableRowView(
                cells = block.header,
                columns = columns,
                spec = spec,
                header = true,
                onOpenLink = onOpenLink,
            )
        }
        block.rows.forEachIndexed { index, row ->
            if (index > 0 || block.header.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline))
            }
            TableRowView(
                cells = row,
                columns = columns,
                spec = spec,
                header = false,
                onOpenLink = onOpenLink,
            )
        }
    }
}

@Composable
private fun TableRowView(
    cells: List<List<com.shijian.md.md.Inline>>,
    columns: Int,
    spec: ThemeSpec,
    header: Boolean,
    onOpenLink: ((String) -> Unit)?,
) {
    val c = spec.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (header) c.primaryWhisper else c.surface),
    ) {
        repeat(columns) { index ->
            val cell = cells.getOrNull(index)
            if (cell == null) {
                Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 9.dp))
            } else {
                InlineText(
                    inlines = cell,
                    spec = spec,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 9.dp),
                    style = if (header) spec.typography.small else spec.typography.small,
                    color = if (header) c.primaryDeep else c.onBackground,
                    onOpenLink = onOpenLink,
                )
            }
            if (index < columns - 1) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.outline))
            }
        }
    }
}

/* ------------------------------------------------------------------ 图片 */

@Composable
private fun ImageBlock(block: MdBlock.Image, spec: ThemeSpec, modifier: Modifier, base: File?) {
    val c = spec.colors
    val target = remember(block.url, base) { resolveImage(block.url, base) }
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (target) {
            is ImageTarget.Local -> AsyncImage(
                model = target.file,
                contentDescription = block.alt,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit,
            )

            is ImageTarget.Content -> AsyncImage(
                model = target.uri,
                contentDescription = block.alt,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit,
            )

            ImageTarget.Remote -> ImagePlaceholder(
                text = block.alt.ifBlank { "网络图片" } + " · 离线模式下不加载网络图片",
                spec = spec,
            )

            ImageTarget.Missing -> ImagePlaceholder(
                text = block.alt.ifBlank { "图片" } + " · 找不到该文件",
                spec = spec,
            )
        }
        if (block.alt.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = block.alt,
                style = spec.typography.caption,
                color = c.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ImagePlaceholder(text: String, spec: ThemeSpec) {
    val c = spec.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, c.outline.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = spec.typography.small,
            color = c.muted,
            textAlign = TextAlign.Center,
        )
    }
}

private sealed interface ImageTarget {
    data class Local(val file: File) : ImageTarget
    data class Content(val uri: android.net.Uri) : ImageTarget
    data object Remote : ImageTarget
    data object Missing : ImageTarget
}

private fun resolveImage(url: String, base: File?): ImageTarget {
    if (url.isBlank()) return ImageTarget.Missing
    val lower = url.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("data:")) {
        return ImageTarget.Remote
    }
    if (lower.startsWith("content://") || lower.startsWith("android.resource://")) {
        return ImageTarget.Content(android.net.Uri.parse(url))
    }
    val path = url.removePrefix("file://")
    val file = if (path.startsWith("/")) File(path) else base?.let { File(it, path) }
    return when {
        file == null -> ImageTarget.Missing
        file.isFile -> ImageTarget.Local(file)
        else -> ImageTarget.Missing
    }
}

/* ------------------------------------------------------------------ 属性卡 */

@Composable
private fun FrontMatterView(block: MdBlock.FrontMatter, spec: ThemeSpec, modifier: Modifier) {
    val c = spec.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(shape)
            .background(c.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, c.outline.copy(alpha = 0.7f), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "属性",
            style = spec.typography.caption,
            color = c.primary,
            fontWeight = FontWeight.Medium,
        )
        block.entries.forEach { (key, value) ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = key,
                    modifier = Modifier.width(96.dp),
                    style = spec.typography.small,
                    color = c.muted,
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = spec.typography.small,
                    color = c.onBackground,
                )
            }
        }
    }
}
