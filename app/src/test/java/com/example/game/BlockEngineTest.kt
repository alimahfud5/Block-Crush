package com.example.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockEngineTest {

    @Test
    fun placingCompletingRowClearsRowAndCollectsGems() {
        val board = BlockEngine.emptyBoard().map { it.toMutableList() }.toMutableList()
        // Fill row 0 cols 0..6, including one DIAMOND gem at (0, 2)
        for (c in 0 until 7) {
            board[0][c] = GridCell(
                color = BlockColor.CYAN,
                gem = if (c == 2) GemType.DIAMOND else null
            )
        }
        val singlePiece = TrayPiece(
            uid = 1L,
            shape = BlockShape("1x1", listOf(BlockOffset(0, 0))),
            color = BlockColor.AMBER
        )

        val immutableBoard = board.map { it.toList() }
        assertTrue(BlockEngine.canPlace(immutableBoard, singlePiece.shape, 0, 7))
        assertFalse(BlockEngine.canPlace(immutableBoard, singlePiece.shape, 0, 0))

        val outcome = BlockEngine.placePieceAndResolve(
            board = immutableBoard,
            piece = singlePiece,
            startRow = 0,
            startCol = 7,
            currentCombo = 0,
            currentComboProgress = 0,
            currentMultiplier = 1
        )

        assertEquals(1, outcome.linesClearedCount)
        assertEquals(8, outcome.clearedCells.size)
        assertEquals(1, outcome.collectedGems[GemType.DIAMOND])
        assertTrue((0 until BOARD_SIZE).all { c -> outcome.nextBoard[0][c] == null })
        assertTrue(outcome.pointsGained > 10)
    }

    @Test
    fun comboProgressIncreasesMultiplierEveryFiveLines() {
        val board = BlockEngine.emptyBoard().map { it.toMutableList() }.toMutableList()
        for (c in 0 until 7) {
            board[0][c] = GridCell(color = BlockColor.EMERALD)
        }
        val singlePiece = TrayPiece(
            uid = 2L,
            shape = BlockShape("1x1", listOf(BlockOffset(0, 0))),
            color = BlockColor.RUBY
        )

        val outcome = BlockEngine.placePieceAndResolve(
            board = board.map { it.toList() },
            piece = singlePiece,
            startRow = 0,
            startCol = 7,
            currentCombo = 4,
            currentComboProgress = 4,
            currentMultiplier = 1
        )

        assertEquals(2, outcome.nextMultiplier)
        assertEquals(0, outcome.nextComboProgress)
        assertEquals(5, outcome.nextComboCount)
    }

    @Test
    fun all96AdventureLevelsBuildCleanlyWithValidTargets() {
        for (lvl in 1..TOTAL_ADVENTURE_LEVELS) {
            val config = BlockEngine.buildAdventureLevel(lvl)
            assertEquals(lvl, config.levelNumber)
            assertTrue(config.gemTargets.isNotEmpty())
            assertTrue(config.targetScore > 0L)
            assertEquals(BOARD_SIZE, config.initialBoard.size)
            assertEquals(BOARD_SIZE, config.initialBoard.first().size)
        }
    }
}
