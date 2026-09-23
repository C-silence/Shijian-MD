package com.shijian.md.ui.home

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shijian.md.AppViewModel
import com.shijian.md.data.RecentDoc
import com.shijian.md.ui.components.AppMark
import com.shijian.md.ui.components.ShijianIcons
import com.shijian.md.ui.theme.LocalMdTheme
import com.shijian.md.ui.theme.ThemeSpec

/** 「最近打开」：跨文件夹的时间线视图，作为笔记库的补充入口。 */
@Composable
fun HomeScreen(vm: AppViewModel, onOpenSettings: () -> Unit, onBack: (() -> Unit)? = null) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val recents = vm.recents.items
    // content:// 的条目一旦丢了长期授权（第三方分享常见）就再也打不开，提前标出来
    val needsRegrant = remember(recents) { vm.recentNeedsRegrant(recents) }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.openExternal(uri) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 48.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = ShijianIcons.Back,
                            contentDescription = "返回笔记库",
                            tint = c.onSurfaceVariant,
                        )
                    }
                }
                AppMark(size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "拾简",
                        style = spec.typography.h2,
                        color = c.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "手机与平板的 Markdown 阅读台",
                        style = spec.typography.caption,
                        color = c.muted,
                    )
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(44.dp)) {
                    Icon(ShijianIcons.Settings, contentDescription = "设置", tint = c.onSurfaceVariant)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionCard(
                    title = "打开文档",
                    desc = "从本机选择 .md 文件",
                    icon = ShijianIcons.Folder,
                    spec = spec,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        openLauncher.launch(
                            arrayOf(
                                "text/markdown",
                                "text/x-markdown",
                                "text/plain",
                                "application/octet-stream",
                            )
                        )
                    },
                )
                ActionCard(
                    title = "新建文档",
                    desc = "从空白开始写",
                    icon = ShijianIcons.Add,
                    spec = spec,
                    modifier = Modifier.weight(1f),
                    onClick = { vm.newDoc() },
                )
            }
        }

        val draft = vm.draftLabel
        if (draft != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.primaryWhisper)
                        .border(1.dp, c.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable { vm.openDraft() }
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "继续上次的草稿",
                            style = spec.typography.small,
                            color = c.primaryDeep,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = draft,
                            style = spec.typography.caption,
                            color = c.muted,
                        )
                    }
                    TextButton(onClick = { vm.discardDraft() }) {
                        Text("丢弃", style = spec.typography.label, color = c.muted)
                    }
                }
            }
        }

        item {
            Text(
                text = "最近打开",
                modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                style = spec.typography.label,
                color = c.muted,
                fontWeight = FontWeight.Medium,
            )
        }

        if (recents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surfaceVariant.copy(alpha = 0.4f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "还没有打开过文档。\n也可以直接把 .md 文件分享到拾简。",
                        style = spec.typography.small,
                        color = c.muted,
                    )
                }
            }
        } else {
            items(recents, key = { it.uri }) { item ->
                RecentRow(
                    item = item,
                    spec = spec,
                    needsRegrant = item.uri in needsRegrant,
                    onOpen = { vm.open(android.net.Uri.parse(item.uri)) },
                    onRemove = { vm.recents.remove(item.uri) },
                )
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "拾简 0.1 · 文档仅在本机处理",
                    style = spec.typography.caption,
                    color = c.muted,
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = spec.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.primarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.primaryDeep,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = spec.typography.title, color = c.onBackground)
        Spacer(Modifier.height(2.dp))
        Text(desc, style = spec.typography.caption, color = c.muted)
    }
}

@Composable
private fun RecentRow(
    item: RecentDoc,
    spec: ThemeSpec,
    needsRegrant: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val c = spec.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline, shape)
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = spec.typography.small,
                color = c.onBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (needsRegrant) {
                    Text(
                        text = "需重新授权",
                        style = spec.typography.caption,
                        color = c.primaryDeep,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(c.primaryWhisper)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = buildString {
                        append(DateUtils.getRelativeTimeSpanString(item.time))
                        if (item.preview.isNotBlank()) {
                            append(" · ")
                            append(item.preview)
                        }
                    },
                    style = spec.typography.caption,
                    color = c.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = ShijianIcons.Delete,
                contentDescription = "从列表移除",
                tint = c.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
