package com.shijian.md.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shijian.md.AppViewModel
import com.shijian.md.data.TrashItem
import com.shijian.md.data.TrashStore
import com.shijian.md.ui.components.ShijianIcons
import com.shijian.md.ui.theme.LocalMdTheme
import com.shijian.md.ui.theme.ThemeSpec

/**
 * 回收站：删除只是把文件搬进库根的 `.trash/`，这里能看到每一条「原来在哪、还剩几天」。
 *
 * 7 天一到，进回收站或下次启动时会被静默清掉；想提前清就点「清空」。
 */
@Composable
fun TrashScreen(vm: AppViewModel, onBack: () -> Unit) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val items = vm.trashItems
    var confirmEmpty by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TrashItem?>(null) }

    BackHandler { onBack() }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(58.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(ShijianIcons.Back, contentDescription = "返回", tint = c.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "回收站",
                        style = spec.typography.title,
                        color = c.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (items.isEmpty()) "空的" else items.size.toString() + " 项 · 保留 " + TrashStore.KEEP_DAYS + " 天",
                        style = spec.typography.caption,
                        color = c.muted,
                    )
                }
                if (items.isNotEmpty()) {
                    TextButton(onClick = { confirmEmpty = true }) {
                        Text(text = "清空", style = spec.typography.label, color = rememberDangerColor())
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (items.isEmpty()) {
                    item { TrashEmptyCard(spec = spec, loading = vm.trashLoading) }
                }
                items(items, key = { it.docId }) { item ->
                    TrashRow(
                        vm = vm,
                        item = item,
                        spec = spec,
                        onRestore = { vm.trashRestore(listOf(item)) },
                        onDelete = { deleteTarget = item },
                    )
                }
            }
        }

        if (confirmEmpty) {
            ConfirmDialog(
                title = "清空回收站？",
                message = "里面的 " + items.size + " 项会被永久删除，没法再还原。",
                confirmLabel = "永久删除",
                onDismiss = { confirmEmpty = false },
                onConfirm = {
                    confirmEmpty = false
                    vm.trashEmpty()
                },
            )
        }
        deleteTarget?.let { target ->
            ConfirmDialog(
                title = "彻底删除「" + target.name + "」？",
                message = "这一步跳过回收站，删掉就找不回来了。",
                confirmLabel = "永久删除",
                onDismiss = { deleteTarget = null },
                onConfirm = {
                    deleteTarget = null
                    vm.trashDelete(listOf(target))
                },
            )
        }
    }
}

@Composable
private fun TrashRow(
    vm: AppViewModel,
    item: TrashItem,
    spec: ThemeSpec,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = spec.colors
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline, shape)
            .padding(start = 12.dp, end = 4.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(c.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (item.isDir) ShijianIcons.Folder else ShijianIcons.Doc,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = spec.typography.small,
                color = c.onBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "原位置：" + vm.trashLocation(item) + " · 剩 " + vm.trashRemainDays(item) + " 天",
                style = spec.typography.caption,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRestore, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = ShijianIcons.Restore,
                contentDescription = "还原",
                tint = c.primaryDeep,
                modifier = Modifier.size(19.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = ShijianIcons.Delete,
                contentDescription = "彻底删除",
                tint = rememberDangerColor(),
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun TrashEmptyCard(spec: ThemeSpec, loading: Boolean) {
    val c = spec.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceVariant.copy(alpha = 0.4f))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (loading) "正在读取…" else "回收站是空的。删掉的笔记会先放这儿，7 天里随时能还原。",
            style = spec.typography.small,
            color = c.muted,
            textAlign = TextAlign.Center,
        )
    }
}
