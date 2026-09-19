package com.shijian.md

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import com.shijian.md.ui.AppRoot

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            val view = LocalView.current
            val keepAwake = vm.settings.keepAwake
            LaunchedEffect(keepAwake) {
                view.keepScreenOn = keepAwake
            }
            AppRoot(vm)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data?.let {
                vm.open(it, edit = false, sourceFlags = intent.flags)
            }
            Intent.ACTION_EDIT -> intent.data?.let {
                vm.open(it, edit = true, sourceFlags = intent.flags)
            }
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val stream = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                when {
                    stream != null -> vm.open(stream, sourceFlags = intent.flags)
                    !text.isNullOrBlank() -> vm.openText("分享的文本.md", text)
                }
            }
            else -> Unit
        }
    }
}
