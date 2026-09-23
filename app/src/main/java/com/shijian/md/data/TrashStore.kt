package com.shijian.md.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** 回收站记录：文件本体已经在 `.trash/` 里，这里只记「从哪来、什么时候删的」。 */
@Immutable
data class TrashEntry(
    val docId: String,
    val name: String,
    val originalParent: String,
    val deletedAt: Long,
)

/** 回收站条目（记录 + 目录里的真实文件），界面直接用这个。 */
@Immutable
data class TrashItem(
    val uri: Uri,
    val docId: String,
    val name: String,
    val originalParent: String,
    val deletedAt: Long,
    val isDir: Boolean,
)

/**
 * 回收站的账本。
 *
 * 「删除」不是真删，而是把文件移动进库根的 `.trash/` 隐藏目录；过期时间记在这里。
 * 记录只是附属信息：文件被用户在电脑上手动删掉后，`retainOnly` 会把对应记录一起清掉。
 */
class TrashStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("shijian.trash", Context.MODE_PRIVATE)

    private var state by mutableStateOf(load())

    val items: List<TrashEntry> get() = state

    fun mark(docId: String, name: String, originalParent: String, deletedAt: Long = System.currentTimeMillis()) {
        state = state.filterNot { it.docId == docId } + TrashEntry(docId, name, originalParent, deletedAt)
        persist()
    }

    /** 目录里有、账本里没有的条目（换设备、云盘同步过来的），按「刚删除」补一条，别让它立刻被清掉。 */
    fun ensure(docId: String, name: String, originalParent: String): TrashEntry {
        state.firstOrNull { it.docId == docId }?.let { return it }
        val entry = TrashEntry(docId, name, originalParent, System.currentTimeMillis())
        state = state + entry
        persist()
        return entry
    }

    fun forget(docId: String) {
        if (state.none { it.docId == docId }) return
        state = state.filterNot { it.docId == docId }
        persist()
    }

    fun retainOnly(docIds: Collection<String>) {
        val next = state.filter { it.docId in docIds }
        if (next.size == state.size) return
        state = next
        persist()
    }

    /** 已过 7 天保留期的条目。 */
    fun expired(now: Long = System.currentTimeMillis()): List<TrashEntry> =
        state.filter { now - it.deletedAt >= KEEP_MILLIS }

    fun clear() {
        if (state.isEmpty()) return
        state = emptyList()
        persist()
    }

    private fun persist() {
        val array = JSONArray()
        state.forEach {
            array.put(
                JSONObject()
                    .put("docId", it.docId)
                    .put("name", it.name)
                    .put("parent", it.originalParent)
                    .put("at", it.deletedAt),
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun load(): List<TrashEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val docId = obj.optString("docId").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                TrashEntry(
                    docId = docId,
                    name = obj.optString("name", Library.nameOfDocId(docId)),
                    originalParent = obj.optString("parent", ""),
                    deletedAt = obj.optLong("at", 0L),
                )
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        const val KEEP_DAYS = 7
        const val KEEP_MILLIS = KEEP_DAYS * 24L * 60L * 60L * 1000L
        private const val KEY = "trash"
    }
}
