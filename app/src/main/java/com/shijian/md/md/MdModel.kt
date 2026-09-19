package com.shijian.md.md

import androidx.compose.runtime.Immutable

/* ------------------------------------------------------------------ 行内元素 */

@Immutable
sealed interface Inline {
    @Immutable data class Text(val text: String) : Inline
    @Immutable data class Emph(val children: List<Inline>) : Inline
    @Immutable data class Strong(val children: List<Inline>) : Inline
    @Immutable data class Strike(val children: List<Inline>) : Inline
    @Immutable data class Highlight(val children: List<Inline>) : Inline
    @Immutable data class Code(val text: String) : Inline
    @Immutable data class Math(val text: String, val display: Boolean) : Inline
    @Immutable data class Link(val url: String, val children: List<Inline>) : Inline
    @Immutable data class ImageRef(val url: String, val alt: String, val title: String?) : Inline
    @Immutable data class FootnoteRef(val label: String) : Inline
    @Immutable data class Break(val hard: Boolean) : Inline
    @Immutable data class Raw(val html: String) : Inline
}

/* ------------------------------------------------------------------ 块级元素 */

@Immutable
sealed interface MdBlock {
    /** 源文件中的行号（1 起），未知为 0 */
    val line: Int

    @Immutable data class Heading(
        val level: Int,
        val content: List<Inline>,
        val plain: String,
        override val line: Int,
    ) : MdBlock

    @Immutable data class Para(val content: List<Inline>, override val line: Int) : MdBlock

    /** 独立成段的公式，v1 以等宽降级呈现 */
    @Immutable data class Formula(val text: String, val display: Boolean, override val line: Int) : MdBlock

    @Immutable data class Bullet(
        val depth: Int,
        val marker: Char,
        val content: List<Inline>,
        override val line: Int,
    ) : MdBlock

    @Immutable data class Ordered(
        val depth: Int,
        val number: String,
        val content: List<Inline>,
        override val line: Int,
    ) : MdBlock

    @Immutable data class Task(
        val depth: Int,
        val checked: Boolean,
        val content: List<Inline>,
        /** 复选框所在字符偏移，用于回写源文件 */
        val markerIndex: Int,
        override val line: Int,
    ) : MdBlock

    @Immutable data class Quote(val depth: Int, val content: List<Inline>, override val line: Int) : MdBlock

    @Immutable data class Code(val language: String, val code: String, override val line: Int) : MdBlock

    @Immutable data class Table(
        val header: List<List<Inline>>,
        val rows: List<List<List<Inline>>>,
        override val line: Int,
    ) : MdBlock

    @Immutable data class Divider(override val line: Int) : MdBlock

    @Immutable data class Image(val url: String, val alt: String, override val line: Int) : MdBlock

    @Immutable data class FrontMatter(
        val entries: List<Pair<String, String>>,
        val raw: String,
        override val line: Int,
    ) : MdBlock

    @Immutable data class Html(val raw: String, override val line: Int) : MdBlock
}

/* ------------------------------------------------------------------ 文档模型 */

@Immutable
data class OutlineEntry(val level: Int, val title: String, val blockIndex: Int, val line: Int)

@Immutable
data class MdDocument(
    val blocks: List<MdBlock> = emptyList(),
    val outline: List<OutlineEntry> = emptyList(),
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val title: String = "",
) {
    val isEmpty: Boolean get() = blocks.isEmpty()

    companion object {
        val Empty = MdDocument()
    }
}

/* ------------------------------------------------------------------ 工具 */

fun Inline.plain(): String = when (this) {
    is Inline.Text -> text
    is Inline.Code -> text
    is Inline.Math -> text
    is Inline.Emph -> children.joinToString("") { it.plain() }
    is Inline.Strong -> children.joinToString("") { it.plain() }
    is Inline.Strike -> children.joinToString("") { it.plain() }
    is Inline.Highlight -> children.joinToString("") { it.plain() }
    is Inline.Link -> children.joinToString("") { it.plain() }
    is Inline.ImageRef -> alt
    is Inline.FootnoteRef -> "[^$label]"
    is Inline.Break -> " "
    is Inline.Raw -> ""
}

fun List<Inline>.plainText(): String = joinToString("") { it.plain() }
