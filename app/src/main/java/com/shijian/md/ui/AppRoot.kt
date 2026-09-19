package com.shijian.md.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.shijian.md.AppViewModel
import com.shijian.md.ui.home.HomeScreen
import com.shijian.md.ui.settings.SettingsScreen
import com.shijian.md.ui.theme.DarkMode
import com.shijian.md.ui.theme.LocalMdTheme
import com.shijian.md.ui.theme.MdSurface
import com.shijian.md.ui.theme.ShijianTheme
import com.shijian.md.ui.theme.Themes
import kotlinx.coroutines.delay
import android.content.Context
import android.content.ContextWrapper
import android.app.Activity

@Composable
fun AppRoot(vm: AppViewModel) {
    val settings = vm.settings
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val spec = remember(
        settings.familyId,
        dark,
        settings.decoration,
        settings.bodySize,
        settings.eyeCare,
    ) {
        Themes.spec(
            familyId = settings.familyId,
            dark = dark,
            decoration = settings.decoration,
            bodySize = settings.bodySize,
            eyeCare = settings.eyeCare,
        )
    }
    val window = rememberWindowClass()
    var showSettings by remember { mutableStateOf(false) }

    ShijianTheme(spec) {
        val view = LocalView.current
        LaunchedEffect(spec.isDark) {
            val activity = view.context.findActivity() ?: return@LaunchedEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !spec.isDark
            controller.isAppearanceLightNavigationBars = !spec.isDark
        }
        MdSurface {
            when {
                showSettings -> SettingsScreen(vm = vm, onBack = { showSettings = false })
                vm.doc != null -> DocumentScreen(
                    vm = vm,
                    window = window,
                    onOpenSettings = { showSettings = true },
                )
                else -> HomeScreen(vm = vm, onOpenSettings = { showSettings = true })
            }
            ToastPill(vm)
        }
    }

    BackHandler(enabled = showSettings || vm.doc != null) {
        if (showSettings) showSettings = false else vm.closeDoc()
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ToastPill(vm: AppViewModel) {
    val message = vm.toast ?: return
    val spec = LocalMdTheme.current
    LaunchedEffect(message) {
        delay(2200)
        vm.clearToast()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 108.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(spec.colors.onBackground.copy(alpha = 0.9f))
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Text(
                text = message,
                style = spec.typography.small,
                color = spec.colors.background,
            )
        }
    }
}
