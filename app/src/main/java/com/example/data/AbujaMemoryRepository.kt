package com.example.data

import com.example.data.local.AbujaMemoryDao
import com.example.data.local.LearnedCorridorEntity
import com.example.data.local.SavedPlaceEntity
import com.example.data.local.SavedRouteEntity
import com.example.data.local.TripSurveyEntity
import com.example.model.PostTripSurveyInput
import com.example.model.RoutePlan
import kotlinx.coroutines.flow.Flow

class AbujaMemoryRepository(private val dao: AbujaMemoryDao) {

    val learnedCorridors: Flow<List<LearnedCorridorEntity>> = dao.observeLearnedCorridors()
    val tripSurveys: Flow<List<TripSurveyEntity>> = dao.observeTripSurveys()
    val savedRoutes: Flow<List<SavedRouteEntity>> = dao.observeSavedRoutes()
    val savedPlaces: Flow<List<SavedPlaceEntity>> = dao.observeSavedPlaces()

    suspend fun ensureSeededAbujaMemory(forceRestore: Boolean = false) {
        if (forceRestore || dao.getLearnedCorridorsSnapshot().isEmpty()) {
            dao.insertLearnedCorridors(
                listOf(
                    LearnedCorridorEntity(
                        originArea = "Wuse Market",
                        destinationArea = "Federal Secretariat, CBD",
                        corridorTitle = "Wuse Market -> Federal Secretariat",
                        trekAlightPoint = "Alight at Ministry / Eagle Square junction and trek 350m to Secretariat gate",
                        commercialBikePoint = "No Okada — banned inside Wuse II & CBD",
                        kekeTimeWindow = "Inner Wuse feeder streets only (avoid CBD main boulevards)",
                        kekeCommercialAllowed = "Restricted to inner feeder streets only",
                        bikeCommercialAllowed = "Banned in Wuse & CBD",
                        verifiedFareNaira = 350,
                        routeSummary = "Board Along taxi or Abuja bus from Wuse Market Gate to Federal Secretariat (est. ₦300–₦500), alight at Ministry junction, trek 350m.",
                        tripCount = 3
                    ),
                    LearnedCorridorEntity(
                        originArea = "Area 1 Shopping Complex",
                        destinationArea = "Area 3 Junction, Garki",
                        corridorTitle = "Area 1 -> Area 3 (Trek it)",
                        trekAlightPoint = "Trek directly along Tafawa Balewa Way walkway (under 1.5 km)",
                        commercialBikePoint = "Not needed",
                        kekeTimeWindow = "Did not take Keke",
                        kekeCommercialAllowed = "Allowed in Garki Area 3 inner streets",
                        bikeCommercialAllowed = "Banned on Garki main roads",
                        verifiedFareNaira = 0,
                        routeSummary = "Under 1.5 km (~1.1 km). Walk directly from Area 1 Shopping Complex to Area 3 Junction in ~12 mins for ₦0.",
                        tripCount = 2
                    ),
                    LearnedCorridorEntity(
                        originArea = "Berger Roundabout",
                        destinationArea = "Gwarinpa 1st Avenue",
                        corridorTitle = "Berger -> Gwarinpa",
                        trekAlightPoint = "Alight at Gwarinpa 1st Avenue Gate to catch inner estate Keke",
                        commercialBikePoint = "Banned on Kubwa Express; outer village fringes only",
                        kekeTimeWindow = "All day (6am–9pm inside Gwarinpa estates)",
                        kekeCommercialAllowed = "Allowed inside Gwarinpa estate roads",
                        bikeCommercialAllowed = "Banned on Kubwa Express & 1st Ave",
                        verifiedFareNaira = 550,
                        routeSummary = "Take Along taxi or bus at Berger underbridge to Gwarinpa 1st Ave Gate (est. ₦350–₦500), then Keke inside estate (est. ₦150–₦250).",
                        tripCount = 4
                    ),
                    LearnedCorridorEntity(
                        originArea = "Lugbe FHA Gate",
                        destinationArea = "Area 1 Roundabout",
                        corridorTitle = "Lugbe FHA -> Area 1 (Airport Road)",
                        trekAlightPoint = "Alight at Area 1 Roundabout underbridge and trek 250m",
                        commercialBikePoint = "Use Okada only inside Lugbe inner estates to reach FHA Gate",
                        kekeTimeWindow = "Morning & Evening along Lugbe inner lanes",
                        kekeCommercialAllowed = "Allowed inside Lugbe estates (not Airport Express)",
                        bikeCommercialAllowed = "Allowed inside Lugbe inner streets; banned on Airport Express",
                        verifiedFareNaira = 600,
                        routeSummary = "Bike/Keke inside Lugbe to FHA Gate (est. ₦150–₦250), then Airport Road Along taxi/bus straight to Area 1 (est. ₦350–₦550).",
                        tripCount = 2
                    )
                )
            )
        }
    }

    suspend fun getMemoryContextPrompt(origin: String = "", destination: String = ""): String {
        val corridors = dao.getLearnedCorridorsSnapshot()
        val places = dao.getSavedPlacesSnapshot()

        val relevantCorridors = if (origin.isNotBlank() || destination.isNotBlank()) {
            val matches = (dao.searchCorridors(origin) + dao.searchCorridors(destination))
                .distinctBy { it.id }
            if (matches.isNotEmpty()) matches else corridors.take(6)
        } else {
            corridors.take(8)
        }

        val corridorLines = relevantCorridors.joinToString("\n") { c ->
            "- [${c.corridorTitle} | ${c.tripCount} trip(s) | Est. ~₦${c.verifiedFareNaira}]: " +
                "Alight/Trek: ${c.trekAlightPoint}. Okada: ${c.commercialBikePoint}. " +
                "Keke: ${c.kekeTimeWindow}. Rules: Keke=${c.kekeCommercialAllowed}, Bike=${c.bikeCommercialAllowed}. " +
                "Summary: ${c.routeSummary}"
        }
        val placeLines = if (places.isNotEmpty()) {
            "\nSAVED COMMUTER PLACES IN ABUJA:\n" + places.joinToString("\n") { p ->
                "- ${p.label}: ${p.placeName} (${String.format("%.4f", p.lat)}, ${String.format("%.4f", p.lng)})"
            }
        } else {
            ""
        }
        return (corridorLines + placeLines).trim()
    }

    suspend fun recordTripSurveyAndTrainMemory(survey: PostTripSurveyInput) {
        dao.insertTripSurvey(
            TripSurveyEntity(
                originName = survey.originName.trim(),
                destinationName = survey.destinationName.trim(),
                alightAndTrekPoint = survey.alightAndTrekPoint.trim(),
                commercialBikePoint = survey.commercialBikePoint.trim(),
                kekeTimeTaken = survey.kekeTimeTaken.trim(),
                kekeAllowedStatus = survey.kekeAllowedStatus.trim(),
                bikeAllowedStatus = survey.bikeAllowedStatus.trim(),
                areaChecked = survey.areaChecked.trim(),
                farePaidNaira = survey.farePaidNaira.coerceAtLeast(0)
            )
        )

        val existingMatches = dao.searchCorridors(survey.originName)
            .firstOrNull {
                it.destinationArea.contains(survey.destinationName.take(8), ignoreCase = true) ||
                    it.originArea.contains(survey.originName.take(8), ignoreCase = true) &&
                    it.destinationArea.equals(survey.destinationName, ignoreCase = true)
            }

        val summary = buildString {
            append("Reported ~₦${survey.farePaidNaira}. ")
            if (survey.alightAndTrekPoint.isNotBlank()) append("Alight & trek: ${survey.alightAndTrekPoint}. ")
            if (survey.commercialBikePoint.isNotBlank() && !survey.commercialBikePoint.contains("Did not", ignoreCase = true)) {
                append("Okada leg: ${survey.commercialBikePoint}. ")
            }
            if (!survey.kekeTimeTaken.contains("Did not", ignoreCase = true)) {
                append("Keke taken: ${survey.kekeTimeTaken}. ")
            }
            append("Area (${survey.areaChecked}): Keke=${survey.kekeAllowedStatus}, Bike=${survey.bikeAllowedStatus}.")
        }.trim()

        val updated = if (existingMatches != null) {
            val blendedFare = if (survey.farePaidNaira > 0) {
                (existingMatches.verifiedFareNaira + survey.farePaidNaira) / 2
            } else {
                existingMatches.verifiedFareNaira
            }
            existingMatches.copy(
                trekAlightPoint = survey.alightAndTrekPoint.ifBlank { existingMatches.trekAlightPoint },
                commercialBikePoint = survey.commercialBikePoint.ifBlank { existingMatches.commercialBikePoint },
                kekeTimeWindow = survey.kekeTimeTaken.ifBlank { existingMatches.kekeTimeWindow },
                kekeCommercialAllowed = "${survey.kekeAllowedStatus} (${survey.areaChecked})",
                bikeCommercialAllowed = "${survey.bikeAllowedStatus} (${survey.areaChecked})",
                verifiedFareNaira = blendedFare,
                routeSummary = summary,
                tripCount = existingMatches.tripCount + 1,
                updatedAtMillis = System.currentTimeMillis()
            )
        } else {
            LearnedCorridorEntity(
                originArea = survey.originName.trim(),
                destinationArea = survey.destinationName.trim(),
                corridorTitle = "${survey.originName.trim()} -> ${survey.destinationName.trim()}",
                trekAlightPoint = survey.alightAndTrekPoint.ifBlank { "Direct drop at destination" },
                commercialBikePoint = survey.commercialBikePoint.ifBlank { "Not used" },
                kekeTimeWindow = survey.kekeTimeTaken.ifBlank { "Anytime" },
                kekeCommercialAllowed = "${survey.kekeAllowedStatus} (${survey.areaChecked})",
                bikeCommercialAllowed = "${survey.bikeAllowedStatus} (${survey.areaChecked})",
                verifiedFareNaira = survey.farePaidNaira,
                routeSummary = summary,
                tripCount = 1,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
        dao.insertLearnedCorridor(updated)
    }

    suspend fun trainMemoryFromTextTip(
        originArea: String,
        destinationArea: String,
        tipSummary: String,
        fareNaira: Int
    ) {
        dao.insertLearnedCorridor(
            LearnedCorridorEntity(
                originArea = originArea.trim(),
                destinationArea = destinationArea.trim(),
                corridorTitle = "${originArea.trim()} -> ${destinationArea.trim()}",
                trekAlightPoint = "See commuter tip",
                commercialBikePoint = "See commuter tip",
                kekeTimeWindow = "Reported by commuter",
                kekeCommercialAllowed = "Check local Abuja district rule",
                bikeCommercialAllowed = "Check local Abuja district rule",
                verifiedFareNaira = fareNaira.coerceAtLeast(0),
                routeSummary = tipSummary.trim(),
                tripCount = 1,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun savePlace(label: String, placeName: String, lat: Double, lng: Double) {
        dao.insertSavedPlace(
            SavedPlaceEntity(
                label = label.trim().ifBlank { "Saved Place" },
                placeName = placeName.trim(),
                lat = lat,
                lng = lng
            )
        )
    }

    suspend fun deleteSavedPlace(id: Int) = dao.deleteSavedPlaceById(id)

    suspend fun saveRoutePlan(plan: RoutePlan) {
        val fareSummary = if (plan.trekRecommended) {
            "₦0 (Trek it)"
        } else {
            "Est. ₦${plan.totalFareMinNaira}–₦${plan.totalFareMaxNaira}"
        }
        val stepsSummary = plan.legs.joinToString(" -> ") { leg ->
            "Step ${leg.stepNumber} (${leg.mode}): ${leg.instruction}"
        }
        dao.insertSavedRoute(
            SavedRouteEntity(
                originName = plan.originName,
                destinationName = plan.destinationName,
                distanceKm = plan.distanceKm,
                estimatedMinutes = plan.totalMinutes,
                fareSummary = fareSummary,
                trekRecommended = plan.trekRecommended,
                stepsSummary = stepsSummary
            )
        )
    }

    suspend fun deleteSavedRoute(id: Int) = dao.deleteSavedRouteById(id)

    suspend fun clearAllMemory() {
        dao.clearLearnedCorridors()
        dao.clearTripSurveys()
        dao.clearSavedRoutes()
        dao.clearSavedPlaces()
    }
}
