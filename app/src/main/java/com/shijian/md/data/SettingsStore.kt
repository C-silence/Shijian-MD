package com.shijian.md.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shijian.md.ui.theme.DarkMode
import com.shijian.md.ui.theme.DecorationLevel
import com.shijian.md.ui.theme.ThemeSpec
import com.shijian.md.ui.theme.Themes

private inline fun <reified T : Enum<T>> enumOf(value: String?, def: T): T =
    value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: def

/** 阅读与外观偏好，落盘在 SharedPreferences，内存中是可观察状态。 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("shijian.settings", Context.MODE_PRIVATE)

    private var sFamily by mutableStateOf(
        prefs.getString(KEY_FAMILY, Themes.DEFAULT_FAMILY) ?: Themes.DEFAULT_FAMILY
    )
    private var sDark by mutableStateOf(enumOf(prefs.getString(KEY_DARK, null), DarkMode.SYSTEM))
    private var sDecoration by mutableStateOf(
        enumOf(prefs.getString(KEY_DECORATION, null), DecorationLevel.RESTRAINED)
    )
    private var sBodySize by mutableStateOf(prefs.getFloat(KEY_BODY_SIZE, 16f))
    private var sEyeCare by mutableStateOf(prefs.getBoolean(KEY_EYE_CARE, false))
    private var sTaskWriteBack by mutableStateOf(prefs.getBoolean(KEY_TASK_WRITE_BACK, true))
    private var sAutosave by mutableStateOf(prefs.getBoolean(KEY_AUTOSAVE, true))
    private var sOutlineOpen by mutableStateOf(prefs.getBoolean(KEY_OUTLINE_OPEN, true))
    private var sKeepAwake by mutableStateOf(prefs.getBoolean(KEY_KEEP_AWAKE, false))

    var familyId: String
        get() = sFamily
        set(value) {
            sFamily = value
            prefs.edit().putString(KEY_FAMILY, value).apply()
        }

    var darkMode: DarkMode
        get() = sDark
        set(value) {
            sDark = value
            prefs.edit().putString(KEY_DARK, value.name).apply()
        }

    var decoration: DecorationLevel
        get() = sDecoration
        set(value) {
            sDecoration = value
            prefs.edit().putString(KEY_DECORATION, value.name).apply()
        }

    var bodySize: Float
        get() = sBodySize
        set(value) {
            sBodySize = value.coerceIn(MIN_BODY, MAX_BODY)
            prefs.edit().putFloat(KEY_BODY_SIZE, sBodySize).apply()
        }

    var eyeCare: Boolean
        get() = sEyeCare
        set(value) {
            sEyeCare = value
            prefs.edit().putBoolean(KEY_EYE_CARE, value).apply()
        }

    var taskWriteBack: Boolean
        get() = sTaskWriteBack
        set(value) {
            sTaskWriteBack = value
            prefs.edit().putBoolean(KEY_TASK_WRITE_BACK, value).apply()
        }

    var autosave: Boolean
        get() = sAutosave
        set(value) {
            sAutosave = value
            prefs.edit().putBoolean(KEY_AUTOSAVE, value).apply()
        }

    var outlineOpen: Boolean
        get() = sOutlineOpen
        set(value) {
            sOutlineOpen = value
            prefs.edit().putBoolean(KEY_OUTLINE_OPEN, value).apply()
        }

    var keepAwake: Boolean
        get() = sKeepAwake
        set(value) {
            sKeepAwake = value
            prefs.edit().putBoolean(KEY_KEEP_AWAKE, value).apply()
        }

    fun spec(systemDark: Boolean): ThemeSpec {
        val dark = when (darkMode) {
            DarkMode.SYSTEM -> systemDark
            DarkMode.LIGHT -> false
            DarkMode.DARK -> true
        }
        return Themes.spec(familyId, dark, decoration, bodySize, eyeCare)
    }

    fun largerText() {
        bodySize = bodySize + 1f
    }

    fun smallerText() {
        bodySize = bodySize - 1f
    }

    companion object {
        const val MIN_BODY = 10f
        const val MAX_BODY = 22f

        private const val KEY_FAMILY = "family"
        private const val KEY_DARK = "darkMode"
        private const val KEY_DECORATION = "decoration"
        private const val KEY_BODY_SIZE = "bodySize"
        private const val KEY_EYE_CARE = "eyeCare"
        private const val KEY_TASK_WRITE_BACK = "taskWriteBack"
        private const val KEY_AUTOSAVE = "autosave"
        private const val KEY_OUTLINE_OPEN = "outlineOpen"
        private const val KEY_KEEP_AWAKE = "keepAwake"
    }
}
