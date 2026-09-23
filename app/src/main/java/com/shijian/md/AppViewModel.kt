package com.shijian.md

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shijian.md.data.DocIo
import com.shijian.md.data.LibEntry
import com.shijian.md.data.Library
import com.shijian.md.data.LibraryStore
import com.shijian.md.data.RecentStore
import com.shijian.md.data.SettingsStore
import com.shijian.md.data.TrashItem
import com.shijian.md.data.TrashStore
import com.shijian.md.md.MdBlock
import com.shijian.md.md.MdDocument
import com.shijian.md.md.MdParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ViewMode { READ, EDIT, SPLIT }

@Stable
class OpenDoc(
    val uri: Uri?,
    val name: String,
    text: String,
    val untitled: Boolean = false,
) {
    var text by mutableStateOf(text)
    var saved by mutableStateOf(text)
    var readOnly by mutableStateOf(false)
    /** 当前编辑的是导入到应用私有目录的副本，原文件不受影响。 */
    var imported by mutableStateOf(false)
    var scrollIndex by mutableStateOf(0)
    var scrollOffset by mutableStateOf(0)
    var justSavedAt by mutableStateOf(0L)
    /** 库里的文档：直接读写真实文件，退出后回到它所在的目录。 */
    var libraryDoc by mutableStateOf(false)
    /** 库内路径文字（面包屑），只用于顶栏副标题。 */
    var folderPath by mutableStateOf<String?>(null)
    /** 阅读进度 0..100，关闭文档时写回最近列表。 */
    var progress by mutableStateOf(0)

    val dirty: Boolean get() = text != saved
    val title: String get() = name.substringBeforeLast('.').ifBlank { name }
}

/** open() 在 IO 线程里解析出来的结果。 */
private class Opened(
    val uri: Uri,
    val name: String,
    val text: String,
    val writable: Boolean,
    val imported: Boolean,
)

/** 等着入库的外部文件（从 QQ / 微信分享或「用其他方式打开」进来）。 */
class PendingImport(
    val uri: Uri,
    val name: String,
    val text: String,
    val edit: Boolean,
    /** true = 还没选过库目录，先引导用户建库。 */
    val needsLibrary: Boolean,
    /**
     * 明确指定落在哪个目录。应用内「从本机导入」时就是用户正打开着的那个文件夹；
     * 为 null 表示走默认的收件箱（QQ / 微信分享过来的，没有「当前文件夹」这个上下文）。
     */
    val targetDir: String? = null,
)

/** 移动时目标文件夹里已经有同名条目，等用户决定「都留着」还是「替换」。 */
class MoveConflict(val targetDirId: String, val names: List<String>)

/** 一次移动的结果：要么搬完了，要么碰上重名要先问一句。 */
private sealed class MoveOutcome {
    class Done(val moved: Int, val failed: Int) : MoveOutcome()
    class Clash(val names: List<String>) : MoveOutcome()
}

/** 刚搬进回收站的一条：docId 是搬完之后重新查到的。 */
private class Trashed(val docId: String, val name: String, val parent: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val settings = SettingsStore(app)
    val recents = RecentStore(app)
    val library = LibraryStore(app)

    /** 回收站的账本：文件本体在 `.trash/` 里，这里只记「从哪来、什么时候删的」。 */
    val trashStore = TrashStore(app)

    private var _doc by mutableStateOf<OpenDoc?>(null)
    val doc: OpenDoc? get() = _doc

    private var _parsed by mutableStateOf(MdDocument.Empty)
    val parsed: MdDocument get() = _parsed

    private var _mode by mutableStateOf(ViewMode.READ)
    val mode: ViewMode get() = _mode

    private var _toast by mutableStateOf<String?>(null)
    val toast: String? get() = _toast

    private var _busy by mutableStateOf(false)
    val busy: Boolean get() = _busy

    private var _draftLabel by mutableStateOf<String?>(null)
    val draftLabel: String? get() = _draftLabel

    private var _pendingImport by mutableStateOf<PendingImport?>(null)
    val pendingImport: PendingImport? get() = _pendingImport

    /** 当前浏览的库目录（null 表示还没开始浏览），以及它的内容。 */
    private var _libDirId by mutableStateOf<String?>(null)
    val libDirId: String? get() = _libDirId

    private var _libEntries by mutableStateOf<List<LibEntry>>(emptyList())
    val libEntries: List<LibEntry> get() = _libEntries

    private var _libLoading by mutableStateOf(false)
    val libLoading: Boolean get() = _libLoading

    /** 库的授权失效（系统清理、换机恢复等），需要用户重新选一次文件夹。 */
    private var _libDenied by mutableStateOf(false)
    val libDenied: Boolean get() = _libDenied

    /** 回收站页面。 */
    private var _inTrash by mutableStateOf(false)
    val inTrash: Boolean get() = _inTrash

    private var _trashItems by mutableStateOf<List<TrashItem>>(emptyList())
    val trashItems: List<TrashItem> get() = _trashItems

    private var _trashLoading by mutableStateOf(false)
    val trashLoading: Boolean get() = _trashLoading

    /** 库根下的一级文件夹：平板左侧栏的快捷入口。 */
    private var _rootFolders by mutableStateOf<List<LibEntry>>(emptyList())
    val rootFolders: List<LibEntry> get() = _rootFolders

    /** 移动弹层：待移动的条目 + 弹层里正在看的目录 + 该目录下的子文件夹。 */
    private var _moveSource by mutableStateOf<List<LibEntry>>(emptyList())
    val moveSource: List<LibEntry> get() = _moveSource

    private var _moveDirId by mutableStateOf<String?>(null)
    val moveDirId: String? get() = _moveDirId

    private var _moveEntries by mutableStateOf<List<LibEntry>>(emptyList())
    val moveEntries: List<LibEntry> get() = _moveEntries

    private var _moveLoading by mutableStateOf(false)
    val moveLoading: Boolean get() = _moveLoading

    private var _moveConflict by mutableStateOf<MoveConflict?>(null)
    val moveConflict: MoveConflict? get() = _moveConflict

    private var parseJob: Job? = null
    private var saveJob: Job? = null

    private val draftFile: File get() = File(getApplication<Application>().filesDir, "draft.md")
    private val draftMeta: File get() = File(getApplication<Application>().filesDir, "draft.name")

    init {
        draftFile.takeIf { it.isFile && it.length() > 0 }?.let {
            _draftLabel = draftMeta.takeIf { f -> f.isFile }?.readText()?.trim().orEmpty().ifBlank { "未命名.md" }
        }
        if (library.hasLibrary) browseLibrary()
        purgeTrashQuietly()
    }

    /* ------------------------------------------------------------ 笔记库 */

    /**
     * 选好（或更换）库目录后的收尾：记下授权、解析出逻辑库根、刷新列表；有文件正等着入库就直接导入。
     *
     * 用户是在「有个文件等着进来」的上下文里选的目录，所以这一次等于已经回答过
     * 「以后都导入到这里」，不再多问一遍。
     */
    fun attachLibrary(picked: Uri) {
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val prepared = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = app.contentResolver
                    val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    val write = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    val granted = runCatching { resolver.takePersistableUriPermission(picked, read or write) }.isSuccess ||
                        runCatching { resolver.takePersistableUriPermission(picked, read) }.isSuccess
                    if (!granted) error("没能拿到长期授权，请在系统选择器里重新选一次")
                    val pickedId = Library.treeRootId(picked)
                    if (Library.isBlockedPick(pickedId)) {
                        error("系统不允许把「下载」目录整个交给应用，请在 Documents 里新建一个文件夹")
                    }
                    // 目录树的 DISPLAY_NAME 不一定查得到（树的 uri 不是文档 uri），拿不到就用文档 id 反推
                    val pickedName = DocIo.displayName(app, picked)
                        .takeIf { it.isNotBlank() && !it.endsWith(":") }
                        ?: Library.nameOfDocId(pickedId)
                    val root = Library.resolveRoot(app, picked, pickedId, pickedName)
                    Triple(root.first, root.second, pickedName)
                }
            }
            _busy = false
            prepared.onSuccess { (rootId, rootName, pickedName) ->
                val hadPending = _pendingImport != null
                library.setLibrary(picked, rootId, rootName, confirmed = hadPending)
                _libDenied = false
                _toast = if (rootName != pickedName) {
                    "已在「$pickedName」里建好「$rootName」，以后的笔记都放这里"
                } else {
                    "笔记库已设为「$rootName」"
                }
                browseLibrary(rootId, force = true)
                _pendingImport?.let { pending ->
                    _pendingImport = null
                    doImport(pending)
                }
            }.onFailure { e ->
                _toast = e.message ?: "选择文件夹失败"
            }
        }
    }

    fun browseLibrary(dirDocId: String? = null, force: Boolean = false) {
        val tree = library.treeUri ?: return
        val root = library.rootDocId ?: return
        val target = dirDocId
            ?: library.lastDir?.takeIf { it == root || it.startsWith("$root/") }
            ?: root
        if (!force && _libDirId == target) return
        _libDirId = target
        library.lastDir = target
        _libLoading = true
        viewModelScope.launch {
            val app = getApplication<Application>()
            val loaded = runCatching { withContext(Dispatchers.IO) { Library.list(app, tree, target) } }
            if (_libDirId != target) return@launch
            _libLoading = false
            loaded.onSuccess { entries ->
                _libDenied = false
                _libEntries = entries
            }
            refreshRootFolders()
            loaded.onFailure { e ->
                _libEntries = emptyList()
                _libDenied = DocIo.isPermissionDenied(e)
                _toast = if (_libDenied) "笔记库授权已失效，请重新选择库文件夹" else "读取笔记库失败：${e.message}"
            }
            fillFolderCounts(target)
        }
    }

    /**
     * 文件夹行的角标。逐个目录查一次 child 列表，云端目录可能很慢，
     * 因此给一秒总预算 —— 拿不到就先只显示时间，别让列表卡在那里转圈。
     */
    private suspend fun fillFolderCounts(dirDocId: String) {
        val tree = library.treeUri ?: return
        val app = getApplication<Application>()
        val deadline = System.currentTimeMillis() + 1000
        for (folder in _libEntries.filter { it.isDir && it.count < 0 }) {
            if (System.currentTimeMillis() > deadline) return
            val count = runCatching {
                withContext(Dispatchers.IO) { Library.countOf(app, tree, folder.docId) }
            }.getOrDefault(-1)
            if (count < 0 || _libDirId != dirDocId) continue
            _libEntries = _libEntries.map { if (it.docId == folder.docId) it.copy(count = count) else it }
        }
    }

    fun libEnter(entry: LibEntry) {
        if (entry.isDir) browseLibrary(entry.docId, force = true) else openLibraryDoc(entry.uri, edit = false)
    }

    fun libUp() {
        val root = library.rootDocId ?: return
        val current = _libDirId ?: return
        if (current == root) return
        browseLibrary(Library.parentOf(current) ?: root, force = true)
    }

    fun atLibraryRoot(): Boolean = _libDirId == null || _libDirId == library.rootDocId

    /** 到当前目录为止的面包屑（含库根）。 */
    fun libTrail(): List<Pair<String, String>> {
        val root = library.rootDocId ?: return emptyList()
        return Library.trail(root, _libDirId ?: root)
    }

    /** 库里最近读过的几篇，用于首页「继续阅读」。 */
    fun continueReading(limit: Int = 3): List<com.shijian.md.data.RecentDoc> {
        val tree = library.treeUri ?: return emptyList()
        return recents.items
            .filter { runCatching { Library.inTree(tree, Uri.parse(it.uri)) }.getOrDefault(false) }
            .take(limit)
    }

    fun openLibraryDoc(uri: Uri, edit: Boolean, message: String? = null) {
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val loaded = runCatching {
                withContext(Dispatchers.IO) {
                    val name = DocIo.displayName(app, uri)
                    name to DocIo.read(app, uri)
                }
            }
            _busy = false
            loaded.onSuccess { (name, text) ->
                val d = OpenDoc(uri, name, text)
                d.libraryDoc = true
                d.folderPath = folderLabelOf(uri)
                _doc = d
                _mode = if (edit) ViewMode.EDIT else ViewMode.READ
                recents.touch(uri.toString(), name, previewOf(text))
                reparse(text)
                if (message != null) _toast = message
                val parent = Library.parentOf(Library.docIdOf(uri))
                if (parent != null) {
                    _libDirId = parent
                    library.lastDir = parent
                }
            }.onFailure { e ->
                _toast = if (DocIo.isPermissionDenied(e)) {
                    "打开失败：没有访问权限，可能需要在设置里重新选择库文件夹"
                } else {
                    "打开失败：${e.message ?: "未知错误"}"
                }
            }
        }
    }

    private fun folderLabelOf(uri: Uri): String? {
        val root = library.rootDocId ?: return null
        val dir = Library.parentOf(Library.docIdOf(uri)) ?: return null
        return Library.trail(root, dir).joinToString(" / ") { it.second }
    }

    /**
     * 外部打开：QQ / 微信的「用其他方式打开」、系统分享、应用内选择文件都走这里。
     *
     * 已经在库里的文件直接打开；不在库里的先复制一份进「收件箱」——分享来源基本只给读授权，
     * 不复制一份，用户永远改不动那份文件。
     *
     * [targetDir] 是应用内「从本机导入」专用的落点：用户已经站在某个文件夹里点进来了，
     * 那份文件就该落在这一层，不必绕回收件箱、也不用再问一次。
     */
    fun openExternal(
        uri: Uri,
        edit: Boolean = false,
        sourceFlags: Int? = null,
        targetDir: String? = null,
    ) {
        val tree = library.treeUri
        if (tree != null && Library.inTree(tree, uri)) {
            openLibraryDoc(uri, edit)
            return
        }
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val loaded = runCatching {
                withContext(Dispatchers.IO) {
                    DocIo.takePersistableGrant(app, uri, sourceFlags)
                    val name = DocIo.displayName(app, uri)
                    name to DocIo.read(app, uri)
                }
            }
            _busy = false
            loaded.onSuccess { (name, text) ->
                when {
                    targetDir != null ->
                        doImport(PendingImport(uri, name, text, edit, needsLibrary = false, targetDir = targetDir))
                    !library.hasLibrary -> _pendingImport = PendingImport(uri, name, text, edit, needsLibrary = true)
                    library.importConfirmed -> doImport(PendingImport(uri, name, text, edit, needsLibrary = false))
                    else -> _pendingImport = PendingImport(uri, name, text, edit, needsLibrary = false)
                }
            }.onFailure { e ->
                _toast = if (DocIo.isPermissionDenied(e)) {
                    "没有访问该文件的权限。它可能来自其他应用的临时分享，请重新从来源应用打开"
                } else {
                    "打开失败：${e.message ?: "未知错误"}"
                }
            }
        }
    }

    /** 用户点了「导入并打开」（或此前已确认过）时执行：复制进库并打开。 */
    fun confirmImport(remember: Boolean) {
        val pending = _pendingImport ?: return
        _pendingImport = null
        if (remember) library.importConfirmed = true
        doImport(pending)
    }

    fun dismissImport() {
        val pending = _pendingImport ?: return
        _pendingImport = null
        _toast = "已取消导入，「" + pending.name + "」没有收进库里"
    }

    private fun doImport(pending: PendingImport) {
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val tree = library.treeUri
            val root = library.rootDocId
            val imported = if (tree == null || root == null) {
                Result.failure(IllegalStateException("还没有选好笔记库"))
            } else {
                runCatching {
                    withContext(Dispatchers.IO) {
                        // 应用内「从本机导入」时落点已经定好；只有外部来源才绕回收件箱
                        val dir = pending.targetDir?.takeIf { Library.isUnderDocId(it, root) }
                            ?: Library.ensureChild(app, tree, root, Library.INBOX)
                        Library.importText(app, tree, dir, pending.name, pending.text)
                    }
                }
            }
            _busy = false
            imported.onSuccess { uri ->
                val where = pending.targetDir?.let { Library.nameOfDocId(it) } ?: Library.INBOX
                openLibraryDoc(uri, pending.edit, "已导入到「" + where + "」")
            }.onFailure { e ->
                _toast = "导入到笔记库失败：${e.message ?: "未知错误"}，先按原样打开"
                open(pending.uri, edit = pending.edit)
            }
        }
    }

    /** 在当前目录新建文档并直接进入编辑。 */
    fun libCreateDoc(rawName: String) {
        val tree = library.treeUri ?: return
        val dir = _libDirId ?: library.rootDocId ?: return
        val name = rawName.trim().ifBlank { "未命名.md" }
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val created = runCatching {
                withContext(Dispatchers.IO) { Library.importText(app, tree, dir, name, SAMPLE_TEMPLATE) }
            }
            _busy = false
            created.onSuccess { uri ->
                browseLibrary(dir, force = true)
                openLibraryDoc(uri, edit = true)
            }.onFailure { e ->
                _toast = "新建失败：${e.message ?: "未知错误"}"
            }
        }
    }

    /* ------------------------------------------------------ 整理（分类归档） */

    /** 在当前目录新建文件夹 —— 自己拉分类，不必回到电脑上建。 */
    fun libCreateFolder(rawName: String) {
        val tree = library.treeUri ?: return
        val dir = _libDirId ?: library.rootDocId ?: return
        val name = Library.safeName(rawName.trim().ifBlank { "新建文件夹" })
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val created = runCatching {
                withContext(Dispatchers.IO) {
                    if (Library.findChild(app, tree, dir, name) != null) {
                        error("这里已经有一个叫「" + name + "」的条目了")
                    }
                    Library.createFolder(app, tree, dir, name) ?: error("这个文件夹不允许新建子文件夹")
                }
            }
            _busy = false
            created.onSuccess {
                browseLibrary(dir, force = true)
                refreshRootFolders()
                _toast = "已新建文件夹「" + name + "」"
            }.onFailure { e -> _toast = e.message ?: "新建文件夹失败" }
        }
    }

    /** 重命名。文件忘了写 `.md` 会自动补上，否则它就从列表里消失了。 */
    fun libRename(entry: LibEntry, rawName: String) {
        val tree = library.treeUri ?: return
        val dir = Library.parentOf(entry.docId) ?: return
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return
        val name = if (entry.isDir) Library.safeName(trimmed) else Library.importName(trimmed)
        if (name == entry.name) return
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val done = runCatching {
                withContext(Dispatchers.IO) {
                    val clash = Library.findChild(app, tree, dir, name)
                    if (clash != null && clash.docId != entry.docId) error("这里已经有一个叫「" + name + "」的条目了")
                    Library.rename(app, entry.uri, name) ?: error("重命名失败，可能是这个目录不允许改名")
                }
            }
            _busy = false
            done.onSuccess {
                browseLibrary(_libDirId, force = true)
                refreshRootFolders()
                _toast = "已改名为「" + name + "」"
            }.onFailure { e -> _toast = e.message ?: "重命名失败" }
        }
    }

    /** 导出到…：把库里的这一份另写一份到别处（库外的目录、网盘都行）。 */
    fun libExportTo(entry: LibEntry, target: Uri) {
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val done = runCatching {
                withContext(Dispatchers.IO) {
                    val text = DocIo.read(app, entry.uri)
                    DocIo.write(app, target, text)
                }
            }
            _busy = false
            done.onSuccess {
                val to = DocIo.displayName(app, target).ifBlank { "所选位置" }
                _toast = "已导出到「" + to + "」"
            }.onFailure { e -> _toast = "导出失败：" + (e.message ?: "未知错误") }
        }
    }

    /* ---- 移动到别的文件夹 ---- */

    fun openMove(entries: List<LibEntry>) {
        if (entries.isEmpty()) return
        _moveSource = entries
        _moveDirId = _libDirId ?: library.rootDocId
        loadMoveDir()
    }

    fun moveClose() {
        _moveSource = emptyList()
        _moveEntries = emptyList()
        _moveDirId = null
        _moveConflict = null
    }

    fun moveInto(dirDocId: String) {
        _moveDirId = dirDocId
        loadMoveDir()
    }

    fun moveCanGoUp(): Boolean {
        val root = library.rootDocId ?: return false
        return (_moveDirId ?: return false) != root
    }

    fun moveUp() {
        val root = library.rootDocId ?: return
        val current = _moveDirId ?: return
        if (current == root) return
        _moveDirId = Library.parentOf(current) ?: root
        loadMoveDir()
    }

    /** 弹层当前所在目录的可读路径。 */
    fun moveLabel(): String {
        val root = library.rootDocId ?: return ""
        val trail = Library.trail(root, _moveDirId ?: root)
        return if (trail.size == 1) trail[0].second else trail.joinToString(" / ") { it.second }
    }

    fun moveDirName(): String = Library.nameOfDocId(_moveDirId ?: library.rootDocId.orEmpty())

    /** 目标就是这些文件现在待的地方 —— 那就没什么好搬的。 */
    fun moveIsNoop(): Boolean = _moveDirId != null && _moveDirId == _libDirId

    private fun loadMoveDir() {
        val tree = library.treeUri ?: return
        val dir = _moveDirId ?: return
        _moveLoading = true
        viewModelScope.launch {
            val app = getApplication<Application>()
            val loaded = runCatching {
                withContext(Dispatchers.IO) { Library.list(app, tree, dir).filter { it.isDir } }
            }
            _moveLoading = false
            if (_moveDirId != dir) return@launch
            loaded.onSuccess { _moveEntries = it }.onFailure { _moveEntries = emptyList() }
        }
    }

    fun moveNewFolder(rawName: String) {
        val tree = library.treeUri ?: return
        val dir = _moveDirId ?: return
        val name = Library.safeName(rawName.trim().ifBlank { "新建文件夹" })
        viewModelScope.launch {
            _moveLoading = true
            val app = getApplication<Application>()
            val created = runCatching {
                withContext(Dispatchers.IO) {
                    Library.createFolder(app, tree, dir, name) ?: error("这个文件夹不允许新建子文件夹")
                }
            }
            _moveLoading = false
            created.onSuccess {
                _toast = "已新建文件夹「" + name + "」"
                loadMoveDir()
                refreshRootFolders()
            }.onFailure { e -> _toast = e.message ?: "新建文件夹失败" }
        }
    }

    /**
     * 把待移动的条目搬到弹层当前目录。
     *
     * [replace] 传 null 表示「先探一下有没有重名」，有就回界面上问一句；true = 覆盖目标里的同名文件，
     * false = 两边都留着（自动加序号）。
     */
    fun moveConfirm(replace: Boolean? = null) {
        val tree = library.treeUri ?: return
        val target = _moveDirId ?: return
        val root = library.rootDocId ?: return
        val sources = _moveSource
        if (sources.isEmpty()) return
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val outcome = runCatching {
                withContext(Dispatchers.IO) { relocate(app, tree, sources, target, replace, root) }
            }
            _busy = false
            outcome.onSuccess { result ->
                when (result) {
                    is MoveOutcome.Clash -> _moveConflict = MoveConflict(target, result.names)
                    is MoveOutcome.Done -> {
                        moveClose()
                        browseLibrary(_libDirId, force = true)
                        refreshRootFolders()
                        _toast = when {
                            result.failed == 0 ->
                                "已移动 " + result.moved + " 项到「" + Library.nameOfDocId(target) + "」"
                            result.moved == 0 -> "移动失败：目标文件夹不可写"
                            else -> "移动完成：" + result.moved + " 项成功，" + result.failed + " 项失败"
                        }
                    }
                }
            }.onFailure { e -> _toast = e.message ?: "移动失败" }
        }
    }

    fun moveResolveConflict(keepBoth: Boolean) {
        _moveConflict = null
        moveConfirm(replace = !keepBoth)
    }

    fun dismissMoveConflict() {
        _moveConflict = null
    }

    /** 逐个搬家（IO 线程）。文件夹搬进自己或自己的子目录会被挡住。 */
    private fun relocate(
        app: Application,
        tree: Uri,
        sources: List<LibEntry>,
        target: String,
        replace: Boolean?,
        root: String,
    ): MoveOutcome {
        if (sources.any { it.isDir && Library.isUnderDocId(target, it.docId) }) {
            error("不能把文件夹移动进它自己里面")
        }
        val existing = Library.list(app, tree, target).filterNot { e -> sources.any { it.docId == e.docId } }
        if (replace == null) {
            val clash = sources.filter { s -> existing.any { it.name.equals(s.name, ignoreCase = true) } }
            if (clash.isNotEmpty()) return MoveOutcome.Clash(clash.map { it.name }.take(3))
        }
        var moved = 0
        var failed = 0
        sources.forEach { entry ->
            val ok = runCatching {
                if (Library.parentOf(entry.docId) == target) return@runCatching true
                val hit = existing.firstOrNull { it.name.equals(entry.name, ignoreCase = true) }
                var current = entry
                if (hit != null) {
                    if (replace == true) {
                        retire(app, tree, root, hit)
                    } else {
                        val free = Library.uniqueName(app, tree, target, entry.name)
                        current = Library.rename(app, entry.uri, free)
                            ?.let { entry.copy(uri = it, docId = Library.docIdOf(it), name = free) }
                            ?: entry
                    }
                }
                Library.moveEntry(app, tree, current, target)
            }.getOrDefault(false)
            if (ok) moved++ else failed++
        }
        return MoveOutcome.Done(moved, failed)
    }

    /**
     * 被「替换」顶掉的那一份先送进回收站，真删是最后手段。
     *
     * 用户点的是「替换」，但没人希望替换等于对面那份当场蒸发 —— 放回收站里 7 天，后悔还来得及。
     */
    private fun retire(app: Application, tree: Uri, root: String, hit: LibEntry) {
        val bin = runCatching { Library.ensureChild(app, tree, root, Library.TRASH) }.getOrNull() ?: return
        if (Library.moveEntry(app, tree, hit, bin)) return
        Library.delete(app, hit.uri)
    }

    /* ---- 回收站：删除先放这儿，7 天后才真删 ---- */

    fun libTrash(entries: List<LibEntry>) {
        if (entries.isEmpty()) return
        val tree = library.treeUri ?: return
        val root = library.rootDocId ?: return
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val done = runCatching {
                withContext(Dispatchers.IO) {
                    val bin = Library.ensureChild(app, tree, root, Library.TRASH)
                    val moved = ArrayList<Trashed>()
                    var failed = 0
                    entries.forEach { entry ->
                        val record = trashOne(app, tree, bin, entry)
                        if (record == null) failed++ else moved += record
                    }
                    moved to failed
                }
            }
            _busy = false
            done.onSuccess { pair ->
                val moved = pair.first
                moved.forEach { trashStore.mark(it.docId, it.name, it.parent) }
                browseLibrary(_libDirId, force = true)
                _toast = when {
                    moved.isEmpty() -> "删除失败：这几个条目搬不进回收站"
                    pair.second == 0 -> "已移入回收站，" + TrashStore.KEEP_DAYS + " 天后自动清理"
                    else -> "已移入回收站 " + moved.size + " 项，" + pair.second + " 项没搬动"
                }
            }.onFailure { e -> _toast = "删除失败：" + (e.message ?: "未知错误") }
        }
    }

    /**
     * 把一个条目搬进回收站（IO 线程）。
     *
     * 记录里存的是**搬完之后**重新查到的 docId：内部存储这类 provider 的文档 id 就是路径，
     * 搬一次就变了，拿旧的记下来将来会对不上。
     */
    private fun trashOne(app: Application, tree: Uri, bin: String, entry: LibEntry): Trashed? = runCatching {
        val parent = Library.parentOf(entry.docId) ?: return null
        if (parent == bin) return null
        val free = Library.uniqueName(app, tree, bin, entry.name)
        val current = if (free != entry.name) {
            Library.rename(app, entry.uri, free)
                ?.let { entry.copy(uri = it, docId = Library.docIdOf(it), name = free) }
                ?: entry
        } else {
            entry
        }
        if (!Library.moveEntry(app, tree, current, bin)) return null
        val listed = Library.findChild(app, tree, bin, current.name, includeHidden = true)
        Trashed(listed?.docId ?: current.docId, entry.name, parent)
    }.getOrNull()

    fun openTrash() {
        val tree = library.treeUri ?: return
        val root = library.rootDocId ?: return
        _inTrash = true
        _trashLoading = true
        val expired = trashStore.expired().map { it.docId }.toSet()
        viewModelScope.launch {
            val app = getApplication<Application>()
            val loaded = runCatching {
                withContext(Dispatchers.IO) {
                    val bin = Library.ensureChild(app, tree, root, Library.TRASH)
                    if (expired.isNotEmpty()) {
                        Library.list(app, tree, bin, includeHidden = true)
                            .filter { it.docId in expired }
                            .forEach { Library.delete(app, it.uri) }
                    }
                    Library.list(app, tree, bin, includeHidden = true)
                }
            }
            _trashLoading = false
            loaded.onSuccess { children ->
                trashStore.retainOnly(children.map { it.docId })
                _trashItems = children.map { child ->
                    val record = trashStore.ensure(child.docId, child.name, root)
                    TrashItem(
                        uri = child.uri,
                        docId = child.docId,
                        name = record.name,
                        originalParent = record.originalParent,
                        deletedAt = record.deletedAt,
                        isDir = child.isDir,
                    )
                }
            }.onFailure { e ->
                _trashItems = emptyList()
                _toast = if (DocIo.isPermissionDenied(e)) {
                    "没有访问回收站的权限，请到设置里重新选一次库文件夹"
                } else {
                    "读取回收站失败：" + (e.message ?: "未知错误")
                }
            }
        }
    }

    fun closeTrash() {
        _inTrash = false
    }

    fun trashRestore(items: List<TrashItem>) {
        if (items.isEmpty()) return
        val tree = library.treeUri ?: return
        val root = library.rootDocId ?: return
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val done = runCatching {
                withContext(Dispatchers.IO) {
                    var ok = 0
                    var failed = 0
                    items.forEach { item ->
                        val back = item.originalParent.takeIf { it.isNotBlank() && it != root }
                        // 原位置可能已经被删掉或改名了，搬不回去就放到库根
                        val placed = (back != null && moveBack(app, tree, item, back)) ||
                            moveBack(app, tree, item, root)
                        if (placed) ok++ else failed++
                    }
                    ok to failed
                }
            }
            _busy = false
            done.onSuccess { pair ->
                _toast = when {
                    pair.first == 0 -> "还原失败：这些文件搬不动"
                    pair.second == 0 -> "已还原 " + pair.first + " 项"
                    else -> "已还原 " + pair.first + " 项，" + pair.second + " 项失败"
                }
                openTrash()
            }.onFailure { e -> _toast = "还原失败：" + (e.message ?: "未知错误") }
        }
    }

    /** 把回收站里的条目搬回 [target]（IO 线程）；重名就先改成不冲突的名字。 */
    private fun moveBack(app: Application, tree: Uri, item: TrashItem, target: String): Boolean = runCatching {
        val entry = LibEntry(item.uri, item.docId, item.name, item.isDir, 0L, 0L)
        val free = Library.uniqueName(app, tree, target, item.name)
        val current = if (free != item.name) {
            Library.rename(app, item.uri, free)
                ?.let { entry.copy(uri = it, docId = Library.docIdOf(it), name = free) }
                ?: entry
        } else {
            entry
        }
        Library.moveEntry(app, tree, current, target)
    }.getOrDefault(false)

    /** 彻底删除：不进回收站，删了就没了。 */
    fun trashDelete(items: List<TrashItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val gone = runCatching {
                withContext(Dispatchers.IO) {
                    items.mapNotNull { item -> if (Library.delete(app, item.uri)) item.docId else null }
                }
            }.getOrDefault(emptyList())
            _busy = false
            gone.forEach { trashStore.forget(it) }
            _trashItems = _trashItems.filterNot { it.docId in gone }
            _toast = if (gone.isEmpty()) "删除失败：这些文件可能已经不在回收站里了" else "已彻底删除 " + gone.size + " 项"
        }
    }

    fun trashEmpty() {
        val items = _trashItems
        if (items.isEmpty()) return
        viewModelScope.launch {
            _busy = true
            val app = getApplication<Application>()
            val ok = runCatching {
                withContext(Dispatchers.IO) { items.count { Library.delete(app, it.uri) } }
            }.getOrDefault(0)
            _busy = false
            trashStore.clear()
            _trashItems = emptyList()
            _toast = "回收站已清空（" + ok + " 项）"
        }
    }

    /** 回收站条目「原来在哪」。 */
    fun trashLocation(item: TrashItem): String {
        val root = library.rootDocId ?: return ""
        val parent = item.originalParent.takeIf { it.isNotBlank() } ?: root
        val trail = Library.trail(root, parent)
        return if (trail.size == 1) trail[0].second else trail.joinToString(" / ") { it.second }
    }

    /** 还有几天被自动清掉。 */
    fun trashRemainDays(item: TrashItem): Long {
        val left = TrashStore.KEEP_MILLIS - (System.currentTimeMillis() - item.deletedAt)
        return if (left <= 0L) 0L else left / (24L * 60L * 60L * 1000L) + 1L
    }

    /** 启动、换库、进回收站时顺手清掉过了 7 天的条目。 */
    private fun purgeTrashQuietly() {
        val tree = library.treeUri ?: return
        val root = library.rootDocId ?: return
        val expired = trashStore.expired().map { it.docId }.toSet()
        if (expired.isEmpty()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val fresh = runCatching {
                withContext(Dispatchers.IO) {
                    val bin = Library.ensureChild(app, tree, root, Library.TRASH)
                    Library.list(app, tree, bin, includeHidden = true)
                        .filter { it.docId in expired }
                        .forEach { Library.delete(app, it.uri) }
                    Library.list(app, tree, bin, includeHidden = true).map { it.docId }
                }
            }.getOrNull() ?: return@launch
            trashStore.retainOnly(fresh)
        }
    }

    /** 平板左侧栏要列库根下的一级文件夹，顺手刷一下。 */
    private fun refreshRootFolders() {
        val tree = library.treeUri ?: return
        val root = library.rootDocId ?: return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val folders = runCatching {
                withContext(Dispatchers.IO) { Library.list(app, tree, root).filter { it.isDir } }
            }.getOrNull() ?: return@launch
            _rootFolders = folders
        }
    }

    /**
     * 打开外部文档。[sourceFlags] 是来源 Intent 的 flags，用来判断对方到底给了读还是写授权；
     * 应用内选文件（ACTION_OPEN_DOCUMENT 只回一个 Uri）传 null。
     *
     * 源文件写不回去时（QQ / 微信等的分享只给读授权），直接把内容导入私有目录副本再打开，
     * 用户才能正常编辑保存；原文件不动。
     */
    fun open(uri: Uri, edit: Boolean = false, sourceFlags: Int? = null) {
        viewModelScope.launch {
            _busy = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val ctx = getApplication<Application>()
                    DocIo.takePersistableGrant(ctx, uri, sourceFlags)
                    val name = DocIo.displayName(ctx, uri)
                    val text = DocIo.read(ctx, uri)
                    val grantedWrite = sourceFlags != null &&
                        sourceFlags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0
                    when {
                        DocIo.canWrite(ctx, uri, grantedWrite) ->
                            Opened(uri, name, text, writable = true, imported = false)
                        // SAF 文档只能读时，复制一份到私有目录继续编辑
                        uri.scheme == "content" -> {
                            val copy = DocIo.importCopy(ctx, uri, name, text)
                            Opened(Uri.fromFile(copy), name, text, writable = true, imported = true)
                        }
                        else -> Opened(uri, name, text, writable = false, imported = false)
                    }
                }
            }.onSuccess { opened ->
                val d = OpenDoc(opened.uri, opened.name, opened.text)
                d.readOnly = !opened.writable
                d.imported = opened.imported
                _doc = d
                _mode = if (edit && !d.readOnly) ViewMode.EDIT else ViewMode.READ
                recents.touch(d.uri.toString(), opened.name, previewOf(opened.text))
                reparse(opened.text)
                when {
                    d.imported ->
                        _toast = "分享来源不可写回，已导入副本继续编辑（原文件不受影响）"
                    d.readOnly ->
                        _toast = "该文件只有读取权限，要修改请用「另存为」导出副本"
                }
            }.onFailure { e ->
                _toast = if (DocIo.isPermissionDenied(e)) {
                    "没有访问该文件的权限。它可能来自其他应用的临时分享，授权已失效，请重新从来源应用打开"
                } else {
                    "打开失败：${e.message ?: "未知错误"}"
                }
            }
            _busy = false
        }
    }

    fun openText(name: String, text: String) {
        val d = OpenDoc(null, name, text, untitled = true)
        _doc = d
        _mode = ViewMode.READ
        reparse(text)
    }

    fun openDraft() {
        val f = draftFile
        if (!f.isFile) return
        runCatching { f.readText() }.onSuccess { text ->
            val name = draftMeta.takeIf { it.isFile }?.readText()?.trim().orEmpty().ifBlank { "未命名.md" }
            val d = OpenDoc(null, name, text, untitled = true)
            _doc = d
            reparse(text)
        }
    }

    fun newDoc() {
        // 有了笔记库就建真文件，别再落成游离在库外的草稿
        if (library.hasLibrary) {
            libCreateDoc("未命名.md")
            return
        }
        val d = OpenDoc(null, "未命名.md", SAMPLE_TEMPLATE, untitled = true)
        _doc = d
        _mode = ViewMode.EDIT
        reparse(d.text)
    }

    fun updateText(value: String) {
        val d = _doc ?: return
        if (d.readOnly) return
        d.text = value
        scheduleParse(value)
        if (settings.autosave) scheduleSave()
    }

    fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1200)
            persist(showToast = false)
        }
    }

    fun saveNow() {
        saveJob?.cancel()
        viewModelScope.launch { persist(showToast = true) }
    }

    /** 另存为：把当前内容写到新位置，并切换过去 */
    fun saveAs(uri: Uri) {
        val d = _doc ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    runCatching {
                        getApplication<Application>().contentResolver
                            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val name = DocIo.displayName(getApplication(), uri)
                    DocIo.write(getApplication(), uri, d.text)
                    name
                }
            }.onSuccess { name ->
                val next = OpenDoc(uri, name, d.text)
                _doc = next
                recents.touch(uri.toString(), name, previewOf(d.text))
                _toast = "已保存到 $name"
            }.onFailure { _toast = "保存失败：${it.message ?: "未知错误"}" }
        }
    }

    fun reload() {
        val d = _doc ?: return
        val uri = d.uri ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { DocIo.read(getApplication(), uri) } }
                .onSuccess {
                    d.text = it
                    d.saved = it
                    reparse(it)
                    _toast = "已重新载入"
                }
                .onFailure { _toast = "载入失败" }
        }
    }

    fun closeDoc() {
        saveJob?.cancel()
        val d = _doc
        if (d != null && d.dirty && settings.autosave) {
            viewModelScope.launch { persist(showToast = false) }
        }
        if (d != null) {
            d.uri?.toString()?.let { recents.setProgress(it, d.progress / 100f) }
            // 库里改过/新建过的文件，回到列表时顺手刷一遍
            if (d.libraryDoc) browseLibrary(_libDirId, force = true)
        }
        _doc = null
        _parsed = MdDocument.Empty
        _mode = ViewMode.READ
    }

    /* ------------------------------------------------------------ 编辑辅助 */

    fun toggleTask(block: MdBlock.Task, checked: Boolean) {
        val d = _doc ?: return
        if (!settings.taskWriteBack) {
            _toast = "已在设置中关闭「勾选回写源文件」"
            return
        }
        if (d.readOnly) {
            _toast = "该文档为只读，无法回写"
            return
        }
        val i = block.markerIndex
        if (i < 0 || i + 1 >= d.text.length) return
        val chars = d.text.toCharArray()
        if (chars[i] != '[') return
        chars[i + 1] = if (checked) 'x' else ' '
        d.text = String(chars)
        reparse(d.text)
        saveJob?.cancel()
        viewModelScope.launch { persist(showToast = false) }
    }

    fun setMode(mode: ViewMode) {
        _mode = mode
        if (mode != ViewMode.READ) reparse(_doc?.text.orEmpty())
    }

    /* ------------------------------------------------------------ 内部 */

    private fun previewOf(text: String): String =
        text.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("---") }?.take(60).orEmpty()

    private fun scheduleParse(text: String) {
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            delay(200)
            reparse(text)
        }
    }

    private fun reparse(text: String) {
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _parsed = withContext(Dispatchers.Default) { MdParser.parse(text) }
        }
    }

    private suspend fun persist(showToast: Boolean) {
        val d = _doc ?: return
        if (d.readOnly) {
            if (showToast) _toast = "该文档为只读，请用「另存为」保存副本"
            return
        }
        val snapshot = d.text
        val app = getApplication<Application>()
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val uri = d.uri
                if (uri == null) {
                    saveDraft(d.name, snapshot)
                } else {
                    DocIo.write(app, uri, snapshot)
                    clearDraft()
                }
            }
        }
        if (result.isSuccess) {
            d.saved = snapshot
            d.justSavedAt = System.currentTimeMillis()
            if (showToast) _toast = if (d.uri == null) "已保存为草稿" else "已保存"
            return
        }
        val e = result.exceptionOrNull()
        // 写不回原文件时，至少把内容留在草稿里，别让改动丢掉
        val kept = d.uri != null &&
            runCatching { withContext(Dispatchers.IO) { saveDraft(d.name, snapshot) } }.isSuccess
        val message = when {
            DocIo.isPermissionDenied(e) && kept ->
                "没有写入权限，改动已存入草稿；可继续编辑后用「另存为」导出"
            DocIo.isPermissionDenied(e) -> "没有写入权限，无法写回该文件"
            showToast -> "保存失败：${e?.message ?: "未知错误"}"
            else -> null
        }
        // 自动保存失败时不要把屏幕上可能正在显示的提示清掉
        if (message != null) _toast = message
    }

    private fun saveDraft(name: String, text: String) {
        draftFile.writeText(text)
        draftMeta.writeText(name)
        _draftLabel = name
    }

    private fun clearDraft() {
        draftFile.delete()
        draftMeta.delete()
        _draftLabel = null
    }

    /**
     * 最近列表里哪些 content:// 条目的授权已经不在手里了（file:// 不受影响）。
     *
     * 库里的文件是「目录树的后代」，授权挂在树上，所以判断要走 [DocIo.hasPersistedGrant]。
     */
    fun recentNeedsRegrant(items: List<com.shijian.md.data.RecentDoc>): Set<String> = runCatching {
        val ctx = getApplication<Application>()
        items.asSequence()
            .map { it.uri }
            .filterTo(HashSet()) { it.startsWith("content://") && !DocIo.hasPersistedGrant(ctx, Uri.parse(it)) }
    }.getOrDefault(emptySet())

    fun toast(message: String) {
        _toast = message
    }

    fun clearToast() {
        _toast = null
    }

    fun discardDraft() {
        draftFile.delete()
        draftMeta.delete()
        _draftLabel = null
    }

    override fun onCleared() {
        parseJob?.cancel()
        saveJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val SAMPLE_TEMPLATE = """# 未命名文档

在这里开始写作。左上角可以切换 **阅读 / 编辑 / 分栏**。

- [ ] 这是一条任务，阅读模式下可以直接勾选
- [x] 勾选会写回源文件

> 支持引用、表格、代码块与行内代码 `code`。
"""
    }
}
