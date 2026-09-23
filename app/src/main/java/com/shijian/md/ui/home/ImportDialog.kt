package com.shijian.md.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shijian.md.AppViewModel
import com.shijian.md.data.Library
import com.shijian.md.ui.theme.LocalMdTheme

/**
 * 第一次从外部（QQ / 微信分享、用其他方式打开）收到 md 时问一次。
 *
 * 勾了「以后都导入到这里」就永久记住，之后静默入库，不再打断。
 */
@Composable
fun ImportConfirmDialog(vm: AppViewModel) {
    val pending = vm.pendingImport ?: return
    val spec = LocalMdTheme.current
    val c = spec.colors
    var always by remember { mutableStateOf(true) }
    val rootName = vm.library.rootName

    AlertDialog(
        onDismissRequest = { vm.dismissImport() },
        containerColor = c.surface,
        title = { Text(text = "收进笔记库吗？", style = spec.typography.title) },
        text = {
            Column {
                Text(
                    text = "拾简需要把文件复制一份到库里，之后才改得动、也才找得回来。",
                    style = spec.typography.caption,
                    color = c.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(c.background)
                        .border(1.dp, c.outline, RoundedCornerShape(13.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                ) {
                    Text(text = "来源", style = spec.typography.caption, color = c.muted)
                    Text(
                        text = pending.name,
                        style = spec.typography.small,
                        color = c.onBackground,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(text = "存到", style = spec.typography.caption, color = c.muted)
                    Text(
                        text = "$rootName / ${Library.INBOX}",
                        style = spec.typography.small,
                        color = c.onBackground,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "内部存储 Documents/拾简，可在设置里改",
                        style = spec.typography.caption,
                        color = c.muted,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { always = !always }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = always, onCheckedChange = { always = it })
                    Spacer(Modifier.width(2.dp))
                    Column {
                        Text(
                            text = "以后都导入到这里",
                            style = spec.typography.small,
                            color = c.onBackground,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "只问这一次，不再打扰",
                            style = spec.typography.caption,
                            color = c.muted,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.confirmImport(remember = always) }) {
                Text(text = "导入并打开", style = spec.typography.label)
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.dismissImport() }) {
                Text(text = "取消", style = spec.typography.label, color = c.muted)
            }
        },
    )
}
