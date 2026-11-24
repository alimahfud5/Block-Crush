package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.PlayerProgress
import com.example.game.TOTAL_ADVENTURE_LEVELS
import com.example.ui.components.WoodBackground
import com.example.ui.components.WoodBottomNavBar
import com.example.ui.components.WoodTactileButton
import com.example.ui.theme.CrownGold
import com.example.ui.theme.CrownGoldDeep
import com.example.ui.theme.EmeraldBadge
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.LetterPaper
import com.example.ui.theme.WoodButtonBorder
import com.example.ui.theme.WoodButtonBottom
import com.example.ui.theme.WoodButtonTop
import com.example.ui.theme.WoodTextDark
import com.example.ui.theme.WoodTileLocked
import com.example.ui.theme.WoodTileLockedBorder

// Exact 11-column Trophy silhouette mapping all 96 levels (from top 94..96 down to base 1..11)
private val TROPHY_ROWS: List<List<Int?>> = listOf(
    listOf(null, null, null, null, null, 94, 95, 96, null, null, null),
    listOf(null, null, null, null, 89, 90, 91, 92, 93, null, null),
    listOf(null, null, null, 82, 83, 84, 85, 86, 87, 88, null),
    listOf(null, null, 73, 74, 75, 76, 77, 78, 79, 80, 81),
    listOf(null, null, 64, 65, 66, 67, 68, 69, 70, 71, 72),
    listOf(null, null, null, 57, 58, 59, 60, 61, 62, 63, null),
    listOf(null, null, null, null, null, 54, 55, 56, null, null, null),
    listOf(null, 49, 50, null, null, 51, 52, 53, null, null, null),
    listOf(42, 43, 44, 45, null, 46, 47, 48, null, null, null),
    listOf(33, 34, 35, 36, null, 37, 38, 39, 40, 41, null),
    listOf(null, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32),
    listOf(12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22),
    listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
)

@Composable
fun AdventureMapScreen(
    progress: PlayerProgress,
    onBackHome: () -> Unit,
    onStartLevel: (Int) -> Unit
) {
    BackHandler { onBackHome() }

    val highestUnlocked = progress.highestAdventureLevel.toInt().coerceIn(1, TOTAL_ADVENTURE_LEVELS)
    val completedSet = progress.completedLevels.map { it.toInt() }.toSet()
    var selectedLevel by remember(highestUnlocked) { mutableStateOf(highestUnlocked) }
    var showTrophySummary by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pointer_bounce")
    val pointerBounce by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pointer_offset"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            WoodBottomNavBar(
                selectedTab = 1,
                onSelectClassic = onBackHome,
                onSelectAdventure = {},
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(max = 560.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top-right Trophy/Overview Magnifier button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(WoodButtonTop, WoodButtonBottom)))
                            .border(2.dp, WoodButtonBorder, RoundedCornerShape(12.dp))
                            .clickable { showTrophySummary = true }
                            .testTag("adventure_overview_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Ringkasan Petualangan",
                            tint = WoodTextDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Laurel Wreath + Trophy Silhouette Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "\uD83C\uDF3F",
                        fontSize = 34.sp,
                        color = WoodTextDark.copy(alpha = 0.7f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (completedSet.size >= TOTAL_ADVENTURE_LEVELS) CrownGold else WoodTextDark.copy(alpha = 0.65f),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(64.dp)
                    )
                    Text(
                        text = "\uD83C\uDF3F",
                        fontSize = 34.sp,
                        color = WoodTextDark.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = stringResource(R.string.adventure_tagline),
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = WoodTextDark.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 96-Tile Trophy Map Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TROPHY_ROWS.forEach { rowCells ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowCells.forEach { lvl ->
                                if (lvl == null) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val isCompleted = lvl in completedSet
                                    val isUnlocked = lvl <= highestUnlocked
                                    val isSelected = lvl == selectedLevel

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(31.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when {
                                                        isSelected -> Brush.verticalGradient(listOf(Color(0xFFFFF5B8), CrownGold))
                                                        isCompleted -> Brush.verticalGradient(listOf(Color(0xFF28C778), EmeraldBadge))
                                                        isUnlocked -> Brush.verticalGradient(listOf(WoodButtonTop, WoodButtonBottom))
                                                        else -> Brush.verticalGradient(listOf(WoodTileLocked, WoodTileLocked))
                                                    }
                                                )
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color.White else WoodTileLockedBorder,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .clickable(enabled = isUnlocked) {
                                                    selectedLevel = lvl
                                                }
                                                .testTag("adventure_level_tile_$lvl"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$lvl",
                                                fontFamily = FredokaFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = when {
                                                    isSelected -> WoodTextDark
                                                    isCompleted -> Color.White
                                                    isUnlocked -> WoodTextDark
                                                    else -> Color(0xFF8C5A32)
                                                }
                                            )
                                        }

                                        if (isSelected) {
                                            // Golden bouncing pointer arrow above selected tile
                                            Canvas(
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .offset(y = (-12 + pointerBounce).dp)
                                                    .size(16.dp)
                                            ) {
                                                drawPath(
                                                    path = Path().apply {
                                                        moveTo(0f, 0f)
                                                        lineTo(size.width, 0f)
                                                        lineTo(size.width / 2f, size.height)
                                                        close()
                                                    },
                                                    color = CrownGold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big tactile wooden CTA: "Level X"
                WoodTactileButton(
                    onClick = { onStartLevel(selectedLevel) },
                    height = 58.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .testTag("start_adventure_level_button")
                ) {
                    Text(
                        text = "Level $selectedLevel",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        color = WoodTextDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showTrophySummary) {
        Dialog(onDismissRequest = { showTrophySummary = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = LetterPaper,
                border = BorderStroke(3.dp, WoodButtonBorder)
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
                            text = "Piala Petualangan",
                            style = MaterialTheme.typography.titleLarge,
                            color = WoodTextDark
                        )
                        IconButton(onClick = { showTrophySummary = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Tutup", tint = WoodTextDark)
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = CrownGoldDeep,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${completedSet.size} / $TOTAL_ADVENTURE_LEVELS Level Selesai",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = WoodTextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Level terbuka saat ini: Level $highestUnlocked\nTotal Bintang: ${progress.totalStars}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WoodTextDark.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = EmeraldBadge)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Selesaikan semua 96 level untuk piala emas!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WoodTextDark
                        )
                    }
                }
            }
        }
    }
}
