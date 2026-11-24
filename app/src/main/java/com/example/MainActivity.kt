package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.auth.AuthScreen
import com.example.auth.signOut
import com.example.data.BlockCrushRepository
import com.example.data.UiState
import com.example.ui.BlockCrushViewModel
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TutorialDialog
import com.example.ui.components.WelcomeEnvelopeOverlay
import com.example.ui.components.WoodBackground
import com.example.ui.screens.AdventureMapScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BlockCrushTheme
import com.example.ui.theme.CreamText
import com.example.ui.theme.CrownGold
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

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

    if (!splashDone) {
        SplashScreen(onFinished = { splashDone = true })
    } else if (currentUser == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val user = currentUser!!
        val uid = user.uid
        val displayName = user.displayName ?: "Pemain"
        key(uid) {
            BlockCrushAppContent(
                currentUserId = uid,
                displayName = displayName
            )
        }
    }
}

@Composable
private fun BlockCrushAppContent(
    currentUserId: String,
    displayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: BlockCrushViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BlockCrushViewModel(
                    repository = BlockCrushRepository(db),
                    currentUserId = currentUserId,
                    displayName = displayName
                )
            }
        }
    )

    val progressUiState by viewModel.progressState.collectAsStateWithLifecycle()
    val gameSession by viewModel.gameState.collectAsStateWithLifecycle()
    val manualWelcomeOpen by viewModel.manualWelcomeOpen.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val hapticPulseTick by viewModel.hapticPulseTick.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionError) {
        val msg = actionError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }
    var isInGameRoute by remember { mutableStateOf(false) }

    val navController = rememberNavController()

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    ) { _ ->
        when (val state = progressUiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    WoodBackground()
                    CircularProgressIndicator(color = CrownGold)
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    WoodBackground()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CreamText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            signOut(credentialManager, scope)
                        }) {
                            Text("Keluar & Coba Lagi")
                        }
                    }
                }
            }

            is UiState.Success -> {
                val progress = state.data
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
                        onToggleSound = { viewModel.toggleSound() },
                        onToggleHaptics = { viewModel.toggleHaptics() },
                        onRestartGame = { viewModel.restartCurrentGame() },
                        onExitToMenu = {
                            navController.popBackStack(ROUTE_HOME, inclusive = false)
                        },
                        onSignOut = {
                            signOut(credentialManager, scope)
                        },
                        onDismiss = { showSettings = false }
                    )
                }
            }
        }
    }
}
