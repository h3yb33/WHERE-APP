package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AbujaMemoryRepository
import com.example.data.AbujaPopularPlace
import com.example.data.GeminiTransitRepository
import com.example.data.OsmTransitService
import com.example.data.OutsideAbujaException
import com.example.data.SampleTripPreset
import com.example.data.local.LearnedCorridorEntity
import com.example.data.local.SavedPlaceEntity
import com.example.data.local.SavedRouteEntity
import com.example.data.local.TripSurveyEntity
import com.example.model.ActiveTripSession
import com.example.model.ChatMessage
import com.example.model.NominatimPlace
import com.example.model.PickMapTarget
import com.example.model.PostTripSurveyInput
import com.example.model.RouteBundle
import com.example.model.RoutePlan
import com.google.firebase.ai.Chat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

sealed interface RoutePlanState {
    data object Idle : RoutePlanState
    data object Loading : RoutePlanState
    data class Success(
        val bundle: RouteBundle,
        val selectedIndex: Int = 0
    ) : RoutePlanState {
        val plan: RoutePlan
            get() = bundle.options[selectedIndex.coerceIn(bundle.options.indices)]
    }
    data class Error(val message: String) : RoutePlanState
}

class WhereViewModel(
    private val memoryRepo: AbujaMemoryRepository,
    private val osmService: OsmTransitService,
    private val geminiRepo: GeminiTransitRepository
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _originInput = MutableStateFlow("")
    val originInput: StateFlow<String> = _originInput.asStateFlow()

    private val _destinationInput = MutableStateFlow("")
    val destinationInput: StateFlow<String> = _destinationInput.asStateFlow()

    private val _selectedOriginPlace = MutableStateFlow<NominatimPlace?>(null)
    val selectedOriginPlace: StateFlow<NominatimPlace?> = _selectedOriginPlace.asStateFlow()

    private val _selectedDestinationPlace = MutableStateFlow<NominatimPlace?>(null)
    val selectedDestinationPlace: StateFlow<NominatimPlace?> = _selectedDestinationPlace.asStateFlow()

    private val _originSuggestions = MutableStateFlow<List<NominatimPlace>>(emptyList())
    val originSuggestions: StateFlow<List<NominatimPlace>> = _originSuggestions.asStateFlow()

    private val _destinationSuggestions = MutableStateFlow<List<NominatimPlace>>(emptyList())
    val destinationSuggestions: StateFlow<List<NominatimPlace>> = _destinationSuggestions.asStateFlow()

    private val _searchNotice = MutableStateFlow<String?>(null)
    val searchNotice: StateFlow<String?> = _searchNotice.asStateFlow()

    private val _pickMapTarget = MutableStateFlow(PickMapTarget.NONE)
    val pickMapTarget: StateFlow<PickMapTarget> = _pickMapTarget.asStateFlow()

    private val _routePlanState = MutableStateFlow<RoutePlanState>(RoutePlanState.Idle)
    val routePlanState: StateFlow<RoutePlanState> = _routePlanState.asStateFlow()

    private val _activeTripSession = MutableStateFlow<ActiveTripSession?>(null)
    val activeTripSession: StateFlow<ActiveTripSession?> = _activeTripSession.asStateFlow()

    private val _showPostTripSurvey = MutableStateFlow(false)
    val showPostTripSurvey: StateFlow<Boolean> = _showPostTripSurvey.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val learnedCorridors: StateFlow<List<LearnedCorridorEntity>> = memoryRepo.learnedCorridors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val tripSurveys: StateFlow<List<TripSurveyEntity>> = memoryRepo.tripSurveys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val savedRoutes: StateFlow<List<SavedRouteEntity>> = memoryRepo.savedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val savedPlaces: StateFlow<List<SavedPlaceEntity>> = memoryRepo.savedPlaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val chatMutex = Mutex()
    private var transitChat: Chat? = null
    private var cachedMemoryContext: String = ""
    private var lastAttemptedPrompt: String? = null

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome_abuja",
                isUser = false,
                text = "Welcome to WHERE Abuja AI Guide. Ask me anything about getting around Abuja — best routes, cheaper options, rush hour timing, landmarks, or where Keke/Okada are restricted."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatSending = MutableStateFlow(false)
    val isChatSending: StateFlow<Boolean> = _isChatSending.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private var originSearchJob: Job? = null
    private var destinationSearchJob: Job? = null
    private var routeSearchJob: Job? = null

    init {
        viewModelScope.launch {
            memoryRepo.ensureSeededAbujaMemory()
            cachedMemoryContext = memoryRepo.getMemoryContextPrompt()
        }
    }

    private suspend fun getOrCreateTransitChat(forceRefresh: Boolean = false): Chat {
        return chatMutex.withLock {
            if (!forceRefresh && transitChat != null) {
                return@withLock transitChat!!
            }
            memoryRepo.ensureSeededAbujaMemory()
            val memoryContext = memoryRepo.getMemoryContextPrompt()
            cachedMemoryContext = memoryContext
            geminiRepo.startTransitChat(memoryContext).also { transitChat = it }
        }
    }

    fun updateOrigin(value: String) {
        _originInput.value = value
        _selectedOriginPlace.value = null
        _searchNotice.value = null
        originSearchJob?.cancel()
        if (value.trim().length < 2) {
            _originSuggestions.value = emptyList()
            return
        }
        originSearchJob = viewModelScope.launch {
            delay(1000L) // Debounce max 1 request per second for Nominatim
            osmService.searchPlaces(value).fold(
                onSuccess = { list ->
                    _originSuggestions.value = list
                },
                onFailure = { err ->
                    _originSuggestions.value = emptyList()
                    if (err is OutsideAbujaException) {
                        _searchNotice.value = err.message
                    }
                }
            )
        }
    }

    fun updateDestination(value: String) {
        _destinationInput.value = value
        _selectedDestinationPlace.value = null
        _searchNotice.value = null
        destinationSearchJob?.cancel()
        if (value.trim().length < 2) {
            _destinationSuggestions.value = emptyList()
            return
        }
        destinationSearchJob = viewModelScope.launch {
            delay(1000L) // Debounce max 1 request per second for Nominatim
            osmService.searchPlaces(value).fold(
                onSuccess = { list ->
                    _destinationSuggestions.value = list
                },
                onFailure = { err ->
                    _destinationSuggestions.value = emptyList()
                    if (err is OutsideAbujaException) {
                        _searchNotice.value = err.message
                    }
                }
            )
        }
    }

    fun selectOriginSuggestion(place: NominatimPlace) {
        originSearchJob?.cancel()
        _originInput.value = place.shortName
        _selectedOriginPlace.value = place
        _originSuggestions.value = emptyList()
        _searchNotice.value = null
    }

    fun selectDestinationSuggestion(place: NominatimPlace) {
        destinationSearchJob?.cancel()
        _destinationInput.value = place.shortName
        _selectedDestinationPlace.value = place
        _destinationSuggestions.value = emptyList()
        _searchNotice.value = null
    }

    fun dismissSuggestions() {
        _originSuggestions.value = emptyList()
        _destinationSuggestions.value = emptyList()
    }

    fun swapLocations() {
        val tempText = _originInput.value
        val tempPlace = _selectedOriginPlace.value
        _originInput.value = _destinationInput.value
        _selectedOriginPlace.value = _selectedDestinationPlace.value
        _destinationInput.value = tempText
        _selectedDestinationPlace.value = tempPlace
        dismissSuggestions()
    }

    fun togglePickOnMapMode() {
        val next = when (_pickMapTarget.value) {
            PickMapTarget.NONE -> if (_originInput.value.isBlank()) PickMapTarget.FROM else PickMapTarget.TO
            PickMapTarget.FROM -> PickMapTarget.TO
            PickMapTarget.TO -> PickMapTarget.NONE
        }
        _pickMapTarget.value = next
    }

    fun cancelPickOnMap() {
        _pickMapTarget.value = PickMapTarget.NONE
    }

    fun handleMapTap(lat: Double, lng: Double) {
        val target = _pickMapTarget.value
        if (target == PickMapTarget.NONE) return

        viewModelScope.launch {
            if (!osmService.isInsideAbuja(lat, lng)) {
                _searchNotice.value = "WHERE currently covers Abuja only."
                _snackbarMessage.value = "WHERE currently covers Abuja only."
                return@launch
            }
            osmService.reverseGeocodeAbuja(lat, lng).fold(
                onSuccess = { place ->
                    if (target == PickMapTarget.FROM) {
                        _originInput.value = place.shortName
                        _selectedOriginPlace.value = place
                        _originSuggestions.value = emptyList()
                        _snackbarMessage.value = "Start set: ${place.shortName}"
                    } else {
                        _destinationInput.value = place.shortName
                        _selectedDestinationPlace.value = place
                        _destinationSuggestions.value = emptyList()
                        _snackbarMessage.value = "Destination set: ${place.shortName}"
                    }
                    _pickMapTarget.value = PickMapTarget.NONE
                    _searchNotice.value = null
                },
                onFailure = { err ->
                    _searchNotice.value = err.message ?: "WHERE currently covers Abuja only."
                }
            )
        }
    }

    fun applyGpsLocation(lat: Double, lng: Double, fromAutoInit: Boolean = false) {
        if (!osmService.isInsideAbuja(lat, lng)) {
            if (fromAutoInit) {
                _userLocation.value = null
                _searchNotice.value = "GPS is currently outside Abuja — map centered on Abuja, FCT."
            } else {
                _searchNotice.value = "WHERE currently covers Abuja only."
                _snackbarMessage.value = "WHERE currently covers Abuja only."
            }
            return
        }
        _userLocation.value = lat to lng
        viewModelScope.launch {
            osmService.reverseGeocodeAbuja(lat, lng).onSuccess { place ->
                if (!fromAutoInit || _originInput.value.isBlank()) {
                    _originInput.value = place.shortName
                    _selectedOriginPlace.value = place
                    _originSuggestions.value = emptyList()
                }
                _searchNotice.value = null
            }
        }
    }

    fun selectPopularDestination(place: AbujaPopularPlace) {
        val nomPlace = NominatimPlace(
            displayName = place.fullName,
            shortName = place.name,
            lat = place.lat,
            lng = place.lng,
            isWithinAbuja = true
        )
        _destinationInput.value = place.name
        _selectedDestinationPlace.value = nomPlace
        _destinationSuggestions.value = emptyList()
        _searchNotice.value = null
        if (_originInput.value.isNotBlank()) {
            findRoute()
        }
    }

    fun selectSavedPlace(place: SavedPlaceEntity, setAsOrigin: Boolean = false) {
        val nomPlace = NominatimPlace(
            displayName = place.placeName,
            shortName = place.placeName,
            lat = place.lat,
            lng = place.lng,
            isWithinAbuja = true
        )
        if (setAsOrigin || _originInput.value.isBlank()) {
            _originInput.value = place.placeName
            _selectedOriginPlace.value = nomPlace
        } else {
            _destinationInput.value = place.placeName
            _selectedDestinationPlace.value = nomPlace
            findRoute()
        }
    }

    fun runSampleTrip(sample: SampleTripPreset) {
        val oPlace = NominatimPlace(sample.originName, sample.originName, sample.originLat, sample.originLng, true)
        val dPlace = NominatimPlace(sample.destinationName, sample.destinationName, sample.destinationLat, sample.destinationLng, true)
        _originInput.value = sample.originName
        _destinationInput.value = sample.destinationName
        _selectedOriginPlace.value = oPlace
        _selectedDestinationPlace.value = dPlace
        dismissSuggestions()
        findRoute(oPlace, dPlace)
    }

    fun findRoute(
        originPlaceOverride: NominatimPlace? = null,
        destinationPlaceOverride: NominatimPlace? = null
    ) {
        val originText = _originInput.value.trim()
        val destText = _destinationInput.value.trim()
        val oPlace = originPlaceOverride ?: _selectedOriginPlace.value
        val dPlace = destinationPlaceOverride ?: _selectedDestinationPlace.value

        if (originText.isBlank() || destText.isBlank()) {
            _routePlanState.value = RoutePlanState.Error("Choose both a start point and a destination in Abuja.")
            return
        }

        dismissSuggestions()
        _searchNotice.value = null
        routeSearchJob?.cancel()

        routeSearchJob = viewModelScope.launch {
            _routePlanState.value = RoutePlanState.Loading
            val memoryContext = memoryRepo.getMemoryContextPrompt(originText, destText)
            val result = geminiRepo.planMultimodalRoute(
                originPlace = oPlace,
                destinationPlace = dPlace,
                originText = originText,
                destinationText = destText,
                memoryContext = memoryContext
            )
            result.fold(
                onSuccess = { bundle ->
                    _routePlanState.value = RoutePlanState.Success(bundle = bundle, selectedIndex = 0)
                },
                onFailure = { err ->
                    val msg = if (err is OutsideAbujaException) {
                        "WHERE currently covers Abuja only."
                    } else {
                        err.localizedMessage ?: "Unable to calculate Abuja route right now."
                    }
                    _routePlanState.value = RoutePlanState.Error(msg)
                }
            )
        }
    }

    fun selectRouteOption(index: Int) {
        val current = _routePlanState.value as? RoutePlanState.Success ?: return
        if (index in current.bundle.options.indices) {
            _routePlanState.value = current.copy(selectedIndex = index)
        }
    }

    fun loadCorridorFromMemory(originArea: String, destinationArea: String) {
        _originInput.value = originArea
        _destinationInput.value = destinationArea
        _selectedOriginPlace.value = null
        _selectedDestinationPlace.value = null
        findRoute()
    }

    fun clearRouteResult() {
        _routePlanState.value = RoutePlanState.Idle
    }

    fun startTrip(plan: RoutePlan) {
        _activeTripSession.value = ActiveTripSession(plan = plan)
        _showPostTripSurvey.value = false
        _snackbarMessage.value = "Trip started! Check off steps and complete the quick survey on arrival."
    }

    fun toggleStepCompleted(index: Int) {
        _activeTripSession.update { session ->
            if (session == null) return@update null
            val nextSet = if (index in session.completedStepIndices) {
                session.completedStepIndices - index
            } else {
                session.completedStepIndices + index
            }
            session.copy(completedStepIndices = nextSet)
        }
    }

    fun requestEndTrip() {
        if (_activeTripSession.value != null) {
            _showPostTripSurvey.value = true
        }
    }

    fun dismissSurveyWithoutEnding() {
        _showPostTripSurvey.value = false
    }

    fun submitTripSurveyAndEnd(survey: PostTripSurveyInput) {
        viewModelScope.launch {
            memoryRepo.recordTripSurveyAndTrainMemory(survey)
            getOrCreateTransitChat(forceRefresh = true)
            _activeTripSession.value = null
            _showPostTripSurvey.value = false
            _snackbarMessage.value = "Trip completed! AI Memory updated with your Abuja trip."
        }
    }

    fun savePlaceFromPlan(label: String, placeName: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            memoryRepo.savePlace(label, placeName, lat, lng)
            getOrCreateTransitChat(forceRefresh = true)
            _snackbarMessage.value = "Saved \"$placeName\" to AI Memory"
        }
    }

    fun deleteSavedPlace(id: Int) {
        viewModelScope.launch {
            memoryRepo.deleteSavedPlace(id)
            _snackbarMessage.value = "Removed saved place"
        }
    }

    fun saveCurrentRoute(plan: RoutePlan) {
        viewModelScope.launch {
            memoryRepo.saveRoutePlan(plan)
            _snackbarMessage.value = "Route saved to AI Memory"
        }
    }

    fun deleteSavedRoute(routeId: Int) {
        viewModelScope.launch {
            memoryRepo.deleteSavedRoute(routeId)
            _snackbarMessage.value = "Saved route removed"
        }
    }

    fun teachAiCustomTip(
        originArea: String,
        destinationArea: String,
        tipSummary: String,
        fareNaira: Int
    ) {
        if (originArea.isBlank() || destinationArea.isBlank() || tipSummary.isBlank()) {
            _snackbarMessage.value = "Please fill origin, destination, and route steps."
            return
        }
        viewModelScope.launch {
            memoryRepo.trainMemoryFromTextTip(originArea, destinationArea, tipSummary, fareNaira)
            getOrCreateTransitChat(forceRefresh = true)
            _snackbarMessage.value = "Added route to AI Abuja Memory!"
        }
    }

    fun clearAllMemory(restoreStarterCorridors: Boolean = false) {
        viewModelScope.launch {
            memoryRepo.clearAllMemory()
            if (restoreStarterCorridors) {
                memoryRepo.ensureSeededAbujaMemory(forceRestore = true)
            }
            getOrCreateTransitChat(forceRefresh = true)
            _snackbarMessage.value = if (restoreStarterCorridors) {
                "Memory reset to default Abuja starter routes."
            } else {
                "All AI Memory cleared."
            }
        }
    }

    fun sendChatMessage(rawPrompt: String) {
        val text = rawPrompt.trim()
        if (text.isBlank() || _isChatSending.value) return
        lastAttemptedPrompt = text

        val userMsg = ChatMessage(
            id = "u_${UUID.randomUUID()}",
            isUser = true,
            text = text
        )
        val assistantId = "a_${UUID.randomUUID()}"
        val placeholderMsg = ChatMessage(
            id = assistantId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        _chatError.value = null
        _chatMessages.update { it + userMsg + placeholderMsg }
        _isChatSending.value = true

        viewModelScope.launch {
            val loc = _userLocation.value
            try {
                val activeChat = getOrCreateTransitChat()
                geminiRepo.sendChatMessageStream(
                    chat = activeChat,
                    message = text,
                    memoryContext = cachedMemoryContext,
                    userLat = loc?.first,
                    userLng = loc?.second
                ).collect { update ->
                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == assistantId) {
                                msg.copy(
                                    text = update.text,
                                    isStreaming = !update.isDone,
                                    webSources = update.webSources,
                                    searchEntryPointHtml = update.searchEntryPointHtml
                                )
                            } else {
                                msg
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat timed out", e)
                _chatError.value = "AI Guide timed out after 60 seconds. Check your internet connection and tap Retry."
                removeEmptyStreamingPlaceholder(assistantId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat error", e)
                _chatError.value = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Failed to reach WHERE Abuja AI Guide (${e::class.simpleName})."
                removeEmptyStreamingPlaceholder(assistantId)
            } finally {
                _isChatSending.value = false
            }
        }
    }

    fun retryLastChatMessage() {
        val prompt = lastAttemptedPrompt ?: return
        _chatError.value = null
        sendChatMessage(prompt)
    }

    fun resetChat() {
        viewModelScope.launch {
            getOrCreateTransitChat(forceRefresh = true)
            _chatError.value = null
            _chatMessages.value = listOf(
                ChatMessage(
                    id = "welcome_${System.currentTimeMillis()}",
                    isUser = false,
                    text = "Fresh Abuja AI Guide chat ready. Ask about Abuja routes, cheaper connections, rush hour, landmarks, or safety."
                )
            )
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun removeEmptyStreamingPlaceholder(id: String) {
        _chatMessages.update { list ->
            list.filterNot { it.id == id && it.text.isBlank() }
                .map { if (it.id == id) it.copy(isStreaming = false) else it }
        }
    }

    private companion object {
        const val TAG = "WhereViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
