package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.game.BlockColor
import com.example.game.GemType
import com.example.ui.theme.ArenaBottom
import com.example.ui.theme.ArenaMid
import com.example.ui.theme.ArenaTop
import com.example.ui.theme.CreamText
import com.example.ui.theme.CrownGold
import com.example.ui.theme.CrownGoldDeep
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.KraftEnvelope
import com.example.ui.theme.KraftEnvelopeDark
import com.example.ui.theme.LetterInk
import com.example.ui.theme.LetterLine
import com.example.ui.theme.LetterPaper
import com.example.ui.theme.RibbonRed
import com.example.ui.theme.RibbonRedDeep
import com.example.ui.theme.WoodBackgroundBottom
import com.example.ui.theme.WoodBackgroundTop
import com.example.ui.theme.WoodButtonBorder
import com.example.ui.theme.WoodButtonBottom
import com.example.ui.theme.WoodButtonTop
import com.example.ui.theme.WoodPlankHighlight
import com.example.ui.theme.WoodPlankSeam
import com.example.ui.theme.WoodTextDark

@Composable
fun WoodBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(WoodBackgroundTop, WoodBackgroundBottom)
            )
        )
        val plankCount = 7
        val plankWidth = size.width / plankCount
        for (i in 1 until plankCount) {
            val x = i * plankWidth
            drawLine(
                color = WoodPlankSeam.copy(alpha = 0.28f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = WoodPlankHighlight.copy(alpha = 0.16f),
                start = Offset(x + 2.dp.toPx(), 0f),
                end = Offset(x + 2.dp.toPx(), size.height),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        // Subtle grain streaks
        for (i in 0 until 14) {
            val x = (size.width / 14f) * i + (size.width * 0.03f)
            val startY = (i % 3) * size.height * 0.12f
            drawLine(
                color = WoodPlankSeam.copy(alpha = 0.08f),
                start = Offset(x, startY),
                end = Offset(x + 3f, startY + size.height * 0.55f),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun PurpleArenaBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(ArenaTop, ArenaMid, ArenaBottom)
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.09f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.35f),
                radius = size.width * 0.85f
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.5f, size.height * 0.35f)
        )
    }
}

@Composable
fun BlockCrushLogo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val blockColors = listOf(
            Color(0xFF28C748) to Color(0xFF148A2D),
            Color(0xFF00B4CC) to Color(0xFF00788A),
            Color(0xFF2D65E8) to Color(0xFF1A3FA3),
            Color(0xFF9133E6) to Color(0xFF5D18A3),
            Color(0xFFE65319) to Color(0xFFA33006)
        )
        val letters = listOf("B", "L", "O", "C", "K")
        val tilts = listOf(-7f, -3f, 0f, 3f, 7f)

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.offset(y = 10.dp)
        ) {
            letters.forEachIndexed { idx, ch ->
                val (topCol, bottomCol) = blockColors[idx]
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(tilts[idx])
                        .shadow(6.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(topCol, bottomCol)))
                        .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = ch,
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 27.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Mahogany wood plaque with golden CRUSH title
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .shadow(10.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF94411C), Color(0xFF69270D))
                    )
                )
                .border(3.5.dp, Color(0xFFAA5329), RoundedCornerShape(18.dp))
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            // Extruded shadow layer
            Text(
                text = "CRUSH",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                letterSpacing = 2.sp,
                color = Color(0xFF421504),
                modifier = Modifier.offset(x = 0.dp, y = 4.dp)
            )
            // Main golden lettering
            Text(
                text = "CRUSH",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                letterSpacing = 2.sp,
                color = CrownGold
            )
        }
    }
}

@Composable
fun WoodTactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 74.dp,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "wood_btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(listOf(WoodButtonTop, WoodButtonBottom))
            )
            .border(2.5.dp, WoodButtonBorder, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

fun DrawScope.drawJewelBlock(
    topLeft: Offset,
    blockSize: Float,
    color: BlockColor,
    gem: GemType? = null,
    alpha: Float = 1f,
    highlightGlow: Boolean = false
) {
    val inset = blockSize * 0.04f
    val s = blockSize - inset * 2f
    val x = topLeft.x + inset
    val y = topLeft.y + inset
    val radius = CornerRadius(s * 0.16f, s * 0.16f)

    // Base fill
    drawRoundRect(
        color = color.border.copy(alpha = alpha),
        topLeft = Offset(x, y),
        size = Size(s, s),
        cornerRadius = radius
    )

    // Beveled inner body
    val bodyInset = s * 0.07f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                color.light.copy(alpha = alpha),
                color.base.copy(alpha = alpha),
                color.dark.copy(alpha = alpha)
            ),
            startY = y + bodyInset,
            endY = y + s - bodyInset
        ),
        topLeft = Offset(x + bodyInset, y + bodyInset),
        size = Size(s - bodyInset * 2f, s - bodyInset * 2f),
        cornerRadius = CornerRadius(s * 0.12f, s * 0.12f)
    )

    // Top glossy bevel triangle/bar
    val innerPad = s * 0.18f
    val shinePath = Path().apply {
        moveTo(x + innerPad, y + innerPad)
        lineTo(x + s - innerPad, y + innerPad)
        lineTo(x + s - innerPad * 1.35f, y + innerPad * 1.6f)
        lineTo(x + innerPad * 1.35f, y + innerPad * 1.6f)
        close()
    }
    drawPath(
        path = shinePath,
        color = color.shine.copy(alpha = 0.55f * alpha)
    )

    // Left bevel highlight
    drawLine(
        color = color.shine.copy(alpha = 0.45f * alpha),
        start = Offset(x + bodyInset * 1.2f, y + s * 0.16f),
        end = Offset(x + bodyInset * 1.2f, y + s * 0.84f),
        strokeWidth = (s * 0.06f).coerceAtLeast(1.5f),
        cap = StrokeCap.Round
    )

    // Optional highlight overlay when line clear is previewed
    if (highlightGlow) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.32f),
            topLeft = Offset(x, y),
            size = Size(s, s),
            cornerRadius = radius
        )
    }

    // Embedded collectible gem for Adventure mode
    if (gem != null) {
        val cx = x + s / 2f
        val cy = y + s / 2f
        val r = s * 0.25f
        val diamond = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r, cy)
            lineTo(cx, cy + r)
            lineTo(cx - r, cy)
            close()
        }
        drawPath(diamond, color = Color.White.copy(alpha = 0.9f * alpha))
        drawPath(
            path = Path().apply {
                moveTo(cx, cy - r * 0.72f)
                lineTo(cx + r * 0.72f, cy)
                lineTo(cx, cy + r * 0.72f)
                lineTo(cx - r * 0.72f, cy)
                close()
            },
            color = gem.primaryColor.copy(alpha = alpha)
        )
        drawCircle(
            color = gem.highlightColor.copy(alpha = 0.85f * alpha),
            radius = r * 0.22f,
            center = Offset(cx - r * 0.18f, cy - r * 0.18f)
        )
    }
}

@Composable
fun WelcomeEnvelopeOverlay(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                // Cream lined stationery paper sticking out of kraft envelope
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 110.dp)
                        .shadow(12.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(LetterPaper)
                        .border(2.dp, Color(0xFFE0C8AE), RoundedCornerShape(14.dp))
                        .padding(top = 12.dp, bottom = 52.dp, start = 20.dp, end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_welcome_letter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Tutup surat",
                                tint = LetterInk,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    val messageLines = listOf(
                        "Selamat datang kembali! Senang",
                        "bertemu kamu lagi!",
                        "Level baru menunggu kamu.",
                        "Pecahkan rekormu dan terus",
                        "bersenang-senang!"
                    )
                    messageLines.forEach { line ->
                        Text(
                            text = line,
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = LetterInk,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        HorizontalDivider(color = LetterLine, thickness = 1.dp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = LetterLine, thickness = 1.dp)
                }

                // Kraft envelope bottom pocket
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(165.dp)
                        .shadow(12.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(KraftEnvelope)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

                        // Left & right folded flaps
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(w * 0.5f, h * 0.42f)
                                lineTo(0f, h)
                                close()
                            },
                            color = KraftEnvelopeDark.copy(alpha = 0.25f)
                        )
                        drawPath(
                            path = Path().apply {
                                moveTo(w, 0f)
                                lineTo(w * 0.5f, h * 0.42f)
                                lineTo(w, h)
                                close()
                            },
                            color = KraftEnvelopeDark.copy(alpha = 0.25f)
                        )
                        // Center bottom flap
                        val flapPath = Path().apply {
                            moveTo(0f, h)
                            lineTo(w * 0.45f, h * 0.20f)
                            quadraticTo(w * 0.5f, h * 0.14f, w * 0.55f, h * 0.20f)
                            lineTo(w, h)
                            close()
                        }
                        drawPath(flapPath, color = KraftEnvelope)
                        drawPath(
                            flapPath,
                            color = Color(0xFFF3C691),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = dash)
                        )

                        // Circular Block Crush stamp
                        val cx = w * 0.5f
                        val cy = h * 0.57f
                        drawCircle(
                            color = LetterInk.copy(alpha = 0.35f),
                            radius = 38.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = LetterInk.copy(alpha = 0.35f),
                            radius = 32.dp.toPx(),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.5.dp.toPx(), pathEffect = dash)
                        )
                    }

                    Text(
                        text = "Block\nCrush",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        color = LetterInk.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 12.dp)
                            .rotate(-14f)
                    )
                }

                // Red wooden stamp decoration bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-8).dp, y = 10.dp)
                        .rotate(32f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE04838), Color(0xFF9C1D12))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(RibbonRedDeep)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 5.dp)
                                .background(CrownGold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = LetterPaper,
            border = BorderStroke(3.dp, WoodButtonBorder),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = CrownGoldDeep,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.how_to_play_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = WoodTextDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = WoodTextDark)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val tips = listOf(
                    "1. Seret balok dari baki bawah ke papan 8×8.",
                    "2. Isi penuh 1 baris atau 1 kolom untuk menghancurkan balok.",
                    "3. Hancurkan baris/kolom berturut-turut untuk menaikkan Combo & Pengganda (×1 hingga ×9)!",
                    "4. Di mode Petualangan, kumpulkan semua Permata target & capai skor target untuk menang."
                )
                tips.forEach { tip ->
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WoodTextDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WoodButtonBottom,
                        contentColor = WoodTextDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Mengerti!", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    showGameControls: Boolean,
    signedInUserEmail: String?,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onOpenCloudSave: () -> Unit,
    onRestartGame: (() -> Unit)? = null,
    onExitToMenu: (() -> Unit)? = null,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = LetterPaper,
            border = BorderStroke(3.dp, WoodButtonBorder),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = WoodTextDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = WoodTextDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            contentDescription = null,
                            tint = WoodTextDark
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Efek Suara", style = MaterialTheme.typography.bodyLarge, color = WoodTextDark)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { onToggleSound() },
                        colors = SwitchDefaults.colors(checkedTrackColor = WoodButtonBottom)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            tint = WoodTextDark
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Getar Haptik", style = MaterialTheme.typography.bodyLarge, color = WoodTextDark)
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = { onToggleHaptics() },
                        colors = SwitchDefaults.colors(checkedTrackColor = WoodButtonBottom)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onOpenCloudSave()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF19A974),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_manual_save_button")
                ) {
                    Text(
                        text = if (signedInUserEmail != null) "Simpan Manual / Cloud" else "Simpan Progres Manual",
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (showGameControls && onRestartGame != null && onExitToMenu != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onRestartGame()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WoodButtonBottom,
                            contentColor = WoodTextDark
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("restart_game_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mulai Ulang", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onExitToMenu()
                        },
                        border = BorderStroke(1.5.dp, WoodButtonBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("exit_to_menu_button")
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, tint = WoodTextDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.back_to_menu), color = WoodTextDark, fontWeight = FontWeight.Bold)
                    }
                }

                if (signedInUserEmail != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = LetterLine)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Akun: $signedInUserEmail",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WoodTextDark.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onSignOut()
                        },
                        border = BorderStroke(1.5.dp, RibbonRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, tint = RibbonRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.sign_out), color = RibbonRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSaveDialog(
    bestScore: Long,
    bestCombo: Long,
    highestLevel: Long,
    completedCount: Int,
    signedInEmail: String?,
    isSyncing: Boolean,
    lastSyncText: String?,
    onSaveLocalNow: () -> Unit,
    onSignInAndSaveCloud: () -> Unit,
    onSaveCloudNow: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = LetterPaper,
            border = BorderStroke(3.dp, WoodButtonBorder),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Penyimpanan Manual",
                        style = MaterialTheme.typography.titleLarge,
                        color = WoodTextDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = WoodTextDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF3E2CD),
                    border = BorderStroke(1.5.dp, LetterLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Rekor Klasik: $bestScore • Combo Terbaik: $bestCombo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = WoodTextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Petualangan: Level $highestLevel ($completedCount/96 selesai)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WoodTextDark.copy(alpha = 0.85f)
                        )
                        if (lastSyncText != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Terakhir disimpan ke Cloud: $lastSyncText",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF107852)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onSaveLocalNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WoodButtonBottom,
                        contentColor = WoodTextDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_local_now_button")
                ) {
                    Text("Simpan di Perangkat Sekarang", fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (signedInEmail == null) {
                    Button(
                        onClick = onSignInAndSaveCloud,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF19A974),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("manual_google_signin_save_button")
                    ) {
                        Text(
                            text = if (isSyncing) "Menyimpan..." else "Masuk Google & Simpan Cloud",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Text(
                        text = "Terhubung: $signedInEmail",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF107852),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSaveCloudNow,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF19A974),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_cloud_now_button")
                    ) {
                        Text(
                            text = if (isSyncing) "Menyimpan ke Cloud..." else "Simpan ke Cloud Sekarang",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.5.dp, RibbonRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Keluar Akun Google", color = RibbonRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WoodBottomNavBar(
    selectedTab: Int, // 0 = Klasik, 1 = Petualangan
    onSelectClassic: () -> Unit,
    onSelectAdventure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WoodButtonBorder)
            .padding(top = 2.dp)
    ) {
        // Left tab: Klasik / Home
        val leftActive = selectedTab == 0
        Box(
            modifier = Modifier
                .weight(1f)
                .height(68.dp)
                .background(if (leftActive) WoodButtonBottom else WoodBackgroundBottom)
                .clickable(onClick = onSelectClassic)
                .testTag("bottom_nav_classic"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Cute 3D house-blocks icon
                Canvas(modifier = Modifier.size(30.dp)) {
                    val w = size.width
                    val h = size.height
                    // Roof
                    drawPath(
                        path = Path().apply {
                            moveTo(w * 0.5f, h * 0.08f)
                            lineTo(w * 0.92f, h * 0.42f)
                            lineTo(w * 0.08f, h * 0.42f)
                            close()
                        },
                        color = Color(0xFFE65319)
                    )
                    // Block pillars
                    drawRoundRect(
                        color = CrownGold,
                        topLeft = Offset(w * 0.18f, h * 0.44f),
                        size = Size(w * 0.26f, h * 0.46f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = CrownGoldDeep,
                        topLeft = Offset(w * 0.56f, h * 0.44f),
                        size = Size(w * 0.26f, h * 0.46f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
                if (leftActive) {
                    Text(
                        text = stringResource(R.string.classic_mode),
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WoodTextDark
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(2.dp)
                .height(68.dp)
                .background(WoodButtonBorder)
        )

        // Right tab: Petualangan
        val rightActive = selectedTab == 1
        Box(
            modifier = Modifier
                .weight(1f)
                .height(68.dp)
                .background(if (rightActive) WoodButtonBottom else WoodBackgroundBottom)
                .clickable(onClick = onSelectAdventure)
                .testTag("bottom_nav_adventure"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(modifier = Modifier.size(30.dp)) {
                    val w = size.width
                    val h = size.height
                    // Stepped golden path tiles
                    drawRoundRect(
                        color = CrownGoldDeep,
                        topLeft = Offset(w * 0.14f, h * 0.48f),
                        size = Size(w * 0.72f, h * 0.36f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = CrownGold,
                        topLeft = Offset(w * 0.34f, h * 0.16f),
                        size = Size(w * 0.32f, h * 0.34f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    // Little cyan gem & heart accent
                    drawCircle(
                        color = Color(0xFF00C9F2),
                        radius = w * 0.08f,
                        center = Offset(w * 0.5f, h * 0.32f)
                    )
                    drawCircle(
                        color = RibbonRed,
                        radius = w * 0.07f,
                        center = Offset(w * 0.32f, h * 0.68f)
                    )
                }
                if (rightActive) {
                    Text(
                        text = stringResource(R.string.adventure_mode),
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WoodTextDark
                    )
                }
            }
        }
    }
}
