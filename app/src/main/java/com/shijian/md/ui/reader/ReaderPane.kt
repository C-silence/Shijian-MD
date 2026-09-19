package com.shijian.md.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shijian.md.md.MdBlock
import com.shijian.md.md.MdDocument
import com.shijian.md.ui.components.MdBlockView
import com.shijian.md.ui.theme.LocalMdTheme
import java.io.File

@Composable
fun ReaderPane(
    document: MdDocument,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    imageBase: File? = null,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    onOpenLink: (String) -> Unit = {},
    onToggleTask: (MdBlock.Task, Boolean) -> Unit = { _, _ -> },
) {
    val spec = LocalMdTheme.current
    if (document.blocks.isEmpty()) {
        EmptyState(modifier)
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 8.dp,
            bottom = 120.dp,
        ),
    ) {
        itemsIndexed(document.blocks, key = { index, _ -> index }) { index, block ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = spec.spacing.contentMaxWidth).fillMaxWidth()) {
                    MdBlockView(
                        block = block,
                        isFirst = index == 0,
                        imageBase = imageBase,
                        onOpenLink = onOpenLink,
                        onToggleTask = onToggleTask,
                    )
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "— 完 —",
                    style = spec.typography.caption,
                    color = spec.colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    val spec = LocalMdTheme.current
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "空白文档",
                style = spec.typography.h3,
                color = spec.colors.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "切换到编辑模式开始写作",
                style = spec.typography.small,
                color = spec.colors.muted,
            )
        }
    }
}
