package com.shijian.md.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** 文件的读取与写回，兼容 SAF(content://) 与应用私有目录。 */
object DocIo {

    private val GBK: Charset? = runCatching { Charset.forName("GBK") }.getOrNull()

    fun read(context: Context, uri: Uri): String {
        val bytes = openInput(context, uri).use { it.readBytes() }
        return decode(bytes)
    }

    fun write(context: Context, uri: Uri, text: String) {
        if (uri.scheme == "file") {
            val f = File(uri.path ?: return)
            f.parentFile?.mkdirs()
            f.writeBytes(text.toByteArray(Charsets.UTF_8))
            return
        }
        val bytes = text.toByteArray(Charsets.UTF_8)
        var lastError: Throwable? = null
        // "wt" 是唯一「截断 + 写」的模式；个别 provider 不认，就退到 rwt / w。
        // 不能只试一次就放弃——库文件写不进去，等于用户的改动白做了。
        for (mode in WRITE_MODES) {
            val result = runCatching {
                context.contentResolver.openOutputStream(uri, mode)?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: throw IllegalStateException("无法写入该文档")
            }
            if (result.isSuccess) return
            lastError = result.exceptionOrNull()
            if (lastError is SecurityException) break
        }
        throw lastError ?: IllegalStateException("无法写入该文档")
    }

    private val WRITE_MODES = arrayOf("wt", "rwt", "w")

    fun displayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") return File(uri.path ?: "").name
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        val name = c.getString(0)
                        if (!name.isNullOrBlank()) return name
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "未命名.md"
    }

    fun sizeOf(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
    }.getOrDefault(0L)

    /**
     * 把外部来源的内容复制到应用私有目录，返回副本文件。
     *
     * 用于源文件写不回去的场景——QQ / 微信 / 钉钉的分享入口只授予读权限，写回必然
     * SecurityException。副本按来源 Uri 去重：同一个来源再次打开时**复用已有副本且不覆盖**，
     * 因为用户可能已经在副本上改过内容，覆盖等于丢数据。
     */
    fun importCopy(context: Context, source: Uri, name: String, text: String): File {
        val dir = File(context.filesDir, "imports").apply { mkdirs() }
        val key = source.toString()
        val existing = dir.listFiles { f -> f.isFile && f.name.endsWith(SRC_SUFFIX) }
            ?.firstOrNull { runCatching { it.readText() }.getOrNull() == key }
            ?.name
            ?.removeSuffix(SRC_SUFFIX)
            ?.let { File(dir, it) }
            ?.takeIf { it.isFile }
        if (existing != null) return existing
        val target = uniqueFile(dir, safeFileName(name))
        target.writeText(text)
        File(dir, target.name + SRC_SUFFIX).writeText(key)
        return target
    }

    private const val SRC_SUFFIX = ".src"

    private val INVALID_NAME_CHARS = "\\/:*?\"<>|".toSet()

    /** 文件系统不接受的字符换成下划线并限制长度，免得私有目录里出现奇怪的文件名。 */
    private fun safeFileName(name: String): String {
        val cleaned = buildString {
            name.forEach { ch -> append(if (ch in INVALID_NAME_CHARS || ch.isISOControl()) '_' else ch) }
        }.trim().trimEnd('.')
        val fallback = cleaned.ifBlank { "未命名.md" }
        val dot = fallback.lastIndexOf('.')
        val base = (if (dot > 0) fallback.take(dot) else fallback).take(48).ifBlank { "未命名" }
        val ext = if (dot > 0) fallback.substring(dot).take(8) else ""
        return base + ext
    }

    private fun uniqueFile(dir: File, name: String): File {
        if (!File(dir, name).exists()) return File(dir, name)
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.take(dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var i = 2
        while (true) {
            val f = File(dir, "$base-$i$ext")
            if (!f.exists()) return f
            i++
        }
    }

    /**
     * 把来源 Intent 带来的临时授权转成长期授权，返回是否拿到了长期授权。
     *
     * [offeredFlags] 是来源 Intent 的 flags；传 null 表示调用方拿不到 flags——
     * 应用内 ACTION_OPEN_DOCUMENT 的返回结果只回一个 Uri，此时按「读+写」再退到「读」试一次。
     *
     * 只有 Intent 明确带了 FLAG_GRANT_PERSISTABLE_URI_PERMISSION 才允许持久化，否则
     * takePersistableUriPermission 会抛 SecurityException。QQ / 微信 / 钉钉这些第三方
     * FileProvider 分享出来的 Intent 基本都不带这个位，所以这里必须先判断再调用。
     */
    fun takePersistableGrant(context: Context, uri: Uri, offeredFlags: Int?): Boolean {
        if (uri.scheme != "content") return false
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val write = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val candidates = when {
            offeredFlags == null -> listOf(read or write, read)
            offeredFlags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == 0 -> return false
            else -> {
                var modes = 0
                if (offeredFlags and read != 0) modes = modes or read
                if (offeredFlags and write != 0) modes = modes or write
                if (modes == 0) return false
                listOf(modes)
            }
        }
        return candidates.any { modes ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, modes) }.isSuccess
        }
    }

    /** 是否持有该 uri 的长期授权（任意模式）。用于判断最近列表里的条目是否还能打开。 */
    fun hasPersistedGrant(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.persistedUriPermissions.any { it.uri == uri || isUnder(uri, it.uri) }
    }.getOrDefault(false)

    private fun hasPersistedWrite(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.persistedUriPermissions.any {
            it.isWritePermission && (it.uri == uri || isUnder(uri, it.uri))
        }
    }.getOrDefault(false)

    /**
     * [uri] 是否落在已授权的目录树 [tree] 里。
     *
     * 笔记库拿到的是一棵目录树的授权，库里的每个文件都是「树的后代」，而不是被单独授权过的
     * Uri；只比对相等会把库里的文件全判成「没权限」，所以必须按文档 id 前缀判断。
     */
    fun isUnder(uri: Uri?, tree: Uri): Boolean {
        if (uri == null || uri.scheme != "content") return false
        return runCatching {
            if (!DocumentsContract.isTreeUri(tree)) return@runCatching false
            val root = DocumentsContract.getTreeDocumentId(tree)
            val id = DocumentsContract.getDocumentId(uri)
            root.isNotEmpty() && id.isNotEmpty() && (id == root || id.startsWith("$root/"))
        }.getOrDefault(false)
    }

    /**
     * 能否写回源文件。
     *
     * 判据只有一条：**手里到底有没有写授权**。不要去看 provider 自报的
     * FLAG_SUPPORTS_WRITE —— 第三方 FileProvider（QQ / 微信 / 钉钉的分享入口）
     * 同样会声称支持写，但分享出来的授权只有读，写下去必然 SecurityException；
     * 而 DocumentsProvider 的 FLAG_SUPPORTS_WRITE 描述的是「这个文档本身可写」，
     * 不代表「你有权写」。所以它只能用来做二次确认，不能单独作为依据。
     */
    fun canWrite(context: Context, uri: Uri, grantedWriteNow: Boolean = false): Boolean {
        if (uri.scheme == "file") return File(uri.path ?: "").canWrite()
        if (grantedWriteNow) return true
        if (uri.scheme != "content") return false
        if (!hasPersistedWrite(context, uri)) return false
        if (!runCatching { DocumentsContract.isDocumentUri(context, uri) }.getOrDefault(false)) return true
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
                null,
                null,
                null,
            )?.use { cursor ->
                cursor.moveToFirst() &&
                    (cursor.getInt(0) and DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0
            } ?: false
        }.getOrDefault(false)
    }

    /** 判定异常是不是授权不足，用来给出可读的提示而不是原始报错。 */
    fun isPermissionDenied(e: Throwable?): Boolean =
        e is SecurityException || e?.message?.contains("Permission", ignoreCase = true) == true

    private fun openInput(context: Context, uri: Uri) =
        if (uri.scheme == "file") File(uri.path ?: "").inputStream()
        else context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法打开该文档")

    /** 优先按 BOM 判断，其次 UTF-8 严格解码，最后退回 GBK。 */
    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        val strict = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return runCatching { strict.decode(java.nio.ByteBuffer.wrap(bytes)).toString() }
            .getOrElse { GBK?.let { cs -> String(bytes, cs) } ?: String(bytes, Charsets.UTF_8) }
    }

    fun titleFromName(name: String): String = name.substringBeforeLast('.').ifBlank { name }
}
