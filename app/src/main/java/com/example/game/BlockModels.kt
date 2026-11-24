package com.example.game

import androidx.compose.ui.graphics.Color

const val BOARD_SIZE = 8
const val TOTAL_ADVENTURE_LEVELS = 96

enum class BlockColor(
    val base: Color,
    val light: Color,
    val dark: Color,
    val shine: Color,
    val border: Color
) {
    CYAN(
        base = Color(0xFF00C9F2),
        light = Color(0xFF66EEFF),
        dark = Color(0xFF0088B8),
        shine = Color(0xFFB8FAFF),
        border = Color(0xFF005F85)
    ),
    AMBER(
        base = Color(0xFFFFC812),
        light = Color(0xFFFFEB6B),
        dark = Color(0xFFD68D00),
        shine = Color(0xFFFFF8B8),
        border = Color(0xFF996000)
    ),
    EMERALD(
        base = Color(0xFF23D160),
        light = Color(0xFF70F59B),
        dark = Color(0xFF0E8E3B),
        shine = Color(0xFFC2FFE0),
        border = Color(0xFF086126)
    ),
    COBALT(
        base = Color(0xFF3375FF),
        light = Color(0xFF7CABFF),
        dark = Color(0xFF1A46B8),
        shine = Color(0xFFC7DCFF),
        border = Color(0xFF102D7D)
    ),
    VIOLET(
        base = Color(0xFFA23BFF),
        light = Color(0xFFCC8AFF),
        dark = Color(0xFF6B18C4),
        shine = Color(0xFFEDD6FF),
        border = Color(0xFF480E87)
    ),
    RUBY(
        base = Color(0xFFFF4057),
        light = Color(0xFFFF8896),
        dark = Color(0xFFB8142C),
        shine = Color(0xFFFFCFD5),
        border = Color(0xFF7D0A1B)
    ),
    ORANGE(
        base = Color(0xFFFF7A1A),
        light = Color(0xFFFFAC66),
        dark = Color(0xFFC24A00),
        shine = Color(0xFFFFDFC2),
        border = Color(0xFF853100)
    )
}

enum class GemType(
    val labelId: String,
    val primaryColor: Color,
    val highlightColor: Color
) {
    DIAMOND("Berlian", Color(0xFF3CE8FF), Color(0xFFD6FCFF)),
    EMERALD("Zamrud", Color(0xFF2CE67A), Color(0xFFD1FFE4)),
    RUBY("Rubi", Color(0xFFFF4D6D), Color(0xFFFFD6DE))
}

data class GridCell(
    val color: BlockColor,
    val gem: GemType? = null
)

data class BlockOffset(val row: Int, val col: Int)

data class BlockShape(
    val id: String,
    val cells: List<BlockOffset>,
    val weight: Int = 10
) {
    val rows: Int = (cells.maxOfOrNull { it.row } ?: 0) + 1
    val cols: Int = (cells.maxOfOrNull { it.col } ?: 0) + 1
}

data class TrayPiece(
    val uid: Long,
    val shape: BlockShape,
    val color: BlockColor,
    val embeddedGem: GemType? = null
)

enum class GameMode {
    CLASSIC,
    ADVENTURE
}

data class AdventureLevelConfig(
    val levelNumber: Int,
    val title: String,
    val gemTargets: Map<GemType, Int>,
    val targetScore: Long,
    val initialBoard: List<List<GridCell?>>
)

data class ParticleBurst(
    val id: Long,
    val row: Int,
    val col: Int,
    val color: Color
)

data class GameSessionState(
    val mode: GameMode = GameMode.CLASSIC,
    val levelNumber: Int = 0,
    val board: List<List<GridCell?>> = List(BOARD_SIZE) { List(BOARD_SIZE) { null } },
    val tray: List<TrayPiece?> = emptyList(),
    val score: Long = 0L,
    val comboCount: Int = 0,
    val bestComboInMatch: Int = 0,
    val comboProgress: Int = 0,
    val multiplier: Int = 1,
    val linesCleared: Int = 0,
    val gemTargets: Map<GemType, Int> = emptyMap(),
    val gemsCollected: Map<GemType, Int> = emptyMap(),
    val targetScore: Long = 0L,
    val isGameOver: Boolean = false,
    val isLevelComplete: Boolean = false,
    val floatingBanner: String? = null,
    val lastPointsGained: Int = 0,
    val clearingCells: Set<Pair<Int, Int>> = emptySet(),
    val particles: List<ParticleBurst> = emptyList()
)
