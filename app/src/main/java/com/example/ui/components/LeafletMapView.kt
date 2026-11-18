package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Outline
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import org.json.JSONArray
import org.json.JSONObject

private const val MAP_TAG = "LeafletAbujaMapView"

private class LeafletBridge(
    private val onTap: (Double, Double) -> Unit,
    private val onReady: () -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onMapTapped(lat: Double, lng: Double) {
        onTap(lat, lng)
    }

    @JavascriptInterface
    fun onMapReady() {
        onReady()
    }

    @JavascriptInterface
    fun onMapError(message: String) {
        onError(message)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
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
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }
    var isLeafletReady by remember { mutableStateOf(false) }
    var mapErrorMessage by remember { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 20.dp.toPx() }

    val mapHtml = remember {
        buildLeafletHtml()
    }

    fun pushMapUpdate() {
        val webView = webViewRef ?: return
        if (!isPageReady) return
        val payload = JSONObject().apply {
            val oPair = routePlan?.let { it.originLat to it.originLng } ?: originMarker
            val dPair = routePlan?.let { it.destinationLat to it.destinationLng } ?: destinationMarker

            if (userLocation != null) {
                put("userLocation", JSONArray().put(userLocation.first).put(userLocation.second))
            }
            if (oPair != null) {
                put("origin", JSONArray().put(oPair.first).put(oPair.second))
                put("originLabel", routePlan?.originName ?: "Start")
            }
            if (dPair != null) {
                put("destination", JSONArray().put(dPair.first).put(dPair.second))
                put("destLabel", routePlan?.destinationName ?: "Destination")
            }
            val legsArr = JSONArray()
            routePlan?.legs?.forEach { leg ->
                val pts = if (leg.pathPoints.size >= 2) {
                    leg.pathPoints
                } else {
                    listOf(
                        LatLngPoint(leg.startLat, leg.startLng),
                        LatLngPoint(leg.endLat, leg.endLng)
                    )
                }
                val ptsArr = JSONArray()
                pts.forEach { pt -> ptsArr.put(JSONArray().put(pt.lat).put(pt.lng)) }
                legsArr.put(JSONObject().put("points", ptsArr))
            }
            put("legs", legsArr)
        }
        webView.post {
            webView.evaluateJavascript(
                "if (window.updateWhereMap) { window.updateWhereMap($payload); } else { window.__pendingWherePayload = $payload; }",
                null
            )
        }
    }

    LaunchedEffect(routePlan, userLocation, originMarker, destinationMarker, isPageReady, isLeafletReady) {
        pushMapUpdate()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(WhereMintSurface, RoundedCornerShape(20.dp))
            .border(1.dp, WhereSubtleBorder, RoundedCornerShape(20.dp))
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.parseColor("#EAF8EF"))
                    outlineProvider = object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
                        }
                    }
                    clipToOutline = true

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = true
                        blockNetworkImage = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        // Standard mobile Chrome UA + app token avoids OSM default WebView (; wv) blocking
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36 WHERE-Abuja/1.0"
                    }

                    addJavascriptInterface(
                        LeafletBridge(
                            onTap = { lat, lng -> post { latestOnTap(lat, lng) } },
                            onReady = {
                                post {
                                    isLeafletReady = true
                                    mapErrorMessage = null
                                    pushMapUpdate()
                                }
                            },
                            onError = { msg ->
                                post {
                                    Log.w(MAP_TAG, "Leaflet JS error: $msg")
                                    mapErrorMessage = msg
                                }
                            }
                        ),
                        "WhereBridge"
                    )

                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            if (consoleMessage != null) {
                                Log.d(MAP_TAG, "JS [${consoleMessage.messageLevel()}]: ${consoleMessage.message()}")
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isPageReady = true
                            view?.postDelayed({
                                view.evaluateJavascript(
                                    "if (window.__whereMap) { window.__whereMap.invalidateSize(true); }",
                                    null
                                )
                            }, 200L)
                            pushMapUpdate()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                mapErrorMessage = error?.description?.toString()
                                    ?: "Unable to load OpenStreetMap tiles."
                            }
                        }
                    }

                    loadDataWithBaseURL(
                        "https://where-abuja.app/",
                        mapHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    webViewRef = this
                }
            },
            update = { webView ->
                webView.invalidate()
                if (reloadNonce > 0 && mapErrorMessage == null && !isLeafletReady) {
                    webView.loadDataWithBaseURL(
                        "https://where-abuja.app/",
                        mapHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("leaflet_osm_webview")
        )

        if (!isLeafletReady && mapErrorMessage == null) {
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

        if (mapErrorMessage != null) {
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
                        text = mapErrorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = WhereMutedText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = {
                            mapErrorMessage = null
                            isLeafletReady = false
                            reloadNonce += 1
                            webViewRef?.loadDataWithBaseURL(
                                "https://where-abuja.app/",
                                mapHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
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

private fun buildLeafletHtml(): String = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
          onerror="this.onerror=null;this.href='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css';" />
    <style>
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow: hidden;
            background: #EAF8EF;
        }
        #map {
            position: fixed;
            top: 0;
            right: 0;
            bottom: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: #EAF8EF;
        }
        .leaflet-control-attribution {
            font-size: 9px !important;
            background: rgba(255,255,255,0.86) !important;
        }
        .where-dest-pin {
            width: 22px;
            height: 22px;
            background: #0B6B43;
            border: 3px solid #FFFFFF;
            border-radius: 50% 50% 50% 0;
            transform: rotate(-45deg);
            box-shadow: 0 2px 6px rgba(0,0,0,0.35);
        }
        .where-gps-dot {
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: #1A73E8;
            border: 3px solid #FFFFFF;
            box-shadow: 0 0 0 6px rgba(26, 115, 232, 0.25);
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        (function() {
            function bootLeaflet() {
                if (typeof L === 'undefined') {
                    if (window.WhereBridge && window.WhereBridge.onMapError) {
                        window.WhereBridge.onMapError('Could not load Leaflet map engine.');
                    }
                    return;
                }

                var fctBounds = L.latLngBounds(
                    [${OsmTransitService.FCT_MIN_LAT}, ${OsmTransitService.FCT_MIN_LNG}],
                    [${OsmTransitService.FCT_MAX_LAT}, ${OsmTransitService.FCT_MAX_LNG}]
                );

                var map = L.map('map', {
                    center: [${OsmTransitService.ABUJA_CENTER_LAT}, ${OsmTransitService.ABUJA_CENTER_LNG}],
                    zoom: 12,
                    minZoom: 10,
                    maxZoom: 18,
                    maxBounds: fctBounds,
                    maxBoundsViscosity: 0.85,
                    zoomControl: true
                });
                window.__whereMap = map;

                var primaryTiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    attribution: '&copy; OpenStreetMap contributors'
                }).addTo(map);

                var tileErrorCount = 0;
                var tileLoadCount = 0;
                var backupAdded = false;
                primaryTiles.on('tileload', function() {
                    tileLoadCount += 1;
                });
                primaryTiles.on('tileerror', function() {
                    tileErrorCount += 1;
                    if (!backupAdded && tileLoadCount === 0 && tileErrorCount >= 3) {
                        backupAdded = true;
                        L.tileLayer('https://a.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            attribution: '&copy; OpenStreetMap contributors'
                        }).addTo(map);
                    }
                });

                var routeLayer = L.layerGroup().addTo(map);
                var gpsLayer = L.layerGroup().addTo(map);

                map.on('click', function(e) {
                    if (window.WhereBridge && window.WhereBridge.onMapTapped) {
                        window.WhereBridge.onMapTapped(e.latlng.lat, e.latlng.lng);
                    }
                });

                window.updateWhereMap = function(payload) {
                    routeLayer.clearLayers();
                    gpsLayer.clearLayers();
                    var boundsPoints = [];

                    if (payload.userLocation) {
                        var gpsIcon = L.divIcon({
                            className: '',
                            html: '<div class="where-gps-dot"></div>',
                            iconSize: [22, 22],
                            iconAnchor: [11, 11]
                        });
                        L.marker(payload.userLocation, { icon: gpsIcon })
                            .bindPopup('Your Abuja Location')
                            .addTo(gpsLayer);
                        boundsPoints.push(payload.userLocation);
                    }

                    if (payload.legs && payload.legs.length > 0) {
                        payload.legs.forEach(function(leg, index) {
                            if (leg.points && leg.points.length >= 2) {
                                L.polyline(leg.points, {
                                    color: '#0B6B43',
                                    weight: 5,
                                    opacity: 0.92,
                                    dashArray: '8, 8'
                                }).addTo(routeLayer);
                                leg.points.forEach(function(pt) { boundsPoints.push(pt); });
                            }
                            if (index > 0 && leg.points && leg.points.length > 0) {
                                L.circleMarker(leg.points[0], {
                                    radius: 5,
                                    color: '#0B6B43',
                                    weight: 2,
                                    fillColor: '#FFFFFF',
                                    fillOpacity: 1
                                }).addTo(routeLayer);
                            }
                        });
                    }

                    if (payload.origin) {
                        L.circleMarker(payload.origin, {
                            radius: 8,
                            color: '#FFFFFF',
                            weight: 3,
                            fillColor: '#0B6B43',
                            fillOpacity: 1
                        }).bindPopup(payload.originLabel || 'Start').addTo(routeLayer);
                        boundsPoints.push(payload.origin);
                    }

                    if (payload.destination) {
                        var destIcon = L.divIcon({
                            className: '',
                            html: '<div class="where-dest-pin"></div>',
                            iconSize: [24, 24],
                            iconAnchor: [12, 24]
                        });
                        L.marker(payload.destination, { icon: destIcon })
                            .bindPopup(payload.destLabel || 'Destination')
                            .addTo(routeLayer);
                        boundsPoints.push(payload.destination);
                    }

                    map.invalidateSize(true);
                    if (boundsPoints.length >= 2) {
                        map.fitBounds(boundsPoints, { padding: [44, 44], maxZoom: 15 });
                    } else if (boundsPoints.length === 1) {
                        map.setView(boundsPoints[0], 14);
                    }
                };

                setTimeout(function() { map.invalidateSize(true); }, 100);
                setTimeout(function() { map.invalidateSize(true); }, 400);
                setTimeout(function() { map.invalidateSize(true); }, 1000);
                window.addEventListener('resize', function() { map.invalidateSize(true); });

                if (window.__pendingWherePayload) {
                    window.updateWhereMap(window.__pendingWherePayload);
                    window.__pendingWherePayload = null;
                }
                if (window.WhereBridge && window.WhereBridge.onMapReady) {
                    window.WhereBridge.onMapReady();
                }
            }

            if (typeof L !== 'undefined') {
                bootLeaflet();
            } else {
                var fallback = document.createElement('script');
                fallback.src = 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js';
                fallback.onload = bootLeaflet;
                fallback.onerror = function() {
                    if (window.WhereBridge && window.WhereBridge.onMapError) {
                        window.WhereBridge.onMapError('Could not load OpenStreetMap Leaflet scripts.');
                    }
                };
                document.head.appendChild(fallback);
            }
        })();
    </script>
</body>
</html>
""".trimIndent()
