package com.shijian.md.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shijian.md.AppViewModel
import com.shijian.md.ui.components.ShijianIcons
import com.shijian.md.ui.theme.LocalMdTheme

/**
 * 底部弹层与两类对话框。
 *
 * 没有用 `ModalBottomSheet`：一是要跟视觉稿的圆角、手柄、行高对齐，二是这些弹层里还有
 * 需要就地改名的输入框，自己搭一层反而更省事。
 */

/** 主题调色板里没有「危险色」这一档，删除类动作用一个在深浅底子上都醒目的红。 */
@Composable
fun rememberDangerColor(): Color =
    if (LocalMdTheme.current.isDark) Color(0xFFF0929B) else Color(0xFFB23A48)

/** 弹层外壳：点空白处或按返回键收起。 */
@Composable
fun BottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val spec = LocalMdTheme.current
    BackHandler { onDismiss() }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.34f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(spec.colors.surface)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 10.dp, bottom = 14.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(spec.colors.outline),
            )
            content()
        }
    }
}

@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 13.dp, bottom = 7.dp),
    ) {
        Text(
            text = title,
            style = spec.typography.title,
            color = c.onBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(text = subtitle, style = spec.typography.caption, color = c.muted)
        }
    }
}

/** 弹层里的一行动作。 */
@Composable
fun SheetAction(
    icon: ImageVector,
    label: String,
    trailing: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val tint = if (danger) rememberDangerColor() else c.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(13.dp))
        Text(
            text = label,
            style = spec.typography.small,
            color = if (danger) tint else c.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(text = trailing, style = spec.typography.caption, color = c.muted)
        }
    }
}

/** 弹层底部的「取消 / 确定」。 */
@Composable
fun SheetButtons(
    cancelLabel: String,
    confirmLabel: String,
    confirmEnabled: Boolean = true,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .border(1.dp, c.outline, shape)
                .clickable(onClick = onCancel)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = cancelLabel, style = spec.typography.label, color = c.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .weight(1.4f)
                .clip(shape)
                .background(if (confirmEnabled) c.primary else c.surfaceVariant)
                .clickable(enabled = confirmEnabled, onClick = onConfirm)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = confirmLabel,
                style = spec.typography.label,
                color = if (confirmEnabled) c.onPrimary else c.muted,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 要一行文字的对话框：新建文档 / 新建文件夹 / 重命名都用它。 */
@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String,
    hint: String? = null,
    confirmLabel: String = "确定",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val spec = LocalMdTheme.current
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = spec.colors.surface,
        title = { Text(text = title, style = spec.typography.title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(text = label) },
                )
                if (hint != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = hint, style = spec.typography.caption, color = spec.colors.muted)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(text = confirmLabel, style = spec.typography.label)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", style = spec.typography.label, color = spec.colors.muted)
            }
        },
    )
}

/** 二次确认：删除、清空回收站这类做完就回不来的动作。 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    danger: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val spec = LocalMdTheme.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = spec.colors.surface,
        title = { Text(text = title, style = spec.typography.title) },
        text = {
            Text(text = message, style = spec.typography.small, color = spec.colors.onSurfaceVariant)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = spec.typography.label,
                    color = if (danger) rememberDangerColor() else spec.colors.primaryDeep,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", style = spec.typography.label, color = spec.colors.muted)
            }
        },
    )
}

/**
 * 「移动到…」弹层：点文件夹就是往里走一层，底部按钮把东西搬进**当前这一层**。
 *
 * 只用一个动作（点行 = 进入）对应视觉稿里的「上层 / 子文件夹 / 新建文件夹」三项，
 * 省得「选中」和「进入」两个手势打架。
 */
@Composable
fun MoveSheet(vm: AppViewModel, onNewFolder: () -> Unit) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val sources = vm.moveSource
    val dirs = sources.count { it.isDir }
    val subtitle = buildString {
        append(sources.size)
        append(" 项")
        if (dirs > 0) {
            append("（含 ")
            append(dirs)
            append(" 个文件夹）")
        }
        append(" · 点文件夹往里走，底部按钮搬过去")
    }
    BottomSheet(onDismiss = { vm.moveClose() }) {
        SheetTitle(title = "移动到…", subtitle = subtitle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 5.dp, bottom = 3.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(c.primaryWhisper)
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(ShijianIcons.Folder, contentDescription = null, tint = c.primaryDeep, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "当前：" + vm.moveLabel(),
                style = spec.typography.caption,
                color = c.primaryDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (vm.moveCanGoUp()) {
                SheetAction(icon = ShijianIcons.Up, label = "上一层", onClick = { vm.moveUp() })
            }
            vm.moveEntries.forEach { folder ->
                SheetAction(
                    icon = ShijianIcons.Folder,
                    label = folder.name,
                    trailing = if (folder.count > 0) folder.count.toString() + " 项" else "进入",
                    onClick = { vm.moveInto(folder.docId) },
                )
            }
            if (vm.moveEntries.isEmpty() && !vm.moveLoading) {
                Text(
                    text = "这一层没有子文件夹，可以直接搬到这里",
                    style = spec.typography.caption,
                    color = c.muted,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 2.dp),
                )
            }
            SheetAction(icon = ShijianIcons.FolderPlus, label = "新建文件夹…", onClick = onNewFolder)
        }
        if (vm.moveIsNoop()) {
            Text(
                text = "这些条目已经在这儿了，往里走一层再搬吧",
                style = spec.typography.caption,
                color = c.muted,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp),
            )
        }
        SheetButtons(
            cancelLabel = "取消",
            confirmLabel = if (vm.moveIsNoop()) "就在这一层" else "移到「" + vm.moveDirName() + "」",
            confirmEnabled = !vm.moveIsNoop() && !vm.moveLoading,
            onCancel = { vm.moveClose() },
            onConfirm = { vm.moveConfirm() },
        )
    }
}
