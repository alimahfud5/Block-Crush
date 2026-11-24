package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BlockCrushRepository
import com.example.data.MatchRecord
import com.example.data.PlayerProgress
import com.example.data.UiState
import com.example.game.BlockEngine
import com.example.game.GameMode
import com.example.game.GameSessionState
import com.example.game.GemType
import com.example.game.ParticleBurst
import com.example.game.SoundFeedback
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BlockCrushViewModel(
    private val repository: BlockCrushRepository,
    private val currentUserId: String,
    private val displayName: String
) : ViewModel() {

    private val soundFeedback = SoundFeedback()
    private var particleSeq = 1L

    val progressState: StateFlow<UiState<PlayerProgress>> = repository.observePlayerProgress(currentUserId)
        .map<PlayerProgress?, UiState<PlayerProgress>> { progress ->
            UiState.Success(progress ?: PlayerProgress(userId = currentUserId, displayName = displayName))
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing player progress", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat progres pemain"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val recentMatchesState: StateFlow<List<MatchRecord>> = repository.observeRecentMatches(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing recent matches", firestoreError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    private val _gameState = MutableStateFlow(GameSessionState())
    val gameState: StateFlow<GameSessionState> = _gameState.asStateFlow()

    private val _manualWelcomeOpen = MutableStateFlow<Boolean?>(null)
    val manualWelcomeOpen: StateFlow<Boolean?> = _manualWelcomeOpen.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _hapticPulseTick = MutableStateFlow(0L)
    val hapticPulseTick: StateFlow<Long> = _hapticPulseTick.asStateFlow()

    init {
        viewModelScope.launch {
            val result = repository.ensurePlayerProgress(currentUserId, displayName)
            result.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Gagal menyinkronkan profil"
            }
        }
    }

    fun startClassicGame() {
        val board = BlockEngine.emptyBoard()
        val tray = BlockEngine.generateTray(board, GameMode.CLASSIC)
        _gameState.value = GameSessionState(
            mode = GameMode.CLASSIC,
            levelNumber = 0,
            board = board,
            tray = tray
        )
    }

    fun startAdventureLevel(levelNumber: Int) {
        val config = BlockEngine.buildAdventureLevel(levelNumber)
        val missingGems = config.gemTargets.keys.toList()
        val tray = BlockEngine.generateTray(
            board = config.initialBoard,
            mode = GameMode.ADVENTURE,
            missingGems = missingGems
        )
        _gameState.value = GameSessionState(
            mode = GameMode.ADVENTURE,
            levelNumber = config.levelNumber,
            board = config.initialBoard,
            tray = tray,
            gemTargets = config.gemTargets,
            gemsCollected = config.gemTargets.keys.associateWith { 0 },
            targetScore = config.targetScore
        )
    }

    fun restartCurrentGame() {
        val current = _gameState.value
        if (current.mode == GameMode.ADVENTURE && current.levelNumber > 0) {
            startAdventureLevel(current.levelNumber)
        } else {
            startClassicGame()
        }
    }

    fun placePiece(trayIndex: Int, startRow: Int, startCol: Int): Boolean {
        val session = _gameState.value
        if (session.isGameOver || session.isLevelComplete) return false
        val piece = session.tray.getOrNull(trayIndex) ?: return false
        if (!BlockEngine.canPlace(session.board, piece.shape, startRow, startCol)) return false

        val progress = (progressState.value as? UiState.Success)?.data
        val soundOn = progress?.soundEnabled ?: true
        val hapticsOn = progress?.hapticsEnabled ?: true

        val outcome = BlockEngine.placePieceAndResolve(
            board = session.board,
            piece = piece,
            startRow = startRow,
            startCol = startCol,
            currentCombo = session.comboCount,
            currentComboProgress = session.comboProgress,
            currentMultiplier = session.multiplier
        )

        val updatedScore = session.score + outcome.pointsGained
        val updatedBestCombo = maxOf(session.bestComboInMatch, outcome.nextComboCount)
        val updatedLines = session.linesCleared + outcome.linesClearedCount

        val updatedGems = session.gemsCollected.toMutableMap()
        outcome.collectedGems.forEach { (gem, count) ->
            updatedGems[gem] = (updatedGems[gem] ?: 0) + count
        }

        val updatedTray = session.tray.toMutableList()
        updatedTray[trayIndex] = null

        val missingGems = session.gemTargets.mapNotNull { (gem, target) ->
            if ((updatedGems[gem] ?: 0) < target) gem else null
        }

        val nextTray = if (updatedTray.all { it == null }) {
            BlockEngine.generateTray(
                board = outcome.nextBoard,
                mode = session.mode,
                missingGems = missingGems
            )
        } else {
            updatedTray.toList()
        }

        val levelWon = session.mode == GameMode.ADVENTURE &&
            missingGems.isEmpty() &&
            updatedScore >= session.targetScore

        val gameOver = !levelWon && !BlockEngine.canAnyPieceFit(outcome.nextBoard, nextTray)

        val bursts = outcome.clearedCells.take(18).map { (r, c) ->
            ParticleBurst(
                id = particleSeq++,
                row = r,
                col = c,
                color = piece.color.light
            )
        }

        if (levelWon) {
            soundFeedback.playWin(soundOn)
        } else if (outcome.linesClearedCount > 0) {
            soundFeedback.playClear(soundOn, outcome.nextComboCount)
        } else {
            soundFeedback.playPlace(soundOn)
        }

        if (hapticsOn && (outcome.linesClearedCount > 0 || levelWon)) {
            _hapticPulseTick.update { it + 1 }
        }

        _gameState.value = session.copy(
            board = outcome.nextBoard,
            tray = nextTray,
            score = updatedScore,
            comboCount = outcome.nextComboCount,
            bestComboInMatch = updatedBestCombo,
            comboProgress = outcome.nextComboProgress,
            multiplier = outcome.nextMultiplier,
            linesCleared = updatedLines,
            gemsCollected = updatedGems,
            isGameOver = gameOver,
            isLevelComplete = levelWon,
            floatingBanner = outcome.bannerText,
            lastPointsGained = outcome.pointsGained,
            clearingCells = outcome.clearedCells,
            particles = bursts
        )

        if (outcome.clearedCells.isNotEmpty() || outcome.bannerText != null) {
            viewModelScope.launch {
                delay(650)
                _gameState.update { it.copy(clearingCells = emptySet(), particles = emptyList(), floatingBanner = null) }
            }
        }

        if (levelWon || gameOver) {
            persistMatchResult(
                mode = if (session.mode == GameMode.ADVENTURE) "adventure" else "classic",
                levelNumber = session.levelNumber,
                score = updatedScore,
                bestCombo = updatedBestCombo.toLong(),
                linesCleared = updatedLines.toLong(),
                won = levelWon
            )
        }

        return true
    }

    private fun persistMatchResult(
        mode: String,
        levelNumber: Int,
        score: Long,
        bestCombo: Long,
        linesCleared: Long,
        won: Boolean
    ) {
        viewModelScope.launch {
            val result = repository.recordGameResult(
                userId = currentUserId,
                mode = mode,
                levelNumber = levelNumber,
                score = score,
                bestCombo = bestCombo,
                linesCleared = linesCleared,
                won = won,
                starsEarned = if (won) 3 else 0
            )
            result.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Gagal menyimpan hasil permainan"
            }
        }
    }

    fun dismissWelcomeLetter() {
        _manualWelcomeOpen.value = false
        viewModelScope.launch {
            repository.markWelcomeLetterSeen(currentUserId)
        }
    }

    fun openWelcomeLetter() {
        _manualWelcomeOpen.value = true
    }

    fun toggleSound() {
        val current = (progressState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = repository.updateSettings(
                userId = currentUserId,
                soundEnabled = !current.soundEnabled,
                hapticsEnabled = current.hapticsEnabled
            )
            res.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Gagal menyimpan pengaturan"
            }
        }
    }

    fun toggleHaptics() {
        val current = (progressState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = repository.updateSettings(
                userId = currentUserId,
                soundEnabled = current.soundEnabled,
                hapticsEnabled = !current.hapticsEnabled
            )
            res.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "Gagal menyimpan pengaturan"
            }
        }
    }

    fun clearError() {
        _actionError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        soundFeedback.release()
    }

    private companion object {
        const val TAG = "BlockCrushVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
