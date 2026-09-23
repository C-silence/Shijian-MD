package com.shijian.md.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shijian.md.ui.theme.LocalMdTheme

/**
 * 应用的小标记：短标题条 + 三行正文条，和应用图标同一组几何。
 *
 * 图标本体是自适应图标（渐变底 + 白条 + 单色层），没法直接当界面里的小图标用；
 * 这里按同样的坐标重画一遍，底色改成**当前主题的主色渐变**，12 套主题各自不同。
 */

/** 四条圆角条在 108×108 视口里的坐标，直接取自 `drawable/ic_launcher_foreground.xml`。 */
private val BARS = arrayOf(
    floatArrayOf(42.79f, 31.58f, 65.21f, 37.48f),
    floatArrayOf(33.35f, 46.92f, 74.65f, 52.82f),
    floatArrayOf(33.35f, 58.72f, 74.65f, 64.62f),
    floatArrayOf(33.35f, 70.52f, 58.13f, 76.42f),
)

@Composable
fun AppMark(modifier: Modifier = Modifier, size: Dp = 30.dp, corner: Dp = size * 0.3f) {
    val colors = LocalMdTheme.current.colors
    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(colors.primary, colors.primaryDeep))),
    ) {
        val unit = this.size.minDimension / 108f
        BARS.forEach { bar ->
            drawRoundRect(
                color = colors.onPrimary,
                topLeft = Offset(bar[0] * unit, bar[1] * unit),
                size = Size((bar[2] - bar[0]) * unit, (bar[3] - bar[1]) * unit),
                cornerRadius = CornerRadius(2.95f * unit, 2.95f * unit),
            )
        }
    }
}
