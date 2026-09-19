package com.shijian.md.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowClass { COMPACT, MEDIUM, EXPANDED }

@Composable
fun rememberWindowClass(): WindowClass {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width >= 840 -> WindowClass.EXPANDED
        width >= 600 -> WindowClass.MEDIUM
        else -> WindowClass.COMPACT
    }
}
