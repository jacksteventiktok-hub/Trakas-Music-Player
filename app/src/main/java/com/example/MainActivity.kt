package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.InterfaceMode
import com.example.model.Song
import com.example.model.VisualizerStyle
import com.example.player.VisualizerStateHolder
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SongOptionsSheet
import com.example.ui.components.TrakasIconType
import com.example.ui.components.TrakasStyledIcon
import com.example.ui.screens.DriveModeScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.ExploreAiStatsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsOverlayScreen
import com.example.ui.screens.MetadataEditorDialog
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PartyModeScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.QueueSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioModeScreen
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalTrakasTokens
import com.example.ui.theme.TrakasTheme
import com.example.viewmodel.TrakasViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.serialization.Serializable

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

@Serializable data object HomeRoute
@Serializable data object LibraryRoute
@Serializable data object ExploreRoute
@Serializable data object PlaylistsRoute
@Serializable data object SettingsRoute
@Serializable data object EqualizerRoute
@Serializable data object PartyModeRoute
@Serializable data object DriveModeRoute
@Serializable data object StudioModeRoute

class MainActivity : ComponentActivity() {

    private val viewModel: TrakasViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        (application as TrakasApp).playbackController.connect()
    }

    override fun onStop() {
        super.onStop()
        (application as TrakasApp).playbackController.disconnect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModel, or Firebase call
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val activeTheme by viewModel.activeTheme.collectAsStateWithLifecycle()

            LaunchedEffect(prefs.animationsEnabled) {
                VisualizerStateHolder.isObserving = true
            }

            TrakasTheme(
                themeConfig = activeTheme,
                iconStyle = prefs.iconStyle,
                language = prefs.language,
                animationsEnabled = prefs.animationsEnabled,
                interfaceMode = prefs.interfaceMode,
                textScaleFactor = prefs.textScaleFactor
            ) {
                TrakasMainScaffold(viewModel = viewModel)
            }
        }
    }
}

private data class NavTabItem(
    val route: Any,
    val label: String,
    val iconType: TrakasIconType,
    val tag: String
)

@Composable
private fun TrakasMainScaffold(viewModel: TrakasViewModel) {
    val context = LocalContext.current
    val tokens = LocalTrakasTokens.current
    val strings = LocalAppStrings.current

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val visualizerConfig by viewModel.visualizerConfig.collectAsStateWithLifecycle()
    val themes by viewModel.allThemes.collectAsStateWithLifecycle()
    val activeTheme by viewModel.activeTheme.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val eqState by viewModel.eqState.collectAsStateWithLifecycle()
    val visualizerFrame by viewModel.visualizerFrame.collectAsStateWithLifecycle()
    val audioOutputStatus by viewModel.audioOutputStatus.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasAudioPermission.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statsPeriod by viewModel.statsPeriod.collectAsStateWithLifecycle()
    val statsSummary by viewModel.statsSummary.collectAsStateWithLifecycle()
    val replaySummary by viewModel.replaySummary.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val libraryTarget by viewModel.libraryTarget.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val requiredPermissions = remember {
        buildList {
            add(audioPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results[audioPermission] == true ||
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(audioGranted)
    }

    val importAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioFiles(uris)
        }
    }

    var pendingBackupFlags by remember { mutableStateOf(Triple(true, true, true)) }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(
                uri = uri,
                includePlaylists = pendingBackupFlags.first,
                includeThemes = pendingBackupFlags.second,
                includeSettings = pendingBackupFlags.third
            )
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromUri(uri)
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onPermissionResult(true)
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var optionSong by remember { mutableStateOf<Song?>(null) }
    var lyricsSong by remember { mutableStateOf<Song?>(null) }
    var metadataEditSong by remember { mutableStateOf<Song?>(null) }

    val navItems = listOf(
        NavTabItem(HomeRoute, strings.navHome, TrakasIconType.HOME, "nav_home"),
        NavTabItem(LibraryRoute, strings.navLibrary, TrakasIconType.LIBRARY, "nav_library"),
        NavTabItem(ExploreRoute, strings.navExplore, TrakasIconType.EXPLORE, "nav_explore"),
        NavTabItem(PlaylistsRoute, strings.navPlaylists, TrakasIconType.PLAYLISTS, "nav_playlists"),
        NavTabItem(SettingsRoute, strings.navSettings, TrakasIconType.SETTINGS, "nav_settings")
    )

    val isFullScreenMode = currentDestination?.hasRoute(PartyModeRoute::class) == true ||
        currentDestination?.hasRoute(DriveModeRoute::class) == true ||
        currentDestination?.hasRoute(StudioModeRoute::class) == true

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun activateInterfaceMode(mode: InterfaceMode) {
        viewModel.setInterfaceMode(mode)
        when (mode) {
            InterfaceMode.TRAKAS_PARTY -> navController.navigate(PartyModeRoute)
            InterfaceMode.TRAKAS_DRIVE -> navController.navigate(DriveModeRoute)
            InterfaceMode.TRAKAS_STUDIO -> navController.navigate(StudioModeRoute)
            InterfaceMode.TRAKAS_PLAYER, InterfaceMode.TRAKAS_NIGHT -> Unit
        }
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val idx = speeds.indexOfFirst { kotlin.math.abs(it - playbackState.playbackSpeed) < 0.05f }
        val next = speeds[(idx + 1) % speeds.size]
        viewModel.setPlaybackSpeed(next)
    }

    fun cycleVisualizer() {
        val entries = VisualizerStyle.entries
        val next = entries[(entries.indexOf(prefs.visualizerStyle) + 1) % entries.size]
        viewModel.setVisualizerStyle(next)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        val useNavRail = maxWidth >= 640.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail && !isFullScreenMode) {
                NavigationRail(
                    containerColor = tokens.surfaceElevated,
                    contentColor = tokens.textColor,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    navItems.forEach { item ->
                        val selected = when (item.route) {
                            HomeRoute -> currentDestination?.hasRoute(HomeRoute::class) == true
                            LibraryRoute -> currentDestination?.hasRoute(LibraryRoute::class) == true
                            ExploreRoute -> currentDestination?.hasRoute(ExploreRoute::class) == true
                            PlaylistsRoute -> currentDestination?.hasRoute(PlaylistsRoute::class) == true
                            SettingsRoute -> currentDestination?.hasRoute(SettingsRoute::class) == true
                            else -> false
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopTab(item.route) },
                            icon = {
                                TrakasStyledIcon(
                                    type = item.iconType,
                                    contentDescription = item.label,
                                    active = selected,
                                    size = 22.dp
                                )
                            },
                            label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = tokens.primary,
                                selectedTextColor = tokens.primary,
                                indicatorColor = tokens.primary.copy(alpha = 0.18f),
                                unselectedIconColor = tokens.mutedTextColor,
                                unselectedTextColor = tokens.mutedTextColor
                            ),
                            modifier = Modifier.testTag(item.tag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = tokens.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useNavRail && !isFullScreenMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tokens.background.copy(alpha = 0.94f))
                        ) {
                            if (playbackState.currentSong != null && !showNowPlaying) {
                                MiniPlayerBar(
                                    playbackState = playbackState,
                                    onExpand = { showNowPlaying = true },
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onSkipNext = { viewModel.skipNext() },
                                    onSkipPrevious = { viewModel.skipPrevious() }
                                )
                            }
                            NavigationBar(
                                containerColor = tokens.surfaceElevated,
                                tonalElevation = 8.dp,
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                navItems.forEach { item ->
                                    val selected = when (item.route) {
                                        HomeRoute -> currentDestination?.hasRoute(HomeRoute::class) == true
                                        LibraryRoute -> currentDestination?.hasRoute(LibraryRoute::class) == true
                                        ExploreRoute -> currentDestination?.hasRoute(ExploreRoute::class) == true
                                        PlaylistsRoute -> currentDestination?.hasRoute(PlaylistsRoute::class) == true
                                        SettingsRoute -> currentDestination?.hasRoute(SettingsRoute::class) == true
                                        else -> false
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopTab(item.route) },
                                        icon = {
                                            TrakasStyledIcon(
                                                type = item.iconType,
                                                contentDescription = item.label,
                                                active = selected,
                                                size = 22.dp
                                            )
                                        },
                                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = tokens.primary,
                                            selectedTextColor = tokens.primary,
                                            indicatorColor = tokens.primary.copy(alpha = 0.18f),
                                            unselectedIconColor = tokens.mutedTextColor,
                                            unselectedTextColor = tokens.mutedTextColor
                                        ),
                                        modifier = Modifier.testTag(item.tag)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                libraryState = libraryState,
                                playbackState = playbackState,
                                visualizerFrame = visualizerFrame,
                                visualizerConfig = visualizerConfig,
                                audioOutputStatus = audioOutputStatus,
                                activeMode = prefs.interfaceMode,
                                hasPermission = hasPermission,
                                onRequestPermission = { permissionLauncher.launch(requiredPermissions) },
                                onImportAudio = { importAudioLauncher.launch(arrayOf("audio/*")) },
                                onCreateDemoTracks = { viewModel.createLocalDemoWavTracks() },
                                onRescanLibrary = { viewModel.rescanLibrary() },
                                onPlaySong = { song, list ->
                                    viewModel.playSong(song, list)
                                    showNowPlaying = true
                                },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onSkipPrevious = { viewModel.skipPrevious() },
                                onSeekTo = { viewModel.seekTo(it) },
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                onToggleSmartShuffle = { viewModel.toggleSmartShuffle() },
                                onCycleRepeat = { viewModel.cycleRepeatMode() },
                                onSetVolume = { viewModel.setVolume(it) },
                                onCycleSpeed = { cyclePlaybackSpeed() },
                                onOpenSleepTimer = { showSleepTimerDialog = true },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onSelectInterfaceMode = { activateInterfaceMode(it) },
                                onOpenNowPlaying = { showNowPlaying = true },
                                onOpenEqualizer = { navController.navigate(EqualizerRoute) },
                                onOpenQueue = { showQueueSheet = true },
                                onOpenLyrics = { playbackState.currentSong?.let { lyricsSong = it } },
                                onOpenExploreAi = { navigateTopTab(ExploreRoute) },
                                onSongLongClick = { optionSong = it }
                            )
                        }

                        composable<LibraryRoute> {
                            LibraryScreen(
                                libraryState = libraryState,
                                playbackState = playbackState,
                                searchQuery = searchQuery,
                                pendingTarget = libraryTarget,
                                onTargetConsumed = { viewModel.consumeLibraryTarget() },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onPlaySong = { song, list ->
                                    viewModel.playSong(song, list)
                                    showNowPlaying = true
                                },
                                onPlayOrdered = { songs, idx ->
                                    viewModel.playOrdered(songs, idx)
                                    showNowPlaying = true
                                },
                                onShufflePlay = { songs ->
                                    viewModel.playShuffled(songs)
                                    showNowPlaying = true
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onToggleAlbumFavorite = { viewModel.toggleAlbumFavorite(it) },
                                onAddToQueue = { viewModel.addToQueue(it) },
                                onPlaySongsNext = { viewModel.playSongsNext(it) },
                                onAddSongsToQueue = { viewModel.addSongsToQueue(it) },
                                onAddAlbumToPlaylist = { plId, songs ->
                                    songs.forEach { s -> viewModel.addSongToPlaylist(plId, s.id) }
                                },
                                onCreatePlaylistFromAlbum = { name, songs ->
                                    viewModel.createPlaylistFromSongs(name, songs)
                                },
                                onSongLongClick = { optionSong = it },
                                onRescanLibrary = { viewModel.rescanLibrary() },
                                onImportAudio = { importAudioLauncher.launch(arrayOf("audio/*")) },
                                onCreateDemoTracks = { viewModel.createLocalDemoWavTracks() }
                            )
                        }

                        composable<ExploreRoute> {
                            ExploreAiStatsScreen(
                                language = prefs.language,
                                allSongs = libraryState.visibleSongs,
                                currentSong = playbackState.currentSong,
                                aiState = aiState,
                                statsPeriod = statsPeriod,
                                statsSummary = statsSummary,
                                replaySummary = replaySummary,
                                onSelectStatsPeriod = { viewModel.setStatsPeriod(it) },
                                onRequestAi = { viewModel.requestAiGeneration(it) },
                                onFindLocalSimilar = { viewModel.findLocalSimilar(it) },
                                onPlaySongs = { list, idx ->
                                    viewModel.playSongs(list, idx)
                                    showNowPlaying = true
                                },
                                onSaveAiPlaylist = { name, songs -> viewModel.createPlaylistFromSongs(name, songs) }
                            )
                        }

                        composable<PlaylistsRoute> {
                            PlaylistsScreen(
                                libraryState = libraryState,
                                language = prefs.language,
                                onCreatePlaylist = { viewModel.createPlaylist(it) },
                                onCreatePlaylistFromSongs = { name, songs -> viewModel.createPlaylistFromSongs(name, songs) },
                                onRenamePlaylist = { id, name -> viewModel.renamePlaylist(id, name) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onAddSongToPlaylist = { plId, sId -> viewModel.addSongToPlaylist(plId, sId) },
                                onRemoveSongFromPlaylist = { plId, sId -> viewModel.removeSongFromPlaylist(plId, sId) },
                                onMovePlaylistSong = { plId, from, to -> viewModel.movePlaylistSong(plId, from, to) },
                                onPlayPlaylist = { list, idx ->
                                    viewModel.playSongs(list, idx)
                                    showNowPlaying = true
                                }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                prefs = prefs,
                                themes = themes,
                                activeTheme = activeTheme,
                                folders = libraryState.folders,
                                audioOutputStatus = audioOutputStatus,
                                onSelectTheme = { viewModel.selectTheme(it) },
                                onSaveCustomTheme = { viewModel.saveCustomTheme(it) },
                                onDeleteCustomTheme = { viewModel.deleteCustomTheme(it) },
                                onSetIconStyle = { viewModel.setIconStyle(it) },
                                onSetVisualizerStyle = { viewModel.setVisualizerStyle(it) },
                                onSetVisualizerIntensity = { viewModel.setVisualizerIntensity(it) },
                                onSetVisualizerSpeed = { viewModel.setVisualizerSpeed(it) },
                                onSetVisualizerSensitivity = { viewModel.setVisualizerSensitivity(it) },
                                onSetVisualizerAlpha = { viewModel.setVisualizerAlpha(it) },
                                onSetVisualizerParticles = { viewModel.setVisualizerParticles(it) },
                                onSetVisualizerUseAlbumColors = { viewModel.setVisualizerUseAlbumColors(it) },
                                onSetAudioQuality = { viewModel.setAudioQuality(it) },
                                onSetGapless = { viewModel.setGaplessPlayback(it) },
                                onSetCrossfade = { viewModel.setCrossfadeSeconds(it) },
                                onSetNormalizeVolume = { viewModel.setNormalizeVolume(it) },
                                onSetReplayGain = { viewModel.setReplayGainEnabled(it) },
                                onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
                                onSetAnimations = { viewModel.setAnimationsEnabled(it) },
                                onSetNotifications = { viewModel.setNotificationsEnabled(it) },
                                onSetLockscreen = { viewModel.setLockscreenControlsEnabled(it) },
                                onSetBluetoothAutoPlay = { viewModel.setBluetoothAutoPlay(it) },
                                onSetLanguage = { viewModel.setLanguage(it) },
                                onToggleFolderExcluded = { viewModel.toggleFolderExcluded(it) },
                                onRescanLibrary = { viewModel.rescanLibrary() },
                                onOpenEqualizer = { navController.navigate(EqualizerRoute) },
                                onExportBackup = { pl, th, st ->
                                    pendingBackupFlags = Triple(pl, th, st)
                                    exportBackupLauncher.launch("trakas_backup.json")
                                },
                                onImportBackup = {
                                    importBackupLauncher.launch(arrayOf("application/json", "text/plain"))
                                },
                                onSetPlayerDynamicBg = { viewModel.setPlayerDynamicBackground(it) },
                                onSetPlayerVisualizerEnabled = { viewModel.setPlayerVisualizerEnabled(it) },
                                onSetImmersivePlayer = { viewModel.setImmersivePlayerMode(it) },
                                onSetTextScaleFactor = { viewModel.setTextScaleFactor(it) }
                            )
                        }

                        composable<EqualizerRoute> {
                            EqualizerScreen(
                                eqState = eqState,
                                onToggleEnabled = { viewModel.eqSetEnabled(it) },
                                onSelectPreset = { viewModel.eqSelectPreset(it) },
                                onBandChange = { idx, lvl -> viewModel.eqSetBandLevel(idx, lvl) },
                                onBassBoostChange = { viewModel.eqSetBassBoost(it) },
                                onVirtualizerChange = { viewModel.eqSetVirtualizer(it) },
                                onBalanceChange = { viewModel.eqSetBalance(it) },
                                onBassToneChange = { viewModel.eqSetBassTone(it) },
                                onTrebleToneChange = { viewModel.eqSetTrebleTone(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<PartyModeRoute> {
                            PartyModeScreen(
                                playbackState = playbackState,
                                visualizerFrame = visualizerFrame,
                                visualizerConfig = visualizerConfig,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onSkipPrevious = { viewModel.skipPrevious() },
                                onCycleVisualizerStyle = { cycleVisualizer() },
                                onExit = {
                                    viewModel.setInterfaceMode(InterfaceMode.TRAKAS_PLAYER)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<DriveModeRoute> {
                            val currentId = playbackState.currentSong?.id
                            DriveModeScreen(
                                playbackState = playbackState,
                                isFavorite = currentId != null && currentId in libraryState.favoriteIds,
                                audioOutputStatus = audioOutputStatus,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onSkipPrevious = { viewModel.skipPrevious() },
                                onToggleFavorite = { currentId?.let { viewModel.toggleFavorite(it) } },
                                onSetVolume = { viewModel.setVolume(it) },
                                onExit = {
                                    viewModel.setInterfaceMode(InterfaceMode.TRAKAS_PLAYER)
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable<StudioModeRoute> {
                            StudioModeScreen(
                                playbackState = playbackState,
                                prefs = prefs,
                                eqState = eqState,
                                visualizerFrame = visualizerFrame,
                                visualizerConfig = visualizerConfig,
                                audioOutputStatus = audioOutputStatus,
                                onOpenEqualizer = { navController.navigate(EqualizerRoute) },
                                onExit = {
                                    viewModel.setInterfaceMode(InterfaceMode.TRAKAS_PLAYER)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    if (useNavRail && playbackState.currentSong != null && !isFullScreenMode && !showNowPlaying) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                        ) {
                            MiniPlayerBar(
                                playbackState = playbackState,
                                onExpand = { showNowPlaying = true },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onSkipPrevious = { viewModel.skipPrevious() }
                            )
                        }
                    }
                }
            }
        }

        // Full-Screen Now Playing Overlay
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val currentId = playbackState.currentSong?.id
            NowPlayingScreen(
                playbackState = playbackState,
                eqState = eqState,
                isFavorite = currentId != null && currentId in libraryState.favoriteIds,
                visualizerFrame = visualizerFrame,
                visualizerConfig = visualizerConfig,
                audioOutputStatus = audioOutputStatus,
                dynamicBackgroundEnabled = prefs.playerDynamicBackground,
                visualizerEnabled = prefs.playerVisualizerEnabled,
                immersiveMode = prefs.immersivePlayerMode,
                textScaleFactor = prefs.textScaleFactor,
                onToggleDynamicBackground = { viewModel.setPlayerDynamicBackground(it) },
                onToggleVisualizerEnabled = { viewModel.setPlayerVisualizerEnabled(it) },
                onSelectVisualizerStyle = { viewModel.setVisualizerStyle(it) },
                onCycleVisualizerStyle = { cycleVisualizer() },
                onToggleImmersiveMode = { viewModel.setImmersivePlayerMode(it) },
                onSelectTextScale = { viewModel.setTextScaleFactor(it) },
                onMinimize = { showNowPlaying = false },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onSeekTo = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleSmartShuffle = { viewModel.toggleSmartShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onSetVolume = { viewModel.setVolume(it) },
                onCycleSpeed = { cyclePlaybackSpeed() },
                onOpenSleepTimer = { showSleepTimerDialog = true },
                onToggleFavorite = { currentId?.let { viewModel.toggleFavorite(it) } },
                onOpenQueue = { showQueueSheet = true },
                onOpenFullEqualizer = {
                    showNowPlaying = false
                    navController.navigate(EqualizerRoute)
                },
                onOpenLyrics = { playbackState.currentSong?.let { lyricsSong = it } },
                onOpenMetadataEditor = { playbackState.currentSong?.let { metadataEditSong = it } },
                onOpenMoreOptions = { optionSong = it },
                onUpdateCoverArt = { song, uri -> viewModel.updateCoverArt(song, uri) },
                onArtistClick = { artist ->
                    showNowPlaying = false
                    viewModel.navigateToArtist(artist)
                    navigateTopTab(LibraryRoute)
                },
                onAlbumClick = { album ->
                    showNowPlaying = false
                    viewModel.navigateToAlbum(album, playbackState.currentSong?.albumId)
                    navigateTopTab(LibraryRoute)
                },
                onEqToggleEnabled = { viewModel.eqSetEnabled(it) },
                onEqSelectPreset = { viewModel.eqSelectPreset(it) },
                onEqBandChange = { idx, lvl -> viewModel.eqSetBandLevel(idx, lvl) },
                onEqBassBoostChange = { viewModel.eqSetBassBoost(it) },
                onEqVirtualizerChange = { viewModel.eqSetVirtualizer(it) },
                onEqBalanceChange = { viewModel.eqSetBalance(it) },
                onEqBassToneChange = { viewModel.eqSetBassTone(it) },
                onEqTrebleToneChange = { viewModel.eqSetTrebleTone(it) }
            )
        }

        // Lyrics Fullscreen Overlay
        val activeLyricsSong = lyricsSong
        if (activeLyricsSong != null) {
            LyricsOverlayScreen(
                song = activeLyricsSong,
                rawLyrics = libraryState.lyricsMap[activeLyricsSong.id],
                playbackState = playbackState,
                prefs = prefs,
                onDismiss = { lyricsSong = null },
                onSaveLyrics = { id, content -> viewModel.saveLyrics(id, content) },
                onSetKaraoke = { viewModel.setLyricsKaraoke(it) },
                onSetFontSize = { viewModel.setLyricsFontSize(it) },
                onSetFontKey = { viewModel.setLyricsFontKey(it) },
                onSetTextColor = { viewModel.setLyricsTextColor(it) },
                onSetHighlightColor = { viewModel.setLyricsHighlightColor(it) },
                onSetDynamicBg = { viewModel.setLyricsDynamicBg(it) }
            )
        }

        if (showQueueSheet) {
            QueueSheet(
                playbackState = playbackState,
                librarySongs = libraryState.visibleSongs,
                onDismiss = { showQueueSheet = false },
                onJumpTo = { viewModel.jumpToQueueIndex(it) },
                onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
                onRemoveItem = { viewModel.removeFromQueue(it) },
                onAddSongToQueue = { viewModel.addToQueue(it) },
                onClearQueue = { viewModel.clearQueue() }
            )
        }

        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                containerColor = tokens.surfaceElevated,
                title = { Text(strings.sleepTimer, color = tokens.textColor) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 30, 45, 60).forEach { mins ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.startSleepTimer(mins)
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$mins minutos", color = tokens.textColor)
                                Text("Apagar audio", color = tokens.primary)
                            }
                        }
                        if (playbackState.sleepTimerRemainingSec != null) {
                            TextButton(onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepTimerDialog = false
                            }) {
                                Text("Desactivar temporizador", color = tokens.secondary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text(strings.cancel, color = tokens.mutedTextColor)
                    }
                }
            )
        }

        val selectedOptionSong = optionSong
        if (selectedOptionSong != null) {
            SongOptionsSheet(
                song = selectedOptionSong,
                isFavorite = selectedOptionSong.id in libraryState.favoriteIds,
                playlists = libraryState.playlists,
                onDismiss = { optionSong = null },
                onPlayNow = {
                    viewModel.playSong(selectedOptionSong)
                    showNowPlaying = true
                },
                onPlayNext = { viewModel.playNext(selectedOptionSong) },
                onAddToQueue = { viewModel.addToQueue(selectedOptionSong) },
                onToggleFavorite = { viewModel.toggleFavorite(selectedOptionSong.id) },
                onAddToPlaylist = { plId -> viewModel.addSongToPlaylist(plId, selectedOptionSong.id) },
                onCreatePlaylistWithSong = { name -> viewModel.createPlaylist(name, selectedOptionSong.id) },
                onViewArtist = { artist ->
                    showNowPlaying = false
                    viewModel.navigateToArtist(artist)
                    navigateTopTab(LibraryRoute)
                },
                onViewAlbum = { album ->
                    showNowPlaying = false
                    viewModel.navigateToAlbum(album, selectedOptionSong.albumId)
                    navigateTopTab(LibraryRoute)
                },
                onEditMetadata = { metadataEditSong = selectedOptionSong },
                onChangeCoverArt = { uri -> viewModel.updateCoverArt(selectedOptionSong, uri) },
                onViewLyrics = { lyricsSong = selectedOptionSong },
                onLoadTechnicalInfo = { viewModel.loadTechnicalInfo(it) },
                onRemoveFromLibrary = { viewModel.removeFromLibrary(it) }
            )
        }

        val editSong = metadataEditSong
        if (editSong != null) {
            MetadataEditorDialog(
                song = editSong,
                onDismiss = { metadataEditSong = null },
                onSave = { sId, title, artist, album, genre, year, track, albumArtist, comment, artUri ->
                    viewModel.saveMetadataOverride(sId, title, artist, album, genre, year, track, albumArtist, comment, artUri)
                }
            )
        }
    }
}
