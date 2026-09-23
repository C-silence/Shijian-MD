package com.shijian.md.ui.home

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.md.AppViewModel
import com.shijian.md.data.LibEntry
import com.shijian.md.data.Library
import com.shijian.md.data.RecentDoc
import com.shijian.md.ui.WindowClass
import com.shijian.md.ui.components.AppMark
import com.shijian.md.ui.components.ShijianIcons
import com.shijian.md.ui.rememberWindowClass
import com.shijian.md.ui.theme.LocalMdTheme
import com.shijian.md.ui.theme.ThemeSpec
import kotlin.math.roundToInt

/**
 * 库首页：以文件夹为单位组织笔记。
 *
 * 手机上是一列列表（长按进入多选批量整理），平板（≥840dp）左边多一栏快捷入口。
 * 阅读进度、回收站这类附属信息存在应用自己的记录里，文件本体始终只有库里的那一份。
 */

/** 列表里能触发的动作集中传下去，省得每层都摊开十几个 lambda。 */
private class LibActions(
    val settings: () -> Unit,
    val recents: () -> Unit,
    val newDoc: () -> Unit,
    val newFolder: () -> Unit,
    val openFile: () -> Unit,
    val repick: () -> Unit,
    val trash: () -> Unit,
    val move: (List<LibEntry>) -> Unit,
    val rename: (LibEntry) -> Unit,
    val export: (LibEntry) -> Unit,
    val askTrash: () -> Unit,
)

/** 当前等着输入名字的那个对话框。 */
private sealed interface NameTarget {
    object NewDoc : NameTarget
    object NewFolder : NameTarget
    object MoveFolder : NameTarget
    class Rename(val entry: LibEntry) : NameTarget
}

@Composable
fun LibraryScreen(vm: AppViewModel, onOpenSettings: () -> Unit, onOpenRecents: () -> Unit) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val window = rememberWindowClass()
    var newSheet by remember { mutableStateOf(false) }
    var nameTarget by remember { mutableStateOf<NameTarget?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    /** 等着确认删除的条目：可能来自多选，也可能来自单条弹层。 */
    var trashTargets by remember { mutableStateOf<List<LibEntry>>(emptyList()) }
    var exportTarget by remember { mutableStateOf<LibEntry?>(null) }

    val entries = vm.libEntries
    val entryByDocId = remember(entries) { entries.associateBy { it.docId } }
    val picked = selected.mapNotNull { entryByDocId[it] }
    val selectionMode = selected.isNotEmpty()
    val folderName = vm.libTrail().lastOrNull()?.second ?: Library.DEFAULT_ROOT_NAME

    // 换目录就把选择清掉，免得选中的东西跟着跑
    LaunchedEffect(vm.libDirId) { selected = emptySet() }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // 站在哪个文件夹里点进来的，就落在哪个文件夹；外面分享进来的才走收件箱
            vm.openExternal(uri, targetDir = vm.libDirId ?: vm.library.rootDocId)
        }
    }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.attachLibrary(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        val entry = exportTarget
        exportTarget = null
        if (uri != null && entry != null) vm.libExportTo(entry, uri)
    }

    val actions = LibActions(
        settings = onOpenSettings,
        recents = onOpenRecents,
        newDoc = {
            newSheet = false
            nameTarget = NameTarget.NewDoc
        },
        newFolder = {
            newSheet = false
            nameTarget = NameTarget.NewFolder
        },
        openFile = {
            openLauncher.launch(
                arrayOf("text/markdown", "text/x-markdown", "text/plain", "application/octet-stream"),
            )
        },
        repick = { treeLauncher.launch(null) },
        trash = { vm.openTrash() },
        move = { vm.openMove(it) },
        rename = { nameTarget = NameTarget.Rename(it) },
        export = { entry ->
            exportTarget = entry
            exportLauncher.launch(entry.name)
        },
        askTrash = { trashTargets = picked },
    )

    BackHandler(enabled = selectionMode || !vm.atLibraryRoot()) {
        if (selectionMode) selected = emptySet() else vm.libUp()
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        Row(Modifier.fillMaxSize()) {
            if (window == WindowClass.EXPANDED) {
                LibrarySidebar(vm = vm, spec = spec, actions = actions, currentDirId = vm.libDirId)
            }
            Box(Modifier.weight(1f)) {
                LibraryPane(
                    vm = vm,
                    spec = spec,
                    selected = selected,
                    picked = picked,
                    onToggle = { entry ->
                        selected = if (entry.docId in selected) selected - entry.docId else selected + entry.docId
                    },
                    onLongPress = { entry -> selected = selected + entry.docId },
                    onClearSelection = { selected = emptySet() },
                    onSelectAll = {
                        selected = if (selected.size == entries.size) emptySet() else entries.map { it.docId }.toSet()
                    },
                    onFabClick = { newSheet = true },
                    actions = actions,
                )
            }
        }

        if (newSheet) {
            BottomSheet(onDismiss = { newSheet = false }) {
                SheetTitle(title = "新建", subtitle = "都放在「" + folderName + "」")
                SheetAction(icon = ShijianIcons.Doc, label = "新建文档", onClick = actions.newDoc)
                SheetAction(icon = ShijianIcons.FolderPlus, label = "新建文件夹", onClick = actions.newFolder)
                SheetAction(icon = ShijianIcons.Folder, label = "从本机导入文件", onClick = actions.openFile)
            }
        }

        if (vm.moveSource.isNotEmpty()) {
            MoveSheet(vm = vm, onNewFolder = { nameTarget = NameTarget.MoveFolder })
        }

        nameTarget?.let { target ->
            when (target) {
                is NameTarget.NewDoc -> NameDialog(
                    title = "新建文档",
                    label = "文件名",
                    initial = "未命名.md",
                    hint = "建在「" + folderName + "」，建好立刻打开",
                    confirmLabel = "创建",
                    onDismiss = { nameTarget = null },
                    onConfirm = {
                        nameTarget = null
                        vm.libCreateDoc(it)
                    },
                )
                is NameTarget.NewFolder -> NameDialog(
                    title = "新建文件夹",
                    label = "文件夹名",
                    initial = "",
                    hint = "建在「" + folderName + "」，用来分类放笔记",
                    confirmLabel = "创建",
                    onDismiss = { nameTarget = null },
                    onConfirm = {
                        nameTarget = null
                        vm.libCreateFolder(it)
                    },
                )
                is NameTarget.MoveFolder -> NameDialog(
                    title = "新建文件夹",
                    label = "文件夹名",
                    initial = "",
                    hint = "建好之后就能把东西搬进去",
                    confirmLabel = "创建",
                    onDismiss = { nameTarget = null },
                    onConfirm = {
                        nameTarget = null
                        vm.moveNewFolder(it)
                    },
                )
                is NameTarget.Rename -> {
                    val entry = target.entry
                    NameDialog(
                        title = "重命名",
                        label = "新名字",
                        initial = entry.name,
                        hint = if (entry.isDir) null else "不带 .md 也可以，会自动补上",
                        confirmLabel = "改名",
                        onDismiss = { nameTarget = null },
                        onConfirm = {
                            nameTarget = null
                            vm.libRename(entry, it)
                        },
                    )
                }
            }
        }

        if (trashTargets.isNotEmpty()) {
            val targets = trashTargets
            val title = if (targets.size == 1) {
                "删除「" + targets[0].name + "」？"
            } else {
                "删除选中的 " + targets.size + " 项？"
            }
            ConfirmDialog(
                title = title,
                message = "会先放进回收站，7 天后自动清理。这期间在回收站里随时能还原。",
                confirmLabel = "移入回收站",
                onDismiss = { trashTargets = emptyList() },
                onConfirm = {
                    trashTargets = emptyList()
                    selected = selected - targets.map { it.docId }.toSet()
                    vm.libTrash(targets)
                },
            )
        }

        vm.moveConflict?.let { conflict ->
            AlertDialog(
                onDismissRequest = { vm.dismissMoveConflict() },
                containerColor = c.surface,
                title = { Text(text = "目标文件夹里有同名的", style = spec.typography.title) },
                text = {
                    Text(
                        text = "「" + conflict.names.joinToString("、") + "」在那边已经有了。两边都留着（自动加序号），还是用这边的替换掉？",
                        style = spec.typography.small,
                        color = c.onSurfaceVariant,
                    )
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = { vm.moveResolveConflict(keepBoth = false) }) {
                            Text(text = "替换", style = spec.typography.label, color = rememberDangerColor())
                        }
                        TextButton(onClick = { vm.moveResolveConflict(keepBoth = true) }) {
                            Text(text = "都留着", style = spec.typography.label)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { vm.dismissMoveConflict() }) {
                        Text(text = "取消", style = spec.typography.label, color = c.muted)
                    }
                },
            )
        }
    }
}

@Composable
private fun LibraryPane(
    vm: AppViewModel,
    spec: ThemeSpec,
    selected: Set<String>,
    picked: List<LibEntry>,
    onToggle: (LibEntry) -> Unit,
    onLongPress: (LibEntry) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onFabClick: () -> Unit,
    actions: LibActions,
) {
    val c = spec.colors
    val entries = vm.libEntries
    val trail = vm.libTrail()
    val atRoot = vm.atLibraryRoot()
    val reading = if (atRoot && selected.isEmpty()) vm.continueReading() else emptyList()
    val folders = entries.filter { it.isDir }
    val files = entries.filterNot { it.isDir }
    val folderName = trail.lastOrNull()?.second ?: Library.DEFAULT_ROOT_NAME
    val selectionMode = selected.isNotEmpty()
    var menuOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    IconButton(onClick = onClearSelection, modifier = Modifier.size(40.dp)) {
                        Icon(ShijianIcons.Close, contentDescription = "退出多选", tint = c.onSurfaceVariant)
                    }
                    Text(
                        text = "已选 " + selected.size + " 项",
                        style = spec.typography.title,
                        color = c.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onSelectAll) {
                        Text(
                            text = if (selected.size == entries.size) "取消全选" else "全选",
                            style = spec.typography.label,
                            color = c.primaryDeep,
                        )
                    }
                } else {
                    if (!atRoot) {
                        IconButton(onClick = { vm.libUp() }, modifier = Modifier.size(40.dp)) {
                            Icon(ShijianIcons.Back, contentDescription = "上一层", tint = c.onSurfaceVariant)
                        }
                    }
                    if (atRoot) {
                        // 库根就是首页：左边放应用标记，中间标题居中放大、用主题主色。
                        // 左边这块和右边两个按钮（40 + 40）一样宽，标题才是真的落在屏幕中间；
                        // 标记再往里缩 10dp，左边缘就和右边两个图标的字形对齐（都是 14dp）。
                        Box(
                            modifier = Modifier.width(80.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            AppMark(modifier = Modifier.padding(start = 10.dp))
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            LibraryTitle(text = vm.library.rootName, spec = spec)
                        }
                    } else {
                        LibraryBreadcrumbs(
                            vm = vm,
                            spec = spec,
                            trail = trail,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    IconButton(onClick = actions.openFile, modifier = Modifier.size(40.dp)) {
                        Icon(ShijianIcons.Folder, contentDescription = "从本机导入文件", tint = c.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(40.dp)) {
                            Icon(ShijianIcons.More, contentDescription = "更多", tint = c.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("新建文档", style = spec.typography.small) },
                                onClick = {
                                    menuOpen = false
                                    actions.newDoc()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("新建文件夹", style = spec.typography.small) },
                                onClick = {
                                    menuOpen = false
                                    actions.newFolder()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("回收站", style = spec.typography.small) },
                                onClick = {
                                    menuOpen = false
                                    actions.trash()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("最近打开", style = spec.typography.small) },
                                onClick = {
                                    menuOpen = false
                                    actions.recents()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("设置", style = spec.typography.small) },
                                onClick = {
                                    menuOpen = false
                                    actions.settings()
                                },
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (vm.libDenied) {
                    item {
                        DeniedCard(spec = spec, onRepick = actions.repick)
                    }
                }
                if (reading.isNotEmpty()) {
                    item { SectionTitle(text = "继续阅读", spec = spec) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            reading.forEach { doc ->
                                ContinueCard(
                                    item = doc,
                                    spec = spec,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        runCatching { android.net.Uri.parse(doc.uri) }.getOrNull()
                                            ?.let { vm.openLibraryDoc(it, edit = false) }
                                    },
                                )
                            }
                            repeat(3 - reading.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                if (folders.isNotEmpty()) {
                    item { SectionTitle(text = "文件夹", spec = spec) }
                    items(folders, key = { it.docId }) { entry ->
                        LibRow(
                            entry = entry,
                            spec = spec,
                            selected = entry.docId in selected,
                            selectionMode = selectionMode,
                            onClick = { if (selectionMode) onToggle(entry) else vm.libEnter(entry) },
                            onLongClick = { if (selectionMode) onToggle(entry) else onLongPress(entry) },
                        )
                    }
                }
                if (files.isNotEmpty()) {
                    item { SectionTitle(text = "文件", spec = spec) }
                    items(files, key = { it.docId }) { entry ->
                        LibRow(
                            entry = entry,
                            spec = spec,
                            selected = entry.docId in selected,
                            selectionMode = selectionMode,
                            onClick = { if (selectionMode) onToggle(entry) else vm.libEnter(entry) },
                            onLongClick = { if (selectionMode) onToggle(entry) else onLongPress(entry) },
                        )
                    }
                }
                if (entries.isEmpty()) {
                    item {
                        Text(
                            text = if (vm.libLoading) "正在读取…" else "",
                            style = spec.typography.caption,
                            color = c.muted,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    if (!vm.libLoading) item { EmptyCard(folderName = folderName, spec = spec) }
                }
            }
        }

        if (selectionMode) {
            BatchBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                picked = picked,
                spec = spec,
                actions = actions,
            )
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 18.dp)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.primary)
                    .clickable(onClick = onFabClick)
                    .height(46.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(ShijianIcons.Add, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "新建",
                    style = spec.typography.small,
                    color = c.onPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/* -------------------------------------------------------- 平板：左侧栏 */

/** 大屏左边那一栏：库根、一级文件夹、回收站、最近打开。手机上不出现。 */
@Composable
private fun LibrarySidebar(
    vm: AppViewModel,
    spec: ThemeSpec,
    actions: LibActions,
    currentDirId: String?,
) {
    val c = spec.colors
    val root = vm.library.rootDocId
    val here = currentDirId ?: root
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(214.dp)
            .background(c.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 14.dp, bottom = 12.dp),
    ) {
        // 侧栏这里只做分区标签：大标题和标记都在右边那栏的顶栏上，这里别重复一遍
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "笔记库",
                style = spec.typography.label,
                color = c.muted,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
        }
        SideItem(
            label = "全部笔记",
            icon = ShijianIcons.Outline,
            selected = here == root,
            spec = spec,
            onClick = { vm.browseLibrary(root, force = true) },
        )
        vm.rootFolders.forEach { folder ->
            SideItem(
                label = folder.name,
                icon = ShijianIcons.Folder,
                selected = here == folder.docId,
                spec = spec,
                onClick = { vm.browseLibrary(folder.docId, force = true) },
            )
        }
        SideItem(
            label = "新建文件夹",
            icon = ShijianIcons.FolderPlus,
            selected = false,
            spec = spec,
            onClick = actions.newFolder,
        )
        Spacer(Modifier.weight(1f))
        SideItem(
            label = "回收站",
            icon = ShijianIcons.Delete,
            selected = false,
            spec = spec,
            onClick = actions.trash,
        )
        SideItem(
            label = "最近打开",
            icon = ShijianIcons.Reader,
            selected = false,
            spec = spec,
            onClick = actions.recents,
        )
        SideItem(
            label = "设置",
            icon = ShijianIcons.Settings,
            selected = false,
            spec = spec,
            onClick = actions.settings,
        )
    }
}

@Composable
private fun SideItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    spec: ThemeSpec,
    onClick: () -> Unit,
) {
    val c = spec.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) c.primaryWhisper else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) c.primaryDeep else c.muted,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = spec.typography.small,
            color = if (selected) c.primaryDeep else c.onBackground,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* -------------------------------------------------------- 多选：底部动作栏 */

@Composable
private fun BatchBar(
    modifier: Modifier,
    picked: List<LibEntry>,
    spec: ThemeSpec,
    actions: LibActions,
) {
    val c = spec.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .fillMaxWidth()
            .shadow(8.dp, shape)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline, shape)
            .padding(vertical = 4.dp),
    ) {
        BatchItem(Modifier.weight(1f), ShijianIcons.MoveTo, "移动", spec) { actions.move(picked) }
        if (picked.size == 1) {
            BatchItem(Modifier.weight(1f), ShijianIcons.Edit, "重命名", spec) { actions.rename(picked[0]) }
            if (!picked[0].isDir) {
                BatchItem(Modifier.weight(1f), ShijianIcons.Export, "导出", spec) { actions.export(picked[0]) }
            }
        }
        BatchItem(Modifier.weight(1f), ShijianIcons.Delete, "删除", spec, danger = true) { actions.askTrash() }
    }
}

@Composable
private fun BatchItem(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    spec: ThemeSpec,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val c = spec.colors
    val tint = if (danger) rememberDangerColor() else c.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = spec.typography.caption,
            color = if (danger) tint else c.onBackground,
        )
    }
}

/* ------------------------------------------------------------ 组件 */

/** 首页标题：跟随主题主色、比面包屑大一档、居中放在顶栏。 */
@Composable
private fun LibraryTitle(text: String, spec: ThemeSpec) {
    Text(
        text = text,
        style = spec.typography.title.copy(
            fontSize = spec.typography.title.fontSize * 1.3f,
            letterSpacing = 2.sp,
        ),
        color = spec.colors.primaryDeep,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 进入子目录后顶栏换成面包屑：每一段都能点着往上跳。 */
@Composable
private fun LibraryBreadcrumbs(
    vm: AppViewModel,
    spec: ThemeSpec,
    trail: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val c = spec.colors
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        trail.forEachIndexed { index, crumb ->
            val last = index == trail.lastIndex
            if (index > 0) {
                Text(
                    text = "›",
                    style = spec.typography.caption,
                    color = c.muted,
                    modifier = Modifier.padding(horizontal = 5.dp),
                )
            }
            Text(
                text = crumb.second,
                style = spec.typography.title,
                color = if (last) c.onBackground else c.muted,
                fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.clickable(enabled = !last) {
                    vm.browseLibrary(crumb.first, force = true)
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, spec: ThemeSpec) {
    Text(
        text = text,
        style = spec.typography.label,
        color = spec.colors.muted,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** 「继续阅读」卡片：文件名 + 读到哪儿 + 进度条。 */
@Composable
private fun ContinueCard(
    item: RecentDoc,
    spec: ThemeSpec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = spec.colors
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.outline, shape)
            .clickable(onClick = onClick)
            .padding(11.dp),
    ) {
        Text(
            text = item.title,
            style = spec.typography.small,
            color = c.onBackground,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (item.progress > 0.01f) {
                "读到 " + (item.progress * 100).roundToInt() + "%"
            } else {
                relativeTime(item.time)
            },
            style = spec.typography.caption,
            color = c.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)).background(c.primarySoft)) {
            Box(
                Modifier
                    .fillMaxWidth(item.progress.coerceIn(0.04f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.primary),
            )
        }
    }
}

/**
 * 列表里的一行。长按进入多选；多选时点击就是勾选，左侧多出一个勾选圈。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibRow(
    entry: LibEntry,
    spec: ThemeSpec,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val c = spec.colors
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) c.primaryWhisper else c.surface)
            .border(1.dp, if (selected) c.primary.copy(alpha = 0.45f) else c.outline, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 12.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            CheckDot(checked = selected, spec = spec)
            Spacer(Modifier.width(11.dp))
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (entry.isDir) c.primarySoft else c.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (entry.isDir) ShijianIcons.Folder else ShijianIcons.Doc,
                contentDescription = null,
                tint = if (entry.isDir) c.primaryDeep else c.muted,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = spec.typography.small,
                color = c.onBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitleOf(entry),
                style = spec.typography.caption,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (entry.isDir && !selectionMode) {
            Icon(
                imageVector = ShijianIcons.ChevronRight,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CheckDot(checked: Boolean, spec: ThemeSpec) {
    val c = spec.colors
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (checked) c.primary else Color.Transparent)
            .border(1.5.dp, if (checked) c.primary else c.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = ShijianIcons.Check,
                contentDescription = null,
                tint = c.onPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun EmptyCard(folderName: String, spec: ThemeSpec) {
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
            text = if (folderName == Library.INBOX) {
                "收件箱还是空的。从 QQ / 微信把 md 分享到拾简，会自动收进这里。"
            } else {
                "「" + folderName + "」里还没有笔记。右下角新建，或长按别的文件移过来。"
            },
            style = spec.typography.small,
            color = c.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeniedCard(spec: ThemeSpec, onRepick: () -> Unit) {
    val c = spec.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.primaryWhisper)
            .border(1.dp, c.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "笔记库授权已失效，重新选一次文件夹即可恢复（文件本身没被动过）",
            style = spec.typography.caption,
            color = c.primaryDeep,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRepick) {
            Text("重新选择", style = spec.typography.label, color = c.primaryDeep)
        }
    }
}

private fun relativeTime(millis: Long): String =
    if (millis <= 0L) "" else DateUtils.getRelativeTimeSpanString(millis).toString()

private fun sizeText(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes < 1024L -> bytes.toString() + " B"
    bytes < 1024L * 1024L -> (bytes / 1024L).toString() + " KB"
    else -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
}

private fun subtitleOf(entry: LibEntry): String = if (entry.isDir) {
    if (entry.count >= 0) entry.count.toString() + " 个文件" else relativeTime(entry.modified)
} else {
    listOf(relativeTime(entry.modified), sizeText(entry.size))
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}
