package com.shijian.md.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shijian.md.AppViewModel
import com.shijian.md.ui.components.ShijianIcons
import com.shijian.md.ui.theme.LocalMdTheme

/**
 * 首次使用的引导：选一个文件夹当笔记库。
 *
 * 只问这一次 —— 选完就把目录树授权长期存下来，之后从 QQ / 微信打开 md 直接静默收进收件箱。
 */
@Composable
fun LibraryGuideScreen(vm: AppViewModel) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val pending = vm.pendingImport

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.attachLibrary(uri) else vm.toast("没有选择文件夹，笔记库还没设置")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(c.primarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShijianIcons.FolderPlus,
                contentDescription = null,
                tint = c.primaryDeep,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "先选一个文件夹当笔记库",
            style = spec.typography.h2,
            color = c.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "从微信 / QQ 打开过的 md 会自动收进来，你可以在这里按文件夹分类整理。",
            style = spec.typography.small,
            color = c.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (pending != null) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.primaryWhisper)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = ShijianIcons.Doc,
                    contentDescription = null,
                    tint = c.primaryDeep,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "「${pending.name}」正等着入库，选好文件夹后会自动收进去",
                    style = spec.typography.caption,
                    color = c.primaryDeep,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(c.surface)
                .border(1.dp, c.outline, RoundedCornerShape(13.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "推荐位置",
                style = spec.typography.caption,
                color = c.muted,
            )
            Text(
                text = "Documents / 拾简",
                style = spec.typography.title,
                color = c.onBackground,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "在内部存储上，电脑连线、云盘同步都能看到同一批文件",
                style = spec.typography.caption,
                color = c.muted,
            )
        }

        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(c.primary)
                .clickable { picker.launch(null) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "选择文件夹",
                style = spec.typography.title,
                color = c.onPrimary,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "系统不允许把「内部存储」根目录和「下载」整个交给应用，所以请在 Documents 里" +
                "新建一个「拾简」文件夹再选它；也可以选 SD 卡或云盘同步目录，之后在设置里能改。",
            style = spec.typography.caption,
            color = c.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
    }
}
