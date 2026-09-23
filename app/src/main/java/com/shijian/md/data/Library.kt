package com.shijian.md.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.Immutable

/** 库里的一个条目：文件夹或文档。字段全部来自同一次 cursor 查询。 */
@Immutable
data class LibEntry(
    val uri: Uri,
    val docId: String,
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val modified: Long,
    /** 文件夹内的条目数；-1 表示还没统计出来。 */
    val count: Int = -1,
)

/**
 * 笔记库 = 用户授权的一棵 SAF 目录树（默认落在内部存储 `Documents/拾简`）。
 *
 * 所有操作都直接作用在真实文件上，App 不留副本；目录读取用
 * [DocumentsContract.buildChildDocumentsUriUsingTree] + 一次 cursor 查询，不用
 * `DocumentFile.listFiles()` —— 后者每个属性都要跨进程问一次，几十个文件就能卡出好几秒。
 *
 * 这里每个函数都会和 DocumentsProvider 打交道，**必须在 IO 线程调用**。
 */
object Library {

    const val INBOX = "收件箱"
    const val TRASH = ".trash"
    const val DEFAULT_ROOT_NAME = "拾简"

    private const val MD_MIME = "text/markdown"
    val DIR_MIME: String = DocumentsContract.Document.MIME_TYPE_DIR

    private val MARKDOWN_SUFFIXES = listOf(".md", ".markdown", ".txt")
    private val INVALID_NAME_CHARS = "\\/:*?\"<>|".toSet()

    private val PROJECTION = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    fun isTree(uri: Uri?): Boolean =
        uri != null && runCatching { DocumentsContract.isTreeUri(uri) }.getOrDefault(false)

    fun docIdOf(uri: Uri): String =
        runCatching { DocumentsContract.getDocumentId(uri) }.getOrDefault("")

    fun treeRootId(tree: Uri): String =
        runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrDefault("")

    /** 由 tree + 文档 id 组装子文档 Uri；授权由 tree 继承，不必逐个申请。 */
    fun docUri(tree: Uri, docId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(tree, docId)

    /** 某个 Uri 是否在这棵树里（文档 id 前缀判断，ExternalStorageProvider 的 id 就是路径）。 */
    fun inTree(tree: Uri, uri: Uri?): Boolean = DocIo.isUnder(uri, tree)

    fun isMarkdown(name: String): Boolean = MARKDOWN_SUFFIXES.any { name.endsWith(it, ignoreCase = true) }

    /** 文档 id 的可读名：`primary:Documents/拾简/工作` → `工作`。 */
    fun nameOfDocId(docId: String): String {
        val tail = docId.substringAfterLast('/')
        val afterVolume = tail.substringAfterLast(':')
        return when {
            afterVolume.isNotBlank() -> afterVolume
            tail.endsWith(":") -> "内部存储"
            else -> tail
        }
    }

    fun parentOf(docId: String): String? {
        val cut = docId.lastIndexOf('/')
        return if (cut <= 0) null else docId.substring(0, cut)
    }

    /** 目录的上级链（含自身），从库根排到当前目录。 */
    fun trail(rootDocId: String, dirDocId: String): List<Pair<String, String>> {
        if (!dirDocId.startsWith(rootDocId)) return listOf(rootDocId to nameOfDocId(rootDocId))
        val rest = dirDocId.removePrefix(rootDocId).trimStart('/')
        val out = ArrayList<Pair<String, String>>()
        out += rootDocId to nameOfDocId(rootDocId)
        if (rest.isNotEmpty()) {
            var acc = rootDocId
            rest.split('/').forEach { seg ->
                acc = "$acc/$seg"
                out += acc to seg
            }
        }
        return out
    }

    /**
     * 列出目录内容：文件夹在前，其后按名称排序，非 md 文件跳过。
     *
     * [includeHidden] 为 true 时把 `.` 开头的条目也列出来。只有回收站目录本身需要这个开关：
     * `.trash` 是隐藏目录，不带上的话 `ensureChild` 永远找不到它，每次都会再建一个。
     */
    fun list(
        context: Context,
        tree: Uri,
        dirDocId: String,
        includeHidden: Boolean = false,
    ): List<LibEntry> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(docUri(tree, dirDocId), dirDocId)
        val out = ArrayList<LibEntry>()
        context.contentResolver.query(children, PROJECTION, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: nameOfDocId(id)
                if (!includeHidden && name.startsWith(".")) continue
                val isDir = cursor.getString(2) == DIR_MIME
                if (!isDir && !isMarkdown(name)) continue
                out += LibEntry(
                    uri = docUri(tree, id),
                    docId = id,
                    name = name,
                    isDir = isDir,
                    size = if (cursor.isNull(3)) 0L else cursor.getLong(3),
                    modified = if (cursor.isNull(4)) 0L else cursor.getLong(4),
                )
            }
        }
        return out.sortedWith(compareByDescending<LibEntry> { it.isDir }.thenBy { it.name.lowercase() })
    }

    /** 目录里的条目数，用于文件夹行右侧的角标。 */
    fun countOf(context: Context, tree: Uri, dirDocId: String): Int {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(docUri(tree, dirDocId), dirDocId)
        var count = 0
        context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0) ?: continue
                    if (!name.startsWith(".")) count++
                }
            }
        return count
    }

    fun findChild(
        context: Context,
        tree: Uri,
        parentDocId: String,
        name: String,
        includeHidden: Boolean = false,
    ): LibEntry? = list(context, tree, parentDocId, includeHidden)
        .firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** 找到（没有就建）子文件夹，返回其 docId。收件箱与回收站都靠它惰性创建。 */
    fun ensureChild(context: Context, tree: Uri, parentDocId: String, name: String): String {
        findChild(context, tree, parentDocId, name, includeHidden = true)?.let { return it.docId }
        val created = createFolder(context, tree, parentDocId, name)
            ?: error("无法在库里创建「$name」")
        return docIdOf(created)
    }

    fun createFolder(context: Context, tree: Uri, parentDocId: String, name: String): Uri? =
        runCatching {
            DocumentsContract.createDocument(
                context.contentResolver,
                docUri(tree, parentDocId),
                DIR_MIME,
                safeName(name),
            )
        }.getOrNull()

    /**
     * 用户选中的目录未必就是库根：选到内部存储根或 `Documents` 时，在它下面建一个「拾简」当库根，
     * 免得整个存储或整个 Documents 都被当成长笔记列表。
     */
    fun resolveRoot(context: Context, tree: Uri, pickedDocId: String, pickedName: String): Pair<String, String> {
        if (pickedName == DEFAULT_ROOT_NAME) return pickedDocId to DEFAULT_ROOT_NAME
        val isVolumeRoot = pickedDocId.endsWith(":")
        val isDocuments = pickedDocId.endsWith(":" + DOCUMENTS_DIR)
        if (!isVolumeRoot && !isDocuments) return pickedDocId to pickedName
        val base = if (isVolumeRoot) ensureChild(context, tree, pickedDocId, DOCUMENTS_DIR) else pickedDocId
        return ensureChild(context, tree, base, DEFAULT_ROOT_NAME) to DEFAULT_ROOT_NAME
    }

    /** Android 11+ 不允许把「下载」目录整个授权给第三方应用。 */
    fun isBlockedPick(pickedDocId: String): Boolean =
        pickedDocId.endsWith(":Download") || pickedDocId.endsWith(":Downloads")

    /** 同名时自动加序号，别让导入悄悄覆盖已有笔记。 */
    fun uniqueName(context: Context, tree: Uri, parentDocId: String, name: String): String {
        val taken = list(context, tree, parentDocId).mapTo(HashSet()) { it.name.lowercase() }
        if (name.lowercase() !in taken) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.take(dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 2
        while (true) {
            val candidate = base + " (" + i + ")" + ext
            if (candidate.lowercase() !in taken) return candidate
            i++
        }
    }

    /** 分享来源的文件名不一定带可识别的后缀，补一个 .md，免得导进来在列表里看不见。 */
    fun importName(sourceName: String): String {
        val cleaned = safeName(sourceName)
        return if (isMarkdown(cleaned)) cleaned else cleaned + ".md"
    }

    fun safeName(name: String): String {
        val cleaned = buildString {
            name.forEach { ch -> append(if (ch in INVALID_NAME_CHARS || ch.isISOControl()) '_' else ch) }
        }.trim().trimEnd('.')
        return cleaned.take(80).ifBlank { "未命名.md" }
    }

    /** 把外部来源的内容复制进库里，返回新文件的 Uri。 */
    fun importText(context: Context, tree: Uri, parentDocId: String, sourceName: String, text: String): Uri {
        val name = uniqueName(context, tree, parentDocId, importName(sourceName))
        val created = runCatching {
            DocumentsContract.createDocument(context.contentResolver, docUri(tree, parentDocId), MD_MIME, name)
        }.getOrNull() ?: error("在库里创建文件失败")
        DocIo.write(context, created, text)
        return created
    }

    fun rename(context: Context, uri: Uri, newName: String): Uri? =
        runCatching { DocumentsContract.renameDocument(context.contentResolver, uri, safeName(newName)) }.getOrNull()

    fun delete(context: Context, uri: Uri): Boolean =
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }.getOrDefault(false)

    fun move(
        context: Context,
        tree: Uri,
        sourceUri: Uri,
        sourceParentDocId: String,
        targetParentDocId: String,
    ): Boolean = runCatching {
        DocumentsContract.moveDocument(
            context.contentResolver,
            sourceUri,
            docUri(tree, sourceParentDocId),
            docUri(tree, targetParentDocId),
        ) != null
    }.getOrDefault(false)

    /**
     * 移动一个条目；个别 provider 不支持 `moveDocument`，md 文件就退化成「复制一份 + 删原件」。
     *
     * 文件夹不做退化处理——递归复制再删太危险，宁可如实报失败。
     */
    fun moveEntry(context: Context, tree: Uri, entry: LibEntry, targetParentDocId: String): Boolean {
        val sourceParent = parentOf(entry.docId) ?: return false
        if (sourceParent == targetParentDocId) return true
        if (move(context, tree, entry.uri, sourceParent, targetParentDocId)) return true
        if (entry.isDir) return false
        val text = runCatching { DocIo.read(context, entry.uri) }.getOrNull() ?: return false
        val copied = runCatching { importText(context, tree, targetParentDocId, entry.name, text) }.getOrNull() ?: return false
        val removed = delete(context, entry.uri)
        return removed || copied != null
    }

    /** [docId] 是否就是 [ancestor] 本身或它的后代——用来挡住「把文件夹移进自己里」。 */
    fun isUnderDocId(docId: String, ancestor: String): Boolean =
        docId == ancestor || docId.startsWith(ancestor + "/")

    private const val DOCUMENTS_DIR = "Documents"
}
