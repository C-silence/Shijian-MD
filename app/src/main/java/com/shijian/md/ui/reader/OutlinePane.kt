package com.shijian.md.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shijian.md.md.OutlineEntry
import com.shijian.md.ui.components.ShijianIcons
import com.shijian.md.ui.theme.LocalMdTheme

@Composable
fun OutlinePane(
    entries: List<OutlineEntry>,
    currentLine: Int,
    onJump: (OutlineEntry) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    Row(modifier = modifier.fillMaxHeight()) {
    Column(modifier = Modifier.fillMaxHeight().weight(1f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "目录",
                style = spec.typography.label,
                color = c.muted,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            if (onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.width(40.dp).height(40.dp)) {
                    Icon(ShijianIcons.Close, contentDescription = "收起目录", tint = c.muted, modifier = Modifier.width(16.dp).height(16.dp))
                }
            }
        }
        if (entries.isEmpty()) {
            Text(
                text = "这篇文档还没有标题",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = spec.typography.small,
                color = c.muted,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 10.dp, end = 10.dp, bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(entries, key = { i, _ -> i }) { _, entry ->
                    val active = entry.line == currentLine
                    val indent = ((entry.level - 1).coerceIn(0, 5) * 10).dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) c.primaryWhisper else c.background.copy(alpha = 0f))
                            .clickable { onJump(entry) }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    when {
                                        entry.level == 1 -> c.primary
                                        entry.level == 2 -> c.primary.copy(alpha = 0.55f)
                                        else -> c.outline
                                    }
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = entry.title,
                            style = if (entry.level <= 2) spec.typography.small else spec.typography.caption,
                            color = if (active) c.primaryDeep else c.onSurfaceVariant,
                            fontWeight = if (entry.level == 1) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
    Box(
        Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(c.outline)
    )
    }
}

@Composable
fun OutlineSheetFrame(content: @Composable () -> Unit) {
    val spec = LocalMdTheme.current
    Box(
        Modifier
            .fillMaxWidth(0.82f)
            .fillMaxHeight()
            .background(spec.colors.background)
            .padding(top = 12.dp)
    ) { content() }
}
