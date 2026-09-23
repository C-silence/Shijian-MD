package com.shijian.md.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shijian.md.AppViewModel
import com.shijian.md.data.SettingsStore
import com.shijian.md.ui.components.ShijianIcons
import com.shijian.md.ui.theme.DarkMode
import com.shijian.md.ui.theme.DecorationLevel
import com.shijian.md.ui.theme.LocalMdTheme
import com.shijian.md.ui.theme.ThemeFamilyInfo
import com.shijian.md.ui.theme.ThemeSpec
import com.shijian.md.ui.theme.Themes

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val spec = LocalMdTheme.current
    val c = spec.colors
    val settings = vm.settings
    val dark = spec.isDark
    val library = vm.library

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.attachLibrary(uri)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(52.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(ShijianIcons.Back, contentDescription = "返回", tint = c.onSurfaceVariant)
            }
            Text(
                text = "外观与阅读",
                style = spec.typography.title,
                color = c.onBackground,
                modifier = Modifier.weight(1f),
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline.copy(alpha = 0.7f)))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("主题", spec)
            }
            items(Themes.families, key = { it.id }) { family ->
                ThemeCard(
                    info = family,
                    selected = family.id == settings.familyId,
                    dark = dark,
                    onClick = { settings.familyId = family.id },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("深浅模式", spec, top = 14.dp)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SegmentedRow(
                    options = listOf(
                        DarkMode.SYSTEM to "跟随系统",
                        DarkMode.LIGHT to "浅色",
                        DarkMode.DARK to "深色",
                    ),
                    selected = settings.darkMode,
                    spec = spec,
                    onSelect = { settings.darkMode = it },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("背景纹理", spec, top = 14.dp)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SegmentedRow(
                    options = listOf(
                        DecorationLevel.FULL to "完整",
                        DecorationLevel.RESTRAINED to "克制",
                        DecorationLevel.MINIMAL to "极简",
                    ),
                    selected = settings.decoration,
                    spec = spec,
                    onSelect = { settings.decoration = it },
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("正文字号", spec, top = 14.dp)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "A",
                            style = spec.typography.caption,
                            color = c.muted,
                        )
                        Slider(
                            value = settings.bodySize,
                            onValueChange = { settings.bodySize = it },
                            valueRange = SettingsStore.MIN_BODY..SettingsStore.MAX_BODY,
                            steps = (SettingsStore.MAX_BODY - SettingsStore.MIN_BODY).toInt() - 1,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = c.primary,
                                activeTrackColor = c.primary.copy(alpha = 0.7f),
                                inactiveTrackColor = c.outline,
                            ),
                        )
                        Text(
                            text = "A",
                            style = spec.typography.h4,
                            color = c.muted,
                        )
                    }
                    Text(
                        text = "当前 ${settings.bodySize.toInt()} sp",
                        style = spec.typography.caption,
                        color = c.muted,
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("笔记库", spec, top = 14.dp)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface)
                        .border(1.dp, c.outline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (library.hasLibrary) {
                        Text("当前库", style = spec.typography.caption, color = c.muted)
                        Text(
                            text = library.rootName,
                            style = spec.typography.small,
                            color = c.onBackground,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = prettyDocPath(library.rootDocId.orEmpty()),
                            style = spec.typography.caption,
                            color = c.muted,
                        )
                    } else {
                        Text(
                            text = "还没有设置笔记库",
                            style = spec.typography.small,
                            color = c.onBackground,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "选一个文件夹后，从 QQ / 微信打开的 md 会自动收进「收件箱」。",
                            style = spec.typography.caption,
                            color = c.muted,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row {
                        TextButton(onClick = { treePicker.launch(null) }) {
                            Text(
                                text = if (library.hasLibrary) "更换文件夹" else "选择文件夹",
                                style = spec.typography.label,
                                color = c.primaryDeep,
                            )
                        }
                        if (library.hasLibrary) {
                            TextButton(onClick = {
                                library.clear()
                                vm.toast("已清除笔记库设置，库里的文件没有动过")
                            }) {
                                Text("清除设置", style = spec.typography.label, color = c.muted)
                            }
                        }
                    }
                    SwitchRow(
                        title = "以后都导入到「收件箱」",
                        desc = "关闭后，每次从外部打开 md 都会先问一次",
                        checked = library.importConfirmed,
                        spec = spec,
                        onCheckedChange = { library.importConfirmed = it },
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("偏好", spec, top = 14.dp)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth()) {
                    SwitchRow(
                        title = "护眼纸质",
                        desc = "把背景换成纸张色，可叠加任意主题",
                        checked = settings.eyeCare,
                        spec = spec,
                        onCheckedChange = { settings.eyeCare = it },
                    )
                    SwitchRow(
                        title = "阅读时勾选任务并回写源文件",
                        desc = "关闭后，阅读模式里的复选框只读",
                        checked = settings.taskWriteBack,
                        spec = spec,
                        onCheckedChange = { settings.taskWriteBack = it },
                    )
                    SwitchRow(
                        title = "自动保存",
                        desc = "停止输入约 1 秒后写回文件；未命名的文档存为草稿",
                        checked = settings.autosave,
                        spec = spec,
                        onCheckedChange = { settings.autosave = it },
                    )
                    SwitchRow(
                        title = "阅读时常亮屏幕",
                        desc = "仅在本应用前台生效",
                        checked = settings.keepAwake,
                        spec = spec,
                        onCheckedChange = { settings.keepAwake = it },
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("关于", spec, top = 14.dp)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceVariant.copy(alpha = 0.45f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("拾简 0.2", style = spec.typography.small, color = c.onBackground, fontWeight = FontWeight.Medium)
                    Text(
                        "面向手机与平板的 Markdown 编辑器。笔记库就是一个普通文件夹（默认 Documents/拾简），" +
                            "电脑连线、云盘同步都能看到同一批文件；所有内容都在本机处理，不联网。",
                        style = spec.typography.caption,
                        color = c.muted,
                    )
                    Text(
                        "支持 CommonMark、表格、任务列表、删除线、==高亮== 与 \$公式\$（v1 以等宽降级呈现）。",
                        style = spec.typography.caption,
                        color = c.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, spec: ThemeSpec, top: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        modifier = Modifier.padding(top = top, bottom = 2.dp),
        style = spec.typography.label,
        color = spec.colors.muted,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun ThemeCard(
    info: ThemeFamilyInfo,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
) {
    val spec = remember(info.id, dark) {
        Themes.spec(info.id, dark, DecorationLevel.MINIMAL, 15f, false)
    }
    val c = spec.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.background)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.primary else c.outline, shape)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxWidth().height(88.dp).padding(12.dp)) {
            Box(Modifier.width(30.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(c.primary))
            Spacer(Modifier.height(7.dp))
            Box(Modifier.fillMaxWidth(0.86f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.onBackground.copy(alpha = 0.32f)))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(0.62f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.onBackground.copy(alpha = 0.22f)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.92f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(c.codeBlockBg))
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c.primary.copy(alpha = 0.35f)))
                Spacer(Modifier.width(6.dp))
                Box(Modifier.fillMaxWidth(0.5f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.onBackground.copy(alpha = 0.22f)))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = info.name,
                style = spec.typography.label,
                color = c.onBackground,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(6.dp))
            if (info.darkOnly) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(c.surfaceVariant)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text("深色", style = spec.typography.caption, color = c.muted)
                }
            }
            Spacer(Modifier.weight(1f))
            if (selected) {
                Icon(
                    imageVector = ShijianIcons.Check,
                    contentDescription = "已选择",
                    tint = c.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun <T> SegmentedRow(
    options: List<Pair<T, String>>,
    selected: T,
    spec: ThemeSpec,
    onSelect: (T) -> Unit,
) {
    val c = spec.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceVariant.copy(alpha = 0.6f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) c.background else Color.Transparent)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = spec.typography.label,
                    color = if (active) c.primary else c.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    spec: ThemeSpec,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = spec.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = spec.typography.small, color = c.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(desc, style = spec.typography.caption, color = c.muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onPrimary,
                checkedTrackColor = c.primary,
                uncheckedThumbColor = c.muted,
                uncheckedTrackColor = c.surfaceVariant,
                uncheckedBorderColor = c.outline,
            ),
        )
    }
}

/** `primary:Documents/拾简` → `内部存储/Documents/拾简`。 */
private fun prettyDocPath(docId: String): String {
    if (docId.isBlank()) return ""
    val volume = docId.substringBefore(':')
    val rest = docId.substringAfter(':', "")
    val label = if (volume == "primary") "内部存储" else volume
    return if (rest.isBlank()) label else "$label/$rest"
}
