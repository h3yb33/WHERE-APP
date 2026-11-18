package com.example.data

import android.util.Log
import com.example.model.GroundingSourceItem
import com.example.model.NominatimPlace
import com.example.model.RoutePlan
import com.example.model.TransitLeg
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiTransitRepo"
private const val MODEL_NAME = "gemini-3.8-flash"

data class ChatStreamUpdate(
    val text: String,
    val isDone: Boolean,
    val webSources: List<GroundingSourceItem> = emptyList(),
    val searchEntryPointHtml: String? = null
)

class GeminiTransitRepository(
    private val osmService: OsmTransitService
) {

    // Route planner uses OpenStreetMap (Nominatim + Overpass + OSRM) + Abuja AI Memory — NO Google Maps or Google Search map data
    private fun createRoutePlannerModel(memoryContext: String) =
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            },
            systemInstruction = content {
                text(
                    """
                    You are the WHERE Abuja Transit Leg Planner (Abuja, FCT, Nigeria ONLY).
                    Use ONLY the provided OpenStreetMap stops/roads and Learned Abuja Memory to split the route into realistic step-by-step legs (`TREK`, `BUS`, `TAXI`, `KEKE`, or `OKADA` where allowed).
                    NEVER invent fixed fares — all fare bands are computed by WHERE's editable AbujaFareTable.
                    Check Abuja restrictions: Okada is banned in Phase 1 / CBD / Wuse II / Maitama / Garki / expressways; Keke is restricted in core CBD and expressways.

                    LEARNED ABUJA MEMORY:
                    $memoryContext

                    Respond ONLY with raw JSON (no markdown fences):
                    {
                      "memoryInsightUsed": "short note on learned Abuja memory or empty string",
                      "antiInflationTip": "concise Abuja tip on avoiding overcharging",
                      "legs": [
                        {
                          "stepNumber": 1,
                          "mode": "TREK | BUS | TAXI | KEKE | OKADA",
                          "boardingPoint": "OpenStreetMap junction/stop name",
                          "alightingPoint": "OpenStreetMap junction/stop name",
                          "startLat": 9.07,
                          "startLng": 7.46,
                          "endLat": 9.06,
                          "endLng": 7.49,
                          "instruction": "Clear step-by-step instruction"
                        }
                      ]
                    }
                    """.trimIndent()
                )
            }
        )

    private fun buildGuideSystemInstruction(memoryContext: String) = content {
        text(
            """
            You are WHERE AI Guide — an assistant strictly for **Abuja, FCT, Nigeria public & local transport**.
            STRICT SCOPE: Answer ONLY Abuja transport questions (best routes, cheaper options, safety tips, rush-hour advice, Abuja landmarks, and Keke/Okada area restrictions).
            If the user asks about any city outside Abuja OR asks non-transport questions, politely decline and state: "I only answer Abuja, FCT transport questions (routes, fair fares, rush hour, landmarks, and local transit safety)."
            Always label fares clearly as **estimates** (never fixed prices), remind users when a trip under 1.5 km is best walked ("Trek it" for ₦0), and answer with clear step-by-step movements.

            USER'S ABUJA AI MEMORY (SAVED PLACES & FREQUENT ROUTES):
            ${memoryContext.ifBlank { "No custom routes recorded yet; use standard Abuja corridors (Wuse, Berger, Maitama, Garki, Utako, Gwarinpa, Kubwa, Nyanya, Lugbe)." }}
            """.trimIndent()
        )
    }

    fun startTransitChat(memoryContext: String, useSearchGrounding: Boolean = true): Chat {
        val chatModel = if (useSearchGrounding) {
            Firebase.ai.generativeModel(
                modelName = MODEL_NAME,
                tools = listOf(Tool.googleSearch()),
                systemInstruction = buildGuideSystemInstruction(memoryContext)
            )
        } else {
            Firebase.ai.generativeModel(
                modelName = MODEL_NAME,
                systemInstruction = buildGuideSystemInstruction(memoryContext)
            )
        }
        return chatModel.startChat()
    }

    suspend fun planMultimodalRoute(
        originPlace: NominatimPlace?,
        destinationPlace: NominatimPlace?,
        originText: String,
        destinationText: String,
        memoryContext: String
    ): Result<RoutePlan> {
        return try {
            val resolvedOrigin = originPlace ?: osmService.resolveSingleAbujaPlace(originText).getOrElse {
                return Result.failure(it)
            }
            val resolvedDest = destinationPlace ?: osmService.resolveSingleAbujaPlace(destinationText).getOrElse {
                return Result.failure(it)
            }

            if (!osmService.isInsideAbuja(resolvedOrigin.lat, resolvedOrigin.lng) ||
                !osmService.isInsideAbuja(resolvedDest.lat, resolvedDest.lng)
            ) {
                return Result.failure(OutsideAbujaException("WHERE currently covers Abuja only."))
            }

            val corridorEntry = AbujaFareTable.findCorridorEntry(
                resolvedOrigin.shortName,
                resolvedDest.shortName
            )

            // Fetch OpenStreetMap road path (OSRM) and nearby OpenStreetMap transit stops (Overpass) in parallel
            val (directFootOsrm, originStops, destStops) = coroutineScope {
                val footDeferred = async {
                    osmService.fetchOsrmPath(
                        resolvedOrigin.lat,
                        resolvedOrigin.lng,
                        resolvedDest.lat,
                        resolvedDest.lng,
                        AbujaTransitMode.TREK
                    )
                }
                val oStopsDeferred = async {
                    osmService.fetchNearbyOsmTransitNodes(resolvedOrigin.lat, resolvedOrigin.lng)
                }
                val dStopsDeferred = async {
                    osmService.fetchNearbyOsmTransitNodes(resolvedDest.lat, resolvedDest.lng)
                }
                Triple(footDeferred.await(), oStopsDeferred.await(), dStopsDeferred.await())
            }

            if (directFootOsrm.distanceKm < 1.5) {
                val roadNote = directFootOsrm.roadNames.takeIf { it.isNotEmpty() }
                    ?.joinToString(" -> ")
                    .orEmpty()
                val trekLeg = TransitLeg(
                    stepNumber = 1,
                    mode = "TREK",
                    boardingPoint = resolvedOrigin.shortName,
                    alightingPoint = resolvedDest.shortName,
                    startLat = resolvedOrigin.lat,
                    startLng = resolvedOrigin.lng,
                    endLat = resolvedDest.lat,
                    endLng = resolvedDest.lng,
                    instruction = if (roadNote.isNotBlank()) {
                        "Trek from ${resolvedOrigin.shortName} to ${resolvedDest.shortName} via $roadNote."
                    } else {
                        "Trek directly from ${resolvedOrigin.shortName} to ${resolvedDest.shortName}."
                    },
                    areaRuleNote = "Under 1.5 km — walk it and keep your naira.",
                    distanceKm = directFootOsrm.distanceKm,
                    distanceText = formatDistance(directFootOsrm.distanceKm),
                    durationMinutes = directFootOsrm.durationMinutes,
                    fareMinNaira = 0,
                    fareMaxNaira = 0,
                    overchargeAboveNaira = 0,
                    roadNamesSummary = roadNote,
                    pathPoints = directFootOsrm.points,
                    isOsrmPath = directFootOsrm.isFromOsrm
                )
                return Result.success(
                    RoutePlan(
                        originName = resolvedOrigin.shortName,
                        destinationName = resolvedDest.shortName,
                        originLat = resolvedOrigin.lat,
                        originLng = resolvedOrigin.lng,
                        destinationLat = resolvedDest.lat,
                        destinationLng = resolvedDest.lng,
                        distanceKm = directFootOsrm.distanceKm,
                        totalMinutes = directFootOsrm.durationMinutes,
                        totalFareMinNaira = 0,
                        totalFareMaxNaira = 0,
                        totalOverchargeAboveNaira = 0,
                        fareEstimateDisclaimer = AbujaFareTable.ESTIMATE_DISCLAIMER,
                        trekRecommended = true,
                        antiInflationTip = "Under 1.5 km — skip short-drop negotiation and trek it for ₦0.",
                        memoryInsightUsed = if (memoryContext.contains(resolvedOrigin.shortName.take(5), ignoreCase = true)) {
                            "Matched Abuja short-trek guidance in AI Memory"
                        } else {
                            ""
                        },
                        legs = listOf(trekLeg)
                    )
                )
            }

            val osmStopsSummary = buildString {
                if (originStops.isNotEmpty()) {
                    append("OpenStreetMap stops near start: ")
                    append(originStops.joinToString(", ") { "${it.shortName} (${it.lat}, ${it.lng})" })
                    append(". ")
                }
                if (destStops.isNotEmpty()) {
                    append("OpenStreetMap stops near destination: ")
                    append(destStops.joinToString(", ") { "${it.shortName} (${it.lat}, ${it.lng})" })
                }
            }.ifBlank { "No named Overpass stops nearby; use major Abuja junctions along corridor." }

            var aiInsight = ""
            var aiTip = "Confirm the price per drop before boarding Along taxis or buses; avoid solo charter rates."
            val draftLegs = mutableListOf<DraftLeg>()

            try {
                val model = createRoutePlannerModel(memoryContext)
                val prompt = "Plan Abuja transit legs from \"${resolvedOrigin.shortName}\" (${resolvedOrigin.lat}, ${resolvedOrigin.lng}) " +
                    "to \"${resolvedDest.shortName}\" (${resolvedDest.lat}, ${resolvedDest.lng}). " +
                    "OpenStreetMap context: $osmStopsSummary. " +
                    "Corridor restriction: ${corridorEntry.zoneRestrictionNote}. Return raw JSON only."

                val response = withTimeout(45_000L) {
                    model.generateContent(prompt)
                }
                val rawJson = response.text?.let { extractJsonObject(it) }
                if (rawJson != null) {
                    aiInsight = rawJson.optString("memoryInsightUsed", "")
                    val tip = rawJson.optString("antiInflationTip", "")
                    if (tip.isNotBlank()) aiTip = tip
                    val arr = rawJson.optJSONArray("legs")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val sLat = obj.optDouble("startLat", resolvedOrigin.lat)
                            val sLng = obj.optDouble("startLng", resolvedOrigin.lng)
                            val eLat = obj.optDouble("endLat", resolvedDest.lat)
                            val eLng = obj.optDouble("endLng", resolvedDest.lng)
                            val safeSLat = if (osmService.isInsideAbuja(sLat, sLng)) sLat else resolvedOrigin.lat
                            val safeSLng = if (osmService.isInsideAbuja(sLat, sLng)) sLng else resolvedOrigin.lng
                            val safeELat = if (osmService.isInsideAbuja(eLat, eLng)) eLat else resolvedDest.lat
                            val safeELng = if (osmService.isInsideAbuja(eLat, eLng)) eLng else resolvedDest.lng

                            draftLegs.add(
                                DraftLeg(
                                    modeRaw = obj.optString("mode", "TAXI"),
                                    boardingPoint = obj.optString("boardingPoint", resolvedOrigin.shortName),
                                    alightingPoint = obj.optString("alightingPoint", resolvedDest.shortName),
                                    startLat = safeSLat,
                                    startLng = safeSLng,
                                    endLat = safeELat,
                                    endLng = safeELng,
                                    instruction = obj.optString("instruction", "Take Abuja transit along corridor.")
                                )
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini leg breakdown failed, falling back to OpenStreetMap + AbujaFareTable", e)
            }

            if (draftLegs.isEmpty()) {
                val firstStop = originStops.firstOrNull()?.shortName ?: resolvedOrigin.shortName
                val lastStop = destStops.firstOrNull()?.shortName ?: resolvedDest.shortName
                draftLegs.add(
                    DraftLeg(
                        modeRaw = "TAXI",
                        boardingPoint = firstStop,
                        alightingPoint = lastStop,
                        startLat = resolvedOrigin.lat,
                        startLng = resolvedOrigin.lng,
                        endLat = resolvedDest.lat,
                        endLng = resolvedDest.lng,
                        instruction = "Board an Along taxi or Abuja bus at $firstStop heading toward $lastStop."
                    )
                )
            } else {
                val first = draftLegs.first()
                draftLegs[0] = first.copy(startLat = resolvedOrigin.lat, startLng = resolvedOrigin.lng)
                val last = draftLegs.last()
                draftLegs[draftLegs.lastIndex] = last.copy(endLat = resolvedDest.lat, endLng = resolvedDest.lng)
            }

            val finalLegs = mutableListOf<TransitLeg>()
            for ((idx, draft) in draftLegs.withIndex()) {
                val safeMode = AbujaFareTable.normalizeModeForCorridor(draft.modeRaw, corridorEntry)
                val osrm = osmService.fetchOsrmPath(
                    draft.startLat,
                    draft.startLng,
                    draft.endLat,
                    draft.endLng,
                    safeMode
                )
                val fareBand = AbujaFareTable.estimateLegFare(
                    origin = resolvedOrigin.shortName,
                    destination = resolvedDest.shortName,
                    mode = safeMode,
                    legDistanceKm = osrm.distanceKm
                )
                val roadSummary = osrm.roadNames.joinToString(" · ")
                finalLegs.add(
                    TransitLeg(
                        stepNumber = idx + 1,
                        mode = safeMode.name,
                        boardingPoint = draft.boardingPoint.ifBlank { resolvedOrigin.shortName },
                        alightingPoint = draft.alightingPoint.ifBlank { resolvedDest.shortName },
                        startLat = draft.startLat,
                        startLng = draft.startLng,
                        endLat = draft.endLat,
                        endLng = draft.endLng,
                        instruction = draft.instruction,
                        areaRuleNote = corridorEntry.zoneRestrictionNote,
                        distanceKm = osrm.distanceKm,
                        distanceText = formatDistance(osrm.distanceKm),
                        durationMinutes = osrm.durationMinutes,
                        fareMinNaira = fareBand.minNaira,
                        fareMaxNaira = fareBand.maxNaira,
                        overchargeAboveNaira = fareBand.overchargeAboveNaira,
                        roadNamesSummary = roadSummary,
                        pathPoints = osrm.points,
                        isOsrmPath = osrm.isFromOsrm
                    )
                )
            }

            val totalDistanceKm = finalLegs.sumOf { it.distanceKm }.coerceAtLeast(0.5)
            val totalMinutes = finalLegs.sumOf { it.durationMinutes }.coerceAtLeast(4)
            val totalMinFare = finalLegs.sumOf { it.fareMinNaira }
            val totalMaxFare = finalLegs.sumOf { it.fareMaxNaira }
            val totalOvercharge = finalLegs.sumOf { it.overchargeAboveNaira }

            Result.success(
                RoutePlan(
                    originName = resolvedOrigin.shortName,
                    destinationName = resolvedDest.shortName,
                    originLat = resolvedOrigin.lat,
                    originLng = resolvedOrigin.lng,
                    destinationLat = resolvedDest.lat,
                    destinationLng = resolvedDest.lng,
                    distanceKm = totalDistanceKm,
                    totalMinutes = totalMinutes,
                    totalFareMinNaira = totalMinFare,
                    totalFareMaxNaira = totalMaxFare,
                    totalOverchargeAboveNaira = totalOvercharge,
                    fareEstimateDisclaimer = AbujaFareTable.ESTIMATE_DISCLAIMER,
                    trekRecommended = false,
                    antiInflationTip = aiTip,
                    memoryInsightUsed = aiInsight,
                    legs = finalLegs
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(Exception("Route calculation timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Route planning failed", e)
            Result.failure(e)
        }
    }

    fun sendChatMessageStream(
        chat: Chat,
        message: String,
        memoryContext: String,
        userLat: Double?,
        userLng: Double?
    ): Flow<ChatStreamUpdate> = flow {
        val enrichedPrompt = if (userLat != null && userLng != null && osmService.isInsideAbuja(userLat, userLng)) {
            "[Commuter Abuja GPS: $userLat, $userLng] $message"
        } else {
            "[Abuja, FCT Nigeria] $message"
        }

        val primaryResult = runStreamAttempt(chat, enrichedPrompt) { update ->
            emit(update)
        }

        if (primaryResult != null) {
            // Primary attempt failed or returned empty text — retry seamlessly without Search Grounding tool
            Log.w(TAG, "Primary AI Guide stream failed/empty (${primaryResult.message}); retrying direct Gemini model")
            val fallbackChat = startTransitChat(memoryContext = memoryContext, useSearchGrounding = false)
            val fallbackError = runStreamAttempt(fallbackChat, enrichedPrompt) { update ->
                emit(update)
            }
            if (fallbackError != null) {
                throw fallbackError
            }
        }
    }

    private suspend fun runStreamAttempt(
        chat: Chat,
        prompt: String,
        onEmit: suspend (ChatStreamUpdate) -> Unit
    ): Exception? {
        val accumulated = StringBuilder()
        var webSources: List<GroundingSourceItem> = emptyList()
        var searchHtml: String? = null

        return try {
            withTimeout(60_000L) {
                chat.sendMessageStream(prompt).collect { chunk ->
                    val partText = chunk.text.orEmpty()
                    if (partText.isNotEmpty()) accumulated.append(partText)
                    val (w, h) = extractSearchGrounding(chunk)
                    if (w.isNotEmpty()) webSources = (webSources + w).distinctBy { it.uri }
                    if (!h.isNullOrBlank()) searchHtml = h

                    if (accumulated.isNotEmpty()) {
                        onEmit(
                            ChatStreamUpdate(
                                text = accumulated.toString(),
                                isDone = false,
                                webSources = webSources,
                                searchEntryPointHtml = searchHtml
                            )
                        )
                    }
                }
            }

            if (accumulated.isBlank()) {
                // Fallback to unary sendMessage if streaming yielded no text chunks
                val unary = withTimeout(45_000L) {
                    chat.sendMessage(prompt)
                }
                val unaryText = unary.text.orEmpty()
                if (unaryText.isNotBlank()) {
                    accumulated.append(unaryText)
                    val (w, h) = extractSearchGrounding(unary)
                    if (w.isNotEmpty()) webSources = (webSources + w).distinctBy { it.uri }
                    if (!h.isNullOrBlank()) searchHtml = h
                }
            }

            if (accumulated.isBlank()) {
                IllegalStateException("AI Guide returned an empty response. Please tap Retry.")
            } else {
                onEmit(
                    ChatStreamUpdate(
                        text = accumulated.toString(),
                        isDone = true,
                        webSources = webSources,
                        searchEntryPointHtml = searchHtml
                    )
                )
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Guide attempt failed", e)
            e
        }
    }

    private fun extractSearchGrounding(
        response: GenerateContentResponse
    ): Pair<List<GroundingSourceItem>, String?> {
        val metadata = response.candidates.firstOrNull()?.groundingMetadata
            ?: return emptyList<GroundingSourceItem>() to null

        val webList = mutableListOf<GroundingSourceItem>()
        metadata.groundingChunks.forEach { chunk ->
            chunk.web?.let { web ->
                val title = web.title?.takeIf { it.isNotBlank() } ?: "Web Source"
                val uri = web.uri?.takeIf { it.isNotBlank() }
                if (uri != null) {
                    webList.add(GroundingSourceItem(title = title, uri = uri))
                }
            }
        }
        return webList to metadata.searchEntryPoint?.renderedContent
    }

    private fun extractJsonObject(raw: String): JSONObject? {
        return try {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start in 0..end) JSONObject(raw.substring(start, end + 1)) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDistance(km: Double): String {
        return if (km < 1.0) "${(km * 1000).toInt()} m" else "${String.format("%.1f", km)} km"
    }

    private data class DraftLeg(
        val modeRaw: String,
        val boardingPoint: String,
        val alightingPoint: String,
        val startLat: Double,
        val startLng: Double,
        val endLat: Double,
        val endLng: Double,
        val instruction: String
    )
}
