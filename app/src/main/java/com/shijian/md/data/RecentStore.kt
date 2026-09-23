package com.shijian.md.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class RecentDoc(
    val uri: String,
    val title: String,
    val time: Long,
    val preview: String = "",
    /** 上次读到的位置（0..1），首页「继续阅读」用它画进度条。 */
    val progress: Float = 0f,
)

class RecentStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("shijian.recent", Context.MODE_PRIVATE)

    private var state by mutableStateOf(load())

    val items: List<RecentDoc> get() = state

    fun touch(uri: String, title: String, preview: String = "", progress: Float = -1f) {
        val kept = state.firstOrNull { it.uri == uri }?.progress ?: 0f
        val next = ArrayList<RecentDoc>(state.size + 1)
        next += RecentDoc(uri, title, System.currentTimeMillis(), preview, if (progress < 0f) kept else progress)
        state.filterTo(next) { it.uri != uri }
        state = next.take(MAX_ITEMS)
        persist()
    }

    /** 只更新阅读进度，不动排序——用户还在读，不该因此把它顶到最前面。 */
    fun setProgress(uri: String, progress: Float) {
        val current = state.firstOrNull { it.uri == uri } ?: return
        if (kotlin.math.abs(current.progress - progress) < 0.01f) return
        state = state.map { if (it.uri == uri) it.copy(progress = progress) else it }
        persist()
    }

    fun remove(uri: String) {
        state = state.filterNot { it.uri == uri }
        persist()
    }

    fun clear() {
        state = emptyList()
        persist()
    }

    private fun persist() {
        val arr = JSONArray()
        state.forEach {
            arr.put(
                JSONObject()
                    .put("uri", it.uri)
                    .put("title", it.title)
                    .put("time", it.time)
                    .put("preview", it.preview)
                    .put("progress", it.progress.toDouble())
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun load(): List<RecentDoc> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val uri = o.optString("uri").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                RecentDoc(
                    uri = uri,
                    title = o.optString("title", uri.substringAfterLast('/')),
                    time = o.optLong("time", 0L),
                    preview = o.optString("preview", ""),
                    progress = o.optDouble("progress", 0.0).toFloat(),
                )
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY = "recent"
        private const val MAX_ITEMS = 20
    }
}
