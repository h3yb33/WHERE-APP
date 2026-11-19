package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.OsmTransitService
import com.example.model.LatLngPoint
import com.example.model.PickMapTarget
import com.example.model.RoutePlan
import com.example.ui.theme.WhereDeepGreen
import com.example.ui.theme.WhereMainText
import com.example.ui.theme.WhereMintBorder
import com.example.ui.theme.WhereMintSurface
import com.example.ui.theme.WhereMutedText
import com.example.ui.theme.WhereSubtleBorder
import com.example.ui.theme.WhereWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

private const val MAP_TAG = "OsmNativeMapView"
private const val OSM_USER_AGENT = "WHERE-Abuja-Commuter/1.0 (OpenStreetMap Abuja FCT App)"
private const val MIN_ZOOM = 10.0
private const val MAX_ZOOM = 17.5

private object OsmTileCache {
    private val cache = LruCache<String, ImageBitmap>(72)
    private val inFlight: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    private val failedKeys: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)
        .build()

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun isInFlight(key: String): Boolean = inFlight.contains(key)

    fun hasFailed(key: String): Boolean = failedKeys.contains(key)

    fun clearFailures() {
        failedKeys.clear()
    }

    suspend fun fetchTile(z: Int, x: Int, y: Int): Boolean {
        val key = "$z/$x/$y"
        if (cache.get(key) != null) return true
        if (!inFlight.add(key)) return false

        return try {
            val bitmap = withContext(Dispatchers.IO) {
                downloadTileBitmap("https://tile.openstreetmap.org/$z/$x/$y.png")
                    ?: downloadTileBitmap("https://a.tile.openstreetmap.fr/osmfr/$z/$x/$y.png")
            }
            if (bitmap != null) {
                failedKeys.remove(key)
                cache.put(key, bitmap)
                true
            } else {
                failedKeys.add(key)
                false
            }
        } catch (e: Exception) {
            Log.w(MAP_TAG, "Tile load error for $key: ${e.message}")
            failedKeys.add(key)
            false
        } finally {
            inFlight.remove(key)
        }
    }

    private fun downloadTileBitmap(url: String): ImageBitmap? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", OSM_USER_AGENT)
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun LeafletAbujaMapView(
    routePlan: RoutePlan?,
    userLocation: Pair<Double, Double>?,
    originMarker: Pair<Double, Double>?,
    destinationMarker: Pair<Double, Double>?,
    pickMapTarget: PickMapTarget,
    onMapTapped: (Double, Double) -> Unit,
    onCancelPickMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnTap by rememberUpdatedState(onMapTapped)
    val latestPickTarget by rememberUpdatedState(pickMapTarget)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val baseTilePx = with(density) { 256.dp.toPx() }

    var centerLat by remember { mutableDoubleStateOf(OsmTransitService.ABUJA_CENTER_LAT) }
    var centerLng by remember { mutableDoubleStateOf(OsmTransitService.ABUJA_CENTER_LNG) }
    var zoom by remember { mutableDoubleStateOf(12.2) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var tileRevision by remember { mutableIntStateOf(0) }
    var loadedTileCount by remember { mutableIntStateOf(0) }
    var failedTileCount by remember { mutableIntStateOf(0) }
    var retryNonce by remember { mutableIntStateOf(0) }

    fun clampLat(lat: Double): Double =
        lat.coerceIn(OsmTransitService.FCT_MIN_LAT, OsmTransitService.FCT_MAX_LAT)

    fun clampLng(lng: Double): Double =
        lng.coerceIn(OsmTransitService.FCT_MIN_LNG, OsmTransitService.FCT_MAX_LNG)

    // Auto-frame Abuja markers / routes whenever they change
    LaunchedEffect(routePlan, originMarker, destinationMarker, userLocation, canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect

        val boundsPts = mutableListOf<Pair<Double, Double>>()
        routePlan?.let { plan ->
            boundsPts.add(plan.originLat to plan.originLng)
            boundsPts.add(plan.destinationLat to plan.destinationLng)
            plan.legs.forEach { leg ->
                leg.pathPoints.forEach { pt -> boundsPts.add(pt.lat to pt.lng) }
            }
        }
        if (boundsPts.isEmpty()) {
            originMarker?.let { boundsPts.add(it) }
            destinationMarker?.let { boundsPts.add(it) }
        }
        if (boundsPts.size >= 2) {
            val minLat = boundsPts.minOf { it.first }
            val maxLat = boundsPts.maxOf { it.first }
            val minLng = boundsPts.minOf { it.second }
            val maxLng = boundsPts.maxOf { it.second }
            centerLat = clampLat((minLat + maxLat) / 2.0)
            centerLng = clampLng((minLng + maxLng) / 2.0)

            var bestZoom = 11.0
            var candidate = 15.5
            while (candidate >= MIN_ZOOM) {
                val n = 2.0.pow(candidate)
                val dx = (lngToWorldX(maxLng, n) - lngToWorldX(minLng, n)) * baseTilePx
                val dy = (latToWorldY(minLat, n) - latToWorldY(maxLat, n)) * baseTilePx
                if (dx <= canvasSize.width * 0.72 && dy <= canvasSize.height * 0.72) {
                    bestZoom = candidate
                    break
                }
                candidate -= 0.25
            }
            zoom = bestZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        } else if (boundsPts.size == 1) {
            centerLat = clampLat(boundsPts[0].first)
            centerLng = clampLng(boundsPts[0].second)
            zoom = 14.0
        } else if (userLocation != null) {
            centerLat = clampLat(userLocation.first)
            centerLng = clampLng(userLocation.second)
            zoom = 13.5
        }
    }

    val tileZ = zoom.toInt().coerceIn(MIN_ZOOM.toInt(), MAX_ZOOM.toInt())
    val tileScale = 2.0.pow(zoom - tileZ).toFloat()
    val renderedTileSizePx = baseTilePx * tileScale
    val nTiles = 1 shl tileZ

    // Request visible OpenStreetMap tiles whenever viewport or retry state changes
    LaunchedEffect(centerLat, centerLng, zoom, canvasSize, retryNonce) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@LaunchedEffect
        val cWorldX = lngToWorldX(centerLng, nTiles.toDouble())
        val cWorldY = latToWorldY(centerLat, nTiles.toDouble())
        val halfWInTiles = (canvasSize.width / 2f) / renderedTileSizePx
        val halfHInTiles = (canvasSize.height / 2f) / renderedTileSizePx

        val minTx = floor(cWorldX - halfWInTiles).toInt().coerceIn(0, nTiles - 1)
        val maxTx = floor(cWorldX + halfWInTiles).toInt().coerceIn(0, nTiles - 1)
        val minTy = floor(cWorldY - halfHInTiles).toInt().coerceIn(0, nTiles - 1)
        val maxTy = floor(cWorldY + halfHInTiles).toInt().coerceIn(0, nTiles - 1)

        for (tx in minTx..maxTx) {
            for (ty in minTy..maxTy) {
                val key = "$tileZ/$tx/$ty"
                val cached = OsmTileCache.get(key)
                if (cached != null) {
                    if (loadedTileCount == 0) loadedTileCount += 1
                    continue
                }
                if (!OsmTileCache.isInFlight(key) && !OsmTileCache.hasFailed(key)) {
                    scope.launch {
                        val ok = OsmTileCache.fetchTile(tileZ, tx, ty)
                        if (ok) {
                            loadedTileCount += 1
                        } else {
                            failedTileCount += 1
                        }
                        tileRevision += 1
                    }
                }
            }
        }
    }

    val showOfflineNotice = loadedTileCount == 0 && failedTileCount >= 4

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFEAF8EF))
            .border(1.dp, WhereSubtleBorder, RoundedCornerShape(20.dp))
            .onSizeChanged { canvasSize = it }
            .pointerInput(baseTilePx) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    val nextZoom = (zoom + log2(zoomChange.toDouble())).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    val curTileZ = nextZoom.toInt().coerceIn(MIN_ZOOM.toInt(), MAX_ZOOM.toInt())
                    val curTileN = (1 shl curTileZ).toDouble()
                    val curTilePx = baseTilePx * 2.0.pow(nextZoom - curTileZ)

                    val curWorldX = lngToWorldX(centerLng, curTileN) - pan.x / curTilePx
                    val curWorldY = latToWorldY(centerLat, curTileN) - pan.y / curTilePx

                    centerLng = clampLng(worldXToLng(curWorldX, curTileN))
                    centerLat = clampLat(worldYToLat(curWorldY, curTileN))
                    zoom = nextZoom
                }
            }
            .pointerInput(baseTilePx) {
                detectTapGestures { offset ->
                    if (latestPickTarget == PickMapTarget.NONE) return@detectTapGestures
                    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectTapGestures
                    val curTileZ = zoom.toInt().coerceIn(MIN_ZOOM.toInt(), MAX_ZOOM.toInt())
                    val curTileN = (1 shl curTileZ).toDouble()
                    val curTilePx = baseTilePx * 2.0.pow(zoom - curTileZ)

                    val cWorldX = lngToWorldX(centerLng, curTileN)
                    val cWorldY = latToWorldY(centerLat, curTileN)
                    val tapWorldX = cWorldX + (offset.x - canvasSize.width / 2f) / curTilePx
                    val tapWorldY = cWorldY + (offset.y - canvasSize.height / 2f) / curTilePx

                    val tappedLat = worldYToLat(tapWorldY, curTileN)
                    val tappedLng = worldXToLng(tapWorldX, curTileN)
                    latestOnTap(tappedLat, tappedLng)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("leaflet_osm_webview")
        ) {
            // Read tileRevision so Compose redraws as OpenStreetMap tiles arrive
            val rev = tileRevision
            if (rev < 0) return@Canvas

            val cWorldX = lngToWorldX(centerLng, nTiles.toDouble())
            val cWorldY = latToWorldY(centerLat, nTiles.toDouble())
            val halfW = size.width / 2f
            val halfH = size.height / 2f

            fun latLngToScreen(lat: Double, lng: Double): Offset {
                val wx = lngToWorldX(lng, nTiles.toDouble())
                val wy = latToWorldY(lat, nTiles.toDouble())
                return Offset(
                    x = halfW + ((wx - cWorldX) * renderedTileSizePx).toFloat(),
                    y = halfH + ((wy - cWorldY) * renderedTileSizePx).toFloat()
                )
            }

            // Subtle placeholder grid under streaming OSM tiles
            val gridStep = 64.dp.toPx()
            val gridColor = Color(0xFFD6EDE0)
            var gx = 0f
            while (gx < size.width) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = 0f
            while (gy < size.height) {
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                gy += gridStep
            }

            val halfWInTiles = halfW / renderedTileSizePx
            val halfHInTiles = halfH / renderedTileSizePx
            val minTx = floor(cWorldX - halfWInTiles).toInt().coerceIn(0, nTiles - 1)
            val maxTx = floor(cWorldX + halfWInTiles).toInt().coerceIn(0, nTiles - 1)
            val minTy = floor(cWorldY - halfHInTiles).toInt().coerceIn(0, nTiles - 1)
            val maxTy = floor(cWorldY + halfHInTiles).toInt().coerceIn(0, nTiles - 1)

            val dstTileSize = ceil(renderedTileSizePx).toInt() + 1

            for (tx in minTx..maxTx) {
                for (ty in minTy..maxTy) {
                    val bmp = OsmTileCache.get("$tileZ/$tx/$ty") ?: continue
                    val left = (halfW + ((tx - cWorldX) * renderedTileSizePx).toFloat()).roundToInt()
                    val top = (halfH + ((ty - cWorldY) * renderedTileSizePx).toFloat()).roundToInt()
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(left, top),
                        dstSize = IntSize(dstTileSize, dstTileSize)
                    )
                }
            }

            // Draw dashed green OSRM route legs
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
            val legStrokeWidth = 4.5.dp.toPx()
            routePlan?.legs?.forEachIndexed { index, leg ->
                val pts = if (leg.pathPoints.size >= 2) {
                    leg.pathPoints
                } else {
                    listOf(
                        LatLngPoint(leg.startLat, leg.startLng),
                        LatLngPoint(leg.endLat, leg.endLng)
                    )
                }
                val path = Path()
                pts.forEachIndexed { ptIdx, pt ->
                    val off = latLngToScreen(pt.lat, pt.lng)
                    if (ptIdx == 0) path.moveTo(off.x, off.y) else path.lineTo(off.x, off.y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF0B6B43).copy(alpha = 0.92f),
                    style = Stroke(
                        width = legStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = dashEffect
                    )
                )

                if (index > 0 && pts.isNotEmpty()) {
                    val transferOff = latLngToScreen(pts.first().lat, pts.first().lng)
                    drawCircle(
                        color = Color.White,
                        radius = 5.5.dp.toPx(),
                        center = transferOff
                    )
                    drawCircle(
                        color = Color(0xFF0B6B43),
                        radius = 5.5.dp.toPx(),
                        center = transferOff,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Draw blue GPS pulse dot
            userLocation?.let { (uLat, uLng) ->
                val uOff = latLngToScreen(uLat, uLng)
                drawCircle(
                    color = Color(0xFF1A73E8).copy(alpha = 0.22f),
                    radius = 15.dp.toPx(),
                    center = uOff
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = uOff
                )
                drawCircle(
                    color = Color(0xFF1A73E8),
                    radius = 5.5.dp.toPx(),
                    center = uOff
                )
            }

            // Draw green start dot
            val oPair = routePlan?.let { it.originLat to it.originLng } ?: originMarker
            oPair?.let { (oLat, oLng) ->
                val oOff = latLngToScreen(oLat, oLng)
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = oOff
                )
                drawCircle(
                    color = Color(0xFF0B6B43),
                    radius = 6.5.dp.toPx(),
                    center = oOff
                )
            }

            // Draw destination pin
            val dPair = routePlan?.let { it.destinationLat to it.destinationLng } ?: destinationMarker
            dPair?.let { (dLat, dLng) ->
                val dOff = latLngToScreen(dLat, dLng)
                val pinHeight = 26.dp.toPx()
                val headRadius = 9.dp.toPx()
                val headCenter = Offset(dOff.x, dOff.y - pinHeight + headRadius)
                val pinPath = Path().apply {
                    moveTo(dOff.x, dOff.y)
                    lineTo(dOff.x - headRadius * 0.8f, headCenter.y + headRadius * 0.35f)
                    lineTo(dOff.x + headRadius * 0.8f, headCenter.y + headRadius * 0.35f)
                    close()
                }
                drawPath(pinPath, color = Color(0xFF0B6B43))
                drawCircle(
                    color = Color(0xFF0B6B43),
                    radius = headRadius,
                    center = headCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = headRadius,
                    center = headCenter,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.2.dp.toPx(),
                    center = headCenter
                )
            }
        }

        if (loadedTileCount == 0 && !showOfflineNotice) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(WhereWhite.copy(alpha = 0.88f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = WhereDeepGreen,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading Abuja OpenStreetMap...",
                        style = MaterialTheme.typography.labelMedium,
                        color = WhereMainText
                    )
                }
            }
        }

        if (showOfflineNotice) {
            Surface(
                color = WhereWhite.copy(alpha = 0.96f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, WhereMintBorder),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = WhereDeepGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "OpenStreetMap needs internet to render tiles",
                        style = MaterialTheme.typography.labelLarge,
                        color = WhereMainText,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Check your connection and tap Retry Map.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WhereMutedText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = {
                            OsmTileCache.clearFailures()
                            failedTileCount = 0
                            retryNonce += 1
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, WhereDeepGreen),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhereDeepGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(" Retry Map", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Top-left Abuja FCT scope badge
        Surface(
            color = WhereWhite.copy(alpha = 0.94f),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, WhereSubtleBorder),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(WhereDeepGreen)
                )
                Text(
                    text = "Abuja, FCT · OpenStreetMap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Top-right Zoom & Center Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = WhereWhite.copy(alpha = 0.94f),
                shape = CircleShape,
                border = BorderStroke(1.dp, WhereSubtleBorder),
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = { zoom = (zoom + 0.8).coerceIn(MIN_ZOOM, MAX_ZOOM) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom in",
                        tint = WhereMainText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Surface(
                color = WhereWhite.copy(alpha = 0.94f),
                shape = CircleShape,
                border = BorderStroke(1.dp, WhereSubtleBorder),
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = { zoom = (zoom - 0.8).coerceIn(MIN_ZOOM, MAX_ZOOM) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom out",
                        tint = WhereMainText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (userLocation != null) {
                Surface(
                    color = WhereWhite.copy(alpha = 0.94f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, WhereSubtleBorder),
                    shadowElevation = 2.dp
                ) {
                    IconButton(
                        onClick = {
                            centerLat = clampLat(userLocation.first)
                            centerLng = clampLng(userLocation.second)
                            zoom = 14.0
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Center on my location",
                            tint = WhereDeepGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Bottom-right explicit OpenStreetMap data credit
        Surface(
            color = WhereWhite.copy(alpha = 0.90f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text(
                text = "© OpenStreetMap contributors",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }

        if (pickMapTarget != PickMapTarget.NONE) {
            val label = if (pickMapTarget == PickMapTarget.FROM) {
                "Tap anywhere on map for Start"
            } else {
                "Tap anywhere on map for Destination"
            }
            Surface(
                color = WhereMintSurface,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, WhereMintBorder),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp, start = 12.dp, end = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = WhereDeepGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = WhereDeepGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel pick on map",
                        tint = WhereDeepGreen,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = onCancelPickMode)
                    )
                }
            }
        }
    }
}

private fun lngToWorldX(lng: Double, nTiles: Double): Double {
    return (lng + 180.0) / 360.0 * nTiles
}

private fun latToWorldY(lat: Double, nTiles: Double): Double {
    val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * nTiles
}

private fun worldXToLng(worldX: Double, nTiles: Double): Double {
    return worldX / nTiles * 360.0 - 180.0
}

private fun worldYToLat(worldY: Double, nTiles: Double): Double {
    return Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * worldY / nTiles))))
}
