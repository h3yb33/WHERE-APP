package com.example.data

import android.util.Log
import com.example.model.GroundingSourceItem
import com.example.model.LatLngPoint
import com.example.model.NominatimPlace
import com.example.model.RouteBundle
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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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

    // Route planner uses OpenStreetMap (Nominatim + Overpass + OSRM) + Abuja AI Memory
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
                      "rushHourNotice": "Rush hour in Abuja: some drivers add ₦100 to ₦200. Agree on the fare before you board.",
                      "legs": [
                        {
                          "stepNumber": 1,
                          "mode": "TREK | BUS | TAXI | KEKE | OKADA",
                          "boardingPoint": "OpenStreetMap junction/stop name",
                          "alightingPoint": "OpenStreetMap junction/stop name",
                          "callingLabel": "Destination sign drivers call out, e.g. Berger or Area 1",
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
    ): Result<RouteBundle> {
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

            val straightKm = osmService.haversineKm(
                resolvedOrigin.lat,
                resolvedOrigin.lng,
                resolvedDest.lat,
                resolvedDest.lng
            )
            val skipOverpassForShortHop = straightKm < 1.35

            // Fetch OpenStreetMap road path (OSRM) and nearby OpenStreetMap transit stops (Overpass) in parallel
            val (directFootOsrm, directDriveOsrm, originStops, destStops) = coroutineScope {
                val footDeferred = async {
                    osmService.fetchOsrmPath(
                        resolvedOrigin.lat,
                        resolvedOrigin.lng,
                        resolvedDest.lat,
                        resolvedDest.lng,
                        AbujaTransitMode.TREK
                    )
                }
                val driveDeferred = async {
                    osmService.fetchOsrmPath(
                        resolvedOrigin.lat,
                        resolvedOrigin.lng,
                        resolvedDest.lat,
                        resolvedDest.lng,
                        AbujaTransitMode.DROP
                    )
                }
                val oStopsDeferred = async {
                    if (skipOverpassForShortHop) emptyList()
                    else osmService.fetchNearbyOsmTransitNodes(resolvedOrigin.lat, resolvedOrigin.lng)
                }
                val dStopsDeferred = async {
                    if (skipOverpassForShortHop) emptyList()
                    else osmService.fetchNearbyOsmTransitNodes(resolvedDest.lat, resolvedDest.lng)
                }
                Quadruple(
                    footDeferred.await(),
                    driveDeferred.await(),
                    oStopsDeferred.await(),
                    dStopsDeferred.await()
                )
            }

            // Option for short trips under 1.5 km: Trek it (₦0) + optional Direct drop
            if (directFootOsrm.distanceKm < 1.5) {
                val roadNote = directFootOsrm.roadNames.takeIf { it.isNotEmpty() }
                    ?.joinToString(" -> ")
                    .orEmpty()
                val trekLeg = TransitLeg(
                    stepNumber = 1,
                    mode = "TREK",
                    headline = "Walk ${formatDistance(directFootOsrm.distanceKm)} to ${resolvedDest.shortName}",
                    subHeadline = "About ${directFootOsrm.durationMinutes} min on foot",
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
                    fareLabel = "FREE",
                    fareMinNaira = 0,
                    fareMaxNaira = 0,
                    overchargeAboveNaira = 0,
                    roadNamesSummary = roadNote,
                    pathPoints = directFootOsrm.points,
                    isOsrmPath = directFootOsrm.isFromOsrm
                )
                val trekPlan = RoutePlan(
                    optionTag = "Trek it",
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
                    changesCount = 0,
                    walkDistanceKm = directFootOsrm.distanceKm,
                    isDoorToDoor = true,
                    rushHourNotice = "Under 1.5 km — walking saves you ₦200–₦500 short-drop charges.",
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
                val dropPlan = buildDirectDropOption(
                    origin = resolvedOrigin,
                    dest = resolvedDest,
                    driveOsrm = directDriveOsrm,
                    corridorEntry = corridorEntry,
                    rushHourNotice = "Short drop in Abuja: agree on a flat cab fare before entering."
                )
                return Result.success(RouteBundle(options = listOf(trekPlan, dropPlan)))
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
            var rushHourNotice = "Rush hour in Abuja: some drivers add ₦100 to ₦200. Agree on the fare before you board."
            val draftLegs = mutableListOf<DraftLeg>()

            try {
                val model = createRoutePlannerModel(memoryContext)
                val prompt = "Plan Abuja transit legs from \"${resolvedOrigin.shortName}\" (${resolvedOrigin.lat}, ${resolvedOrigin.lng}) " +
                    "to \"${resolvedDest.shortName}\" (${resolvedDest.lat}, ${resolvedDest.lng}). " +
                    "OpenStreetMap context: $osmStopsSummary. " +
                    "Corridor restriction: ${corridorEntry.zoneRestrictionNote}. Return raw JSON only."

                val response = withTimeoutOrNull(12_000L) {
                    model.generateContent(prompt)
                }
                if (response == null) {
                    Log.w(TAG, "Gemini leg breakdown timed out after 12s; using OpenStreetMap + AbujaFareTable fallback")
                }
                val rawJson = response?.text?.let { extractJsonObject(it) }
                if (rawJson != null) {
                    aiInsight = rawJson.optString("memoryInsightUsed", "")
                    val tip = rawJson.optString("antiInflationTip", "")
                    if (tip.isNotBlank()) aiTip = tip
                    val rush = rawJson.optString("rushHourNotice", "")
                    if (rush.isNotBlank()) rushHourNotice = rush
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
                                    callingLabel = obj.optString("callingLabel", ""),
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
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Gemini leg breakdown timed out, using OpenStreetMap + AbujaFareTable fallback", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini leg breakdown failed, falling back to OpenStreetMap + AbujaFareTable", e)
            }

            // Build Option 1 (Cheapest) and Option 2 (Fastest) leg paths concurrently
            val cheapestDrafts = buildCheapestDrafts(
                rawDrafts = draftLegs,
                origin = resolvedOrigin,
                dest = resolvedDest,
                originStops = originStops,
                destStops = destStops,
                driveOsrm = directDriveOsrm,
                corridorEntry = corridorEntry
            )
            val fastestDrafts = buildFastestDrafts(
                origin = resolvedOrigin,
                dest = resolvedDest,
                originStops = originStops,
                destStops = destStops,
                driveOsrm = directDriveOsrm,
                corridorEntry = corridorEntry
            )

            val (cheapestPlan, fastestPlan) = coroutineScope {
                val cheapestDeferred = async {
                    materializeOptionPlan(
                        optionTag = "Cheapest",
                        drafts = cheapestDrafts,
                        origin = resolvedOrigin,
                        dest = resolvedDest,
                        corridorEntry = corridorEntry,
                        rushHourNotice = rushHourNotice,
                        antiInflationTip = aiTip,
                        memoryInsightUsed = aiInsight
                    )
                }
                val fastestDeferred = async {
                    materializeOptionPlan(
                        optionTag = "Fastest",
                        drafts = fastestDrafts,
                        origin = resolvedOrigin,
                        dest = resolvedDest,
                        corridorEntry = corridorEntry,
                        rushHourNotice = rushHourNotice,
                        antiInflationTip = "Take direct Along taxis at major junctions to cut transfer wait times.",
                        memoryInsightUsed = aiInsight
                    )
                }
                cheapestDeferred.await() to fastestDeferred.await()
            }

            // Build Option 3: Direct drop (door-to-door private Drop cab)
            val directDropPlan = buildDirectDropOption(
                origin = resolvedOrigin,
                dest = resolvedDest,
                driveOsrm = directDriveOsrm,
                corridorEntry = corridorEntry,
                rushHourNotice = rushHourNotice
            )

            Result.success(
                RouteBundle(
                    options = listOf(cheapestPlan, fastestPlan, directDropPlan)
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

    private fun buildCheapestDrafts(
        rawDrafts: List<DraftLeg>,
        origin: NominatimPlace,
        dest: NominatimPlace,
        originStops: List<NominatimPlace>,
        destStops: List<NominatimPlace>,
        driveOsrm: OsrmRouteResult,
        corridorEntry: CorridorFareEntry
    ): List<DraftLeg> {
        if (rawDrafts.size >= 2) {
            val copy = rawDrafts.toMutableList()
            copy[0] = copy.first().copy(startLat = origin.lat, startLng = origin.lng)
            copy[copy.lastIndex] = copy.last().copy(endLat = dest.lat, endLng = dest.lng)
            return copy
        }

        val firstStop = originStops.firstOrNull()?.shortName ?: origin.shortName
        val midPoint = midpointAlongPath(driveOsrm.points, 0.72)
        val junctionName = destStops.firstOrNull()?.shortName
            ?: driveOsrm.roadNames.lastOrNull()?.takeIf { it.isNotBlank() }?.let { "$it Junction" }
            ?: "${dest.shortName} Gate"

        val finalMode = if (AbujaTransitMode.KEKE in corridorEntry.allowedModes) "KEKE" else "TREK"
        return listOf(
            DraftLeg(
                modeRaw = "BUS",
                boardingPoint = firstStop,
                alightingPoint = junctionName,
                callingLabel = dest.shortName.split(" ").firstOrNull() ?: dest.shortName,
                startLat = origin.lat,
                startLng = origin.lng,
                endLat = midPoint.lat,
                endLng = midPoint.lng,
                instruction = "Board an Abuja bus or shared Along at $firstStop toward $junctionName."
            ),
            DraftLeg(
                modeRaw = finalMode,
                boardingPoint = junctionName,
                alightingPoint = dest.shortName,
                callingLabel = dest.shortName,
                startLat = midPoint.lat,
                startLng = midPoint.lng,
                endLat = dest.lat,
                endLng = dest.lng,
                instruction = if (finalMode == "TREK") {
                    "Walk the final stretch from $junctionName to ${dest.shortName}."
                } else {
                    "Board a Keke at $junctionName heading to ${dest.shortName}."
                }
            )
        )
    }

    private fun buildFastestDrafts(
        origin: NominatimPlace,
        dest: NominatimPlace,
        originStops: List<NominatimPlace>,
        destStops: List<NominatimPlace>,
        driveOsrm: OsrmRouteResult,
        corridorEntry: CorridorFareEntry
    ): List<DraftLeg> {
        val firstStop = originStops.firstOrNull()?.shortName ?: origin.shortName
        val mid1 = midpointAlongPath(driveOsrm.points, 0.55)
        val mid2 = midpointAlongPath(driveOsrm.points, 0.85)
        val jct1 = driveOsrm.roadNames.firstOrNull()?.let { "$it Axis" } ?: "Main Junction"
        val jct2 = destStops.firstOrNull()?.shortName ?: "${dest.shortName} Stop"

        return if (driveOsrm.distanceKm >= 8.0) {
            val feederMode = if (AbujaTransitMode.KEKE in corridorEntry.allowedModes) "KEKE" else "TAXI"
            listOf(
                DraftLeg(
                    modeRaw = "TAXI",
                    boardingPoint = firstStop,
                    alightingPoint = jct1,
                    callingLabel = jct1,
                    startLat = origin.lat,
                    startLng = origin.lng,
                    endLat = mid1.lat,
                    endLng = mid1.lng,
                    instruction = "Board a fast Along taxi at $firstStop calling \"$jct1\"."
                ),
                DraftLeg(
                    modeRaw = "BUS",
                    boardingPoint = jct1,
                    alightingPoint = jct2,
                    callingLabel = dest.shortName,
                    startLat = mid1.lat,
                    startLng = mid1.lng,
                    endLat = mid2.lat,
                    endLng = mid2.lng,
                    instruction = "Connect an express Abuja corridor bus at $jct1 to $jct2."
                ),
                DraftLeg(
                    modeRaw = feederMode,
                    boardingPoint = jct2,
                    alightingPoint = dest.shortName,
                    callingLabel = dest.shortName,
                    startLat = mid2.lat,
                    startLng = mid2.lng,
                    endLat = dest.lat,
                    endLng = dest.lng,
                    instruction = "Finish the last hop from $jct2 to ${dest.shortName}."
                )
            )
        } else {
            listOf(
                DraftLeg(
                    modeRaw = "TAXI",
                    boardingPoint = firstStop,
                    alightingPoint = jct2,
                    callingLabel = dest.shortName,
                    startLat = origin.lat,
                    startLng = origin.lng,
                    endLat = mid2.lat,
                    endLng = mid2.lng,
                    instruction = "Board an Along taxi at $firstStop straight toward $jct2."
                ),
                DraftLeg(
                    modeRaw = if (AbujaTransitMode.KEKE in corridorEntry.allowedModes) "KEKE" else "TAXI",
                    boardingPoint = jct2,
                    alightingPoint = dest.shortName,
                    callingLabel = dest.shortName,
                    startLat = mid2.lat,
                    startLng = mid2.lng,
                    endLat = dest.lat,
                    endLng = dest.lng,
                    instruction = "Hop a short feeder ride at $jct2 to ${dest.shortName}."
                )
            )
        }
    }

    private suspend fun materializeOptionPlan(
        optionTag: String,
        drafts: List<DraftLeg>,
        origin: NominatimPlace,
        dest: NominatimPlace,
        corridorEntry: CorridorFareEntry,
        rushHourNotice: String,
        antiInflationTip: String,
        memoryInsightUsed: String
    ): RoutePlan = coroutineScope {
        val finalLegs = drafts.mapIndexed { idx, draft ->
            async {
                val safeMode = AbujaFareTable.normalizeModeForCorridor(draft.modeRaw, corridorEntry)
                val osrm = osmService.fetchOsrmPath(
                    draft.startLat,
                    draft.startLng,
                    draft.endLat,
                    draft.endLng,
                    safeMode
                )
                val fareBand = AbujaFareTable.estimateLegFare(
                    origin = origin.shortName,
                    destination = dest.shortName,
                    mode = safeMode,
                    legDistanceKm = osrm.distanceKm
                )
                val roadSummary = osrm.roadNames.joinToString(" · ")
                val board = draft.boardingPoint.ifBlank { origin.shortName }
                val alight = draft.alightingPoint.ifBlank { dest.shortName }
                val calling = draft.callingLabel.ifBlank { alight }

                val headline = when (safeMode) {
                    AbujaTransitMode.TREK -> "Walk ${formatDistance(osrm.distanceKm)} to $alight"
                    AbujaTransitMode.BUS -> "Board an Abuja bus at $board"
                    AbujaTransitMode.TAXI -> "Board an Along taxi at $board"
                    AbujaTransitMode.KEKE -> "Board a Keke at $board"
                    AbujaTransitMode.OKADA -> "Board a commercial bike at $board"
                    AbujaTransitMode.DROP -> "Take a Direct drop cab from $board"
                }

                val subHeadline = when (safeMode) {
                    AbujaTransitMode.TREK -> "About ${osrm.durationMinutes} min on foot"
                    AbujaTransitMode.DROP -> "Door-to-door ride straight to $alight."
                    else -> "Look for one calling \"$calling\". Get off at $alight."
                }

                val stops = if (safeMode == AbujaTransitMode.TREK || safeMode == AbujaTransitMode.DROP) {
                    0
                } else {
                    (osrm.distanceKm / 1.4).toInt().coerceIn(2, 12)
                }
                val waitMins = when (safeMode) {
                    AbujaTransitMode.TREK -> 0
                    AbujaTransitMode.KEKE, AbujaTransitMode.OKADA -> 3
                    AbujaTransitMode.BUS -> 6
                    AbujaTransitMode.TAXI -> 4
                    AbujaTransitMode.DROP -> 4
                }
                val helperNote = when (safeMode) {
                    AbujaTransitMode.TREK -> ""
                    AbujaTransitMode.BUS -> "Have small ₦100/₦200 notes ready before boarding."
                    AbujaTransitMode.TAXI -> "Confirm Along drop price with the driver before moving."
                    AbujaTransitMode.KEKE -> "Check inner-street access; Keke is restricted on expressways."
                    AbujaTransitMode.OKADA -> "Outer estate feeder streets only (banned in Phase 1 & CBD)."
                    AbujaTransitMode.DROP -> "Agree on the final drop fare upfront before the trip starts."
                }
                val changeAt = if (idx < drafts.lastIndex) "Change at $alight" else ""

                TransitLeg(
                    stepNumber = idx + 1,
                    mode = safeMode.name,
                    headline = headline,
                    subHeadline = subHeadline,
                    boardingPoint = board,
                    alightingPoint = alight,
                    startLat = draft.startLat,
                    startLng = draft.startLng,
                    endLat = draft.endLat,
                    endLng = draft.endLng,
                    instruction = draft.instruction,
                    areaRuleNote = corridorEntry.zoneRestrictionNote,
                    distanceKm = osrm.distanceKm,
                    distanceText = formatDistance(osrm.distanceKm),
                    durationMinutes = osrm.durationMinutes,
                    stopCount = stops,
                    waitMinutes = waitMins,
                    fareLabel = "FAIR FARE",
                    isFixedFare = false,
                    fareMinNaira = fareBand.minNaira,
                    fareMaxNaira = fareBand.maxNaira,
                    overchargeAboveNaira = fareBand.overchargeAboveNaira,
                    fareHelperNote = helperNote,
                    changeAtNote = changeAt,
                    roadNamesSummary = roadSummary,
                    pathPoints = osrm.points,
                    isOsrmPath = osrm.isFromOsrm
                )
            }
        }.awaitAll()

        val totalDistKm = finalLegs.sumOf { it.distanceKm }.coerceAtLeast(0.5)
        val totalMinutes = finalLegs.sumOf { it.durationMinutes + it.waitMinutes }.coerceAtLeast(4)
        val walkDistKm = finalLegs.filter { it.mode == "TREK" }.sumOf { it.distanceKm }
        val changesCount = (finalLegs.size - 1).coerceAtLeast(0)

        RoutePlan(
            optionTag = optionTag,
            originName = origin.shortName,
            destinationName = dest.shortName,
            originLat = origin.lat,
            originLng = origin.lng,
            destinationLat = dest.lat,
            destinationLng = dest.lng,
            distanceKm = totalDistKm,
            totalMinutes = totalMinutes,
            totalFareMinNaira = finalLegs.sumOf { it.fareMinNaira },
            totalFareMaxNaira = finalLegs.sumOf { it.fareMaxNaira },
            totalOverchargeAboveNaira = finalLegs.sumOf { it.overchargeAboveNaira },
            changesCount = changesCount,
            walkDistanceKm = walkDistKm,
            isDoorToDoor = false,
            rushHourNotice = rushHourNotice,
            fareEstimateDisclaimer = AbujaFareTable.ESTIMATE_DISCLAIMER,
            trekRecommended = false,
            antiInflationTip = antiInflationTip,
            memoryInsightUsed = memoryInsightUsed,
            legs = finalLegs
        )
    }

    private fun buildDirectDropOption(
        origin: NominatimPlace,
        dest: NominatimPlace,
        driveOsrm: OsrmRouteResult,
        corridorEntry: CorridorFareEntry,
        rushHourNotice: String
    ): RoutePlan {
        val fareBand = AbujaFareTable.estimateLegFare(
            origin = origin.shortName,
            destination = dest.shortName,
            mode = AbujaTransitMode.DROP,
            legDistanceKm = driveOsrm.distanceKm
        )
        val roadSummary = driveOsrm.roadNames.joinToString(" · ")
        val leg = TransitLeg(
            stepNumber = 1,
            mode = AbujaTransitMode.DROP.name,
            headline = "Take a Direct drop cab from ${origin.shortName}",
            subHeadline = "Door-to-door ride straight to ${dest.shortName}.",
            boardingPoint = origin.shortName,
            alightingPoint = dest.shortName,
            startLat = origin.lat,
            startLng = origin.lng,
            endLat = dest.lat,
            endLng = dest.lng,
            instruction = "Hire a private cab or ride-hailing Drop from ${origin.shortName} straight to ${dest.shortName}.",
            areaRuleNote = corridorEntry.zoneRestrictionNote,
            distanceKm = driveOsrm.distanceKm,
            distanceText = formatDistance(driveOsrm.distanceKm),
            durationMinutes = driveOsrm.durationMinutes,
            stopCount = 0,
            waitMinutes = 4,
            fareLabel = "DIRECT DROP ESTIMATE",
            isFixedFare = false,
            fareMinNaira = fareBand.minNaira,
            fareMaxNaira = fareBand.maxNaira,
            overchargeAboveNaira = fareBand.overchargeAboveNaira,
            fareHelperNote = "Agree on the full drop fare upfront before the trip starts.",
            changeAtNote = "",
            roadNamesSummary = roadSummary,
            pathPoints = driveOsrm.points,
            isOsrmPath = driveOsrm.isFromOsrm
        )
        return RoutePlan(
            optionTag = "Direct drop",
            originName = origin.shortName,
            destinationName = dest.shortName,
            originLat = origin.lat,
            originLng = origin.lng,
            destinationLat = dest.lat,
            destinationLng = dest.lng,
            distanceKm = driveOsrm.distanceKm,
            totalMinutes = driveOsrm.durationMinutes + 4,
            totalFareMinNaira = fareBand.minNaira,
            totalFareMaxNaira = fareBand.maxNaira,
            totalOverchargeAboveNaira = fareBand.overchargeAboveNaira,
            changesCount = 0,
            walkDistanceKm = 0.0,
            isDoorToDoor = true,
            rushHourNotice = rushHourNotice,
            fareEstimateDisclaimer = AbujaFareTable.ESTIMATE_DISCLAIMER,
            trekRecommended = false,
            antiInflationTip = "Direct drops cost significantly more than Along or Bus — compare quotes before riding.",
            memoryInsightUsed = "",
            legs = listOf(leg)
        )
    }

    private fun midpointAlongPath(points: List<LatLngPoint>, fraction: Double): LatLngPoint {
        if (points.isEmpty()) return LatLngPoint(OsmTransitService.ABUJA_CENTER_LAT, OsmTransitService.ABUJA_CENTER_LNG)
        val idx = ((points.lastIndex) * fraction).toInt().coerceIn(0, points.lastIndex)
        return points[idx]
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
        val callingLabel: String = "",
        val startLat: Double,
        val startLng: Double,
        val endLat: Double,
        val endLng: Double,
        val instruction: String
    )

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
