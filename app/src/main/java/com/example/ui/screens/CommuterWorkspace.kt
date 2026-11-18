package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AbujaFareTable
import com.example.data.AbujaPopularPlace
import com.example.data.SampleTripPreset
import com.example.data.local.LearnedCorridorEntity
import com.example.data.local.SavedPlaceEntity
import com.example.model.ActiveTripSession
import com.example.model.NominatimPlace
import com.example.model.PickMapTarget
import com.example.model.PostTripSurveyInput
import com.example.model.RoutePlan
import com.example.ui.RoutePlanState
import com.example.ui.components.ActiveTripTrackerCard
import com.example.ui.components.LeafletAbujaMapView
import com.example.ui.components.PostTripSurveyDialog
import com.example.ui.components.RouteResultCard
import com.example.ui.theme.OverchargeAmber
import com.example.ui.theme.OverchargeSoftBg
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereInfoBg
import com.example.ui.theme.WhereInfoTitle
import com.example.ui.theme.WhereMainText
import com.example.ui.theme.WhereMintBorder
import com.example.ui.theme.WhereMintSurface
import com.example.ui.theme.WhereMutedText
import com.example.ui.theme.WhereSubtleBorder
import com.example.ui.theme.WhereWhite
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

private fun checkLocationGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun CommuterWorkspace(
    originInput: String,
    destinationInput: String,
    userLocation: Pair<Double, Double>?,
    selectedOriginPlace: NominatimPlace?,
    selectedDestinationPlace: NominatimPlace?,
    originSuggestions: List<NominatimPlace>,
    destinationSuggestions: List<NominatimPlace>,
    searchNotice: String?,
    pickMapTarget: PickMapTarget,
    routePlanState: RoutePlanState,
    activeTripSession: ActiveTripSession?,
    showPostTripSurvey: Boolean,
    savedPlaces: List<SavedPlaceEntity>,
    learnedCorridors: List<LearnedCorridorEntity>,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSelectOriginSuggestion: (NominatimPlace) -> Unit,
    onSelectDestinationSuggestion: (NominatimPlace) -> Unit,
    onSwapLocations: () -> Unit,
    onGpsResolved: (Double, Double, Boolean) -> Unit,
    onTogglePickOnMap: () -> Unit,
    onCancelPickOnMap: () -> Unit,
    onMapTapped: (Double, Double) -> Unit,
    onSelectPopularPlace: (AbujaPopularPlace) -> Unit,
    onSelectSavedPlace: (SavedPlaceEntity) -> Unit,
    onRunSampleTrip: (SampleTripPreset) -> Unit,
    onFindRoute: () -> Unit,
    onClearResult: () -> Unit,
    onSavePlace: (String, String, Double, Double) -> Unit,
    onSaveRoute: (RoutePlan) -> Unit,
    onStartTrip: (RoutePlan) -> Unit,
    onToggleStepCompleted: (Int) -> Unit,
    onRequestEndTrip: () -> Unit,
    onDismissSurveyWithoutEnding: () -> Unit,
    onSubmitSurveyAndEnd: (PostTripSurveyInput) -> Unit,
    onOpenAiGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasLocationPermission by remember { mutableStateOf(checkLocationGranted(context)) }
    var hasAutoPromptedLocation by rememberSaveable { mutableStateOf(false) }
    var wasDeniedOnce by rememberSaveable { mutableStateOf(false) }
    var gpsStatusNote by remember { mutableStateOf<String?>(null) }

    @SuppressLint("MissingPermission")
    fun fetchCurrentGps(fromAutoInit: Boolean) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation
            .addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    gpsStatusNote = null
                    onGpsResolved(lastLoc.latitude, lastLoc.longitude, fromAutoInit)
                } else {
                    val cts = CancellationTokenSource()
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { freshLoc ->
                            if (freshLoc != null) {
                                gpsStatusNote = null
                                onGpsResolved(freshLoc.latitude, freshLoc.longitude, fromAutoInit)
                            } else if (!fromAutoInit) {
                                gpsStatusNote = "GPS fix unavailable yet; type an Abuja area or tap Pick on map."
                            }
                        }
                        .addOnFailureListener {
                            if (!fromAutoInit) {
                                gpsStatusNote = "Could not read GPS location; type an Abuja area or tap Pick on map."
                            }
                        }
                }
            }
            .addOnFailureListener {
                if (!fromAutoInit) {
                    gpsStatusNote = "Could not read GPS location; type an Abuja area or tap Pick on map."
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            gpsStatusNote = null
            fetchCurrentGps(fromAutoInit = true)
        } else {
            wasDeniedOnce = true
            gpsStatusNote = "Location access is required to use the Abuja map."
        }
    }

    fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Re-sync permission state when returning from Android App Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowGranted = checkLocationGranted(context)
                if (nowGranted != hasLocationPermission) {
                    hasLocationPermission = nowGranted
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Automatically request location access when opening the map workspace
    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !hasAutoPromptedLocation) {
            hasAutoPromptedLocation = true
            requestLocationPermission()
        }
    }

    // Whenever permission is active, resolve initial GPS position in Abuja
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fetchCurrentGps(fromAutoInit = true)
        }
    }

    val activity = context as? Activity
    val isPermanentlyDenied = !hasLocationPermission && wasDeniedOnce && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)

    val currentPlan = activeTripSession?.plan ?: (routePlanState as? RoutePlanState.Success)?.plan
    val originMarker = selectedOriginPlace?.let { it.lat to it.lng }
    val destMarker = selectedDestinationPlace?.let { it.lat to it.lng }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(WhereWhite)
    ) {
        val isDesktopLayout = maxWidth >= 840.dp

        if (isDesktopLayout) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(480.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RouteSidebarContent(
                        originInput = originInput,
                        destinationInput = destinationInput,
                        originSuggestions = originSuggestions,
                        destinationSuggestions = destinationSuggestions,
                        searchNotice = searchNotice ?: gpsStatusNote,
                        pickMapTarget = pickMapTarget,
                        routePlanState = routePlanState,
                        activeTripSession = activeTripSession,
                        savedPlaces = savedPlaces,
                        learnedCorridors = learnedCorridors,
                        onOriginChange = onOriginChange,
                        onDestinationChange = onDestinationChange,
                        onSelectOriginSuggestion = onSelectOriginSuggestion,
                        onSelectDestinationSuggestion = onSelectDestinationSuggestion,
                        onSwapLocations = onSwapLocations,
                        onUseMyLocationClick = {
                            if (hasLocationPermission) fetchCurrentGps(fromAutoInit = false)
                            else requestLocationPermission()
                        },
                        onTogglePickOnMap = {
                            if (hasLocationPermission) onTogglePickOnMap()
                            else requestLocationPermission()
                        },
                        onSelectPopularPlace = onSelectPopularPlace,
                        onSelectSavedPlace = onSelectSavedPlace,
                        onRunSampleTrip = onRunSampleTrip,
                        onFindRoute = onFindRoute,
                        onClearResult = onClearResult,
                        onSavePlace = onSavePlace,
                        onSaveRoute = onSaveRoute,
                        onStartTrip = onStartTrip,
                        onToggleStepCompleted = onToggleStepCompleted,
                        onRequestEndTrip = onRequestEndTrip,
                        onOpenAiGuide = onOpenAiGuide
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    MapSlotWithPermissionGate(
                        hasLocationPermission = hasLocationPermission,
                        isPermanentlyDenied = isPermanentlyDenied,
                        routePlan = currentPlan,
                        userLocation = userLocation,
                        originMarker = originMarker,
                        destinationMarker = destMarker,
                        pickMapTarget = pickMapTarget,
                        onRequestPermission = { requestLocationPermission() },
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        },
                        onMapTapped = onMapTapped,
                        onCancelPickMode = onCancelPickOnMap,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MapSlotWithPermissionGate(
                    hasLocationPermission = hasLocationPermission,
                    isPermanentlyDenied = isPermanentlyDenied,
                    routePlan = currentPlan,
                    userLocation = userLocation,
                    originMarker = originMarker,
                    destinationMarker = destMarker,
                    pickMapTarget = pickMapTarget,
                    onRequestPermission = { requestLocationPermission() },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    onMapTapped = onMapTapped,
                    onCancelPickMode = onCancelPickOnMap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                )

                RouteSidebarContent(
                    originInput = originInput,
                    destinationInput = destinationInput,
                    originSuggestions = originSuggestions,
                    destinationSuggestions = destinationSuggestions,
                    searchNotice = searchNotice ?: gpsStatusNote,
                    pickMapTarget = pickMapTarget,
                    routePlanState = routePlanState,
                    activeTripSession = activeTripSession,
                    savedPlaces = savedPlaces,
                    learnedCorridors = learnedCorridors,
                    onOriginChange = onOriginChange,
                    onDestinationChange = onDestinationChange,
                    onSelectOriginSuggestion = onSelectOriginSuggestion,
                    onSelectDestinationSuggestion = onSelectDestinationSuggestion,
                    onSwapLocations = onSwapLocations,
                    onUseMyLocationClick = {
                        if (hasLocationPermission) fetchCurrentGps(fromAutoInit = false)
                        else requestLocationPermission()
                    },
                    onTogglePickOnMap = {
                        if (hasLocationPermission) onTogglePickOnMap()
                        else requestLocationPermission()
                    },
                    onSelectPopularPlace = onSelectPopularPlace,
                    onSelectSavedPlace = onSelectSavedPlace,
                    onRunSampleTrip = onRunSampleTrip,
                    onFindRoute = onFindRoute,
                    onClearResult = onClearResult,
                    onSavePlace = onSavePlace,
                    onSaveRoute = onSaveRoute,
                    onStartTrip = onStartTrip,
                    onToggleStepCompleted = onToggleStepCompleted,
                    onRequestEndTrip = onRequestEndTrip,
                    onOpenAiGuide = onOpenAiGuide
                )
            }
        }
    }

    if (showPostTripSurvey && activeTripSession != null) {
        PostTripSurveyDialog(
            plan = activeTripSession.plan,
            onDismissWithoutEnding = onDismissSurveyWithoutEnding,
            onSubmitAndEndTrip = onSubmitSurveyAndEnd
        )
    }
}

@Composable
private fun MapSlotWithPermissionGate(
    hasLocationPermission: Boolean,
    isPermanentlyDenied: Boolean,
    routePlan: RoutePlan?,
    userLocation: Pair<Double, Double>?,
    originMarker: Pair<Double, Double>?,
    destinationMarker: Pair<Double, Double>?,
    pickMapTarget: PickMapTarget,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onMapTapped: (Double, Double) -> Unit,
    onCancelPickMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (hasLocationPermission) {
        LeafletAbujaMapView(
            routePlan = routePlan,
            userLocation = userLocation,
            originMarker = originMarker,
            destinationMarker = destinationMarker,
            pickMapTarget = pickMapTarget,
            onMapTapped = onMapTapped,
            onCancelPickMode = onCancelPickMode,
            modifier = modifier
        )
    } else {
        Surface(
            color = WhereMintSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, WhereMintBorder),
            modifier = modifier.testTag("location_permission_gate")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WhereWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location access required",
                        tint = WhereDeepGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Allow location access to use the map",
                    style = MaterialTheme.typography.titleMedium,
                    color = WhereMainText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "WHERE uses your device location to load the Abuja OpenStreetMap and pinpoint nearby stops.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhereMutedText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRequestPermission,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhereDeepGreen,
                            contentColor = WhereWhite
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("grant_location_for_map_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Allow Location Access", style = MaterialTheme.typography.labelLarge)
                    }

                    if (isPermanentlyDenied) {
                        TextButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = WhereDeepGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Settings", color = WhereDeepGreen)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteSidebarContent(
    originInput: String,
    destinationInput: String,
    originSuggestions: List<NominatimPlace>,
    destinationSuggestions: List<NominatimPlace>,
    searchNotice: String?,
    pickMapTarget: PickMapTarget,
    routePlanState: RoutePlanState,
    activeTripSession: ActiveTripSession?,
    savedPlaces: List<SavedPlaceEntity>,
    learnedCorridors: List<LearnedCorridorEntity>,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSelectOriginSuggestion: (NominatimPlace) -> Unit,
    onSelectDestinationSuggestion: (NominatimPlace) -> Unit,
    onSwapLocations: () -> Unit,
    onUseMyLocationClick: () -> Unit,
    onTogglePickOnMap: () -> Unit,
    onSelectPopularPlace: (AbujaPopularPlace) -> Unit,
    onSelectSavedPlace: (SavedPlaceEntity) -> Unit,
    onRunSampleTrip: (SampleTripPreset) -> Unit,
    onFindRoute: () -> Unit,
    onClearResult: () -> Unit,
    onSavePlace: (String, String, Double, Double) -> Unit,
    onSaveRoute: (RoutePlan) -> Unit,
    onStartTrip: (RoutePlan) -> Unit,
    onToggleStepCompleted: (Int) -> Unit,
    onRequestEndTrip: () -> Unit,
    onOpenAiGuide: () -> Unit
) {
    if (activeTripSession != null) {
        ActiveTripTrackerCard(
            session = activeTripSession,
            onToggleStep = onToggleStepCompleted,
            onRequestEndTrip = onRequestEndTrip
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Where are you going?",
            style = MaterialTheme.typography.headlineMedium,
            color = WhereMainText,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Routes across Abuja",
            style = MaterialTheme.typography.bodyMedium,
            color = WhereMutedText
        )
    }

    Surface(
        color = WhereWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, WhereSubtleBorder),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(WhereDeepGreen)
                    )
                    BasicTextField(
                        value = originInput,
                        onValueChange = onOriginChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = WhereMainText,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(WhereDeepGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("origin_input"),
                        decorationBox = { innerTextField ->
                            if (originInput.isEmpty()) {
                                Text(
                                    text = "Choose a start point",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WhereMutedText
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                HorizontalDivider(color = WhereSubtleBorder)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = WhereDeepGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    BasicTextField(
                        value = destinationInput,
                        onValueChange = onDestinationChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = WhereMainText,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(WhereDeepGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("destination_input"),
                        decorationBox = { innerTextField ->
                            if (destinationInput.isEmpty()) {
                                Text(
                                    text = "Where to?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WhereMutedText
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            IconButton(
                onClick = onSwapLocations,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(WhereMintSurface)
                    .testTag("swap_locations_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Swap start and destination",
                    tint = WhereDeepGreen
                )
            }
        }
    }

    if (!searchNotice.isNullOrBlank()) {
        Surface(
            color = OverchargeSoftBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, OverchargeAmber.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = OverchargeAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = searchNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = WhereMainText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (originSuggestions.isNotEmpty()) {
        NominatimSuggestionList(
            title = "Abuja Start Suggestions",
            places = originSuggestions,
            onSelect = onSelectOriginSuggestion
        )
    }
    if (destinationSuggestions.isNotEmpty()) {
        NominatimSuggestionList(
            title = "Abuja Destination Suggestions",
            places = destinationSuggestions,
            onSelect = onSelectDestinationSuggestion
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onUseMyLocationClick,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WhereDeepGreen,
                contentColor = WhereWhite
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("use_gps_button")
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Use my location",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onTogglePickOnMap,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (pickMapTarget != PickMapTarget.NONE) WhereDeepGreen else WhereMintSurface,
                contentColor = if (pickMapTarget != PickMapTarget.NONE) WhereWhite else WhereDeepGreen
            ),
            border = BorderStroke(1.dp, if (pickMapTarget != PickMapTarget.NONE) WhereDeepGreen else WhereMintBorder),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("pick_on_map_button")
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when (pickMapTarget) {
                    PickMapTarget.NONE -> "Pick on map"
                    PickMapTarget.FROM -> "Picking Start"
                    PickMapTarget.TO -> "Picking End"
                },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Button(
        onClick = onFindRoute,
        enabled = routePlanState !is RoutePlanState.Loading,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WhereDeepGreen,
            contentColor = WhereWhite
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("find_route_button")
    ) {
        if (routePlanState is RoutePlanState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = WhereWhite,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("Calculating Abuja route & fair fares...")
        } else {
            Icon(imageVector = Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Get Route & Fair Fare", style = MaterialTheme.typography.labelLarge)
        }
    }

    AnimatedVisibility(visible = routePlanState !is RoutePlanState.Idle) {
        when (routePlanState) {
            is RoutePlanState.Idle -> Unit
            is RoutePlanState.Loading -> {
                Surface(
                    color = WhereMintSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, WhereMintBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = WhereDeepGreen,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Routing via OpenStreetMap & checking Abuja corridor fare table...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WhereMainText
                        )
                    }
                }
            }

            is RoutePlanState.Error -> {
                Surface(
                    color = OverchargeSoftBg,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, OverchargeAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Route Notice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WhereMainText
                        )
                        Text(
                            text = routePlanState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WhereMainText
                        )
                    }
                }
            }

            is RoutePlanState.Success -> {
                val isTripRunning = activeTripSession?.plan?.originName == routePlanState.plan.originName &&
                    activeTripSession.plan.destinationName == routePlanState.plan.destinationName
                RouteResultCard(
                    plan = routePlanState.plan,
                    isTripActiveForPlan = isTripRunning,
                    onStartTrip = { onStartTrip(routePlanState.plan) },
                    onSavePlace = {
                        onSavePlace(
                            routePlanState.plan.destinationName,
                            routePlanState.plan.destinationName,
                            routePlanState.plan.destinationLat,
                            routePlanState.plan.destinationLng
                        )
                    },
                    onSaveRoute = { onSaveRoute(routePlanState.plan) },
                    onEndActiveTrip = onRequestEndTrip,
                    onClearResult = onClearResult
                )
            }
        }
    }

    if (savedPlaces.isNotEmpty() || learnedCorridors.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "From your AI Memory",
                style = MaterialTheme.typography.titleMedium,
                color = WhereMainText,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedPlaces.take(4).forEach { saved ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = WhereMintSurface,
                        border = BorderStroke(1.dp, WhereMintBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onSelectSavedPlace(saved) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = WhereDeepGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = saved.placeName,
                                style = MaterialTheme.typography.labelMedium,
                                color = WhereDeepGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Popular destinations",
            style = MaterialTheme.typography.titleMedium,
            color = WhereMainText,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AbujaFareTable.POPULAR_PLACES.forEach { place ->
                val isSelected = destinationInput.equals(place.name, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) WhereDeepGreen else WhereMintSurface,
                    border = BorderStroke(1.dp, if (isSelected) WhereDeepGreen else WhereMintBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onSelectPopularPlace(place) }
                ) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) WhereWhite else WhereDeepGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Try a sample trip",
            style = MaterialTheme.typography.titleMedium,
            color = WhereMainText,
            fontWeight = FontWeight.Bold
        )
        AbujaFareTable.SAMPLE_TRIPS.forEach { sample ->
            Surface(
                color = WhereWhite,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, WhereSubtleBorder),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onRunSampleTrip(sample) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = sample.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = WhereMainText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = sample.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = WhereMutedText
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Try sample trip",
                        tint = WhereDeepGreen
                    )
                }
            }
        }
    }

    Surface(
        color = WhereInfoBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, WhereMintBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "How WHERE keeps you from overpaying",
                style = MaterialTheme.typography.titleMedium,
                color = WhereInfoTitle,
                fontWeight = FontWeight.Bold
            )
            OverpayingInfoRow(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                text = "Under 1.5 km? WHERE tells you to trek it and keep your naira."
            )
            OverpayingInfoRow(
                icon = Icons.Default.Payments,
                text = "Every leg shows a fair fare range and the price that means you are being overcharged."
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenAiGuide)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WhereWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = WhereDeepGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Need tips, rush hour advice, or safer junctions? Tap to ask the AI Guide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WhereMainText,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OverpayingInfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(WhereWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WhereDeepGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = WhereMainText,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NominatimSuggestionList(
    title: String,
    places: List<NominatimPlace>,
    onSelect: (NominatimPlace) -> Unit
) {
    Surface(
        color = WhereWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WhereMintBorder),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = WhereMutedText,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
            places.forEachIndexed { idx, place ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(place) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = place.shortName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WhereMainText
                    )
                    Text(
                        text = place.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = WhereMutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (idx < places.lastIndex) {
                    HorizontalDivider(color = WhereSubtleBorder.copy(alpha = 0.6f))
                }
            }
        }
    }
}
