package com.example.data

import kotlin.math.roundToInt

enum class AbujaTransitMode(val label: String) {
    TREK("Trek"),
    BUS("Bus"),
    TAXI("Along"),
    KEKE("Keke"),
    OKADA("Okada"),
    DROP("Drop")
}

data class FareBand(
    val minNaira: Int,
    val maxNaira: Int,
    val overchargeAboveNaira: Int
)

data class CorridorFareEntry(
    val id: String,
    val corridorLabel: String,
    val keywords: List<String>,
    val zoneRestrictionNote: String,
    val allowedModes: Set<AbujaTransitMode>,
    val modeFareEstimates: Map<AbujaTransitMode, FareBand>
)

data class AbujaPopularPlace(
    val name: String,
    val fullName: String,
    val lat: Double,
    val lng: Double
)

data class SampleTripPreset(
    val title: String,
    val subtitle: String,
    val originName: String,
    val originLat: Double,
    val originLng: Double,
    val destinationName: String,
    val destinationLat: Double,
    val destinationLng: Double
)

/**
 * Single editable data table for estimated Abuja, FCT transit fares (₦) and mode availability.
 * All fares are estimates only and vary slightly with fuel costs, weather, and rush hour.
 */
object AbujaFareTable {

    const val ESTIMATE_DISCLAIMER =
        "Estimated fair fares from the Abuja corridor table (subject to rush hour & fuel shifts). Always confirm before boarding."

    val POPULAR_PLACES = listOf(
        AbujaPopularPlace("Wuse Market", "Wuse Market, Wuse Zone 5, Abuja", 9.0695, 7.4633),
        AbujaPopularPlace("Federal Secretariat", "Federal Secretariat, Central Business District, Abuja", 9.0616, 7.4998),
        AbujaPopularPlace("Berger", "Berger Roundabout, Utako / Wuse Zone 6, Abuja", 9.0666, 7.4508),
        AbujaPopularPlace("Gwarinpa", "Gwarinpa 1st Avenue, Abuja", 9.1108, 7.4022),
        AbujaPopularPlace("Jabi Lake Mall", "Jabi Lake Mall, Bala Sokoto Way, Abuja", 9.0762, 7.4254),
        AbujaPopularPlace("Nyanya", "Nyanya Bridge / Bus Park, Abuja", 9.0218, 7.5673),
        AbujaPopularPlace("Kubwa", "Kubwa Village / PW Junction, Abuja", 9.1558, 7.3316),
        AbujaPopularPlace("Utako", "Utako Motor Park, Abuja", 9.0692, 7.4404),
        AbujaPopularPlace("Garki", "Garki Area 11 / International Market, Abuja", 9.0355, 7.4905),
        AbujaPopularPlace("Maitama", "Maitama District (Transcorp / Farmers Market), Abuja", 9.0882, 7.4934),
        AbujaPopularPlace("Airport Road", "Lugbe FHA Gate, Airport Road, Abuja", 8.9839, 7.3728)
    )

    val SAMPLE_TRIPS = listOf(
        SampleTripPreset(
            title = "Wuse Market to Federal Secretariat",
            subtitle = "Bus or Along Taxi across CBD · Keke/Okada restricted in core CBD",
            originName = "Wuse Market, Abuja",
            originLat = 9.0695,
            originLng = 7.4633,
            destinationName = "Federal Secretariat, CBD, Abuja",
            destinationLat = 9.0616,
            destinationLng = 7.4998
        ),
        SampleTripPreset(
            title = "Berger to Gwarinpa",
            subtitle = "Along Taxi / Bus to 1st Ave Gate, then inner estate Keke",
            originName = "Berger Roundabout, Abuja",
            originLat = 9.0666,
            originLng = 7.4508,
            destinationName = "Gwarinpa 1st Avenue, Abuja",
            destinationLat = 9.1108,
            destinationLng = 7.4022
        ),
        SampleTripPreset(
            title = "Area 1 Complex to Area 3 Junction",
            subtitle = "Under 1.5 km, so trek it (₦0)",
            originName = "Area 1 Shopping Complex, Garki, Abuja",
            originLat = 9.0268,
            originLng = 7.4754,
            destinationName = "Area 3 Junction, Garki, Abuja",
            destinationLat = 9.0338,
            destinationLng = 7.4820
        )
    )

    /**
     * EDITABLE CORRIDOR & MODE FARE TABLE (Abuja FCT Estimates in Nigerian Naira ₦)
     */
    val CORRIDOR_TABLE: List<CorridorFareEntry> = listOf(
        CorridorFareEntry(
            id = "phase1_core",
            corridorLabel = "Phase 1 Core (Wuse, CBD, Maitama, Asokoro, Garki)",
            keywords = listOf("wuse", "secretariat", "cbd", "maitama", "asokoro", "garki", "area 1", "area 3", "area 11", "eagle square"),
            zoneRestrictionNote = "Okada is banned across Abuja Phase 1 & CBD. Keke is restricted to select inner feeder streets only.",
            allowedModes = setOf(AbujaTransitMode.TREK, AbujaTransitMode.BUS, AbujaTransitMode.TAXI, AbujaTransitMode.KEKE),
            modeFareEstimates = mapOf(
                AbujaTransitMode.TREK to FareBand(0, 0, 0),
                AbujaTransitMode.BUS to FareBand(200, 350, 450),
                AbujaTransitMode.TAXI to FareBand(300, 500, 650),
                AbujaTransitMode.KEKE to FareBand(150, 250, 350)
            )
        ),
        CorridorFareEntry(
            id = "utako_jabi_berger",
            corridorLabel = "Berger – Utako – Jabi – Life Camp Corridor",
            keywords = listOf("berger", "utako", "jabi", "life camp", "kado", "mabushi"),
            zoneRestrictionNote = "Okada is restricted on major arterials; Keke operates inside Jabi/Utako/Kado inner roads.",
            allowedModes = setOf(AbujaTransitMode.TREK, AbujaTransitMode.BUS, AbujaTransitMode.TAXI, AbujaTransitMode.KEKE),
            modeFareEstimates = mapOf(
                AbujaTransitMode.TREK to FareBand(0, 0, 0),
                AbujaTransitMode.BUS to FareBand(200, 350, 450),
                AbujaTransitMode.TAXI to FareBand(300, 450, 600),
                AbujaTransitMode.KEKE to FareBand(150, 300, 400)
            )
        ),
        CorridorFareEntry(
            id = "gwarinpa_kubwa_express",
            corridorLabel = "Berger – Gwarinpa – Kubwa Expressway Corridor",
            keywords = listOf("gwarinpa", "kubwa", "dawaki", "katampe", "jah"),
            zoneRestrictionNote = "Okada & Keke are banned on Kubwa Expressway; Keke operates inside Gwarinpa & Kubwa inner streets.",
            allowedModes = setOf(AbujaTransitMode.TREK, AbujaTransitMode.BUS, AbujaTransitMode.TAXI, AbujaTransitMode.KEKE, AbujaTransitMode.OKADA),
            modeFareEstimates = mapOf(
                AbujaTransitMode.TREK to FareBand(0, 0, 0),
                AbujaTransitMode.BUS to FareBand(300, 500, 650),
                AbujaTransitMode.TAXI to FareBand(400, 650, 800),
                AbujaTransitMode.KEKE to FareBand(150, 300, 400),
                AbujaTransitMode.OKADA to FareBand(150, 300, 400)
            )
        ),
        CorridorFareEntry(
            id = "airport_lugbe",
            corridorLabel = "Airport Road – Lugbe – Area 1 Corridor",
            keywords = listOf("airport", "lugbe", "fha", "galadimawa", "lokogoma", "trademore"),
            zoneRestrictionNote = "Okada & Keke are banned on Umaru Musa Yar'Adua Expressway, but allowed inside Lugbe/Lokogoma estates.",
            allowedModes = setOf(AbujaTransitMode.TREK, AbujaTransitMode.BUS, AbujaTransitMode.TAXI, AbujaTransitMode.KEKE, AbujaTransitMode.OKADA),
            modeFareEstimates = mapOf(
                AbujaTransitMode.TREK to FareBand(0, 0, 0),
                AbujaTransitMode.BUS to FareBand(350, 550, 700),
                AbujaTransitMode.TAXI to FareBand(400, 650, 850),
                AbujaTransitMode.KEKE to FareBand(150, 250, 350),
                AbujaTransitMode.OKADA to FareBand(150, 300, 400)
            )
        ),
        CorridorFareEntry(
            id = "nyanya_karu_mararaba",
            corridorLabel = "AYA – Nyanya – Karu – Apo Corridor",
            keywords = listOf("nyanya", "karu", "aya", "apo", "kurudu", "jabi", "mararaba", "guzape"),
            zoneRestrictionNote = "High-capacity buses & Along taxis run the main expressway; Keke & Okada serve inner Nyanya/Karu feeders.",
            allowedModes = setOf(AbujaTransitMode.TREK, AbujaTransitMode.BUS, AbujaTransitMode.TAXI, AbujaTransitMode.KEKE, AbujaTransitMode.OKADA),
            modeFareEstimates = mapOf(
                AbujaTransitMode.TREK to FareBand(0, 0, 0),
                AbujaTransitMode.BUS to FareBand(300, 500, 650),
                AbujaTransitMode.TAXI to FareBand(350, 600, 750),
                AbujaTransitMode.KEKE to FareBand(150, 250, 350),
                AbujaTransitMode.OKADA to FareBand(150, 250, 350)
            )
        )
    )

    fun findCorridorEntry(origin: String, destination: String): CorridorFareEntry {
        val combined = "$origin $destination".lowercase()
        return CORRIDOR_TABLE.maxByOrNull { entry ->
            entry.keywords.count { kw -> combined.contains(kw) }
        } ?: CORRIDOR_TABLE.first()
    }

    fun normalizeModeForCorridor(requestedModeRaw: String, entry: CorridorFareEntry): AbujaTransitMode {
        val mapped = when (requestedModeRaw.uppercase().trim()) {
            "WALK", "TREK" -> AbujaTransitMode.TREK
            "BUS", "BRT" -> AbujaTransitMode.BUS
            "KEKE", "TRICYCLE" -> AbujaTransitMode.KEKE
            "BIKE", "OKADA", "MOTORBIKE" -> AbujaTransitMode.OKADA
            "DROP", "BOLT", "UBER", "CHARTER" -> AbujaTransitMode.DROP
            else -> AbujaTransitMode.TAXI
        }
        if (mapped == AbujaTransitMode.DROP) return AbujaTransitMode.DROP
        return if (mapped in entry.allowedModes) {
            mapped
        } else if (AbujaTransitMode.TAXI in entry.allowedModes) {
            AbujaTransitMode.TAXI
        } else {
            AbujaTransitMode.BUS
        }
    }

    fun estimateLegFare(
        origin: String,
        destination: String,
        mode: AbujaTransitMode,
        legDistanceKm: Double
    ): FareBand {
        if (mode == AbujaTransitMode.TREK) return FareBand(0, 0, 0)
        if (mode == AbujaTransitMode.DROP) {
            val baseMin = (1200.0 + legDistanceKm * 320.0)
            val min = roundToNearest50(baseMin).coerceAtLeast(1500)
            val max = roundToNearest50(min * 1.32).coerceAtLeast(min + 400)
            val overcharge = roundToNearest50(max * 1.22).coerceAtLeast(max + 500)
            return FareBand(min, max, overcharge)
        }
        val entry = findCorridorEntry(origin, destination)
        val base = entry.modeFareEstimates[mode] ?: FareBand(200, 400, 500)

        // Scale lightly by leg distance while staying anchored to the corridor table
        val multiplier = when {
            legDistanceKm <= 2.5 -> 0.85
            legDistanceKm <= 7.0 -> 1.0
            legDistanceKm <= 14.0 -> 1.25
            else -> 1.5
        }
        val min = roundToNearest50(base.minNaira * multiplier).coerceAtLeast(100)
        val max = roundToNearest50(base.maxNaira * multiplier).coerceAtLeast(min + 50)
        val overcharge = roundToNearest50(base.overchargeAboveNaira * multiplier).coerceAtLeast(max + 100)
        return FareBand(min, max, overcharge)
    }

    private fun roundToNearest50(value: Double): Int {
        return ((value / 50.0).roundToInt() * 50).coerceAtLeast(0)
    }
}
