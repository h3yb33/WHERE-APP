package com.example.model

data class LatLngPoint(
    val lat: Double,
    val lng: Double
)

data class NominatimPlace(
    val displayName: String,
    val shortName: String,
    val lat: Double,
    val lng: Double,
    val isWithinAbuja: Boolean
)

enum class PickMapTarget {
    NONE,
    FROM,
    TO
}

data class TransitLeg(
    val stepNumber: Int = 1,
    val mode: String, // TREK, BUS, TAXI, KEKE, OKADA, DROP
    val headline: String = "",
    val subHeadline: String = "",
    val boardingPoint: String = "",
    val alightingPoint: String = "",
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val instruction: String,
    val areaRuleNote: String = "",
    val distanceKm: Double,
    val distanceText: String,
    val durationMinutes: Int,
    val stopCount: Int = 0,
    val waitMinutes: Int = 0,
    val fareLabel: String = "FAIR FARE",
    val isFixedFare: Boolean = false,
    val fareMinNaira: Int,
    val fareMaxNaira: Int,
    val overchargeAboveNaira: Int,
    val fareHelperNote: String = "",
    val changeAtNote: String = "",
    val roadNamesSummary: String = "",
    val pathPoints: List<LatLngPoint> = emptyList(),
    val isOsrmPath: Boolean = false
)

data class GroundingSourceItem(
    val title: String,
    val uri: String
)

data class RoutePlan(
    val optionTag: String = "Cheapest", // Cheapest, Fastest, Direct drop, Trek it
    val originName: String,
    val destinationName: String,
    val originLat: Double,
    val originLng: Double,
    val destinationLat: Double,
    val destinationLng: Double,
    val distanceKm: Double,
    val totalMinutes: Int,
    val totalFareMinNaira: Int,
    val totalFareMaxNaira: Int,
    val totalOverchargeAboveNaira: Int,
    val changesCount: Int = 0,
    val walkDistanceKm: Double = 0.0,
    val isDoorToDoor: Boolean = false,
    val rushHourNotice: String = "",
    val fareEstimateDisclaimer: String,
    val trekRecommended: Boolean,
    val antiInflationTip: String,
    val memoryInsightUsed: String = "",
    val legs: List<TransitLeg>,
    val webSources: List<GroundingSourceItem> = emptyList(),
    val searchEntryPointHtml: String? = null
)

data class RouteBundle(
    val options: List<RoutePlan>
)

data class ActiveTripSession(
    val plan: RoutePlan,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val completedStepIndices: Set<Int> = emptySet()
)

data class PostTripSurveyInput(
    val originName: String,
    val destinationName: String,
    val alightAndTrekPoint: String,
    val commercialBikePoint: String,
    val kekeTimeTaken: String,
    val kekeAllowedStatus: String,
    val bikeAllowedStatus: String,
    val areaChecked: String,
    val farePaidNaira: Int
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val webSources: List<GroundingSourceItem> = emptyList(),
    val searchEntryPointHtml: String? = null
)
