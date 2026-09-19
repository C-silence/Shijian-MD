package com.shijian.md.md

import org.commonmark.ext.front.matter.YamlFrontMatterBlock
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.front.matter.YamlFrontMatterNode
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.CustomNode
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Nodes
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.node.Visitor
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

/* ------------------------------------------------------------------ 自定义节点 */

class HighlightNode : CustomNode() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

class MathNode(val text: String, val display: Boolean) : CustomNode() {
    override fun accept(visitor: Visitor) = visitor.visit(this)
}

/** `==高亮==` */
private class HighlightDelimiterProcessor : DelimiterProcessor {
    override fun getOpeningCharacter(): Char = '='
    override fun getClosingCharacter(): Char = '='
    override fun getMinLength(): Int = 2

    override fun process(opener: DelimiterRun, closer: DelimiterRun): Int {
        if (opener.length() != closer.length() || opener.length() > 2) return 0
        val openerText = opener.opener
        val node = HighlightNode()
        for (n in Nodes.between(openerText, closer.closer).toList()) node.appendChild(n)
        openerText.insertAfter(node)
        return opener.length()
    }
}

/** `$公式$` 与 `$$公式$$`，仅在贴合文本时才认定，避免误伤价格写法 */
private class MathDelimiterProcessor : DelimiterProcessor {
    override fun getOpeningCharacter(): Char = '$'
    override fun getClosingCharacter(): Char = '$'
    override fun getMinLength(): Int = 1

    override fun process(opener: DelimiterRun, closer: DelimiterRun): Int {
        val use = minOf(opener.length(), closer.length(), 2)
        if (use == 0) return 0
        val openerText = opener.opener
        val inner = Nodes.between(openerText, closer.closer).toList()
        val body = inner.joinToString("") { rawTextOf(it) }
        val display = use == 2
        val formula = body.isNotEmpty() &&
            (
                display || (
                    !body.first().isWhitespace() &&
                        !body.last().isWhitespace() &&
                        body.any { !it.isDigit() && it != '.' && it != ',' }
                    )
                )
        if (!formula) return 0
        inner.forEach { it.unlink() }
        openerText.insertAfter(MathNode(if (display) body.trim() else body, display))
        return use
    }
}

private fun rawTextOf(node: Node): String = when (node) {
    is Text -> node.literal ?: ""
    is Code -> node.literal ?: ""
    else -> {
        val sb = StringBuilder()
        var c = node.firstChild
        while (c != null) {
            sb.append(rawTextOf(c))
            c = c.next
        }
        sb.toString()
    }
}

/* ------------------------------------------------------------------ 解析入口 */

private val KBD_TAG = Regex("<kbd>(.*?)</kbd>", RegexOption.IGNORE_CASE)
private val FOOTNOTE_REF = Regex("\\[\\^([^\\]\\s]{1,32})\\]")
private val CHECKBOX = Regex("\\[([ xX])\\]")
private val CJK = Regex("[\\u3400-\\u4dbf\\u4e00-\\u9fff\\u3040-\\u30ff]")

object MdParser {

    private val parser: Parser = Parser.builder()
        .extensions(
            listOf(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListItemsExtension.create(),
                YamlFrontMatterExtension.create(),
            )
        )
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .customDelimiterProcessor(HighlightDelimiterProcessor())
        .customDelimiterProcessor(MathDelimiterProcessor())
        .build()

    fun parse(source: String): MdDocument {
        if (source.isBlank()) return MdDocument.Empty
        val pre = KBD_TAG.replace(source) { m -> "`" + m.groupValues[1] + "`" }
        val root = try {
            parser.parse(pre)
        } catch (t: Throwable) {
            return MdDocument.Empty
        }
        return Converter().convert(root, pre)
    }
}

/* ------------------------------------------------------------------ AST → 模型 */

private class Converter {

    private val blocks = ArrayList<MdBlock>()
    private val outline = ArrayList<OutlineEntry>()
    private var source: String = ""

    fun convert(root: Node, source: String): MdDocument {
        this.source = source
        if (root is Document) walk(root, 0) else walk(root, 0)
        val title = outline.firstOrNull { it.level == 1 }?.title
            ?: outline.firstOrNull()?.title
            ?: ""
        return MdDocument(
            blocks = blocks.toList(),
            outline = outline.toList(),
            wordCount = countWords(source),
            charCount = source.length,
            title = title,
        )
    }

    private fun countWords(source: String): Int {
        val cjk = CJK.findAll(source).count()
        val latin = source.split(Regex("[^A-Za-z0-9_\u0027-]+")).count { it.length > 1 }
        return cjk + latin
    }

    private fun lineOf(node: Node): Int = (node.sourceSpans.firstOrNull()?.lineIndex ?: -1) + 1

    private fun indexOf(node: Node): Int = node.sourceSpans.firstOrNull()?.inputIndex ?: -1

    /** 复选框所在字符偏移：优先用源信息，退化为按行查找。 */
    private fun checkboxIndex(node: Node, fallbackLine: Int): Int {
        val direct = indexOf(node)
        if (direct >= 0 && direct < source.length && source[direct] == '[') return direct
        if (fallbackLine <= 0) return -1
        var start = 0
        var current = 1
        while (current < fallbackLine) {
            val nl = source.indexOf('\n', start)
            if (nl < 0) return -1
            start = nl + 1
            current++
        }
        val end = source.indexOf('\n', start).let { if (it < 0) source.length else it }
        val lineText = source.substring(start, end)
        val match = CHECKBOX.find(lineText) ?: return -1
        return start + match.range.first
    }

    private fun walk(parent: Node, depth: Int) {
        var n = parent.firstChild
        while (n != null) {
            block(n, depth)
            n = n.next
        }
    }

    private fun block(node: Node, depth: Int) {
        when (node) {
            is Heading -> {
                val content = inlines(node)
                val plain = content.plainText()
                blocks += MdBlock.Heading(node.level, content, plain, lineOf(node))
                if (plain.isNotBlank()) {
                    outline += OutlineEntry(node.level, plain, blocks.size - 1, lineOf(node))
                }
            }

            is Paragraph -> paragraph(node)

            is BlockQuote -> {
                var c = node.firstChild
                while (c != null) {
                    when (c) {
                        is Paragraph -> blocks += MdBlock.Quote(depth + 1, inlines(c), lineOf(c))
                        is BlockQuote -> block(c, depth + 1)
                        else -> block(c, depth)
                    }
                    c = c.next
                }
            }

            is BulletList -> list(node, depth, node.bulletMarker)
            is OrderedList -> list(node, depth, null)

            is FencedCodeBlock -> {
                val info = node.info?.trim().orEmpty()
                val lang = info.substringBefore(' ').substringBefore('{').trim()
                blocks += MdBlock.Code(lang, node.literal.orEmpty(), lineOf(node))
            }

            is IndentedCodeBlock -> blocks += MdBlock.Code("", node.literal.orEmpty(), lineOf(node))
            is ThematicBreak -> blocks += MdBlock.Divider(lineOf(node))
            is HtmlBlock -> blocks += MdBlock.Html(node.literal.orEmpty().trim(), lineOf(node))

            is YamlFrontMatterBlock -> {
                val entries = ArrayList<Pair<String, String>>()
                var c = node.firstChild
                while (c != null) {
                    if (c is YamlFrontMatterNode) {
                        entries += c.key to c.values.orEmpty().joinToString(" ")
                    }
                    c = c.next
                }
                blocks += MdBlock.FrontMatter(
                    entries = entries,
                    raw = entries.joinToString("\n") { "${it.first}: ${it.second}" },
                    line = lineOf(node),
                )
            }

            is TableBlock -> table(node)

            is ListItem -> {
                var c = node.firstChild
                while (c != null) {
                    block(c, depth)
                    c = c.next
                }
            }

            else -> walk(node, depth)
        }
    }

    private fun paragraph(node: Node) {
        val raw = rawTextOf(node).trim()
        if (raw.length > 4 && raw.startsWith("$$") && raw.endsWith("$$")) {
            val body = raw.substring(2, raw.length - 2).trim()
            if (body.isNotEmpty()) {
                blocks += MdBlock.Formula(body, true, lineOf(node))
                return
            }
        }
        val kids = childrenOf(node)
        if (kids.size == 1) {
            val only = kids.first()
            if (only is Image) {
                blocks += MdBlock.Image(only.destination.orEmpty(), altOf(only), lineOf(node))
                return
            }
            if (only is MathNode && only.display) {
                blocks += MdBlock.Formula(only.text, true, lineOf(node))
                return
            }
        }
        blocks += MdBlock.Para(inlines(node), lineOf(node))
    }

    private fun list(listNode: Node, depth: Int, marker: Char?) {
        val start = (listNode as? OrderedList)?.startNumber ?: 1
        var index = 0
        var item = listNode.firstChild
        while (item != null) {
            if (item is ListItem) {
                listItem(item, depth + 1, if (marker != null) marker.toString() else "${start + index}.")
                index++
            }
            item = item.next
        }
    }

    private fun listItem(item: ListItem, depth: Int, marker: String) {
        val first = item.firstChild
        if (first is TaskListItemMarker) {
            val content = ArrayList<Inline>()
            var child = first.next
            while (child != null && child !is org.commonmark.node.Block) {
                content += inlineOf(child)
                child = child.next
            }
            blocks += MdBlock.Task(
                depth = depth,
                checked = first.isChecked,
                content = content,
                markerIndex = checkboxIndex(first, lineOf(item)),
                line = lineOf(item),
            )
            while (child != null) {
                when (child) {
                    is BulletList -> list(child, depth, child.bulletMarker)
                    is OrderedList -> list(child, depth, null)
                    else -> block(child, depth)
                }
                child = child.next
            }
            return
        }
        var firstParagraph = true
        var c = item.firstChild
        while (c != null) {
            when (c) {
                is Paragraph -> {
                    val content = inlines(c)
                    if (firstParagraph) {
                        if (marker.endsWith(".") && marker.dropLast(1).all { it.isDigit() }) {
                            blocks += MdBlock.Ordered(depth, marker.dropLast(1), content, lineOf(c))
                        } else {
                            blocks += MdBlock.Bullet(depth, marker.firstOrNull() ?: '\u2022', content, lineOf(c))
                        }
                        firstParagraph = false
                    } else {
                        blocks += MdBlock.Para(content, lineOf(c))
                    }
                }
                is BulletList -> list(c, depth, c.bulletMarker)
                is OrderedList -> list(c, depth, null)
                else -> block(c, depth)
            }
            c = c.next
        }
    }

    private fun table(node: TableBlock) {
        var header: List<List<Inline>> = emptyList()
        val rows = ArrayList<List<List<Inline>>>()
        var n = node.firstChild
        while (n != null) {
            when (n) {
                is TableHead -> header = cells(n.firstChild)
                is TableBody -> {
                    var r = n.firstChild
                    while (r != null) {
                        if (r is TableRow) rows += cells(r)
                        r = r.next
                    }
                }
                is TableRow -> rows += cells(n)
                else -> Unit
            }
            n = n.next
        }
        if (header.isNotEmpty() || rows.isNotEmpty()) {
            blocks += MdBlock.Table(header, rows, lineOf(node))
        }
    }

    private fun cells(row: Node?): List<List<Inline>> {
        val out = ArrayList<List<Inline>>()
        var c = row?.firstChild
        while (c != null) {
            if (c is TableCell) out += inlines(c)
            c = c.next
        }
        return out
    }

    /* -------------------------------------------------- 行内 */

    private fun childrenOf(node: Node): List<Node> {
        val out = ArrayList<Node>(4)
        var c = node.firstChild
        while (c != null) {
            out += c
            c = c.next
        }
        return out
    }

    private fun inlines(parent: Node): List<Inline> = inlinesBetween(parent.firstChild, null)

    private fun inlinesBetween(first: Node?, endExclusive: Node?): List<Inline> {
        val out = ArrayList<Inline>()
        var n = first
        while (n != null && n !== endExclusive) {
            out += inlineOf(n)
            n = n.next
        }
        return out
    }

    private fun inlineOf(node: Node): List<Inline> = when (node) {
        is Text -> ArrayList<Inline>(2).also { appendText(it, node.literal.orEmpty()) }
        is Emphasis -> listOf(Inline.Emph(inlines(node)))
        is StrongEmphasis -> listOf(Inline.Strong(inlines(node)))
        is Strikethrough -> listOf(Inline.Strike(inlines(node)))
        is HighlightNode -> listOf(Inline.Highlight(inlines(node)))
        is MathNode -> listOf(Inline.Math(node.text, node.display))
        is Code -> listOf(Inline.Code(node.literal.orEmpty()))
        is Link -> listOf(Inline.Link(node.destination.orEmpty(), inlines(node)))
        is Image -> listOf(Inline.ImageRef(node.destination.orEmpty(), altOf(node), node.title))
        is SoftLineBreak -> listOf(Inline.Break(false))
        is HardLineBreak -> listOf(Inline.Break(true))
        is HtmlInline -> if (node.literal.orEmpty().startsWith("<br", true)) listOf(Inline.Break(true)) else emptyList()
        else -> inlines(node)
    }

    private fun appendText(out: MutableList<Inline>, text: String) {
        if (text.isEmpty()) return
        var cursor = 0
        for (m in FOOTNOTE_REF.findAll(text)) {
            if (m.range.first > cursor) out += Inline.Text(text.substring(cursor, m.range.first))
            out += Inline.FootnoteRef(m.groupValues[1])
            cursor = m.range.last + 1
        }
        if (cursor < text.length) out += Inline.Text(text.substring(cursor))
    }

    private fun altOf(image: Image): String {
        val sb = StringBuilder()
        var c = image.firstChild
        while (c != null) {
            if (c is Text) sb.append(c.literal)
            c = c.next
        }
        return sb.toString()
    }
}
