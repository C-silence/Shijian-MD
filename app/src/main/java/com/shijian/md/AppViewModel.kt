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
import com.shijian.md.data.RecentStore
import com.shijian.md.data.SettingsStore
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

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val settings = SettingsStore(app)
    val recents = RecentStore(app)

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

    private var parseJob: Job? = null
    private var saveJob: Job? = null

    private val draftFile: File get() = File(getApplication<Application>().filesDir, "draft.md")
    private val draftMeta: File get() = File(getApplication<Application>().filesDir, "draft.name")

    init {
        draftFile.takeIf { it.isFile && it.length() > 0 }?.let {
            _draftLabel = draftMeta.takeIf { f -> f.isFile }?.readText()?.trim().orEmpty().ifBlank { "未命名.md" }
        }
    }

    /* ------------------------------------------------------------ 打开与保存 */

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

    /** 最近列表里哪些 content:// 条目的授权已经不在手里了（file:// 不受影响）。 */
    fun recentNeedsRegrant(items: List<com.shijian.md.data.RecentDoc>): Set<String> = runCatching {
        val ctx = getApplication<Application>()
        val persisted = ctx.contentResolver.persistedUriPermissions.mapTo(HashSet()) { it.uri.toString() }
        items.asSequence()
            .map { it.uri }
            .filterTo(HashSet()) { it.startsWith("content://") && it !in persisted }
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
