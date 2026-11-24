package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BlockCrushRepository
import com.example.data.LocalProgressStore
import com.example.data.PlayerProgress
import com.example.game.BlockEngine
import com.example.game.GameMode
import com.example.game.GameSessionState
import com.example.game.ParticleBurst
import com.example.game.SoundFeedback
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BlockCrushViewModel(
    private val localStore: LocalProgressStore,
    private val repository: BlockCrushRepository
) : ViewModel() {

    private val soundFeedback = SoundFeedback()
    private var particleSeq = 1L

    val progress: StateFlow<PlayerProgress> = localStore.progressFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PlayerProgress()
        )

    val lastCloudSyncMillis: StateFlow<Long> = localStore.lastCloudSyncFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0L
        )

    private val _gameState = MutableStateFlow(GameSessionState())
    val gameState: StateFlow<GameSessionState> = _gameState.asStateFlow()

    private val _manualWelcomeOpen = MutableStateFlow<Boolean?>(null)
    val manualWelcomeOpen: StateFlow<Boolean?> = _manualWelcomeOpen.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _hapticPulseTick = MutableStateFlow(0L)
    val hapticPulseTick: StateFlow<Long> = _hapticPulseTick.asStateFlow()

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

        val currentProgress = progress.value
        val soundOn = currentProgress.soundEnabled
        val hapticsOn = currentProgress.hapticsEnabled

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
                _gameState.update {
                    it.copy(clearingCells = emptySet(), particles = emptyList(), floatingBanner = null)
                }
            }
        }

        // Save local progress immediately whenever a score improves or match finishes
        val modeName = if (session.mode == GameMode.ADVENTURE) "adventure" else "classic"
        if (levelWon || gameOver || (session.mode == GameMode.CLASSIC && updatedScore > currentProgress.classicBestScore)) {
            viewModelScope.launch {
                localStore.recordGameResult(
                    mode = modeName,
                    levelNumber = session.levelNumber,
                    score = updatedScore,
                    bestCombo = updatedBestCombo.toLong(),
                    linesCleared = if (levelWon || gameOver) updatedLines.toLong() else 0L,
                    won = levelWon
                )
            }
        }

        return true
    }

    /**
     * Manual save triggered explicitly by the player inside the game.
     * Persists local snapshot to Firestore when signed in with Google.
     */
    fun saveProgressManuallyToCloud() {
        val user = Firebase.auth.currentUser
        if (user == null) {
            _statusMessage.value = "Masuk dengan Google terlebih dahulu untuk menyimpan ke Cloud."
            return
        }
        if (_isCloudSyncing.value) return

        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val localSnapshot = localStore.getSnapshot()
                val displayName = user.displayName ?: "Pemain"
                val syncResult = repository.syncLocalAndCloudProgress(
                    userId = user.uid,
                    displayName = displayName,
                    local = localSnapshot
                )
                syncResult.onSuccess { merged ->
                    localStore.applyMergedProgress(merged, System.currentTimeMillis())
                    val currentMatch = _gameState.value
                    if (currentMatch.score > 0L) {
                        val modeName = if (currentMatch.mode == GameMode.ADVENTURE) "adventure" else "classic"
                        repository.recordGameResult(
                            userId = user.uid,
                            mode = modeName,
                            levelNumber = currentMatch.levelNumber,
                            score = currentMatch.score,
                            bestCombo = currentMatch.bestComboInMatch.toLong(),
                            linesCleared = currentMatch.linesCleared.toLong(),
                            won = currentMatch.isLevelComplete,
                            starsEarned = if (currentMatch.isLevelComplete) 3 else 0
                        )
                    }
                    _statusMessage.value = "Progres berhasil disimpan ke Cloud!"
                }.onFailure { err ->
                    _statusMessage.value = "Gagal menyimpan ke Cloud: ${err.localizedMessage ?: "Periksa koneksi internet"}"
                }
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun saveLocalSnapshotNow() {
        val session = _gameState.value
        val modeName = if (session.mode == GameMode.ADVENTURE) "adventure" else "classic"
        viewModelScope.launch {
            localStore.recordGameResult(
                mode = modeName,
                levelNumber = session.levelNumber,
                score = session.score,
                bestCombo = session.bestComboInMatch.toLong(),
                linesCleared = session.linesCleared.toLong(),
                won = session.isLevelComplete
            )
            _statusMessage.value = "Progres tersimpan di perangkat!"
        }
    }

    fun dismissWelcomeLetter() {
        _manualWelcomeOpen.value = false
        viewModelScope.launch {
            localStore.markWelcomeLetterSeen()
        }
    }

    fun openWelcomeLetter() {
        _manualWelcomeOpen.value = true
    }

    fun toggleSound() {
        val current = progress.value
        viewModelScope.launch {
            localStore.updateSettings(
                soundEnabled = !current.soundEnabled,
                hapticsEnabled = current.hapticsEnabled
            )
        }
    }

    fun toggleHaptics() {
        val current = progress.value
        viewModelScope.launch {
            localStore.updateSettings(
                soundEnabled = current.soundEnabled,
                hapticsEnabled = !current.hapticsEnabled
            )
        }
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        soundFeedback.release()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
