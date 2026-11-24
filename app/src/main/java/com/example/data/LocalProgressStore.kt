package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.TOTAL_ADVENTURE_LEVELS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.blockCrushDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "block_crush_local"
)

class LocalProgressStore(private val context: Context) {

    private object Keys {
        val CLASSIC_BEST_SCORE = longPreferencesKey("classic_best_score")
        val CLASSIC_BEST_COMBO = longPreferencesKey("classic_best_combo")
        val HIGHEST_ADVENTURE_LEVEL = longPreferencesKey("highest_adventure_level")
        val COMPLETED_LEVELS = stringPreferencesKey("completed_levels")
        val TOTAL_STARS = longPreferencesKey("total_stars")
        val TOTAL_LINES_CLEARED = longPreferencesKey("total_lines_cleared")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val WELCOME_LETTER_SEEN = booleanPreferencesKey("welcome_letter_seen")
        val LAST_CLOUD_SYNC_MS = longPreferencesKey("last_cloud_sync_ms")
    }

    val progressFlow: Flow<PlayerProgress> = context.blockCrushDataStore.data.map { prefs ->
        val completed = prefs[Keys.COMPLETED_LEVELS]
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.filter { it in 1L..TOTAL_ADVENTURE_LEVELS.toLong() }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

        PlayerProgress(
            userId = "",
            displayName = "Pemain",
            classicBestScore = prefs[Keys.CLASSIC_BEST_SCORE] ?: 0L,
            classicBestCombo = prefs[Keys.CLASSIC_BEST_COMBO] ?: 0L,
            highestAdventureLevel = (prefs[Keys.HIGHEST_ADVENTURE_LEVEL] ?: 1L)
                .coerceIn(1L, TOTAL_ADVENTURE_LEVELS.toLong()),
            completedLevels = completed,
            totalStars = prefs[Keys.TOTAL_STARS] ?: 0L,
            totalLinesCleared = prefs[Keys.TOTAL_LINES_CLEARED] ?: 0L,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            welcomeLetterSeen = prefs[Keys.WELCOME_LETTER_SEEN] ?: false
        )
    }

    val lastCloudSyncFlow: Flow<Long> = context.blockCrushDataStore.data.map { prefs ->
        prefs[Keys.LAST_CLOUD_SYNC_MS] ?: 0L
    }

    suspend fun getSnapshot(): PlayerProgress = progressFlow.first()

    suspend fun recordGameResult(
        mode: String,
        levelNumber: Int,
        score: Long,
        bestCombo: Long,
        linesCleared: Long,
        won: Boolean
    ) {
        context.blockCrushDataStore.edit { prefs ->
            val currentBestScore = prefs[Keys.CLASSIC_BEST_SCORE] ?: 0L
            val currentBestCombo = prefs[Keys.CLASSIC_BEST_COMBO] ?: 0L
            val currentHighest = (prefs[Keys.HIGHEST_ADVENTURE_LEVEL] ?: 1L).coerceIn(1L, 96L)
            val currentTotalLines = prefs[Keys.TOTAL_LINES_CLEARED] ?: 0L
            val currentCompleted = prefs[Keys.COMPLETED_LEVELS]
                ?.split(",")
                ?.mapNotNull { it.trim().toLongOrNull() }
                ?.filter { it in 1L..96L }
                ?.toMutableSet()
                ?: mutableSetOf()

            if (mode == "classic") {
                prefs[Keys.CLASSIC_BEST_SCORE] = maxOf(currentBestScore, score)
            }
            prefs[Keys.CLASSIC_BEST_COMBO] = maxOf(currentBestCombo, bestCombo)
            prefs[Keys.TOTAL_LINES_CLEARED] = currentTotalLines + linesCleared

            if (mode == "adventure" && won) {
                val safeLvl = levelNumber.coerceIn(1, 96).toLong()
                currentCompleted.add(safeLvl)
                val sortedCompleted = currentCompleted.sorted()
                prefs[Keys.COMPLETED_LEVELS] = sortedCompleted.joinToString(",")
                prefs[Keys.HIGHEST_ADVENTURE_LEVEL] = maxOf(currentHighest, (safeLvl + 1L).coerceAtMost(96L))
                prefs[Keys.TOTAL_STARS] = (sortedCompleted.size * 3L).coerceAtMost(1000L)
            }
        }
    }

    suspend fun updateSettings(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        context.blockCrushDataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = soundEnabled
            prefs[Keys.HAPTICS_ENABLED] = hapticsEnabled
        }
    }

    suspend fun markWelcomeLetterSeen() {
        context.blockCrushDataStore.edit { prefs ->
            prefs[Keys.WELCOME_LETTER_SEEN] = true
        }
    }

    suspend fun applyMergedProgress(merged: PlayerProgress, syncTimestampMillis: Long) {
        context.blockCrushDataStore.edit { prefs ->
            prefs[Keys.CLASSIC_BEST_SCORE] = merged.classicBestScore
            prefs[Keys.CLASSIC_BEST_COMBO] = merged.classicBestCombo
            prefs[Keys.HIGHEST_ADVENTURE_LEVEL] = merged.highestAdventureLevel.coerceIn(1L, 96L)
            prefs[Keys.COMPLETED_LEVELS] = merged.completedLevels.distinct().sorted().joinToString(",")
            prefs[Keys.TOTAL_STARS] = merged.totalStars
            prefs[Keys.TOTAL_LINES_CLEARED] = merged.totalLinesCleared
            prefs[Keys.SOUND_ENABLED] = merged.soundEnabled
            prefs[Keys.HAPTICS_ENABLED] = merged.hapticsEnabled
            prefs[Keys.WELCOME_LETTER_SEEN] = merged.welcomeLetterSeen
            prefs[Keys.LAST_CLOUD_SYNC_MS] = syncTimestampMillis
        }
    }
}
