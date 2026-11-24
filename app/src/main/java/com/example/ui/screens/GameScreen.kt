package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProgress
import com.example.game.BOARD_SIZE
import com.example.game.BlockEngine
import com.example.game.GameMode
import com.example.game.GameSessionState
import com.example.game.TOTAL_ADVENTURE_LEVELS
import com.example.game.TrayPiece
import com.example.ui.components.PurpleArenaBackground
import com.example.ui.components.drawJewelBlock
import com.example.ui.theme.BestScorePillBottom
import com.example.ui.theme.BestScorePillTop
import com.example.ui.theme.BoardCellBorder
import com.example.ui.theme.BoardCellEmpty
import com.example.ui.theme.BoardFrameInner
import com.example.ui.theme.BoardFrameOuter
import com.example.ui.theme.CreamText
import com.example.ui.theme.CrownGold
import com.example.ui.theme.CrownGoldDeep
import com.example.ui.theme.EmeraldBadge
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.LetterPaper
import com.example.ui.theme.ScorePillDark
import com.example.ui.theme.WoodButtonBorder
import com.example.ui.theme.WoodButtonBottom
import com.example.ui.theme.WoodTextDark
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    session: GameSessionState,
    progress: PlayerProgress,
    hapticPulseTick: Long,
    onPlacePiece: (trayIndex: Int, row: Int, col: Int) -> Boolean,
    onRestartGame: () -> Unit,
    onNextAdventureLevel: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCloudSave: () -> Unit,
    onBackToMenu: () -> Unit
) {
    BackHandler { onBackToMenu() }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(hapticPulseTick) {
        if (hapticPulseTick > 0L) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val animatedScore by animateIntAsState(
        targetValue = session.score.toInt(),
        animationSpec = tween(durationMillis = 220),
        label = "animated_score"
    )
    val bestScore = maxOf(progress.classicBestScore, session.score)
    val bestComboDisplay = maxOf(progress.classicBestCombo.toInt(), session.bestComboInMatch)

    // Root-coordinate drag tracking
    var boardOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var boardPixelSize by remember { mutableFloatStateOf(0f) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragFingerInRoot by remember { mutableStateOf(Offset.Zero) }

    val fingerLiftPx = with(density) { 88.dp.toPx() }
    val cellPx = if (boardPixelSize > 0f) boardPixelSize / BOARD_SIZE else 0f

    val activePiece: TrayPiece? = if (draggingIndex in 0..2) session.tray.getOrNull(draggingIndex) else null

    val draggedPieceTopLeft: Offset? = if (activePiece != null && cellPx > 0f) {
        val pieceW = activePiece.shape.cols * cellPx
        val pieceH = activePiece.shape.rows * cellPx
        Offset(
            x = dragFingerInRoot.x - pieceW / 2f,
            y = dragFingerInRoot.y - fingerLiftPx - pieceH / 2f
        )
    } else null

    val hoverGridPos: Pair<Int, Int>? = if (activePiece != null && draggedPieceTopLeft != null && cellPx > 0f) {
        val relX = draggedPieceTopLeft.x - boardOriginInRoot.x
        val relY = draggedPieceTopLeft.y - boardOriginInRoot.y
        val col = (relX / cellPx).roundToInt()
        val row = (relY / cellPx).roundToInt()
        if (BlockEngine.canPlace(session.board, activePiece.shape, row, col)) {
            row to col
        } else {
            null
        }
    } else null

    val clearPreview = remember(session.board, activePiece, hoverGridPos) {
        if (activePiece != null && hoverGridPos != null) {
            BlockEngine.previewClearedLines(session.board, activePiece.shape, hoverGridPos.first, hoverGridPos.second)
        } else {
            BlockEngine.ClearPreview(emptySet(), emptySet())
        }
    }

    // Particle burst animation progress
    val burstProgress = remember { Animatable(0f) }
    LaunchedEffect(session.particles) {
        if (session.particles.isNotEmpty()) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, animationSpec = tween(520))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PurpleArenaBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Best Combo / Level title + Settings gear
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (session.mode == GameMode.CLASSIC) {
                        Text(
                            text = "${stringResource(R.string.best_combo_label)} ",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF421504)
                        )
                        Text(
                            text = "$bestComboDisplay",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = CrownGold
                        )
                    } else {
                        Text(
                            text = "Petualangan • Level ${session.levelNumber}",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = CrownGold
                        )
                    }
                }

                // Purple tactile settings button top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(48.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFB5AFEC), Color(0xFF8C86CE)))
                        )
                        .border(2.dp, Color(0xFF3E3872), RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSettings)
                        .testTag("game_settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = Color(0xFF322C5A),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score Pills Row (Matches screenshot 200107!)
            Row(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left pill: Best Score (Classic) or Target Score (Adventure)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(6.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(BestScorePillTop, BestScorePillBottom)))
                        .border(2.dp, Color(0xFFD572D1).copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFF381642),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (session.mode == GameMode.CLASSIC) "$bestScore" else "${session.targetScore}",
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = CrownGold
                        )
                    }
                }

                // Right pill: Current Score
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ScorePillDark)
                        .border(2.dp, Color(0xFF6B65A8).copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$animatedScore",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = CreamText,
                        modifier = Modifier.testTag("current_score_text")
                    )
                }
            }

            // Adventure Gem Targets Strip
            if (session.mode == GameMode.ADVENTURE && session.gemTargets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    session.gemTargets.forEach { (gem, target) ->
                        val collected = (session.gemsCollected[gem] ?: 0).coerceAtMost(target)
                        val done = collected >= target
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (done) EmeraldBadge.copy(alpha = 0.85f) else ScorePillDark,
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Canvas(modifier = Modifier.size(16.dp)) {
                                    val r = size.minDimension * 0.45f
                                    val c = center
                                    drawPath(
                                        path = Path().apply {
                                            moveTo(c.x, c.y - r)
                                            lineTo(c.x + r, c.y)
                                            lineTo(c.x, c.y + r)
                                            lineTo(c.x - r, c.y)
                                            close()
                                        },
                                        color = gem.primaryColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$collected/$target",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                if (done) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Tercapai",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Combo Progress Bar + Multiplier Badge (Matches "0/5   x 1")
            Row(
                modifier = Modifier.fillMaxWidth(0.92f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3A3462))
                        .border(1.5.dp, Color(0xFF282246), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val fillFraction = (session.comboProgress.coerceIn(0, 5)) / 5f
                    if (fillFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fillFraction)
                                .height(16.dp)
                                .align(Alignment.CenterStart)
                                .background(
                                    Brush.horizontalGradient(listOf(CrownGoldDeep, CrownGold))
                                )
                        )
                    }
                    Text(
                        text = "${session.comboProgress}/5",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "× ${session.multiplier}",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CrownGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 8x8 Board Container (Square, responsive via BoxWithConstraints)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                val maxBoardSide = minOf(maxWidth, maxHeight)
                Box(
                    modifier = Modifier
                        .size(maxBoardSide)
                        .shadow(12.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(BoardFrameOuter)
                        .border(3.dp, Color(0xFF4F4992), RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BoardFrameInner)
                            .onGloballyPositioned { coords ->
                                boardOriginInRoot = coords.positionInRoot()
                                boardPixelSize = coords.size.width.toFloat()
                            }
                            .testTag("game_board_canvas")
                    ) {
                        val cell = size.width / BOARD_SIZE

                        // 1. Draw 64 empty slate cells
                        for (r in 0 until BOARD_SIZE) {
                            for (c in 0 until BOARD_SIZE) {
                                val topLeft = Offset(c * cell + 2f, r * cell + 2f)
                                val cellSize = Size(cell - 4f, cell - 4f)
                                val isHighlightedLine = r in clearPreview.fullRows || c in clearPreview.fullCols
                                drawRoundRect(
                                    color = if (isHighlightedLine) Color(0xFF4E4282) else BoardCellEmpty,
                                    topLeft = topLeft,
                                    size = cellSize,
                                    cornerRadius = CornerRadius(cell * 0.14f, cell * 0.14f)
                                )
                                drawRoundRect(
                                    color = BoardCellBorder,
                                    topLeft = topLeft,
                                    size = cellSize,
                                    cornerRadius = CornerRadius(cell * 0.14f, cell * 0.14f),
                                    style = Stroke(width = 1.5f)
                                )
                            }
                        }

                        // 2. Draw placed jewel blocks
                        for (r in 0 until BOARD_SIZE) {
                            for (c in 0 until BOARD_SIZE) {
                                val cellData = session.board[r][c] ?: continue
                                val isLineHighlight = r in clearPreview.fullRows || c in clearPreview.fullCols
                                drawJewelBlock(
                                    topLeft = Offset(c * cell, r * cell),
                                    blockSize = cell,
                                    color = cellData.color,
                                    gem = cellData.gem,
                                    highlightGlow = isLineHighlight
                                )
                            }
                        }

                        // 3. Draw Ghost Preview of hovered piece
                        if (activePiece != null && hoverGridPos != null) {
                            val (hr, hc) = hoverGridPos
                            activePiece.shape.cells.forEachIndexed { idx, offset ->
                                val r = hr + offset.row
                                val c = hc + offset.col
                                val gem = if (idx == 0) activePiece.embeddedGem else null
                                drawJewelBlock(
                                    topLeft = Offset(c * cell, r * cell),
                                    blockSize = cell,
                                    color = activePiece.color,
                                    gem = gem,
                                    alpha = 0.52f
                                )
                            }
                        }

                        // 4. Draw Shatter Particles
                        if (session.particles.isNotEmpty()) {
                            val t = burstProgress.value
                            session.particles.forEachIndexed { idx, burst ->
                                val cx = (burst.col + 0.5f) * cell
                                val cy = (burst.row + 0.5f) * cell
                                val angle = (idx * 55) * (Math.PI / 180.0)
                                val dist = cell * 0.9f * t
                                val px = cx + (kotlin.math.cos(angle) * dist).toFloat()
                                val py = cy + (kotlin.math.sin(angle) * dist).toFloat()
                                drawCircle(
                                    color = burst.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                                    radius = cell * 0.18f * (1f - t * 0.6f),
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }

                    // Floating Combo / Bonus Banner
                    this@Column.AnimatedVisibility(
                        visible = session.floatingBanner != null,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF281B4B).copy(alpha = 0.92f),
                            border = BorderStroke(2.5.dp, CrownGold),
                            shadowElevation = 10.dp
                        ) {
                            Text(
                                text = session.floatingBanner ?: "",
                                fontFamily = FredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = CrownGold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f, fill = true))

            // Bottom Block Tray (3 slots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (slotIndex in 0 until 3) {
                    val piece = session.tray.getOrNull(slotIndex)
                    val isDraggingThis = draggingIndex == slotIndex
                    var slotOriginInRoot by remember(slotIndex) { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .onGloballyPositioned { coords ->
                                slotOriginInRoot = coords.positionInRoot()
                            }
                            .then(
                                if (piece != null && !session.isGameOver && !session.isLevelComplete) {
                                    Modifier.pointerInput(piece.uid) {
                                        detectDragGestures(
                                            onDragStart = { startOffset ->
                                                draggingIndex = slotIndex
                                                dragFingerInRoot = slotOriginInRoot + startOffset
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                dragFingerInRoot = slotOriginInRoot + change.position
                                            },
                                            onDragEnd = {
                                                val target = hoverGridPos
                                                if (target != null) {
                                                    onPlacePiece(slotIndex, target.first, target.second)
                                                }
                                                draggingIndex = -1
                                            },
                                            onDragCancel = {
                                                draggingIndex = -1
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("tray_slot_$slotIndex"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (piece != null && !isDraggingThis) {
                            TrayPieceThumbnail(piece = piece)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom subtle decorative divider matching screenshot 200107
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(Color(0xFF39346A))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color(0xFF39346A),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.5.dp)
                        .background(Color(0xFF39346A))
                )
            }
        }

        // Floating Dragged Piece Overlay at exact board cell scale
        if (activePiece != null && draggedPieceTopLeft != null && cellPx > 0f) {
            val widthPx = (activePiece.shape.cols * cellPx).roundToInt()
            val heightPx = (activePiece.shape.rows * cellPx).roundToInt()
            val widthDp = with(density) { widthPx.toDp() }
            val heightDp = with(density) { heightPx.toDp() }

            Canvas(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = draggedPieceTopLeft.x.roundToInt(),
                            y = draggedPieceTopLeft.y.roundToInt()
                        )
                    }
                    .size(widthDp, heightDp)
            ) {
                activePiece.shape.cells.forEachIndexed { idx, cellOffset ->
                    val gem = if (idx == 0) activePiece.embeddedGem else null
                    drawJewelBlock(
                        topLeft = Offset(cellOffset.col * cellPx, cellOffset.row * cellPx),
                        blockSize = cellPx,
                        color = activePiece.color,
                        gem = gem
                    )
                }
            }
        }

        // Game Over / Level Complete Modal
        if (session.isGameOver || session.isLevelComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = LetterPaper,
                    border = BorderStroke(3.dp, WoodButtonBorder),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (session.isLevelComplete) Icons.Rounded.EmojiEvents else Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = if (session.isLevelComplete) CrownGoldDeep else WoodTextDark,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (session.isLevelComplete) {
                                stringResource(R.string.level_complete_title)
                            } else {
                                stringResource(R.string.game_over_title)
                            },
                            fontFamily = FredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = WoodTextDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (session.mode == GameMode.CLASSIC && session.score >= progress.classicBestScore && session.score > 0L) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CrownGold
                            ) {
                                Text(
                                    text = "REKOR BARU!",
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = WoodTextDark,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "Skor Akhir: ${session.score}\nCombo Terbaik: ${session.bestComboInMatch} • Baris Dihancurkan: ${session.linesCleared}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = WoodTextDark.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        if (session.isLevelComplete && session.levelNumber < TOTAL_ADVENTURE_LEVELS) {
                            Button(
                                onClick = { onNextAdventureLevel(session.levelNumber + 1) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldBadge,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("next_level_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Lanjut Level ${session.levelNumber + 1}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Button(
                            onClick = onRestartGame,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WoodButtonBottom,
                                contentColor = WoodTextDark
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("play_again_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.play_again), fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onOpenCloudSave,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldBadge,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("game_over_save_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Progres Manual", fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onBackToMenu,
                            border = BorderStroke(1.5.dp, WoodButtonBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("game_over_menu_button")
                        ) {
                            Icon(Icons.Rounded.Home, contentDescription = null, tint = WoodTextDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.back_to_menu), color = WoodTextDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrayPieceThumbnail(piece: TrayPiece) {
    val maxDim = maxOf(piece.shape.rows, piece.shape.cols).coerceAtLeast(1)
    val thumbCellDp = when {
        maxDim <= 2 -> 24.dp
        maxDim == 3 -> 20.dp
        maxDim == 4 -> 17.dp
        else -> 14.dp
    }
    val w = thumbCellDp * piece.shape.cols
    val h = thumbCellDp * piece.shape.rows

    Canvas(modifier = Modifier.size(w, h)) {
        val cell = size.width / piece.shape.cols
        // Subtle drop shadow behind each block
        piece.shape.cells.forEach { offset ->
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(offset.col * cell + 3f, offset.row * cell + 5f),
                size = Size(cell - 2f, cell - 2f),
                cornerRadius = CornerRadius(cell * 0.16f, cell * 0.16f)
            )
        }
        piece.shape.cells.forEachIndexed { idx, offset ->
            val gem = if (idx == 0) piece.embeddedGem else null
            drawJewelBlock(
                topLeft = Offset(offset.col * cell, offset.row * cell),
                blockSize = cell,
                color = piece.color,
                gem = gem
            )
        }
    }
}
