package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BlockCrushRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    fun observePlayerProgress(userId: String): Flow<PlayerProgress?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID pengguna tidak cocok." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        PlayerProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeRecentMatches(userId: String): Flow<List<MatchRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "UID pengguna tidak cocok." }
        val path = "users/$uid/matches"
        emitAll(
            db.collection("users").document(uid).collection("matches")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(15)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensurePlayerProgress(userId: String, displayName: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID pengguna tidak cocok." }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = displayName.trim().ifBlank { "Pemain" }.take(60)
                val initialPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "classicBestScore" to 0L,
                    "classicBestCombo" to 0L,
                    "highestAdventureLevel" to 1L,
                    "completedLevels" to emptyList<Long>(),
                    "totalStars" to 0L,
                    "totalLinesCleared" to 0L,
                    "soundEnabled" to true,
                    "hapticsEnabled" to true,
                    "welcomeLetterSeen" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(initialPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSettings(
        userId: String,
        soundEnabled: Boolean,
        hapticsEnabled: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID pengguna tidak cocok." }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "soundEnabled" to soundEnabled,
                        "hapticsEnabled" to hapticsEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markWelcomeLetterSeen(userId: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID pengguna tidak cocok." }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "welcomeLetterSeen" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncLocalAndCloudProgress(
        userId: String,
        displayName: String,
        local: PlayerProgress
    ): Result<PlayerProgress> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID pengguna tidak cocok." }
            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            val safeName = displayName.trim().ifBlank { "Pemain" }.take(60)

            val merged: PlayerProgress = if (snapshot.exists()) {
                val cloud = snapshot.toObject(
                    PlayerProgress::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PlayerProgress(userId = uid)

                val mergedCompleted = (cloud.completedLevels + local.completedLevels)
                    .filter { it in 1L..96L }
                    .distinct()
                    .sorted()
                val mergedHighest = maxOf(
                    cloud.highestAdventureLevel,
                    local.highestAdventureLevel,
                    ((mergedCompleted.maxOrNull() ?: 0L) + 1L).coerceAtMost(96L)
                ).coerceIn(1L, 96L)
                val mergedBestScore = maxOf(cloud.classicBestScore, local.classicBestScore)
                    .coerceIn(0L, 10_000_000L)
                val mergedBestCombo = maxOf(cloud.classicBestCombo, local.classicBestCombo)
                    .coerceIn(0L, 10_000L)
                val mergedTotalStars = maxOf(
                    cloud.totalStars,
                    local.totalStars,
                    (mergedCompleted.size * 3L)
                ).coerceIn(0L, 1000L)
                val mergedLines = maxOf(cloud.totalLinesCleared, local.totalLinesCleared)
                    .coerceIn(0L, 10_000_000L)
                val mergedWelcome = cloud.welcomeLetterSeen || local.welcomeLetterSeen

                userRef.update(
                    mapOf(
                        "displayName" to safeName,
                        "classicBestScore" to mergedBestScore,
                        "classicBestCombo" to mergedBestCombo,
                        "highestAdventureLevel" to mergedHighest,
                        "completedLevels" to mergedCompleted,
                        "totalStars" to mergedTotalStars,
                        "totalLinesCleared" to mergedLines,
                        "soundEnabled" to local.soundEnabled,
                        "hapticsEnabled" to local.hapticsEnabled,
                        "welcomeLetterSeen" to mergedWelcome,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                cloud.copy(
                    userId = uid,
                    displayName = safeName,
                    classicBestScore = mergedBestScore,
                    classicBestCombo = mergedBestCombo,
                    highestAdventureLevel = mergedHighest,
                    completedLevels = mergedCompleted,
                    totalStars = mergedTotalStars,
                    totalLinesCleared = mergedLines,
                    soundEnabled = local.soundEnabled,
                    hapticsEnabled = local.hapticsEnabled,
                    welcomeLetterSeen = mergedWelcome
                )
            } else {
                val safeCompleted = local.completedLevels
                    .filter { it in 1L..96L }
                    .distinct()
                    .sorted()
                val safeHighest = local.highestAdventureLevel.coerceIn(1L, 96L)
                val safeBestScore = local.classicBestScore.coerceIn(0L, 10_000_000L)
                val safeBestCombo = local.classicBestCombo.coerceIn(0L, 10_000L)
                val safeStars = local.totalStars.coerceIn(0L, 1000L)
                val safeLines = local.totalLinesCleared.coerceIn(0L, 10_000_000L)

                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "classicBestScore" to safeBestScore,
                        "classicBestCombo" to safeBestCombo,
                        "highestAdventureLevel" to safeHighest,
                        "completedLevels" to safeCompleted,
                        "totalStars" to safeStars,
                        "totalLinesCleared" to safeLines,
                        "soundEnabled" to local.soundEnabled,
                        "hapticsEnabled" to local.hapticsEnabled,
                        "welcomeLetterSeen" to local.welcomeLetterSeen,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                local.copy(
                    userId = uid,
                    displayName = safeName,
                    classicBestScore = safeBestScore,
                    classicBestCombo = safeBestCombo,
                    highestAdventureLevel = safeHighest,
                    completedLevels = safeCompleted,
                    totalStars = safeStars,
                    totalLinesCleared = safeLines
                )
            }

            Result.success(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordGameResult(
        userId: String,
        mode: String,
        levelNumber: Int,
        score: Long,
        bestCombo: Long,
        linesCleared: Long,
        won: Boolean,
        starsEarned: Int
    ): Result<Unit> {
        val userDocPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID pengguna tidak cocok." }
            val safeMode = if (mode == "adventure") "adventure" else "classic"
            val safeLevel = if (safeMode == "adventure") levelNumber.coerceIn(1, 96).toLong() else 0L
            val safeScore = score.coerceIn(0L, 10_000_000L)
            val safeCombo = bestCombo.coerceIn(0L, 10_000L)
            val safeLines = linesCleared.coerceIn(0L, 10_000L)

            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            val current = snapshot.toObject(
                PlayerProgress::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: PlayerProgress(userId = uid)

            val newBestScore = if (safeMode == "classic") {
                maxOf(current.classicBestScore, safeScore).coerceIn(0L, 10_000_000L)
            } else {
                current.classicBestScore.coerceIn(0L, 10_000_000L)
            }
            val newBestCombo = maxOf(current.classicBestCombo, safeCombo).coerceIn(0L, 10_000L)
            val newTotalLines = (current.totalLinesCleared + safeLines).coerceIn(0L, 10_000_000L)

            val newCompleted = if (safeMode == "adventure" && won) {
                (current.completedLevels + safeLevel)
                    .distinct()
                    .filter { it in 1L..96L }
                    .sorted()
            } else {
                current.completedLevels.filter { it in 1L..96L }.distinct().sorted()
            }

            val newHighestLevel = if (safeMode == "adventure" && won) {
                maxOf(current.highestAdventureLevel, (safeLevel + 1L).coerceAtMost(96L)).coerceIn(1L, 96L)
            } else {
                current.highestAdventureLevel.coerceIn(1L, 96L)
            }

            val newTotalStars = if (safeMode == "adventure" && won) {
                maxOf(current.totalStars, (newCompleted.size * 3L)).coerceAtMost(1000L)
            } else {
                current.totalStars.coerceIn(0L, 1000L)
            }

            if (snapshot.exists()) {
                userRef.update(
                    mapOf(
                        "classicBestScore" to newBestScore,
                        "classicBestCombo" to newBestCombo,
                        "highestAdventureLevel" to newHighestLevel,
                        "completedLevels" to newCompleted,
                        "totalStars" to newTotalStars,
                        "totalLinesCleared" to newTotalLines,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to (auth.currentUser?.displayName?.trim()?.ifBlank { "Pemain" }?.take(60) ?: "Pemain"),
                        "classicBestScore" to newBestScore,
                        "classicBestCombo" to newBestCombo,
                        "highestAdventureLevel" to newHighestLevel,
                        "completedLevels" to newCompleted,
                        "totalStars" to newTotalStars,
                        "totalLinesCleared" to newTotalLines,
                        "soundEnabled" to true,
                        "hapticsEnabled" to true,
                        "welcomeLetterSeen" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val matchRef = userRef.collection("matches").document()
            matchRef.set(
                mapOf(
                    "userId" to uid,
                    "mode" to safeMode,
                    "levelNumber" to safeLevel,
                    "score" to safeScore,
                    "bestCombo" to safeCombo,
                    "linesCleared" to safeLines,
                    "won" to won,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }
}
