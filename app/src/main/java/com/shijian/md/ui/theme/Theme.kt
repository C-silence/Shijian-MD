package com.shijian.md.ui.theme

import android.graphics.Color as GColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/* ------------------------------------------------------------------ 装饰与纹理 */

enum class PatternKind { NONE, GRID, CROSS, DOT, STAR, HEX, TRI }

enum class DecorationLevel { FULL, RESTRAINED, MINIMAL }

enum class DarkMode { SYSTEM, LIGHT, DARK }

/* ------------------------------------------------------------------ 令牌 */

@Immutable
data class SyntaxColors(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val function: Color,
    val variable: Color,
    val tag: Color,
    val punctuation: Color,
)

@Immutable
data class MdColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryDeep: Color,
    val primarySoft: Color,
    val primaryWhisper: Color,
    val link: Color,
    val codeInlineFg: Color,
    val codeInlineBg: Color,
    val codeBlockBg: Color,
    val codeHeaderBg: Color,
    val codeFg: Color,
    val shadow: Color,
    val syntax: SyntaxColors,
)

@Immutable
data class MdTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,
    val h5: TextStyle,
    val h6: TextStyle,
    val body: TextStyle,
    val small: TextStyle,
    val caption: TextStyle,
    val code: TextStyle,
    val inlineCode: TextStyle,
    val kbd: TextStyle,
    val title: TextStyle,
    val label: TextStyle,
)

@Immutable
data class MdShapes(val xs: Dp, val s: Dp, val m: Dp, val l: Dp)

@Immutable
data class MdSpacing(
    val page: Dp,
    val pageTablet: Dp,
    val block: Dp,
    val paragraph: Dp,
    val contentMaxWidth: Dp,
    val touch: Dp,
    val patternCell: Dp,
)

@Immutable
data class ThemeSpec(
    val id: String,
    val familyId: String,
    val name: String,
    val isDark: Boolean,
    val pattern: PatternKind,
    val decoration: DecorationLevel,
    val colors: MdColors,
    val typography: MdTypography,
    val shapes: MdShapes,
    val spacing: MdSpacing,
)

val LocalMdTheme = staticCompositionLocalOf { Themes.spec(Themes.DEFAULT_FAMILY, false, DecorationLevel.RESTRAINED) }

/* ------------------------------------------------------------------ 色板定义 */

private class Family(
    val id: String,
    val name: String,
    val primary: Long,
    val deep: Long,
    val soft: Long,
    val whisper: Long,
    val bg: Long,
    val surface: Long,
    val line: Long,
    val text: Long,
    val text2: Long,
    val muted: Long,
    val pattern: PatternKind,
    val darkOnly: Boolean = false,
)

private val FAMILIES = listOf(
    Family("sakura", "樱", 0xFFE2708F, 0xFFA83C5F, 0xFFFBE6EC, 0xFFFFF1F5, 0xFFFFF9FB, 0xFFFFFFFF, 0xFFEBD8DE, 0xFF2E2A2C, 0xFF57504F, 0xFF8C8286, PatternKind.GRID),
    Family("caramel", "焦糖", 0xFFC07A4C, 0xFF8A4D28, 0xFFF7E9DD, 0xFFFDF4EC, 0xFFFDF9F5, 0xFFFFFFFF, 0xFFEBDCCE, 0xFF332C27, 0xFF5C5148, 0xFF8F8479, PatternKind.CROSS),
    Family("mint", "薄荷", 0xFF35A79F, 0xFF137069, 0xFFE0F4F2, 0xFFEFFAF9, 0xFFF7FCFB, 0xFFFFFFFF, 0xFFD3E7E5, 0xFF26302F, 0xFF4E5C5B, 0xFF7E8C8B, PatternKind.DOT),
    Family("sky", "天青", 0xFF3F86C6, 0xFF1E5C90, 0xFFE4F0FA, 0xFFF0F7FD, 0xFFF8FBFF, 0xFFFFFFFF, 0xFFD6E5F2, 0xFF262F38, 0xFF4C5863, 0xFF7D8A96, PatternKind.CROSS),
    Family("forest", "森", 0xFF2F8C60, 0xFF175F41, 0xFFE2F2EA, 0xFFEFF9F4, 0xFFF7FBF9, 0xFFFFFFFF, 0xFFD6E8DE, 0xFF23302A, 0xFF4A5A52, 0xFF7B8A83, PatternKind.HEX),
    Family("mauve", "紫藤", 0xFF8A66A6, 0xFF583A72, 0xFFEFE7F6, 0xFFF7F2FB, 0xFFFAF8FC, 0xFFFFFFFF, 0xFFE2D8EC, 0xFF2E2735, 0xFF554C5E, 0xFF877E90, PatternKind.STAR),
    Family("prussian", "靛", 0xFF2C5E93, 0xFF133A5E, 0xFFE3EDF6, 0xFFF0F5FA, 0xFFF7FAFD, 0xFFFFFFFF, 0xFFD5E2EE, 0xFF232D38, 0xFF495562, 0xFF7A8794, PatternKind.GRID),
    Family("cherry", "樱红", 0xFFB0334D, 0xFF7C1B33, 0xFFF9E3E7, 0xFFFDF0F2, 0xFFFFF8F9, 0xFFFFFFFF, 0xFFEDD5DA, 0xFF332527, 0xFF5B4A4D, 0xFF8E7C80, PatternKind.CROSS),
    Family("slate", "石墨", 0xFF5A6B7C, 0xFF33414F, 0xFFE9EDF1, 0xFFF3F6F8, 0xFFF7F9FA, 0xFFFFFFFF, 0xFFDCE3E9, 0xFF252B31, 0xFF4C555E, 0xFF7C858E, PatternKind.DOT),
    // 原生深色族（下面的取值本身就是深色）
    Family("pine", "松烟", 0xFFD9A470, 0xFFE9C29A, 0xFF2E2822, 0xFF26221E, 0xFF1B1917, 0xFF232120, 0xFF34302C, 0xFFE6E3DF, 0xFFBDB8B2, 0xFF8C8781, PatternKind.CROSS, darkOnly = true),
    Family("night", "夜幕", 0xFF8AA9E0, 0xFFB7CDF2, 0xFF23283A, 0xFF1E2231, 0xFF15171C, 0xFF1C1F27, 0xFF2C3140, 0xFFE2E5EC, 0xFFB6BCC9, 0xFF848B99, PatternKind.STAR, darkOnly = true),
    Family("oled", "墨黑", 0xFF8FB4E8, 0xFFBED3F5, 0xFF14171C, 0xFF101216, 0xFF000000, 0xFF0D0F12, 0xFF22262C, 0xFFE4E7EC, 0xFFB3B9C2, 0xFF7E858F, PatternKind.NONE, darkOnly = true),
)

data class ThemeFamilyInfo(val id: String, val name: String, val primary: Long, val background: Long, val darkOnly: Boolean)

object Themes {
    const val DEFAULT_FAMILY = "sakura"

    val families: List<ThemeFamilyInfo> = FAMILIES.map {
        val dark = it.darkOnly
        ThemeFamilyInfo(
            id = it.id,
            name = it.name,
            primary = if (dark) it.primary else it.primary,
            background = if (dark) it.bg else it.bg,
            darkOnly = dark,
        )
    }

    private val cache = HashMap<String, ThemeSpec>()

    fun spec(familyId: String, dark: Boolean, decoration: DecorationLevel = DecorationLevel.RESTRAINED, bodySize: Float = 16f, eyeCare: Boolean = false): ThemeSpec {
        val key = "$familyId|$dark|$decoration|$bodySize|$eyeCare"
        return cache.getOrPut(key) {
            val f = FAMILIES.firstOrNull { it.id == familyId } ?: FAMILIES.first()
            val isDark = dark || f.darkOnly
            val colors = if (f.darkOnly) darkOnlyColors(f) else if (isDark) derivedDark(f) else lightColors(f)
            ThemeSpec(
                id = key,
                familyId = f.id,
                name = f.name,
                isDark = isDark,
                pattern = f.pattern,
                decoration = decoration,
                colors = if (eyeCare) eyeCareColors(colors, isDark) else colors,
                typography = typography(bodySize),
                shapes = MdShapes(4.dp, 8.dp, 12.dp, 16.dp),
                spacing = MdSpacing(16.dp, 28.dp, 12.dp, 10.dp, 720.dp, 48.dp, 20.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------ 色板派生 */

private fun Long.toColor() = Color(this)

private fun shift(argb: Long, sMul: Float = 1f, vAdd: Float = 0f, vCap: Float? = null): Long {
    val hsv = FloatArray(3)
    GColor.colorToHSV(argb.toInt(), hsv)
    hsv[1] = (hsv[1] * sMul).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] + vAdd).coerceIn(0f, 1f)
    if (vCap != null) hsv[2] = hsv[2].coerceAtMost(vCap)
    return (GColor.HSVToColor(hsv).toLong() and 0xFFFFFFFFL)
}

private fun darkTint(seed: Long, s: Float, v: Float): Long {
    val hsv = FloatArray(3)
    GColor.colorToHSV(seed.toInt(), hsv)
    return (GColor.HSVToColor(floatArrayOf(hsv[0], s, v)).toLong() and 0xFFFFFFFFL)
}

private val LIGHT_SYNTAX = SyntaxColors(
    keyword = Color(0xFFA626A4), string = Color(0xFF3E8E41), number = Color(0xFF1694B6),
    comment = Color(0xFF9A9A92), function = Color(0xFFB07A00), variable = Color(0xFFC05B5B),
    tag = Color(0xFF4E8E8E), punctuation = Color(0xFF6B7280),
)

private val DARK_SYNTAX = SyntaxColors(
    keyword = Color(0xFFD9A8DE), string = Color(0xFF8ECF8A), number = Color(0xFF7FC7DE),
    comment = Color(0xFF7E838C), function = Color(0xFFE3C170), variable = Color(0xFFE39A9A),
    tag = Color(0xFF8FC4C4), punctuation = Color(0xFFA8AFB9),
)

private fun lightColors(f: Family) = MdColors(
    background = f.bg.toColor(),
    surface = f.surface.toColor(),
    surfaceVariant = f.soft.toColor(),
    outline = f.line.toColor(),
    onBackground = f.text.toColor(),
    onSurfaceVariant = f.text2.toColor(),
    muted = f.muted.toColor(),
    primary = f.primary.toColor(),
    onPrimary = Color(0xFFFFFFFF),
    primaryDeep = f.deep.toColor(),
    primarySoft = f.soft.toColor(),
    primaryWhisper = f.whisper.toColor(),
    link = f.deep.toColor(),
    codeInlineFg = f.deep.toColor(),
    codeInlineBg = f.soft.toColor(),
    codeBlockBg = blend(f.bg, f.primary, 0.035f).toColor(),
    codeHeaderBg = blend(f.bg, f.primary, 0.075f).toColor(),
    codeFg = Color(0xFF4A4144),
    shadow = Color(0x14000000),
    syntax = LIGHT_SYNTAX,
)

private fun derivedDark(f: Family): MdColors {
    val seed = f.primary
    val bg = darkTint(seed, 0.07f, 0.105f)
    val surface = darkTint(seed, 0.07f, 0.145f)
    val soft = darkTint(seed, 0.26f, 0.24f)
    val whisper = darkTint(seed, 0.20f, 0.175f)
    return MdColors(
        background = bg.toColor(),
        surface = surface.toColor(),
        surfaceVariant = soft.toColor(),
        outline = darkTint(seed, 0.12f, 0.28f).toColor(),
        onBackground = Color(0xFFE4E6EB),
        onSurfaceVariant = Color(0xFFB7BCC5),
        muted = Color(0xFF878D97),
        primary = shift(seed, 0.80f, 0.22f).toColor(),
        onPrimary = Color(0xFF1A1116),
        primaryDeep = shift(f.deep, 0.62f, 0.44f).toColor(),
        primarySoft = soft.toColor(),
        primaryWhisper = whisper.toColor(),
        link = shift(f.deep, 0.62f, 0.44f).toColor(),
        codeInlineFg = shift(f.deep, 0.55f, 0.50f).toColor(),
        codeInlineBg = darkTint(seed, 0.22f, 0.20f).toColor(),
        codeBlockBg = darkTint(seed, 0.06f, 0.13f).toColor(),
        codeHeaderBg = darkTint(seed, 0.08f, 0.175f).toColor(),
        codeFg = Color(0xFFC9CDD4),
        shadow = Color(0x66000000),
        syntax = DARK_SYNTAX,
    )
}

private fun darkOnlyColors(f: Family) = MdColors(
    background = f.bg.toColor(),
    surface = f.surface.toColor(),
    surfaceVariant = f.soft.toColor(),
    outline = f.line.toColor(),
    onBackground = f.text.toColor(),
    onSurfaceVariant = f.text2.toColor(),
    muted = f.muted.toColor(),
    primary = f.primary.toColor(),
    onPrimary = Color(0xFF141210),
    primaryDeep = f.deep.toColor(),
    primarySoft = f.soft.toColor(),
    primaryWhisper = f.whisper.toColor(),
    link = f.deep.toColor(),
    codeInlineFg = f.deep.toColor(),
    codeInlineBg = f.soft.toColor(),
    codeBlockBg = blend(f.bg, 0xFFFFFFFF, 0.06f).toColor(),
    codeHeaderBg = blend(f.bg, 0xFFFFFFFF, 0.10f).toColor(),
    codeFg = Color(0xFFC9CDD4),
    shadow = Color(0x66000000),
    syntax = DARK_SYNTAX,
)

private fun eyeCareColors(c: MdColors, isDark: Boolean): MdColors = if (isDark) c else c.copy(
    background = Color(0xFFF6F0E4),
    surface = Color(0xFFFBF6EC),
    codeBlockBg = Color(0xFFF1E9DA),
    codeHeaderBg = Color(0xFFEADFCB),
    onBackground = Color(0xFF3A342C),
    onSurfaceVariant = Color(0xFF5E5548),
)

private fun blend(a: Long, b: Long, t: Float): Long {
    val ar = (a shr 16 and 0xFF) / 255f; val ag = (a shr 8 and 0xFF) / 255f; val ab = (a and 0xFF) / 255f
    val br = (b shr 16 and 0xFF) / 255f; val bg = (b shr 8 and 0xFF) / 255f; val bb = (b and 0xFF) / 255f
    val r = (ar + (br - ar) * t * 255).toLong()
    val g = (ag + (bg - ag) * t * 255).toLong()
    val bl = (ab + (bb - ab) * t * 255).toLong()
    return (0xFFL shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (bl and 0xFF)
}

/* ------------------------------------------------------------------ 字体 */

/**
 * 派生字号的绝对下限：正文字号最低可以调到 10sp，但比 10sp 更小的字在手机上已经读不动了。
 * 字号 ≥ 12.5sp 时所有派生字号都在这条线之上，因此不影响既有观感。
 */
private const val MIN_TEXT_SP = 10f

private fun typography(bodySize: Float): MdTypography {
    fun t(size: Float, weight: FontWeight = FontWeight.Normal, lineHeight: Float = 1.72f, family: FontFamily = FontFamily.Default): TextStyle {
        val s = size.coerceAtLeast(MIN_TEXT_SP)
        return TextStyle(fontSize = s.sp, fontWeight = weight, lineHeight = (s * lineHeight).sp, fontFamily = family)
    }
    return MdTypography(
        h1 = t(bodySize * 1.62f, FontWeight.Bold, 1.32f),
        h2 = t(bodySize * 1.30f, FontWeight.Bold, 1.42f),
        h3 = t(bodySize * 1.16f, FontWeight.Bold, 1.45f),
        h4 = t(bodySize * 1.05f, FontWeight.Bold, 1.5f),
        h5 = t(bodySize * 1.00f, FontWeight.Bold, 1.5f),
        h6 = t(bodySize * 0.95f, FontWeight.Bold, 1.5f),
        body = t(bodySize, FontWeight.Normal, 1.72f),
        small = t(bodySize * 0.90f, FontWeight.Normal, 1.6f),
        caption = t(bodySize * 0.80f, FontWeight.Normal, 1.5f),
        code = t(bodySize * 0.88f, FontWeight.Normal, 1.62f, FontFamily.Monospace),
        inlineCode = t(bodySize * 0.88f, FontWeight.Normal, 1.5f, FontFamily.Monospace),
        kbd = t(bodySize * 0.82f, FontWeight.Medium, 1.3f, FontFamily.Monospace),
        title = t(bodySize * 1.05f, FontWeight.SemiBold, 1.3f),
        label = t(bodySize * 0.84f, FontWeight.Medium, 1.3f),
    )
}

/* ------------------------------------------------------------------ 纹理背景 */

private fun buildPatternBrush(kind: PatternKind, color: Color, cell: Int): ShaderBrush? {
    if (kind == PatternKind.NONE) return null
    val bmp = ImageBitmap(cell, cell)
    val canvas = Canvas(bmp)
    val strokeW = max(1f, cell / 22f)
    val stroke = Paint().apply {
        this.color = color
        style = PaintingStyle.Stroke
        strokeWidth = strokeW
        isAntiAlias = true
    }
    val fill = Paint().apply {
        this.color = color
        style = PaintingStyle.Fill
        isAntiAlias = true
    }
    val f = cell.toFloat()
    when (kind) {
        PatternKind.GRID -> {
            canvas.drawLine(Offset(0f, 0f), Offset(0f, f), stroke)
            canvas.drawLine(Offset(0f, 0f), Offset(f, 0f), stroke)
        }
        PatternKind.CROSS -> {
            canvas.drawLine(Offset(0f, 0f), Offset(f, f), stroke)
            canvas.drawLine(Offset(f, 0f), Offset(0f, f), stroke)
        }
        PatternKind.DOT -> canvas.drawCircle(Offset(f / 2f, f / 2f), max(1f, f / 18f), fill)
        PatternKind.STAR -> {
            canvas.drawCircle(Offset(f / 2f, f / 2f), max(1f, f / 24f), fill)
            canvas.drawCircle(Offset(0f, 0f), max(1f, f / 30f), fill)
        }
        PatternKind.HEX -> {
            val r = f / 2.6f
            val path = Path()
            for (i in 0 until 6) {
                val angle = Math.toRadians((60 * i - 30).toDouble())
                val x = (f / 2 + r * Math.cos(angle)).toFloat()
                val y = (f / 2 + r * Math.sin(angle)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            canvas.drawPath(path, stroke)
        }
        PatternKind.TRI -> {
            val path = Path().apply {
                moveTo(0f, 0f); lineTo(f / 2f, f / 2f); lineTo(f, 0f); close()
            }
            canvas.drawPath(path, fill)
        }
        PatternKind.NONE -> return null
    }
    return ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
}

@Composable
fun rememberPatternBrush(spec: ThemeSpec): ShaderBrush? {
    val density = LocalDensity.current
    val alpha = when (spec.decoration) {
        DecorationLevel.FULL -> 0.045f
        DecorationLevel.RESTRAINED -> 0.02f
        DecorationLevel.MINIMAL -> 0f
    }
    return remember(spec.id) {
        if (alpha <= 0f) return@remember null
        val cell = with(density) { spec.spacing.patternCell.roundToPx() }.coerceAtLeast(8)
        buildPatternBrush(spec.pattern, spec.colors.primary.copy(alpha = alpha), cell)
    }
}

@Composable
fun MdSurface(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val spec = LocalMdTheme.current
    val brush = rememberPatternBrush(spec)
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(spec.colors.background)
                brush?.let { drawRect(brush = it) }
            },
        content = content,
    )
}

/* ------------------------------------------------------------------ 主题包装 */

@Composable
fun ShijianTheme(spec: ThemeSpec, content: @Composable () -> Unit) {
    val scheme = remember(spec.id) {
        val c = spec.colors
        if (spec.isDark) {
            darkColorScheme(
                primary = c.primary, onPrimary = c.onPrimary,
                background = c.background, onBackground = c.onBackground,
                surface = c.surface, onSurface = c.onBackground,
                surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
                outline = c.outline, error = Color(0xFFB3261E),
            )
        } else {
            lightColorScheme(
                primary = c.primary, onPrimary = c.onPrimary,
                background = c.background, onBackground = c.onBackground,
                surface = c.surface, onSurface = c.onBackground,
                surfaceVariant = c.surfaceVariant, onSurfaceVariant = c.onSurfaceVariant,
                outline = c.outline, error = Color(0xFFB3261E),
            )
        }
    }
    // Material3 1.4 起 MaterialTheme 不再代设 LocalContentColor（默认值是纯黑），
    // 这里必须自己接上，否则依赖环境色的文本在深色下会变成黑底黑字。
    CompositionLocalProvider(
        LocalMdTheme provides spec,
        LocalContentColor provides spec.colors.onBackground,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
