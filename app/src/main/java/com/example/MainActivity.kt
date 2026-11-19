package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AbujaMemoryRepository
import com.example.data.GeminiTransitRepository
import com.example.data.OsmTransitService
import com.example.data.local.AbujaMemoryDatabase
import com.example.firebase.configureAppCheck
import com.example.ui.WhereViewModel
import com.example.ui.screens.AbujaMemoryScreen
import com.example.ui.screens.AiGuideScreen
import com.example.ui.screens.CommuterWorkspace
import com.example.ui.screens.LandingScreen
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereMainText
import com.example.ui.theme.WhereMintBorder
import com.example.ui.theme.WhereMintSurface
import com.example.ui.theme.WhereMutedText
import com.example.ui.theme.WhereSubtleBorder
import com.example.ui.theme.WhereTheme
import com.example.ui.theme.WhereWhite
import kotlinx.serialization.Serializable

@Serializable
data object RouteDestination

@Serializable
data object GuideDestination

@Serializable
data object MemoryDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            WhereTheme {
                WhereAppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
private fun WhereAppRoot() {
    var showLanding by rememberSaveable { mutableStateOf(true) }

    if (showLanding) {
        LandingScreen(
            onFindMyRoute = { showLanding = false }
        )
    } else {
        BackHandler {
            showLanding = true
        }
        WhereMainShell(
            onBackToLanding = { showLanding = true }
        )
    }
}

@Composable
private fun WhereMainShell(
    onBackToLanding: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: WhereViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dao = AbujaMemoryDatabase.getInstance(app).abujaMemoryDao()
                val osm = OsmTransitService()
                WhereViewModel(
                    memoryRepo = AbujaMemoryRepository(dao),
                    osmService = osm,
                    geminiRepo = GeminiTransitRepository(osm)
                )
            }
        }
    )

    val originInput by viewModel.originInput.collectAsStateWithLifecycle()
    val destinationInput by viewModel.destinationInput.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val selectedOriginPlace by viewModel.selectedOriginPlace.collectAsStateWithLifecycle()
    val selectedDestinationPlace by viewModel.selectedDestinationPlace.collectAsStateWithLifecycle()
    val originSuggestions by viewModel.originSuggestions.collectAsStateWithLifecycle()
    val destinationSuggestions by viewModel.destinationSuggestions.collectAsStateWithLifecycle()
    val searchNotice by viewModel.searchNotice.collectAsStateWithLifecycle()
    val pickMapTarget by viewModel.pickMapTarget.collectAsStateWithLifecycle()
    val routePlanState by viewModel.routePlanState.collectAsStateWithLifecycle()
    val activeTripSession by viewModel.activeTripSession.collectAsStateWithLifecycle()
    val showPostTripSurvey by viewModel.showPostTripSurvey.collectAsStateWithLifecycle()
    val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
    val learnedCorridors by viewModel.learnedCorridors.collectAsStateWithLifecycle()
    val tripSurveys by viewModel.tripSurveys.collectAsStateWithLifecycle()
    val savedRoutes by viewModel.savedRoutes.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatSending by viewModel.isChatSending.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val isRouteSelected = currentDestination?.hasRoute<RouteDestination>() == true
    val isGuideSelected = currentDestination?.hasRoute<GuideDestination>() == true
    val isMemorySelected = currentDestination?.hasRoute<MemoryDestination>() == true

    fun navigateTopLevel(dest: Any) {
        navController.navigate(dest) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val showHeaderTabs = maxWidth >= 840.dp

        Scaffold(
            containerColor = WhereWhite,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                WhereLightHeader(
                    showHeaderTabs = showHeaderTabs,
                    isRouteSelected = isRouteSelected,
                    isGuideSelected = isGuideSelected,
                    isMemorySelected = isMemorySelected,
                    onLogoClick = onBackToLanding,
                    onSelectTab = { dest -> navigateTopLevel(dest) }
                )
            },
            bottomBar = {
                if (!showHeaderTabs) {
                    NavigationBar(
                        containerColor = WhereWhite,
                        tonalElevation = 0.dp,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WhereWhite,
                            selectedTextColor = WhereDeepGreen,
                            indicatorColor = WhereDeepGreen,
                            unselectedIconColor = WhereMutedText,
                            unselectedTextColor = WhereMutedText
                        )
                        NavigationBarItem(
                            selected = isRouteSelected,
                            onClick = { navigateTopLevel(RouteDestination) },
                            colors = navColors,
                            icon = {
                                Icon(
                                    imageVector = if (isRouteSelected) Icons.Filled.Explore else Icons.Outlined.Explore,
                                    contentDescription = "Route"
                                )
                            },
                            label = { Text("Route") },
                            modifier = Modifier.testTag("nav_route")
                        )
                        NavigationBarItem(
                            selected = isGuideSelected,
                            onClick = { navigateTopLevel(GuideDestination) },
                            colors = navColors,
                            icon = {
                                Icon(
                                    imageVector = if (isGuideSelected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                    contentDescription = "AI Guide"
                                )
                            },
                            label = { Text("AI Guide") },
                            modifier = Modifier.testTag("nav_guide")
                        )
                        NavigationBarItem(
                            selected = isMemorySelected,
                            onClick = { navigateTopLevel(MemoryDestination) },
                            colors = navColors,
                            icon = {
                                Icon(
                                    imageVector = if (isMemorySelected) Icons.Filled.Psychology else Icons.Outlined.Psychology,
                                    contentDescription = "AI Memory"
                                )
                            },
                            label = { Text("AI Memory") },
                            modifier = Modifier.testTag("nav_memory")
                        )
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
                    startDestination = RouteDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<RouteDestination> {
                        CommuterWorkspace(
                            originInput = originInput,
                            destinationInput = destinationInput,
                            userLocation = userLocation,
                            selectedOriginPlace = selectedOriginPlace,
                            selectedDestinationPlace = selectedDestinationPlace,
                            originSuggestions = originSuggestions,
                            destinationSuggestions = destinationSuggestions,
                            searchNotice = searchNotice,
                            pickMapTarget = pickMapTarget,
                            routePlanState = routePlanState,
                            activeTripSession = activeTripSession,
                            showPostTripSurvey = showPostTripSurvey,
                            savedPlaces = savedPlaces,
                            learnedCorridors = learnedCorridors,
                            onOriginChange = viewModel::updateOrigin,
                            onDestinationChange = viewModel::updateDestination,
                            onSelectOriginSuggestion = viewModel::selectOriginSuggestion,
                            onSelectDestinationSuggestion = viewModel::selectDestinationSuggestion,
                            onSwapLocations = viewModel::swapLocations,
                            onGpsResolved = { lat, lng, fromAutoInit ->
                                viewModel.applyGpsLocation(lat, lng, fromAutoInit)
                            },
                            onTogglePickOnMap = viewModel::togglePickOnMapMode,
                            onCancelPickOnMap = viewModel::cancelPickOnMap,
                            onMapTapped = viewModel::handleMapTap,
                            onSelectPopularPlace = viewModel::selectPopularDestination,
                            onSelectSavedPlace = { place -> viewModel.selectSavedPlace(place) },
                            onRunSampleTrip = viewModel::runSampleTrip,
                            onFindRoute = { viewModel.findRoute() },
                            onSelectRouteOption = viewModel::selectRouteOption,
                            onClearResult = viewModel::clearRouteResult,
                            onSavePlace = viewModel::savePlaceFromPlan,
                            onSaveRoute = viewModel::saveCurrentRoute,
                            onStartTrip = viewModel::startTrip,
                            onToggleStepCompleted = viewModel::toggleStepCompleted,
                            onRequestEndTrip = viewModel::requestEndTrip,
                            onDismissSurveyWithoutEnding = viewModel::dismissSurveyWithoutEnding,
                            onSubmitSurveyAndEnd = viewModel::submitTripSurveyAndEnd,
                            onOpenAiGuide = { navigateTopLevel(GuideDestination) }
                        )
                    }

                    composable<GuideDestination> {
                        AiGuideScreen(
                            messages = chatMessages,
                            learnedCorridorsCount = learnedCorridors.size,
                            isSending = isChatSending,
                            chatError = chatError,
                            onSendMessage = viewModel::sendChatMessage,
                            onRetryLastMessage = viewModel::retryLastChatMessage,
                            onResetChat = viewModel::resetChat
                        )
                    }

                    composable<MemoryDestination> {
                        AbujaMemoryScreen(
                            savedPlaces = savedPlaces,
                            learnedCorridors = learnedCorridors,
                            tripSurveys = tripSurveys,
                            savedRoutes = savedRoutes,
                            onLoadRoute = { origin, dest ->
                                viewModel.loadCorridorFromMemory(origin, dest)
                                navigateTopLevel(RouteDestination)
                            },
                            onSelectSavedPlace = { place ->
                                viewModel.selectSavedPlace(place)
                                navigateTopLevel(RouteDestination)
                            },
                            onDeleteSavedPlace = viewModel::deleteSavedPlace,
                            onDeleteSavedRoute = viewModel::deleteSavedRoute,
                            onTeachAiTip = viewModel::teachAiCustomTip,
                            onClearMemory = viewModel::clearAllMemory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhereLightHeader(
    showHeaderTabs: Boolean,
    isRouteSelected: Boolean,
    isGuideSelected: Boolean,
    isMemorySelected: Boolean,
    onLogoClick: () -> Unit,
    onSelectTab: (Any) -> Unit
) {
    Surface(
        color = WhereWhite,
        border = BorderStroke(1.dp, WhereSubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLogoClick)
                    .padding(end = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WhereDeepGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "WHERE",
                        tint = WhereWhite,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Text(
                    text = "WHERE",
                    color = WhereMainText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.5.sp
                )
            }

            if (showHeaderTabs) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderTabPill(
                        label = "Route",
                        selected = isRouteSelected,
                        onClick = { onSelectTab(RouteDestination) }
                    )
                    HeaderTabPill(
                        label = "AI Guide",
                        selected = isGuideSelected,
                        onClick = { onSelectTab(GuideDestination) }
                    )
                    HeaderTabPill(
                        label = "AI Memory",
                        selected = isMemorySelected,
                        onClick = { onSelectTab(MemoryDestination) }
                    )
                }
            } else {
                Surface(
                    color = WhereMintSurface,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, WhereMintBorder)
                ) {
                    Text(
                        text = "Abuja, FCT",
                        style = MaterialTheme.typography.labelSmall,
                        color = WhereDeepGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) WhereDeepGreen else WhereMintSurface,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) WhereDeepGreen else WhereMintBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) WhereWhite else WhereDeepGreen,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
