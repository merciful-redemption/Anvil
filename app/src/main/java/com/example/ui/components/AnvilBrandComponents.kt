package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnvilEmber
import com.example.ui.theme.AnvilGold
import com.example.ui.theme.AnvilIndigo
import com.example.ui.theme.AnvilMint
import com.example.ui.theme.AnvilSky
import com.example.ui.theme.AnvilViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Normative Anvil v5.0 Mark (Part 2.2 SVG rendered via Compose Canvas)
 */
@Composable
fun AnvilMarkLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Anvil mark logo" }
    ) {
        val w = this.size.width
        val h = this.size.height
        val sx = w / 200f
        val sy = h / 160f

        val anvilPath = Path().apply {
            moveTo(30f * sx, 100f * sy)
            lineTo(30f * sx, 115f * sy)
            lineTo(45f * sx, 115f * sy)
            lineTo(45f * sx, 130f * sy)
            lineTo(155f * sx, 130f * sy)
            lineTo(155f * sx, 115f * sy)
            lineTo(170f * sx, 115f * sy)
            lineTo(170f * sx, 100f * sy)
            lineTo(140f * sx, 100f * sy)
            lineTo(140f * sx, 85f * sy)
            lineTo(120f * sx, 85f * sy)
            lineTo(120f * sx, 70f * sy)
            lineTo(80f * sx, 70f * sy)
            lineTo(80f * sx, 85f * sy)
            lineTo(60f * sx, 85f * sy)
            lineTo(60f * sx, 100f * sy)
            close()
        }

        drawPath(
            path = anvilPath,
            color = Color(0xFF7B8FA6),
            style = Stroke(width = 10f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Strike surface in Anvil Indigo (#4C5BD4)
        drawLine(
            color = AnvilIndigo,
            start = Offset(60f * sx, 70f * sy),
            end = Offset(140f * sx, 70f * sy),
            strokeWidth = 11f * sx,
            cap = StrokeCap.Round
        )

        // Hovering Amber Spark (#FF9F43)
        drawCircle(
            color = AnvilEmber,
            radius = 8f * sx,
            center = Offset(100f * sx, 46f * sy)
        )
        drawLine(
            color = AnvilEmber,
            start = Offset(100f * sx, 30f * sy),
            end = Offset(100f * sx, 20f * sy),
            strokeWidth = 4.5f * sx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = AnvilEmber,
            start = Offset(113f * sx, 37f * sy),
            end = Offset(122f * sx, 30f * sy),
            strokeWidth = 4.5f * sx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = AnvilEmber,
            start = Offset(87f * sx, 37f * sy),
            end = Offset(78f * sx, 30f * sy),
            strokeWidth = 4.5f * sx,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Persistent Command Bar at the top of every screen (Part 5.2)
 */
@Composable
fun AnvilCommandBar(
    calmMode: Boolean,
    onSearchClick: () -> Unit,
    onAiTutorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnvilMarkLogo(size = 36.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ANVIL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "v5.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (calmMode) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AnvilMint.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Spa,
                                        contentDescription = "Calm Mode Active",
                                        tint = AnvilMint,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Calm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AnvilMint
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Strike. Shape. Master.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("command_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search curriculum, concepts, and glossary",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onAiTutorClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("command_ai_tutor_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Open Gemini AI Tutor and Studio",
                        tint = AnvilViolet
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("command_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Open Settings and Theme Catalog",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Syntax-Highlighted Code Editor + Mobile Code Keyboard Bar (Parts 5.13 & 5.14)
 */
@Composable
fun AnvilCodeEditorWithKeyboardBar(
    code: String,
    language: String,
    onCodeChange: (String) -> Unit,
    onInsertToken: (String) -> Unit,
    onRunClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "code_editor"
) {
    val row1Tokens = listOf("    ", "(", ")", "{", "}", "[", "]", ":", ";", "=", "+", "-", "*", "**", "==", "<=", "\"", "'")
    val row2Actions = listOf("print()", "def ", "return ", "SELECT ", "WHERE ", "=> ")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0E131A))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
    ) {
        // Editor Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF171E29))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AnvilMint)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$language Sandbox • UTF-8",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFA3B1C6)
                )
            }
            Surface(
                color = AnvilIndigo,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .clickable(onClick = onRunClick)
                    .testTag("${testTagPrefix}_run_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run Code",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Run",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Code Buffer with Line Numbers
        val lines = code.lines().ifEmpty { listOf("") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(end = 10.dp),
                horizontalAlignment = Alignment.End
            ) {
                lines.indices.forEach { idx ->
                    Text(
                        text = "${idx + 1}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF526075)
                    )
                }
            }
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFFE6EDF3)
                ),
                cursorBrush = SolidColor(AnvilEmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("${testTagPrefix}_input")
            )
        }

        // Syntax Preview Strip
        Text(
            text = highlightSyntax(code),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121822))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        // Part 5.14 Mobile Code Keyboard Bar (2 configurable rows)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF171E29))
                .padding(vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row1Tokens.forEach { tok ->
                    val label = if (tok == "    ") "⇥ Tab" else tok
                    Surface(
                        color = Color(0xFF242E3D),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onInsertToken(tok) }
                    ) {
                        Text(
                            text = label,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row2Actions.forEach { snip ->
                    Surface(
                        color = AnvilIndigo.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onInsertToken(snip) }
                    ) {
                        Text(
                            text = snip.trim(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = AnvilSky,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun highlightSyntax(code: String) = buildAnnotatedString {
    val keywords = setOf("def", "return", "for", "in", "if", "else", "print", "const", "let", "SELECT", "FROM", "WHERE", "ORDER", "BY", "sum", "int")
    val tokens = code.replace("\n", " ↵ ").split(" ")
    tokens.forEachIndexed { idx, word ->
        val color = when {
            word in keywords -> AnvilViolet
            word.any { it.isDigit() } -> AnvilEmber
            word.startsWith("#") || word.startsWith("//") -> Color(0xFF6B7B93)
            word.contains("(") || word.contains(")") -> AnvilSky
            else -> AnvilMint
        }
        withStyle(SpanStyle(color = color)) {
            append(word)
        }
        if (idx < tokens.lastIndex) append(" ")
    }
}

/**
 * MIPR Page 2 Proficiency Radar Chart (Part 23.8)
 */
@Composable
fun MiprProficiencyRadarChart(
    domainScores: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .semantics { contentDescription = "Proficiency radar chart across computer science domains" }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = (size.minDimension / 2f) * 0.78f
        val n = domainScores.size.coerceAtLeast(3)

        // Concentric grid rings (25%, 50%, 75%, 100%)
        listOf(0.25f, 0.50f, 0.75f, 1.0f).forEach { level ->
            drawCircle(
                color = Color(0xFF4C5BD4).copy(alpha = 0.22f),
                radius = radius * level,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )
        }

        // Spokes and data polygon
        val polyPath = Path()
        domainScores.forEachIndexed { i, (_, score) ->
            val angle = (2.0 * PI * i / n) - (PI / 2.0)
            val endX = cx + (radius * cos(angle)).toFloat()
            val endY = cy + (radius * sin(angle)).toFloat()
            drawLine(
                color = Color(0xFF5B6575).copy(alpha = 0.35f),
                start = Offset(cx, cy),
                end = Offset(endX, endY),
                strokeWidth = 1.5f
            )

            val clamped = score.coerceIn(0.15f, 1.0f)
            val px = cx + (radius * clamped * cos(angle)).toFloat()
            val py = cy + (radius * clamped * sin(angle)).toFloat()
            if (i == 0) polyPath.moveTo(px, py) else polyPath.lineTo(px, py)
        }
        polyPath.close()

        drawPath(
            path = polyPath,
            color = AnvilIndigo.copy(alpha = 0.35f)
        )
        drawPath(
            path = polyPath,
            color = AnvilEmber,
            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Vertex nodes
        domainScores.forEachIndexed { i, (_, score) ->
            val angle = (2.0 * PI * i / n) - (PI / 2.0)
            val clamped = score.coerceIn(0.15f, 1.0f)
            val px = cx + (radius * clamped * cos(angle)).toFloat()
            val py = cy + (radius * clamped * sin(angle)).toFloat()
            drawCircle(color = AnvilGold, radius = 6f, center = Offset(px, py))
        }
    }
}
