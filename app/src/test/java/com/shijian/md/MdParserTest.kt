package com.shijian.md

import com.shijian.md.md.Inline
import com.shijian.md.md.MdBlock
import com.shijian.md.md.MdParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MdParserTest {

    @Test
    fun headingsFeedOutline() {
        val doc = MdParser.parse("# 一\n\n正文\n\n## 二\n\n### 三\n")
        assertEquals(3, doc.outline.size)
        assertEquals(1, doc.outline[0].level)
        assertEquals("一", doc.outline[0].title)
        assertEquals(2, doc.outline[1].level)
        assertEquals(3, doc.outline[2].level)
        assertEquals("一", doc.title)
    }

    @Test
    fun taskMarkerIndexPointsAtCheckbox() {
        val src = "- [ ] 待办\n- [x] 已完成\n  - [X] 缩进项\n"
        val doc = MdParser.parse(src)
        val tasks = doc.blocks.filterIsInstance<MdBlock.Task>()
        assertEquals(3, tasks.size)
        assertFalse(tasks[0].checked)
        assertTrue(tasks[1].checked)
        assertTrue(tasks[2].checked)
        assertEquals(2, tasks[2].depth)
        assertTrue("idx0=${tasks[0].markerIndex}", src[tasks[0].markerIndex] == '[')
        assertTrue("char0=${src[tasks[0].markerIndex + 1]}", src[tasks[0].markerIndex + 1] == ' ')
        assertTrue("char1=${src[tasks[1].markerIndex + 1]}", src[tasks[1].markerIndex + 1] == 'x')
        assertTrue("idx2=${tasks[2].markerIndex}", src[tasks[2].markerIndex] == '[')
    }

    @Test
    fun togglingTaskRewritesOnlyTheCheckbox() {
        val src = "- [ ] 待办 A\n- [ ] 待办 B\n"
        val task = MdParser.parse(src).blocks.filterIsInstance<MdBlock.Task>().first()
        val chars = src.toCharArray()
        chars[task.markerIndex + 1] = 'x'
        val updated = String(chars)
        assertTrue(updated.contains("- [x] 待办 A"))
        assertTrue(updated.contains("- [ ] 待办 B"))
        assertTrue(
            "updated=$updated markerIndex=${task.markerIndex}",
            MdParser.parse(updated).blocks.filterIsInstance<MdBlock.Task>().first().checked,
        )
    }

    @Test
    fun tablesAreParsed() {
        val doc = MdParser.parse(
            """
            | 名称 | 数量 |
            | --- | --- |
            | 苹果 | 3 |
            | 梨 | 5 |
            """.trimIndent()
        )
        val table = doc.blocks.filterIsInstance<MdBlock.Table>().first()
        assertEquals(2, table.header.size)
        assertEquals(2, table.rows.size)
        assertEquals("名称", table.header[0].joinToString("") { render(it) })
        assertEquals("苹果", table.rows[0][0].joinToString("") { render(it) })
    }

    @Test
    fun fencedCodeKeepsLanguageAndBody() {
        val doc = MdParser.parse("```kotlin\nval a = 1\n```\n")
        val code = doc.blocks.filterIsInstance<MdBlock.Code>().first()
        assertEquals("kotlin", code.language)
        assertEquals("val a = 1\n", code.code)
    }

    @Test
    fun highlightAndStrikeAndCode() {
        val doc = MdParser.parse("段落里有 ==高亮==、~~删除~~ 和 `代码`。\n")
        val para = doc.blocks.filterIsInstance<MdBlock.Para>().first()
        assertTrue(para.content.any { it is Inline.Highlight })
        assertTrue(para.content.any { it is Inline.Strike })
        assertTrue(para.content.any { it is Inline.Code })
    }

    @Test
    fun inlineMathIsRecognisedButPricesAreNot() {
        val withMath = MdParser.parse("勾股定理 \$a^2+b^2=c^2\$ 成立。\n")
        val para = withMath.blocks.filterIsInstance<MdBlock.Para>().first()
        assertTrue(para.content.any { it is Inline.Math })

        val price = MdParser.parse("这本书 \$5 元，那本 \$8 元。\n")
        val pricePara = price.blocks.filterIsInstance<MdBlock.Para>().first()
        assertFalse(pricePara.content.any { it is Inline.Math })
    }

    @Test
    fun displayMathBecomesItsOwnBlock() {
        val doc = MdParser.parse("\$\$\nE = mc^2\n\$\$\n")
        assertTrue(
            "blocks=${doc.blocks.map { it::class.simpleName }}",
            doc.blocks.any { it is MdBlock.Formula },
        )
    }

    @Test
    fun quotesCarryDepth() {
        val doc = MdParser.parse("> 第一层\n>\n> > 第二层\n")
        val quotes = doc.blocks.filterIsInstance<MdBlock.Quote>()
        assertTrue(quotes.isNotEmpty())
        assertEquals(1, quotes.first().depth)
        assertTrue(quotes.any { it.depth == 2 })
    }

    @Test
    fun frontMatterIsCollected() {
        val doc = MdParser.parse("---\ntitle: 拾简\n tags: a, b\n---\n\n# 正文\n")
        val fm = doc.blocks.filterIsInstance<MdBlock.FrontMatter>().first()
        assertTrue(fm.entries.any { it.first == "title" && it.second == "拾简" })
        assertTrue(doc.blocks.any { it is MdBlock.Heading })
    }

    @Test
    fun standaloneImageBecomesBlockAndInlineImageStaysInline() {
        val block = MdParser.parse("![封面](cover.png)\n").blocks.filterIsInstance<MdBlock.Image>().first()
        assertEquals("cover.png", block.url)
        assertEquals("封面", block.alt)

        val inline = MdParser.parse("文字 ![小图](a.png) 之后\n").blocks.filterIsInstance<MdBlock.Para>().first()
        assertTrue(inline.content.any { it is Inline.ImageRef })
    }

    @Test
    fun linksAndFootnotes() {
        val doc = MdParser.parse("见 [官网](https://example.com) 与 [^note]。\n")
        val para = doc.blocks.filterIsInstance<MdBlock.Para>().first()
        val link = para.content.filterIsInstance<Inline.Link>().first()
        assertEquals("https://example.com", link.url)
        assertTrue(para.content.any { it is Inline.FootnoteRef })
    }

    @Test
    fun kbdTagBecomesInlineCode() {
        val doc = MdParser.parse("按 <kbd>Ctrl</kbd> 加 <kbd>S</kbd> 保存。\n")
        val para = doc.blocks.filterIsInstance<MdBlock.Para>().first()
        assertEquals(2, para.content.filterIsInstance<Inline.Code>().size)
    }

    @Test
    fun listMarkersAndOrderedNumbers() {
        val doc = MdParser.parse("- 甲\n- 乙\n\n1. 一\n2. 二\n")
        val bullets = doc.blocks.filterIsInstance<MdBlock.Bullet>()
        val ordered = doc.blocks.filterIsInstance<MdBlock.Ordered>()
        assertEquals(2, bullets.size)
        assertEquals(2, ordered.size)
        assertEquals("1", ordered[0].number)
        assertEquals("2", ordered[1].number)
    }

    @Test
    fun blankDocumentParsesToEmpty() {
        assertTrue(MdParser.parse("").isEmpty)
        assertTrue(MdParser.parse("   \n\n").isEmpty)
    }

    @Test
    fun wordCountCountsCjkAndLatin() {
        val doc = MdParser.parse("拾简 markdown 编辑器\n")
        assertTrue(doc.wordCount >= 4)
    }

    private fun render(inline: Inline): String = when (inline) {
        is Inline.Text -> inline.text
        is Inline.Code -> inline.text
        is Inline.Emph -> inline.children.joinToString("") { render(it) }
        is Inline.Strong -> inline.children.joinToString("") { render(it) }
        is Inline.Strike -> inline.children.joinToString("") { render(it) }
        is Inline.Highlight -> inline.children.joinToString("") { render(it) }
        is Inline.Link -> inline.children.joinToString("") { render(it) }
        else -> ""
    }
}
