package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PlayerProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Pemain",
    val classicBestScore: Long = 0L,
    val classicBestCombo: Long = 0L,
    val highestAdventureLevel: Long = 1L,
    val completedLevels: List<Long> = emptyList(),
    val totalStars: Long = 0L,
    val totalLinesCleared: Long = 0L,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val welcomeLetterSeen: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "classic",
    val levelNumber: Long = 0L,
    val score: Long = 0L,
    val bestCombo: Long = 0L,
    val linesCleared: Long = 0L,
    val won: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
