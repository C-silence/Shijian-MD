package com.shijian.md.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.shijian.md.ui.theme.ThemeSpec

/** 轻量语法高亮：够用、可预期、不引入额外依赖。 */
object CodeHighlight {

    private val KEYWORDS = hashSetOf(
        "fun", "val", "var", "if", "else", "when", "for", "while", "return", "class", "object",
        "interface", "import", "package", "private", "public", "internal", "protected", "override",
        "suspend", "null", "true", "false", "this", "super", "try", "catch", "finally", "throw",
        "new", "const", "let", "function", "async", "await", "def", "elif", "lambda", "from", "as",
        "in", "is", "not", "and", "or", "None", "True", "False", "void", "int", "float", "double",
        "char", "boolean", "String", "struct", "enum", "switch", "case", "break", "continue", "do",
        "static", "final", "extends", "implements", "typeof", "instanceof", "export", "default",
        "yield", "with", "print", "echo", "then", "fi", "done", "local", "type", "val", "sealed",
        "data", "companion", "init", "it", "where", "using", "namespace", "select", "from", "insert",
        "update", "delete", "table", "where", "group", "order", "by", "join", "on", "as", "limit",
    )

    private val HASH_COMMENT_LANGS = hashSetOf(
        "python", "py", "sh", "bash", "shell", "zsh", "yaml", "yml", "toml", "ini", "conf", "makefile", "r", "rb", "ruby", "perl",
    )

    private val TOKEN = Regex(
        "(//[^\\n]*)" +                     // 1 行注释
            "|(/\\*[\\s\\S]*?\\*/)" +       // 2 块注释
            "|(\"(?:\\\\.|[^\"\\\\])*\")" + // 3 双引号串
            "|('(?:\\\\.|[^'\\\\])*')" +    // 4 单引号串
            "|(`[^`\\n]*`)" +               // 5 反引号
            "|(\\b\\d[\\d_]*(?:\\.\\d+)?\\b)" + // 6 数字
            "|([A-Za-z_][A-Za-z0-9_]*)",    // 7 标识符
    )

    private val TOKEN_HASH = Regex(
        "(//[^\\n]*|#[^\\n]*)" +
            "|(/\\*[\\s\\S]*?\\*/)" +
            "|(\"(?:\\\\.|[^\"\\\\])*\")" +
            "|('(?:\\\\.|[^'\\\\])*')" +
            "|(`[^`\\n]*`)" +
            "|(\\b\\d[\\d_]*(?:\\.\\d+)?\\b)" +
            "|([A-Za-z_][A-Za-z0-9_]*)",
    )

    fun highlight(code: String, language: String, spec: ThemeSpec): AnnotatedString {
        val s = spec.colors.syntax
        val lang = language.lowercase()
        val hashComments = lang in HASH_COMMENT_LANGS
        val token = if (hashComments) TOKEN_HASH else TOKEN
        return buildAnnotatedString {
            var cursor = 0
            for (m in token.findAll(code)) {
                if (m.range.first > cursor) append(code.substring(cursor, m.range.first))
                val g = m.groupValues
                when {
                    g[1].isNotEmpty() || g[2].isNotEmpty() ->
                        withStyle(SpanStyle(color = s.comment, fontStyle = FontStyle.Italic)) { append(m.value) }

                    g[3].isNotEmpty() || g[4].isNotEmpty() || g[5].isNotEmpty() ->
                        withStyle(SpanStyle(color = s.string)) { append(m.value) }

                    g[6].isNotEmpty() ->
                        withStyle(SpanStyle(color = s.number)) { append(m.value) }

                    else -> {
                        val word = g[7]
                        val nextChar = code.getOrNull(m.range.last + 1)
                        when {
                            word in KEYWORDS ->
                                withStyle(SpanStyle(color = s.keyword, fontWeight = FontWeight.Medium)) { append(word) }

                            nextChar == '(' ->
                                withStyle(SpanStyle(color = s.function)) { append(word) }

                            word.firstOrNull()?.isUpperCase() == true ->
                                withStyle(SpanStyle(color = s.tag)) { append(word) }

                            else -> append(word)
                        }
                    }
                }
                cursor = m.range.last + 1
            }
            if (cursor < code.length) append(code.substring(cursor))
        }
    }
}
