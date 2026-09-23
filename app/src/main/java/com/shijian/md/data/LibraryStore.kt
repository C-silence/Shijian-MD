package com.shijian.md.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 笔记库的位置与几个「只问一次」的开关，落在 SharedPreferences。
 *
 * 库根分两层记录：[treeUri] 是系统授权的目录树（授权凭证），[rootDocId] 是树里的逻辑根
 * （默认 `Documents/拾简`）。用户选了内部存储或 Documents 时，逻辑根会比授权根更深一层。
 */
class LibraryStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("shijian.library", Context.MODE_PRIVATE)

    private var sTree by mutableStateOf(prefs.getString(KEY_TREE, null))
    private var sRoot by mutableStateOf(prefs.getString(KEY_ROOT, null))
    private var sRootName by mutableStateOf(prefs.getString(KEY_ROOT_NAME, null))
    private var sConfirmed by mutableStateOf(prefs.getBoolean(KEY_CONFIRMED, false))
    private var sLastDir by mutableStateOf(prefs.getString(KEY_LAST_DIR, null))

    val treeUri: Uri? get() = sTree?.let { Uri.parse(it) }

    val rootDocId: String? get() = sRoot

    val rootName: String get() = sRootName ?: Library.DEFAULT_ROOT_NAME

    val hasLibrary: Boolean get() = sTree != null && sRoot != null

    /** 是否已经确认过「以后都导入到这里」；没确认时第一次导入会问一次。 */
    var importConfirmed: Boolean
        get() = sConfirmed
        set(value) {
            sConfirmed = value
            prefs.edit().putBoolean(KEY_CONFIRMED, value).apply()
        }

    /** 上次浏览到的目录，重进应用时直接回到那里。 */
    var lastDir: String?
        get() = sLastDir
        set(value) {
            sLastDir = value
            prefs.edit().putString(KEY_LAST_DIR, value).apply()
        }

    fun setLibrary(tree: Uri, rootDocId: String, rootName: String, confirmed: Boolean = false) {
        sTree = tree.toString()
        sRoot = rootDocId
        sRootName = rootName
        sConfirmed = confirmed
        sLastDir = rootDocId
        prefs.edit()
            .putString(KEY_TREE, sTree)
            .putString(KEY_ROOT, sRoot)
            .putString(KEY_ROOT_NAME, rootName)
            .putBoolean(KEY_CONFIRMED, confirmed)
            .putString(KEY_LAST_DIR, rootDocId)
            .apply()
    }

    fun clear() {
        sTree = null
        sRoot = null
        sRootName = null
        sConfirmed = false
        sLastDir = null
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_TREE = "tree"
        const val KEY_ROOT = "rootDocId"
        const val KEY_ROOT_NAME = "rootName"
        const val KEY_CONFIRMED = "importConfirmed"
        const val KEY_LAST_DIR = "lastDir"
    }
}
