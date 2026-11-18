package com.example.data

import android.util.Log
import com.example.model.LatLngPoint
import com.example.model.NominatimPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "OsmTransitService"

class OutsideAbujaException(
    message: String = "WHERE currently covers Abuja only."
) : IllegalArgumentException(message)

data class OsrmRouteResult(
    val points: List<LatLngPoint>,
    val distanceKm: Double,
    val durationMinutes: Int,
    val roadNames: List<String>,
    val isFromOsrm: Boolean
)

class OsmTransitService {

    companion object {
        const val ABUJA_CENTER_LAT = 9.0765
        const val ABUJA_CENTER_LNG = 7.3986

        // Abuja FCT bounding box (south, west, north, east)
        const val FCT_MIN_LAT = 8.40
        const val FCT_MAX_LAT = 9.40
        const val FCT_MIN_LNG = 6.75
        const val FCT_MAX_LNG = 7.75

        private const val NOMINATIM_MIN_INTERVAL_MS = 1050L
        private const val USER_AGENT = "WHERE-Abuja-Commuter/1.0 (OpenStreetMap Abuja FCT App)"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val nominatimMutex = Mutex()
    private var lastNominatimCallMs = 0L

    fun isInsideAbuja(lat: Double, lng: Double): Boolean {
        return lat in FCT_MIN_LAT..FCT_MAX_LAT && lng in FCT_MIN_LNG..FCT_MAX_LNG
    }

    suspend fun searchPlaces(rawQuery: String): Result<List<NominatimPlace>> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.length < 2) return@withContext Result.success(emptyList())

        val lower = query.lowercase()
        val knownNonAbujaCities = listOf(
            "lagos", "ikeja", "lekki", "yaba", "kano", "ibadan",
            "port harcourt", "enugu", "kaduna", "benin city", "london", "new york"
        )
        if (knownNonAbujaCities.any { lower.contains(it) && !lower.contains("abuja") }) {
            return@withContext Result.failure(OutsideAbujaException("WHERE currently covers Abuja only."))
        }

        val localMatches = AbujaFareTable.POPULAR_PLACES.filter {
            it.name.contains(query, ignoreCase = true) || it.fullName.contains(query, ignoreCase = true)
        }.map {
            NominatimPlace(
                displayName = it.fullName,
                shortName = it.name,
                lat = it.lat,
                lng = it.lng,
                isWithinAbuja = true
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val viewbox = "$FCT_MIN_LNG,$FCT_MAX_LAT,$FCT_MAX_LNG,$FCT_MIN_LAT"
            val url = "https://nominatim.openstreetmap.org/search" +
                "?format=jsonv2&q=$encodedQuery&countrycodes=ng&viewbox=$viewbox&bounded=0&limit=6&addressdetails=1"

            val body = executeNominatimGet(url)
            if (body != null) {
                val arr = JSONArray(body)
                val parsed = mutableListOf<NominatimPlace>()
                var sawOutsideAbuja = false

                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                    val lng = obj.optString("lon").toDoubleOrNull() ?: continue
                    val displayName = obj.optString("display_name", query)
                    val short = obj.optString("name").ifBlank {
                        displayName.split(",").firstOrNull()?.trim().orEmpty().ifBlank { query }
                    }
                    if (!isInsideAbuja(lat, lng)) {
                        sawOutsideAbuja = true
                    } else {
                        parsed.add(
                            NominatimPlace(
                                displayName = displayName,
                                shortName = short,
                                lat = lat,
                                lng = lng,
                                isWithinAbuja = true
                            )
                        )
                    }
                }

                val combined = (localMatches + parsed).distinctBy {
                    "${it.shortName.lowercase()}_${String.format("%.3f", it.lat)}"
                }
                if (combined.isEmpty() && sawOutsideAbuja) {
                    return@withContext Result.failure(OutsideAbujaException("WHERE currently covers Abuja only."))
                }
                return@withContext Result.success(combined)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim search failed, using local Abuja matches", e)
        }

        Result.success(localMatches)
    }

    suspend fun resolveSingleAbujaPlace(query: String): Result<NominatimPlace> {
        val cleaned = query.trim()
        AbujaFareTable.POPULAR_PLACES.firstOrNull {
            cleaned.equals(it.name, ignoreCase = true) ||
                cleaned.equals(it.fullName, ignoreCase = true) ||
                cleaned.contains(it.name, ignoreCase = true)
        }?.let { pop ->
            return Result.success(
                NominatimPlace(
                    displayName = pop.fullName,
                    shortName = pop.name,
                    lat = pop.lat,
                    lng = pop.lng,
                    isWithinAbuja = true
                )
            )
        }

        val searchRes = searchPlaces(cleaned)
        return searchRes.fold(
            onSuccess = { list ->
                val match = list.firstOrNull { it.isWithinAbuja }
                if (match != null) {
                    Result.success(match)
                } else {
                    Result.failure(
                        IllegalArgumentException("Could not find \"$cleaned\" in OpenStreetMap for Abuja. Try an Abuja district or Pick on map.")
                    )
                }
            },
            onFailure = { err -> Result.failure(err) }
        )
    }

    suspend fun reverseGeocodeAbuja(lat: Double, lng: Double): Result<NominatimPlace> = withContext(Dispatchers.IO) {
        if (!isInsideAbuja(lat, lng)) {
            return@withContext Result.failure(OutsideAbujaException("WHERE currently covers Abuja only."))
        }
        val fallbackName = "Abuja Pin (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lng&zoom=16"
            val body = executeNominatimGet(url)
            if (body != null) {
                val obj = JSONObject(body)
                val displayName = obj.optString("display_name", fallbackName)
                val short = obj.optString("name").ifBlank {
                    val parts = displayName.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    parts.take(2).joinToString(", ").ifBlank { fallbackName }
                }
                return@withContext Result.success(
                    NominatimPlace(
                        displayName = displayName,
                        shortName = short,
                        lat = lat,
                        lng = lng,
                        isWithinAbuja = true
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim reverse geocode failed", e)
        }
        Result.success(
            NominatimPlace(
                displayName = fallbackName,
                shortName = fallbackName,
                lat = lat,
                lng = lng,
                isWithinAbuja = true
            )
        )
    }

    /**
     * Queries OpenStreetMap Overpass API for real Abuja bus stops, taxi stands, and stations near a coordinate.
     */
    suspend fun fetchNearbyOsmTransitNodes(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 1800
    ): List<NominatimPlace> = withContext(Dispatchers.IO) {
        if (!isInsideAbuja(lat, lng)) return@withContext emptyList()
        try {
            val overpassQuery = """
                [out:json][timeout:6];
                (
                  node["highway"="bus_stop"](around:$radiusMeters,$lat,$lng);
                  node["amenity"="bus_station"](around:$radiusMeters,$lat,$lng);
                  node["amenity"="taxi"](around:$radiusMeters,$lat,$lng);
                  node["public_transport"="station"](around:$radiusMeters,$lat,$lng);
                );
                out body 8;
            """.trimIndent()

            val formBody = FormBody.Builder()
                .add("data", overpassQuery)
                .build()
            val req = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .header("User-Agent", USER_AGENT)
                .post(formBody)
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val elements = JSONObject(body).optJSONArray("elements") ?: return@withContext emptyList()
                val results = mutableListOf<NominatimPlace>()
                for (i in 0 until elements.length()) {
                    val el = elements.optJSONObject(i) ?: continue
                    val nLat = el.optDouble("lat", Double.NaN)
                    val nLng = el.optDouble("lon", Double.NaN)
                    if (nLat.isNaN() || nLng.isNaN() || !isInsideAbuja(nLat, nLng)) continue
                    val tags = el.optJSONObject("tags")
                    val name = tags?.optString("name")?.takeIf { it.isNotBlank() } ?: continue
                    results.add(
                        NominatimPlace(
                            displayName = "$name (OpenStreetMap stop)",
                            shortName = name,
                            lat = nLat,
                            lng = nLng,
                            isWithinAbuja = true
                        )
                    )
                }
                return@withContext results.distinctBy { it.shortName.lowercase() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Overpass OpenStreetMap stop lookup skipped", e)
            emptyList()
        }
    }

    /**
     * Queries public OSRM (powered by OpenStreetMap road data) for route geometry and real street names.
     */
    suspend fun fetchOsrmPath(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        mode: AbujaTransitMode
    ): OsrmRouteResult = withContext(Dispatchers.IO) {
        val profile = if (mode == AbujaTransitMode.TREK) "foot" else "driving"
        val straightDistKm = haversineKm(startLat, startLng, endLat, endLng).coerceAtLeast(0.05)
        val fallbackMinutes = estimateMinutes(straightDistKm, mode)
        val fallbackPoints = listOf(LatLngPoint(startLat, startLng), LatLngPoint(endLat, endLng))

        try {
            val url = "https://router.project-osrm.org/route/v1/$profile/" +
                "$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson&steps=true"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext OsrmRouteResult(fallbackPoints, straightDistKm, fallbackMinutes, emptyList(), false)
                }
                val body = response.body?.string()
                    ?: return@withContext OsrmRouteResult(fallbackPoints, straightDistKm, fallbackMinutes, emptyList(), false)

                val root = JSONObject(body)
                val routes = root.optJSONArray("routes")
                val first = routes?.optJSONObject(0)
                    ?: return@withContext OsrmRouteResult(fallbackPoints, straightDistKm, fallbackMinutes, emptyList(), false)

                val distKm = (first.optDouble("distance", straightDistKm * 1000.0) / 1000.0).coerceAtLeast(0.05)
                val durationSecs = first.optDouble("duration", fallbackMinutes * 60.0)
                val osrmMinutes = (durationSecs / 60.0).toInt().coerceAtLeast(2)

                val adjustedMins = if (mode == AbujaTransitMode.TREK) {
                    (distKm * 12.5).toInt().coerceAtLeast(2)
                } else {
                    maxOf(osrmMinutes, estimateMinutes(distKm, mode))
                }

                // Extract real OpenStreetMap road names from OSRM steps
                val roadNames = mutableListOf<String>()
                val legsArr = first.optJSONArray("legs")
                if (legsArr != null) {
                    for (l in 0 until legsArr.length()) {
                        val stepsArr = legsArr.optJSONObject(l)?.optJSONArray("steps") ?: continue
                        for (s in 0 until stepsArr.length()) {
                            val name = stepsArr.optJSONObject(s)?.optString("name")?.trim().orEmpty()
                            if (name.isNotBlank() && name != "unnamed road") {
                                roadNames.add(name)
                            }
                        }
                    }
                }

                val coords = first.optJSONObject("geometry")?.optJSONArray("coordinates")
                val points = mutableListOf<LatLngPoint>()
                if (coords != null) {
                    for (i in 0 until coords.length()) {
                        val pt = coords.optJSONArray(i) ?: continue
                        val lng = pt.optDouble(0)
                        val lat = pt.optDouble(1)
                        points.add(LatLngPoint(lat = lat, lng = lng))
                    }
                }

                if (points.size >= 2) {
                    return@withContext OsrmRouteResult(
                        points = points,
                        distanceKm = distKm,
                        durationMinutes = adjustedMins,
                        roadNames = roadNames.distinct().take(4),
                        isFromOsrm = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "OSRM route request failed, falling back to straight dashed line", e)
        }

        OsrmRouteResult(fallbackPoints, straightDistKm, fallbackMinutes, emptyList(), false)
    }

    private suspend fun executeNominatimGet(url: String): String? {
        return nominatimMutex.withLock {
            val now = System.currentTimeMillis()
            val wait = NOMINATIM_MIN_INTERVAL_MS - (now - lastNominatimCallMs)
            if (wait > 0) delay(wait)
            lastNominatimCallMs = System.currentTimeMillis()

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "en")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun estimateMinutes(distKm: Double, mode: AbujaTransitMode): Int {
        val speedKmh = when (mode) {
            AbujaTransitMode.TREK -> 4.8
            AbujaTransitMode.KEKE -> 22.0
            AbujaTransitMode.OKADA -> 28.0
            AbujaTransitMode.BUS -> 24.0
            AbujaTransitMode.TAXI -> 30.0
        }
        return ((distKm / speedKmh) * 60.0).toInt().coerceAtLeast(2)
    }
}
