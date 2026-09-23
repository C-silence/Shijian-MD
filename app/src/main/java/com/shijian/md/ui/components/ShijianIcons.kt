package com.shijian.md.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun icon(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { d ->
        builder.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black),
        )
    }
    return builder.build()
}

/** 全部图标为自绘矢量路径，风格统一、无外部依赖。 */
object ShijianIcons {

    val Outline: ImageVector by lazy {
        icon(
            "outline",
            "M3 5h18v2H3z",
            "M6 10h15v2H6z",
            "M9 15h12v2H9z",
            "M12 20h9v2h-9z",
        )
    }

    val Reader: ImageVector by lazy {
        icon(
            "reader",
            "M18 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V4a2 2 0 0 0-2-2zm0 18H6V4h1v8l2.5-1.5L12 12V4h6v16z",
        )
    }

    val Edit: ImageVector by lazy {
        icon(
            "edit",
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25z",
            "M20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
        )
    }

    val Split: ImageVector by lazy {
        icon(
            "split",
            "M3 5h8v14H3z",
            "M13 5h8v14h-8z",
        )
    }

    val Settings: ImageVector by lazy {
        icon(
            "settings",
            "M3 6h11v2H3z",
            "M14 4h4v6h-4z",
            "M3 11h5v2H3z",
            "M8 9h4v6H8z",
            "M12 11h9v2h-9z",
            "M3 16h7v2H3z",
            "M10 14h4v6h-4z",
            "M14 16h7v2h-7z",
        )
    }

    val Back: ImageVector by lazy {
        icon("back", "M20 11H7.8l5.6-5.6L12 4l-8 8 8 8 1.4-1.4L7.8 13H20z")
    }

    val Add: ImageVector by lazy {
        icon("add", "M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6z")
    }

    val More: ImageVector by lazy {
        icon(
            "more",
            "M12 4.8m-1.8 0a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0",
            "M12 12m-1.8 0a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0",
            "M12 19.2m-1.8 0a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0-3.6 0",
        )
    }

    val Copy: ImageVector by lazy {
        icon(
            "copy",
            "M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1z",
            "M19 5H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z",
        )
    }

    val Check: ImageVector by lazy {
        icon("check", "M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z")
    }

    val Close: ImageVector by lazy {
        icon("close", "M19 6.4 17.6 5 12 10.6 6.4 5 5 6.4 10.6 12 5 17.6 6.4 19 12 13.4 17.6 19 19 17.6 13.4 12z")
    }

    val Folder: ImageVector by lazy {
        icon("folder", "M10 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-8l-2-2z")
    }

    val Save: ImageVector by lazy {
        icon(
            "save",
            "M17 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V7l-4-4zm-5 16a3 3 0 1 1 0-6 3 3 0 0 1 0 6zm3-10H5V5h10v4z",
        )
    }

    val Share: ImageVector by lazy {
        icon(
            "share",
            "M18 16.1c-.8 0-1.5.3-2 .8l-7.1-4.2c.1-.2.1-.5.1-.7s0-.5-.1-.7L16 7.1c.5.5 1.2.8 2 .8a3 3 0 1 0-3-3c0 .3 0 .5.1.7L8 9.8A3 3 0 1 0 6 15c.8 0 1.5-.3 2-.8l7.1 4.2c0 .2-.1.4-.1.6a2.9 2.9 0 1 0 3-2.9z",
        )
    }

    val Sun: ImageVector by lazy {
        icon(
            "sun",
            "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zm0 2a3 3 0 1 1 0 6 3 3 0 0 1 0-6z",
            "M11 1h2v3.2h-2z",
            "M11 19.8h2V23h-2z",
            "M1 11h3.2v2H1z",
            "M19.8 11H23v2h-3.2z",
            "M3.9 2.5 5.3 1.1l2.3 2.3-1.4 1.4z",
            "M16.4 20.6l1.4-1.4 2.3 2.3-1.4 1.4z",
            "M20.1 2.5l1.4 1.4-2.3 2.3-1.4-1.4z",
            "M3.9 21.5l-1.4-1.4 2.3-2.3 1.4 1.4z",
        )
    }

    val Moon: ImageVector by lazy {
        icon(
            "moon",
            "M12.4 3.1A9 9 0 1 0 20.9 14c.1-.5-.5-.9-.9-.6a6.4 6.4 0 0 1-9.4-9.4c.3-.5-.1-1.1-.6-1z",
        )
    }

    val TextSize: ImageVector by lazy {
        icon(
            "textsize",
            "M2.5 20 8.2 6h2.6L16.5 20h-2.6l-1.1-3h-6L5.6 20H2.5zm3.9-5.2h4.6L8.7 8.6 6.4 14.8z",
            "M15.4 12.4h3.9c.6 0 1 .5.7 1.1l-3.4 5.2h3.6V20h-4.6c-.6 0-1-.5-.7-1.1l3.4-5.2h-3V12.4z",
        )
    }

    val Search: ImageVector by lazy {
        icon(
            "search",
            "M10.5 3a7.5 7.5 0 1 0 0 15 7.5 7.5 0 0 0 0-15zm0 2a5.5 5.5 0 1 1 0 11 5.5 5.5 0 0 1 0-11z",
            "M15.6 14.2l1.4-1.4 5 5-1.4 1.4z",
        )
    }

    val Delete: ImageVector by lazy {
        icon(
            "delete",
            "M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
        )
    }

    val Up: ImageVector by lazy {
        icon("up", "M7.4 14.4 12 9.8l4.6 4.6L18 13l-6-6-6 6z")
    }

    val Down: ImageVector by lazy {
        icon("down", "M7.4 9.6 12 14.2l4.6-4.6L18 11l-6 6-6-6z")
    }

    val Task: ImageVector by lazy {
        icon(
            "task",
            "M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm0 16H5V5h14v14z",
            "M10.6 14.6 8 12l-1.4 1.4 4 4 7-7-1.4-1.4z",
        )
    }

    /** 文档：库列表里的 .md 文件。 */
    val Doc: ImageVector by lazy {
        icon(
            "doc",
            "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 7V3.5L18.5 9H13z",
        )
    }

    /** 新建文件夹：引导页大图标与后续的「新建文件夹」。 */
    val FolderPlus: ImageVector by lazy {
        icon(
            "folderPlus",
            "M10 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-8l-2-2z",
            "M13 10.5h-2v2.5H8.5v2H11v2.5h2V15h2.5v-2H13v-2.5z",
        )
    }

    val ChevronRight: ImageVector by lazy {
        icon("chevronRight", "M9.4 6.4 8 7.8l4.2 4.2L8 16.2l1.4 1.4 5.6-5.6z")
    }

    /** 移动：文件夹轮廓 + 指进去的箭头（文件夹内部是挖空的，箭头才看得见）。 */
    val MoveTo: ImageVector by lazy {
        icon(
            "moveTo",
            "M10 4H4a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-8l-2-2zm10 16H4V8h16v12z",
            "M6 16V8h14v8z",
            "M9 11h5V9.6L17.4 12 14 14.4V13H9z",
        )
    }

    /** 还原：回收站把文件搬回原处。 */
    val Restore: ImageVector by lazy {
        icon(
            "restore",
            "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z",
        )
    }

    /** 导出：把库里的笔记另存一份到别处。 */
    val Export: ImageVector by lazy {
        icon(
            "export",
            "M9 16h6v-6h4l-7-7-7 7h4z",
            "M5 18h14v2H5z",
        )
    }
}
