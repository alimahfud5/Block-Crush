package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOut
import com.example.data.BlockCrushRepository
import com.example.data.LocalProgressStore
import com.example.ui.BlockCrushViewModel
import com.example.ui.components.CloudSaveDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TutorialDialog
import com.example.ui.components.WelcomeEnvelopeOverlay
import com.example.ui.screens.AdventureMapScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BlockCrushTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ROUTE_HOME = "home"
private const val ROUTE_ADVENTURE_MAP = "adventure_map"
private const val ROUTE_GAME = "game/{mode}/{level}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockCrushTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    var splashDone by remember { mutableStateOf(false) }

    if (!splashDone) {
        SplashScreen(onFinished = { splashDone = true })
    } else {
        BlockCrushAppContent()
    }
}

@Composable
private fun BlockCrushAppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val viewModel: BlockCrushViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BlockCrushViewModel(
                    localStore = LocalProgressStore(app),
                    repository = BlockCrushRepository(db)
                )
            }
        }
    )

    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val lastCloudSyncMs by viewModel.lastCloudSyncMillis.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val gameSession by viewModel.gameState.collectAsStateWithLifecycle()
    val manualWelcomeOpen by viewModel.manualWelcomeOpen.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val hapticPulseTick by viewModel.hapticPulseTick.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    var showCloudSave by remember { mutableStateOf(false) }
    var isInGameRoute by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    val lastSyncFormatted = remember(lastCloudSyncMs) {
        if (lastCloudSyncMs > 0L) {
            SimpleDateFormat("dd MMM HH:mm", Locale.forLanguageTag("id-ID")).format(Date(lastCloudSyncMs))
        } else null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    ) { _ ->
        val showWelcomeLetter = manualWelcomeOpen ?: !progress.welcomeLetterSeen

        NavHost(
            navController = navController,
            startDestination = ROUTE_HOME
        ) {
            composable(ROUTE_HOME) {
                isInGameRoute = false
                HomeScreen(
                    progress = progress,
                    onPlayAdventure = {
                        navController.navigate(ROUTE_ADVENTURE_MAP) {
                            launchSingleTop = true
                        }
                    },
                    onPlayClassic = {
                        viewModel.startClassicGame()
                        navController.navigate("game/classic/0")
                    },
                    onOpenWelcomeLetter = { viewModel.openWelcomeLetter() },
                    onOpenCloudSave = { showCloudSave = true },
                    onOpenTutorial = { showTutorial = true },
                    onOpenSettings = { showSettings = true }
                )
            }

            composable(ROUTE_ADVENTURE_MAP) {
                isInGameRoute = false
                AdventureMapScreen(
                    progress = progress,
                    onBackHome = {
                        navController.popBackStack(ROUTE_HOME, inclusive = false)
                    },
                    onStartLevel = { level ->
                        viewModel.startAdventureLevel(level)
                        navController.navigate("game/adventure/$level")
                    }
                )
            }

            composable(
                route = ROUTE_GAME,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("level") { type = NavType.IntType }
                )
            ) {
                isInGameRoute = true
                GameScreen(
                    session = gameSession,
                    progress = progress,
                    hapticPulseTick = hapticPulseTick,
                    onPlacePiece = { idx, r, c -> viewModel.placePiece(idx, r, c) },
                    onRestartGame = { viewModel.restartCurrentGame() },
                    onNextAdventureLevel = { nextLvl ->
                        viewModel.startAdventureLevel(nextLvl)
                    },
                    onOpenSettings = { showSettings = true },
                    onOpenCloudSave = { showCloudSave = true },
                    onBackToMenu = {
                        navController.popBackStack()
                    }
                )
            }
        }

        if (showWelcomeLetter && !isInGameRoute) {
            WelcomeEnvelopeOverlay(
                onDismiss = { viewModel.dismissWelcomeLetter() }
            )
        }

        if (showTutorial) {
            TutorialDialog(onDismiss = { showTutorial = false })
        }

        if (showSettings) {
            SettingsDialog(
                soundEnabled = progress.soundEnabled,
                hapticsEnabled = progress.hapticsEnabled,
                showGameControls = isInGameRoute,
                signedInUserEmail = currentUser?.email ?: currentUser?.displayName,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onOpenCloudSave = { showCloudSave = true },
                onRestartGame = { viewModel.restartCurrentGame() },
                onExitToMenu = {
                    navController.popBackStack(ROUTE_HOME, inclusive = false)
                },
                onSignOut = {
                    signOut(credentialManager, scope) {
                        viewModel.showMessage("Berhasil keluar dari akun Google.")
                    }
                },
                onDismiss = { showSettings = false }
            )
        }

        if (showCloudSave) {
            CloudSaveDialog(
                bestScore = progress.classicBestScore,
                bestCombo = progress.classicBestCombo,
                highestLevel = progress.highestAdventureLevel,
                completedCount = progress.completedLevels.size,
                signedInEmail = currentUser?.email ?: currentUser?.displayName,
                isSyncing = isCloudSyncing,
                lastSyncText = lastSyncFormatted,
                onSaveLocalNow = {
                    viewModel.saveLocalSnapshotNow()
                },
                onSignInAndSaveCloud = {
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                            viewModel.saveProgressManuallyToCloud()
                        },
                        onAuthError = { msg ->
                            viewModel.showMessage(msg)
                        },
                        scope = scope,
                        onAuthCancelled = {
                            viewModel.showMessage("Login Google dibatalkan.")
                        }
                    )
                },
                onSaveCloudNow = {
                    viewModel.saveProgressManuallyToCloud()
                },
                onSignOut = {
                    signOut(credentialManager, scope) {
                        viewModel.showMessage("Berhasil keluar dari akun Google.")
                    }
                },
                onDismiss = { showCloudSave = false }
            )
        }
    }
}
