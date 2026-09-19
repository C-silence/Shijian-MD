package com.shijian.md.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shijian.md.AppViewModel
import com.shijian.md.OpenDoc
import com.shijian.md.ViewMode
import com.shijian.md.ui.components.ShijianIcons
import com.shijian.md.ui.editor.EditorPane
import com.shijian.md.ui.editor.EditorToolbarHeight
import com.shijian.md.ui.reader.OutlinePane
import com.shijian.md.ui.reader.ReaderPane
import com.shijian.md.ui.theme.LocalMdTheme
import com.shijian.md.ui.theme.ThemeSpec
import com.shijian.md.util.copyPlainText
import com.shijian.md.util.sharePlainText
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DocumentScreen(
    vm: AppViewModel,
    window: WindowClass,
    onOpenSettings: () -> Unit,
) {
    val doc = vm.doc ?: return
    key(doc) {
        DocumentContent(vm = vm, doc = doc, window = window, onOpenSettings = onOpenSettings)
    }
}

@Composable
private fun DocumentContent(
    vm: AppViewModel,
    doc: OpenDoc,
    window: WindowClass,
    onOpenSettings: () -> Unit,
) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val settings = vm.settings
    val parsed = vm.parsed
    val mode = vm.mode
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState(doc.scrollIndex, doc.scrollOffset)
    var editorValue by remember { mutableStateOf(TextFieldValue(doc.text)) }
    var outlineSheet by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri -> if (uri != null) vm.saveAs(uri) }

    LaunchedEffect(doc.text) {
        if (editorValue.text != doc.text) {
            val caret = editorValue.selection.start.coerceIn(0, doc.text.length)
            editorValue = TextFieldValue(doc.text, TextRange(caret))
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                doc.scrollIndex = index
                doc.scrollOffset = offset
            }
    }

    val currentLine = remember(listState.firstVisibleItemIndex, parsed.outline) {
        parsed.outline.lastOrNull { it.blockIndex <= listState.firstVisibleItemIndex }?.line ?: -1
    }
    val progress = if (parsed.blocks.isEmpty()) 100 else {
        ((listState.firstVisibleItemIndex + 1) * 100 / parsed.blocks.size).coerceIn(0, 100)
    }
    val imageBase = remember(doc.uri) {
        doc.uri?.takeIf { it.scheme == "file" }?.path?.let { File(it).parentFile }
    }
    val showOutlinePanel = window == WindowClass.EXPANDED &&
        settings.outlineOpen &&
        parsed.outline.isNotEmpty() &&
        mode == ViewMode.READ

    // 阅读/分栏预览共用同一个 listState，直接观察它的滚动位置
    val topThreshold = with(LocalDensity.current) { 160.dp.roundToPx() }
    val scrolledDown by remember(topThreshold) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > topThreshold
        }
    }
    // 源码编辑用的是 BasicTextField 的内部滚动，没有可观察的滚动量，
    // 因此改看光标：只要光标不在文首就给出回顶入口（回顶会把光标一并带回文首）。
    val showBackToTop = if (mode == ViewMode.EDIT) editorValue.selection.min > 0 else scrolledDown
    val backToTop: () -> Unit = {
        if (mode == ViewMode.EDIT) {
            editorValue = editorValue.copy(selection = TextRange(0))
        } else {
            scope.launch { listState.animateScrollToItem(0) }
        }
    }
    // 编辑模式下工具条贴在内容区底部，按钮要让开它
    val backToTopBottom = if (mode == ViewMode.EDIT && !doc.readOnly) EditorToolbarHeight + 10.dp else 16.dp

    Column(Modifier.fillMaxSize().background(c.background)) {
        DocTopBar(
            doc = doc,
            spec = spec,
            mode = mode,
            window = window,
            wordCount = parsed.wordCount,
            outlineAvailable = parsed.outline.isNotEmpty(),
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            onBack = { vm.closeDoc() },
            onMode = { vm.setMode(it) },
            onOutline = {
                if (window == WindowClass.EXPANDED) settings.outlineOpen = !settings.outlineOpen
                else outlineSheet = true
            },
            onSave = { vm.saveNow() },
            onSaveAs = { saveAsLauncher.launch(doc.name) },
            onReload = { vm.reload() },
            onCopy = {
                copyPlainText(context, doc.text, doc.name)
                vm.toast("已复制全文")
            },
            onShare = { sharePlainText(context, doc.text, doc.title) },
            onSettings = onOpenSettings,
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            Row(Modifier.fillMaxSize()) {
                if (showOutlinePanel) {
                    OutlinePane(
                        entries = parsed.outline,
                        currentLine = currentLine,
                        onJump = { entry ->
                            scope.launch { listState.animateScrollToItem(entry.blockIndex) }
                        },
                        modifier = Modifier.width(212.dp),
                        onClose = { settings.outlineOpen = false },
                    )
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (mode) {
                        ViewMode.READ -> ReaderPane(
                            document = parsed,
                            listState = listState,
                            imageBase = imageBase,
                            horizontalPadding = if (window == WindowClass.COMPACT) 16.dp else spec.spacing.pageTablet,
                            onOpenLink = { url -> openLink(context, vm, url) },
                            onToggleTask = { block, checked -> vm.toggleTask(block, checked) },
                        )

                        ViewMode.EDIT -> EditorPane(
                            value = editorValue,
                            onValueChange = {
                                editorValue = it
                                vm.updateText(it.text)
                            },
                            readOnly = doc.readOnly,
                            horizontalPadding = if (window == WindowClass.COMPACT) 16.dp else spec.spacing.pageTablet,
                        )

                        ViewMode.SPLIT -> Row(Modifier.fillMaxSize()) {
                            EditorPane(
                                value = editorValue,
                                onValueChange = {
                                    editorValue = it
                                    vm.updateText(it.text)
                                },
                                modifier = Modifier.weight(1f),
                                horizontalPadding = 14.dp,
                                showToolbar = false,
                                readOnly = doc.readOnly,
                            )
                            Box(Modifier.width(1.dp).fillMaxHeight().background(c.outline))
                            ReaderPane(
                                document = parsed,
                                listState = listState,
                                modifier = Modifier.weight(1f),
                                imageBase = imageBase,
                                horizontalPadding = 14.dp,
                                onOpenLink = { url -> openLink(context, vm, url) },
                                onToggleTask = { block, checked -> vm.toggleTask(block, checked) },
                            )
                        }
                    }
                }
            }

            BackToTopButton(
                visible = showBackToTop,
                spec = spec,
                onClick = backToTop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // 编辑模式下工具条也会随键盘上移，按钮跟着走才不会脱节
                    .imePadding()
                    .padding(
                        end = if (window == WindowClass.COMPACT) 16.dp else 24.dp,
                        bottom = backToTopBottom,
                    ),
            )

            if (outlineSheet || menuOpen) {
                // 占位，保证抽屉与菜单关闭后可点击
            }

            if (outlineSheet) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .width(288.dp)
                            .fillMaxHeight()
                            .background(c.background)
                    ) {
                        OutlinePane(
                            entries = parsed.outline,
                            currentLine = currentLine,
                            onJump = { entry ->
                                scope.launch { listState.animateScrollToItem(entry.blockIndex) }
                                outlineSheet = false
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            onClose = { outlineSheet = false },
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable { outlineSheet = false }
                    )
                }
            }
        }

        if (mode == ViewMode.READ) {
            ReaderBottomBar(
                spec = spec,
                bodySize = settings.bodySize,
                progress = progress,
                readOnly = doc.readOnly,
                onSmaller = { settings.smallerText() },
                onLarger = { settings.largerText() },
                onEdit = { vm.setMode(ViewMode.EDIT) },
            )
        }
    }
}

/* ------------------------------------------------------------------ 顶栏 */

@Composable
private fun DocTopBar(
    doc: OpenDoc,
    spec: ThemeSpec,
    mode: ViewMode,
    window: WindowClass,
    wordCount: Int,
    outlineAvailable: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onMode: (ViewMode) -> Unit,
    onOutline: () -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onReload: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = spec.colors
    Column(Modifier.fillMaxWidth().background(c.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(ShijianIcons.Back, contentDescription = "返回", tint = c.onSurfaceVariant)
            }
            Column(Modifier.weight(1f).padding(horizontal = 2.dp)) {
                Text(
                    text = doc.name,
                    style = spec.typography.title,
                    color = c.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(if (doc.dirty) "未保存" else "已保存")
                        append(" · ")
                        append(wordCount)
                        append(" 字")
                        if (doc.readOnly) append(" · 只读")
                        if (doc.imported) append(" · 导入副本")
                    },
                    style = spec.typography.caption,
                    color = c.muted,
                    maxLines = 1,
                )
            }
            if (outlineAvailable) {
                IconButton(onClick = onOutline, modifier = Modifier.size(44.dp)) {
                    Icon(ShijianIcons.Outline, contentDescription = "目录", tint = c.onSurfaceVariant)
                }
            }
            if (window == WindowClass.COMPACT) {
                IconButton(
                    onClick = { onMode(if (mode == ViewMode.READ) ViewMode.EDIT else ViewMode.READ) },
                    modifier = Modifier.size(44.dp),
                    enabled = !doc.readOnly,
                ) {
                    Icon(
                        imageVector = if (mode == ViewMode.READ) ShijianIcons.Edit else ShijianIcons.Reader,
                        contentDescription = if (mode == ViewMode.READ) "编辑" else "阅读",
                        tint = if (doc.readOnly) c.muted else c.primary,
                    )
                }
            } else {
                ModePill(mode = mode, spec = spec, readOnly = doc.readOnly, onMode = onMode)
            }
            IconButton(
                onClick = onSave,
                modifier = Modifier.size(44.dp),
                enabled = doc.dirty && !doc.readOnly,
            ) {
                Icon(
                    imageVector = if (doc.dirty) ShijianIcons.Save else ShijianIcons.Check,
                    contentDescription = "保存",
                    tint = if (doc.dirty) c.primary else c.muted.copy(alpha = 0.7f),
                )
            }
            Box {
                IconButton(
                    onClick = { onMenuOpenChange(true) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(ShijianIcons.More, contentDescription = "更多", tint = c.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) },
                    containerColor = c.surface,
                ) {
                    MenuItem("保存", spec, enabled = !doc.readOnly) {
                        onMenuOpenChange(false); onSave()
                    }
                    MenuItem("另存为…", spec) { onMenuOpenChange(false); onSaveAs() }
                    MenuItem("重新载入", spec, enabled = doc.uri != null) {
                        onMenuOpenChange(false); onReload()
                    }
                    MenuItem("复制全文", spec) { onMenuOpenChange(false); onCopy() }
                    MenuItem("分享", spec) { onMenuOpenChange(false); onShare() }
                    MenuItem("外观设置", spec) { onMenuOpenChange(false); onSettings() }
                    MenuItem("关闭文档", spec) { onMenuOpenChange(false); onBack() }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline.copy(alpha = 0.7f)))
    }
}

@Composable
private fun MenuItem(
    label: String,
    spec: ThemeSpec,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = spec.typography.small,
                color = if (enabled) spec.colors.onBackground else spec.colors.muted,
            )
        },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun ModePill(
    mode: ViewMode,
    spec: ThemeSpec,
    readOnly: Boolean,
    onMode: (ViewMode) -> Unit,
) {
    val c = spec.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfaceVariant.copy(alpha = 0.7f))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeButton(ShijianIcons.Reader, "阅读", mode == ViewMode.READ, spec) { onMode(ViewMode.READ) }
        ModeButton(ShijianIcons.Edit, "编辑", mode == ViewMode.EDIT, spec, enabled = !readOnly) {
            onMode(ViewMode.EDIT)
        }
        ModeButton(ShijianIcons.Split, "分栏", mode == ViewMode.SPLIT, spec, enabled = !readOnly) {
            onMode(ViewMode.SPLIT)
        }
    }
}

@Composable
private fun ModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    spec: ThemeSpec,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = spec.colors
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) c.background else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> c.muted.copy(alpha = 0.5f)
                active -> c.primary
                else -> c.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp),
        )
    }
}

/* ------------------------------------------------------------------ 阅读底栏 */

@Composable
private fun ReaderBottomBar(
    spec: ThemeSpec,
    bodySize: Float,
    progress: Int,
    readOnly: Boolean,
    onSmaller: () -> Unit,
    onLarger: () -> Unit,
    onEdit: () -> Unit,
) {
    val c = spec.colors
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline.copy(alpha = 0.7f)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onSmaller) {
                Text("A-", style = spec.typography.label, color = c.onSurfaceVariant)
            }
            Text(
                text = bodySize.toInt().toString(),
                style = spec.typography.caption,
                color = c.muted,
                modifier = Modifier.width(24.dp),
            )
            TextButton(onClick = onLarger) {
                Text("A+", style = spec.typography.label, color = c.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "$progress%",
                style = spec.typography.caption,
                color = c.muted,
            )
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onEdit, enabled = !readOnly) {
                Text(
                    text = "编辑",
                    style = spec.typography.label,
                    color = if (readOnly) c.muted else c.primary,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ 回到顶部 */

/**
 * 悬浮回顶按钮：滚出首屏后淡入，点一下回到文首（编辑模式下同时把光标带回文首）。
 */
@Composable
private fun BackToTopButton(
    visible: Boolean,
    spec: ThemeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = spec.colors
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.86f),
        exit = fadeOut() + scaleOut(targetScale = 0.86f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(4.dp, CircleShape, ambientColor = c.shadow, spotColor = c.shadow)
                .clip(CircleShape)
                .background(c.surface)
                .border(1.dp, c.outline.copy(alpha = 0.8f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShijianIcons.Up,
                contentDescription = "回到顶部",
                tint = c.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun openLink(context: android.content.Context, vm: AppViewModel, url: String) {
    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("mailto:")) {
        runCatching {
            context.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { vm.toast("没有可以打开该链接的应用") }
    } else {
        vm.toast("相对链接：$url")
    }
}
