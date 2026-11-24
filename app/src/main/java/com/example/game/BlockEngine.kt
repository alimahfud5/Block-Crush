package com.example.game

import kotlin.random.Random

object BlockEngine {

    private var nextPieceUid = 1L

    val shapes: List<BlockShape> = listOf(
        // Singles & Lines
        BlockShape("1x1", listOf(BlockOffset(0, 0)), weight = 8),
        BlockShape("1x2", listOf(BlockOffset(0, 0), BlockOffset(0, 1)), weight = 12),
        BlockShape("2x1", listOf(BlockOffset(0, 0), BlockOffset(1, 0)), weight = 12),
        BlockShape("1x3", listOf(BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(0, 2)), weight = 14),
        BlockShape("3x1", listOf(BlockOffset(0, 0), BlockOffset(1, 0), BlockOffset(2, 0)), weight = 14),
        BlockShape("1x4", listOf(BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(0, 2), BlockOffset(0, 3)), weight = 10),
        BlockShape("4x1", listOf(BlockOffset(0, 0), BlockOffset(1, 0), BlockOffset(2, 0), BlockOffset(3, 0)), weight = 10),
        BlockShape("1x5", listOf(BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(0, 2), BlockOffset(0, 3), BlockOffset(0, 4)), weight = 6),
        BlockShape("5x1", listOf(BlockOffset(0, 0), BlockOffset(1, 0), BlockOffset(2, 0), BlockOffset(3, 0), BlockOffset(4, 0)), weight = 6),

        // Squares & Rectangles
        BlockShape("2x2", listOf(BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(1, 0), BlockOffset(1, 1)), weight = 16),
        BlockShape("2x3", listOf(
            BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(0, 2),
            BlockOffset(1, 0), BlockOffset(1, 1), BlockOffset(1, 2)
        ), weight = 9),
        BlockShape("3x2", listOf(
            BlockOffset(0, 0), BlockOffset(0, 1),
            BlockOffset(1, 0), BlockOffset(1, 1),
            BlockOffset(2, 0), BlockOffset(2, 1)
        ), weight = 9),
        BlockShape("3x3", listOf(
            BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(0, 2),
            BlockOffset(1, 0), BlockOffset(1, 1), BlockOffset(1, 2),
            BlockOffset(2, 0), BlockOffset(2, 1), BlockOffset(2, 2)
        ), weight = 7),

        // Small L-shapes (2x2)
        BlockShape("L2_TL", listOf(BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(1, 0)), weight = 10),
        BlockShape("L2_TR", listOf(BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(1, 1)), weight = 10),
        BlockShape("L2_BL", listOf(BlockOffset(0, 0), BlockOffset(1, 0), BlockOffset(1, 1)), weight = 10),
        BlockShape("L2_BR", listOf(BlockOffset(0, 1), BlockOffset(1, 0), BlockOffset(1, 1)), weight = 10),

        // Big L-shapes (3x3 corners)
        BlockShape("L3_TL", listOf(
            BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(0, 2),
            BlockOffset(1, 0), BlockOffset(2, 0)
        ), weight = 7),
        BlockShape("L3_TR", listOf(
            BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(0, 2),
            BlockOffset(1, 2), BlockOffset(2, 2)
        ), weight = 7),
        BlockShape("L3_BL", listOf(
            BlockOffset(0, 0), BlockOffset(1, 0),
            BlockOffset(2, 0), BlockOffset(2, 1), BlockOffset(2, 2)
        ), weight = 7),
        BlockShape("L3_BR", listOf(
            BlockOffset(0, 2), BlockOffset(1, 2),
            BlockOffset(2, 0), BlockOffset(2, 1), BlockOffset(2, 2)
        ), weight = 7),

        // T-shapes & Z-shapes
        BlockShape("T_UP", listOf(BlockOffset(0, 1), BlockOffset(1, 0), BlockOffset(1, 1), BlockOffset(1, 2)), weight = 8),
        BlockShape("T_DOWN", listOf(BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(0, 2), BlockOffset(1, 1)), weight = 8),
        BlockShape("Z_H", listOf(BlockOffset(0, 0), BlockOffset(0, 1), BlockOffset(1, 1), BlockOffset(1, 2)), weight = 7),
        BlockShape("S_H", listOf(BlockOffset(0, 1), BlockOffset(0, 2), BlockOffset(1, 0), BlockOffset(1, 1)), weight = 7),

        // Diagonals (like the 3-diagonal cyan piece in Block Crush!)
        BlockShape("DIAG_2", listOf(BlockOffset(0, 1), BlockOffset(1, 0)), weight = 6),
        BlockShape("DIAG_3_R", listOf(BlockOffset(0, 2), BlockOffset(1, 1), BlockOffset(2, 0)), weight = 6),
        BlockShape("DIAG_3_L", listOf(BlockOffset(0, 0), BlockOffset(1, 1), BlockOffset(2, 2)), weight = 6)
    )

    fun emptyBoard(): List<List<GridCell?>> =
        List(BOARD_SIZE) { List(BOARD_SIZE) { null } }

    fun canPlace(
        board: List<List<GridCell?>>,
        shape: BlockShape,
        startRow: Int,
        startCol: Int
    ): Boolean {
        for (cell in shape.cells) {
            val r = startRow + cell.row
            val c = startCol + cell.col
            if (r !in 0 until BOARD_SIZE || c !in 0 until BOARD_SIZE) return false
            if (board[r][c] != null) return false
        }
        return true
    }

    fun canShapeFitAnywhere(board: List<List<GridCell?>>, shape: BlockShape): Boolean {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (canPlace(board, shape, r, c)) return true
            }
        }
        return false
    }

    fun canAnyPieceFit(board: List<List<GridCell?>>, tray: List<TrayPiece?>): Boolean {
        return tray.filterNotNull().any { canShapeFitAnywhere(board, it.shape) }
    }

    data class ClearPreview(
        val fullRows: Set<Int>,
        val fullCols: Set<Int>
    ) {
        val totalLines: Int get() = fullRows.size + fullCols.size
    }

    fun previewClearedLines(
        board: List<List<GridCell?>>,
        shape: BlockShape,
        startRow: Int,
        startCol: Int
    ): ClearPreview {
        if (!canPlace(board, shape, startRow, startCol)) {
            return ClearPreview(emptySet(), emptySet())
        }
        val placedPositions = shape.cells.map { (startRow + it.row) to (startCol + it.col) }.toSet()

        val fullRows = mutableSetOf<Int>()
        for (r in 0 until BOARD_SIZE) {
            if ((0 until BOARD_SIZE).all { c -> board[r][c] != null || (r to c) in placedPositions }) {
                fullRows.add(r)
            }
        }

        val fullCols = mutableSetOf<Int>()
        for (c in 0 until BOARD_SIZE) {
            if ((0 until BOARD_SIZE).all { r -> board[r][c] != null || (r to c) in placedPositions }) {
                fullCols.add(c)
            }
        }
        return ClearPreview(fullRows, fullCols)
    }

    data class PlacementOutcome(
        val nextBoard: List<List<GridCell?>>,
        val clearedCells: Set<Pair<Int, Int>>,
        val linesClearedCount: Int,
        val pointsGained: Int,
        val nextComboCount: Int,
        val nextComboProgress: Int,
        val nextMultiplier: Int,
        val collectedGems: Map<GemType, Int>,
        val bannerText: String?
    )

    fun placePieceAndResolve(
        board: List<List<GridCell?>>,
        piece: TrayPiece,
        startRow: Int,
        startCol: Int,
        currentCombo: Int,
        currentComboProgress: Int,
        currentMultiplier: Int
    ): PlacementOutcome {
        val mutableBoard = board.map { it.toMutableList() }.toMutableList()
        piece.shape.cells.forEachIndexed { idx, offset ->
            val r = startRow + offset.row
            val c = startCol + offset.col
            val gemForCell = if (idx == 0) piece.embeddedGem else null
            mutableBoard[r][c] = GridCell(color = piece.color, gem = gemForCell)
        }

        val fullRows = (0 until BOARD_SIZE).filter { r ->
            (0 until BOARD_SIZE).all { c -> mutableBoard[r][c] != null }
        }.toSet()
        val fullCols = (0 until BOARD_SIZE).filter { c ->
            (0 until BOARD_SIZE).all { r -> mutableBoard[r][c] != null }
        }.toSet()

        val linesCount = fullRows.size + fullCols.size
        val clearedCells = mutableSetOf<Pair<Int, Int>>()
        val gemsFound = mutableMapOf<GemType, Int>()

        for (r in fullRows) {
            for (c in 0 until BOARD_SIZE) {
                clearedCells.add(r to c)
            }
        }
        for (c in fullCols) {
            for (r in 0 until BOARD_SIZE) {
                clearedCells.add(r to c)
            }
        }

        for ((r, c) in clearedCells) {
            mutableBoard[r][c]?.gem?.let { gem ->
                gemsFound[gem] = (gemsFound[gem] ?: 0) + 1
            }
            mutableBoard[r][c] = null
        }

        val placementPoints = piece.shape.cells.size * 10
        val nextCombo: Int
        val nextProgress: Int
        val nextMult: Int
        val linePoints: Int
        val banner: String?

        if (linesCount > 0) {
            nextCombo = currentCombo + linesCount
            val rawProgress = currentComboProgress + linesCount
            nextMult = (currentMultiplier + rawProgress / 5).coerceAtMost(9)
            nextProgress = rawProgress % 5
            val multiLineBonus = linesCount * linesCount * 40
            linePoints = (multiLineBonus + nextCombo * 25) * nextMult
            banner = when {
                linesCount >= 4 -> "SPEKTAKULER! ×$nextMult"
                linesCount == 3 -> "LUAR BIASA! ×$nextMult"
                linesCount == 2 -> "HEBAT! ×$nextMult"
                nextCombo >= 3 -> "COMBO $nextCombo!"
                else -> "+$linePoints"
            }
        } else {
            nextCombo = 0
            nextProgress = 0
            nextMult = 1
            linePoints = 0
            banner = null
        }

        return PlacementOutcome(
            nextBoard = mutableBoard.map { it.toList() },
            clearedCells = clearedCells,
            linesClearedCount = linesCount,
            pointsGained = placementPoints + linePoints,
            nextComboCount = nextCombo,
            nextComboProgress = nextProgress,
            nextMultiplier = nextMult,
            collectedGems = gemsFound,
            bannerText = banner
        )
    }

    fun generateTray(
        board: List<List<GridCell?>>,
        mode: GameMode,
        missingGems: List<GemType> = emptyList(),
        random: Random = Random.Default
    ): List<TrayPiece> {
        val result = mutableListOf<TrayPiece>()
        val colors = BlockColor.entries
        val totalWeight = shapes.sumOf { it.weight }

        fun pickWeightedShape(): BlockShape {
            var roll = random.nextInt(totalWeight)
            for (s in shapes) {
                roll -= s.weight
                if (roll < 0) return s
            }
            return shapes.first()
        }

        for (i in 0 until 3) {
            val shape = if (i == 0) {
                // Guarantee the tray stays playable whenever any shape can fit
                val candidate = pickWeightedShape()
                if (canShapeFitAnywhere(board, candidate)) {
                    candidate
                } else {
                    shapes.shuffled(random).firstOrNull { canShapeFitAnywhere(board, it) } ?: candidate
                }
            } else {
                pickWeightedShape()
            }
            val color = colors[random.nextInt(colors.size)]
            val gem = if (mode == GameMode.ADVENTURE && missingGems.isNotEmpty() && random.nextFloat() < 0.45f) {
                missingGems[random.nextInt(missingGems.size)]
            } else {
                null
            }
            result.add(
                TrayPiece(
                    uid = nextPieceUid++,
                    shape = shape,
                    color = color,
                    embeddedGem = gem
                )
            )
        }
        return result
    }

    fun buildAdventureLevel(levelNumber: Int): AdventureLevelConfig {
        val safeLevel = levelNumber.coerceIn(1, TOTAL_ADVENTURE_LEVELS)
        val rng = Random(safeLevel * 7919 + 1337)
        val board = Array(BOARD_SIZE) { arrayOfNulls<GridCell>(BOARD_SIZE) }

        val diamondTarget = 3 + (safeLevel / 4).coerceAtMost(10)
        val emeraldTarget = if (safeLevel >= 4) 2 + (safeLevel / 6).coerceAtMost(9) else 0
        val rubyTarget = if (safeLevel >= 10) 2 + (safeLevel / 8).coerceAtMost(8) else 0
        val targetScore = (400L + safeLevel * 65L)

        val gemTargets = buildMap {
            put(GemType.DIAMOND, diamondTarget)
            if (emeraldTarget > 0) put(GemType.EMERALD, emeraldTarget)
            if (rubyTarget > 0) put(GemType.RUBY, rubyTarget)
        }

        // Starter pre-placed obstacles & gems arranged in curated symmetric patterns
        val fillCount = (10 + (safeLevel / 3)).coerceAtMost(26)
        val availableColors = BlockColor.entries
        val gemPool = buildList {
            repeat((diamondTarget / 2).coerceAtLeast(2)) { add(GemType.DIAMOND) }
            if (emeraldTarget > 0) repeat((emeraldTarget / 2).coerceAtLeast(1)) { add(GemType.EMERALD) }
            if (rubyTarget > 0) repeat((rubyTarget / 2).coerceAtLeast(1)) { add(GemType.RUBY) }
        }

        val candidateCoords = mutableListOf<Pair<Int, Int>>()
        val patternIndex = safeLevel % 5
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val include = when (patternIndex) {
                    0 -> (r in 2..5 && c in 2..5) && ((r + c) % 2 == 0)
                    1 -> (r == 2 || r == 5 || c == 2 || c == 5) && !((r == 2 || r == 5) && (c == 2 || c == 5))
                    2 -> (r == c || r + c == BOARD_SIZE - 1) && r in 1..6
                    3 -> (r in 1..6 && (c == 1 || c == 6 || r == 3 || r == 4)) && ((r + c) % 2 == 1)
                    else -> ((r == 1 || r == 6) && c in 2..5) || ((c == 1 || c == 6) && r in 2..5)
                }
                if (include) candidateCoords.add(r to c)
            }
        }
        candidateCoords.shuffle(rng)
        val chosenCoords = candidateCoords.take(fillCount)
        chosenCoords.forEachIndexed { idx, (r, c) ->
            val gem = gemPool.getOrNull(idx)
            val color = when (gem) {
                GemType.DIAMOND -> BlockColor.CYAN
                GemType.EMERALD -> BlockColor.EMERALD
                GemType.RUBY -> BlockColor.RUBY
                null -> availableColors[rng.nextInt(availableColors.size)]
            }
            board[r][c] = GridCell(color = color, gem = gem)
        }

        return AdventureLevelConfig(
            levelNumber = safeLevel,
            title = "Level $safeLevel",
            gemTargets = gemTargets,
            targetScore = targetScore,
            initialBoard = board.map { it.toList() }
        )
    }
}
