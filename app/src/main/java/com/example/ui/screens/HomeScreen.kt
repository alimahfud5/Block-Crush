package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProgress
import com.example.game.TOTAL_ADVENTURE_LEVELS
import com.example.ui.components.BlockCrushLogo
import com.example.ui.components.WoodBackground
import com.example.ui.components.WoodBottomNavBar
import com.example.ui.components.WoodTactileButton
import com.example.ui.theme.CrownGold
import com.example.ui.theme.CrownGoldDeep
import com.example.ui.theme.EmeraldBadge
import com.example.ui.theme.EmeraldBadgeBorder
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.RibbonRed
import com.example.ui.theme.RibbonRedDeep
import com.example.ui.theme.WoodButtonBorder
import com.example.ui.theme.WoodButtonBottom
import com.example.ui.theme.WoodButtonTop
import com.example.ui.theme.WoodTextDark
import java.util.Locale

@Composable
fun HomeScreen(
    progress: PlayerProgress,
    onPlayAdventure: () -> Unit,
    onPlayClassic: () -> Unit,
    onOpenWelcomeLetter: () -> Unit,
    onOpenCloudSave: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val percentComplete = (progress.completedLevels.size.toFloat() / TOTAL_ADVENTURE_LEVELS.toFloat()) * 100f
    val formattedPercent = String.format(Locale.forLanguageTag("id-ID"), "%.2f%% selesai", percentComplete)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            WoodBottomNavBar(
                selectedTab = 0,
                onSelectClassic = {},
                onSelectAdventure = onPlayAdventure,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WoodBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 540.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top action row: Best score badge + envelope + settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF6B3E1B).copy(alpha = 0.72f),
                        border = BorderStroke(1.5.dp, WoodButtonTop.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = "Skor Terbaik",
                                tint = CrownGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rekor: ${progress.classicBestScore}",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFFFF2CC)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onOpenCloudSave,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldBadge)
                                .border(2.dp, EmeraldBadgeBorder, RoundedCornerShape(12.dp))
                                .testTag("home_cloud_save_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = "Simpan Progres Manual",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onOpenWelcomeLetter,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WoodButtonBottom.copy(alpha = 0.85f))
                                .border(2.dp, WoodButtonBorder, RoundedCornerShape(12.dp))
                                .testTag("open_welcome_letter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MailOutline,
                                contentDescription = "Surat sambutan",
                                tint = WoodTextDark
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WoodButtonBottom.copy(alpha = 0.85f))
                                .border(2.dp, WoodButtonBorder, RoundedCornerShape(12.dp))
                                .testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = WoodTextDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.7f))

                BlockCrushLogo(modifier = Modifier.fillMaxWidth(0.84f))

                Spacer(modifier = Modifier.weight(0.85f))

                // Green Progress Bubble + Tutorial Cap Button + Adventure Button with Red "Baru!" Ribbon
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.82f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Green speech bubble badge
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldBadge,
                                border = BorderStroke(1.5.dp, EmeraldBadgeBorder),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "\uD83D\uDE0D", fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formattedPercent,
                                        fontFamily = FredokaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Adventure Wooden Button
                        Box(modifier = Modifier.fillMaxWidth()) {
                            WoodTactileButton(
                                onClick = onPlayAdventure,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("menu_adventure_button")
                            ) {
                                MiniBoardBadge(isAdventure = true)
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = stringResource(R.string.adventure_mode),
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 24.sp,
                                    color = WoodTextDark,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Red "Baru!" ribbon tag overlapping top-right
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RibbonRed,
                                border = BorderStroke(1.5.dp, RibbonRedDeep),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 12.dp, y = (-12).dp)
                                    .rotate(3f)
                            ) {
                                Text(
                                    text = stringResource(R.string.new_badge),
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Circular Golden Graduation Cap (Tutorial) button on far right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(54.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(listOf(WoodButtonTop, WoodButtonBottom))
                            )
                            .border(3.dp, Color(0xFFA86B32), CircleShape)
                            .clickable(onClick = onOpenTutorial)
                            .testTag("tutorial_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = stringResource(R.string.how_to_play_title),
                            tint = CrownGold,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Classic 8x8 Wooden Button
                WoodTactileButton(
                    onClick = onPlayClassic,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .testTag("menu_classic_button")
                ) {
                    MiniBoardBadge(isAdventure = false)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.classic_mode),
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            fontSize = 24.sp,
                            color = WoodTextDark
                        )
                        Text(
                            text = stringResource(R.string.classic_board_subtitle),
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp,
                            color = WoodTextDark.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniBoardBadge(isAdventure: Boolean) {
    Canvas(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, WoodButtonBorder, RoundedCornerShape(8.dp))
    ) {
        drawRect(color = Color(0xFF3E200B))
        val cell = size.width / 5f
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val fill = if (isAdventure) {
                    when {
                        r >= 3 || (r == 2 && (c == 0 || c == 4)) -> Color(0xFFD69F59)
                        r == 1 && c == 2 -> Color(0xFF00C9F2)
                        else -> Color(0xFF47250D)
                    }
                } else {
                    if ((r == 1 && c in 1..3) || (r == 2 && c == 2) || r >= 3) {
                        Color(0xFFD69F59)
                    } else {
                        Color(0xFF47250D)
                    }
                }
                drawRoundRect(
                    color = fill,
                    topLeft = Offset(c * cell + 1.5f, r * cell + 1.5f),
                    size = Size(cell - 3f, cell - 3f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
        if (isAdventure) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.34f
            val r = size.width * 0.14f
            drawPath(
                path = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                },
                color = Color(0xFF3CE8FF)
            )
        }
    }
}
