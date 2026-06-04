package com.dolmus.netapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Serializable
data class RouteData(
    val route_name: String,
    val direction: String,
    val points: List<PointData>,
    val city: String = ""
)

@Serializable
data class PointData(
    val lat: Double,
    val lng: Double,
    val label: String = ""
)

fun filterOutliers(points: List<GeoPoint>, thresholdMeters: Float = 80f): List<GeoPoint> {
    if (points.size < 4) return points

    val filtered = mutableListOf<GeoPoint>()
    for (i in points.indices) {
        val windowStart = maxOf(0, i - 2)
        val windowEnd   = minOf(points.lastIndex, i + 2)
        val window      = points.subList(windowStart, windowEnd + 1)
        val avgLat      = window.map { it.latitude }.average()
        val avgLng      = window.map { it.longitude }.average()

        val results = FloatArray(1)
        Location.distanceBetween(
            points[i].latitude, points[i].longitude,
            avgLat, avgLng,
            results
        )
        if (results[0] <= thresholdMeters) {
            filtered.add(points[i])
        }
    }
    return if (filtered.size < points.size * 0.5) points else filtered
}

fun calculateRouteDistance(points: List<GeoPoint>): Double {
    var total = 0.0
    for (i in 1 until points.size) {
        val results = FloatArray(1)
        Location.distanceBetween(
            points[i-1].latitude, points[i-1].longitude,
            points[i].latitude,   points[i].longitude,
            results
        )
        total += results[0]
    }
    return total / 1000.0
}

fun saveRouteLocally(context: Context, routeName: String, direction: String, points: List<PointData>) {
    val prefs = context.getSharedPreferences("dolmus_routes", Context.MODE_PRIVATE)
    val key   = "route_${routeName}_${direction}"
    val json  = Json.encodeToString(points)
    prefs.edit().putString(key, json).apply()
}

@SuppressLint("MissingPermission")
@Composable
fun CalibrationScreen(
    lang: String,
    routeName: String,
    onDone: () -> Unit,
    onStartWork: () -> Unit = {}
) {
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val driverCity = remember { getDriverCity(context) }

    var startLabel      by remember { mutableStateOf("") }
    var endLabel        by remember { mutableStateOf("") }

    var phase           by remember { mutableStateOf("idle") }
    var goingPoints     by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var returningPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var filteredGoingCount     by remember { mutableStateOf(0) }
    var filteredReturningCount by remember { mutableStateOf(0) }
    var statusMessage   by remember { mutableStateOf("") }
    var isUploading     by remember { mutableStateOf(false) }
    var routeDistanceKm by remember { mutableStateOf(0.0) }
    var currentGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }

    val mapView           = remember { MapView(context) }
    val goingPolyline     = remember {
        Polyline().apply {
            outlinePaint.color = android.graphics.Color.parseColor("#1E88E5")
            outlinePaint.strokeWidth = 10f
        }
    }
    val returningPolyline = remember {
        Polyline().apply {
            outlinePaint.color = android.graphics.Color.parseColor("#E53935")
            outlinePaint.strokeWidth = 10f
        }
    }
    val currentMarker = remember { Marker(mapView) }

    LaunchedEffect(Unit) {
        if (CalibrationSession.hasPendingGoing(context)) {
            startLabel  = CalibrationSession.getStartLabel(context)
            endLabel    = CalibrationSession.getEndLabel(context)
            val saved   = CalibrationSession.getGoingPoints(context)
            val geoList = saved.map { GeoPoint(it.lat, it.lng) }
            goingPoints = geoList
            goingPolyline.setPoints(geoList)
            mapView.invalidate()
            phase         = "going_done"
            statusMessage = "تم استرجاع خط الذهاب — ابدأ تسجيل العودة"
        }
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.0)
        mapView.overlays.addAll(listOf(goingPolyline, returningPolyline, currentMarker))

        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_START_CALIBRATION
        }
        context.startForegroundService(intent)

        LocationForegroundService.onNewLocation = { loc ->
            val gp = GeoPoint(loc.latitude, loc.longitude)
            currentGeoPoint = gp
            currentMarker.position = gp
            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.controller.setCenter(gp)
            mapView.invalidate()
        }

        onDispose { LocationForegroundService.onNewLocation = null }
    }

    LaunchedEffect(phase) {
        while (phase == "going" || phase == "returning") {
            delay(1000L)
            val servicePts = LocationForegroundService.calibrationPoints.toList()
            val geoPoints  = servicePts.map { GeoPoint(it.latitude, it.longitude) }

            if (phase == "going") {
                goingPoints = geoPoints
                if (geoPoints.isNotEmpty()) {
                    goingPolyline.setPoints(geoPoints)
                    mapView.invalidate()
                }
            } else if (phase == "returning") {
                returningPoints = geoPoints
                if (geoPoints.isNotEmpty()) {
                    returningPolyline.setPoints(geoPoints)
                    mapView.invalidate()
                }
            }
        }
    }

    // ─── شاشة النتيجة ───────────────────────────────────────────
    if (phase == "done") {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("✅", fontSize = 56.sp)
                Text(
                    when(lang) { "tr" -> "Hat Kaydedildi!"; "en" -> "Route Saved!"; else -> "تم حفظ الخط!" },
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoRow(
                            label = when(lang) { "tr" -> "Hat Adı"; "en" -> "Route Name"; else -> "اسم الخط" },
                            value = routeName
                        )
                        HorizontalDivider()
                        if (driverCity.isNotBlank()) {
                            InfoRow(
                                label = when(lang) { "tr" -> "Şehir"; "en" -> "City"; else -> "المدينة" },
                                value = driverCity,
                                valueColor = Color(0xFF388E3C)
                            )
                            HorizontalDivider()
                        }
                        InfoRow(
                            label = when(lang) { "tr" -> "Toplam Mesafe"; "en" -> "Total Distance"; else -> "المسافة الكلية" },
                            value = "${(routeDistanceKm * 10).roundToInt() / 10.0} كم"
                        )
                        HorizontalDivider()
                        InfoRow(
                            label = when(lang) { "tr" -> "Gidiş Noktaları"; "en" -> "Going Points"; else -> "نقاط الذهاب" },
                            value = "${goingPoints.size}",
                            valueColor = Color(0xFF1E88E5)
                        )
                        if (filteredGoingCount > 0) {
                            InfoRow(
                                label = when(lang) { "tr" -> "Filtrelenen"; "en" -> "Filtered Out"; else -> "نقاط محذوفة" },
                                value = "$filteredGoingCount",
                                valueColor = Color(0xFFFF6F00)
                            )
                        }
                        HorizontalDivider()
                        InfoRow(
                            label = when(lang) { "tr" -> "Dönüş Noktaları"; "en" -> "Return Points"; else -> "نقاط العودة" },
                            value = "${returningPoints.size}",
                            valueColor = Color(0xFFE53935)
                        )
                        if (filteredReturningCount > 0) {
                            InfoRow(
                                label = when(lang) { "tr" -> "Filtrelenen"; "en" -> "Filtered Out"; else -> "نقاط محذوفة" },
                                value = "$filteredReturningCount",
                                valueColor = Color(0xFFFF6F00)
                            )
                        }
                        HorizontalDivider()
                        InfoRow(
                            label = when(lang) { "tr" -> "Tahmini Süre"; "en" -> "Est. Duration"; else -> "المدة التقديرية" },
                            value = "${(routeDistanceKm / 30 * 60).roundToInt()} ${when(lang) { "tr" -> "dk"; "en" -> "min"; else -> "دقيقة" }}"
                        )
                    }
                }
                Button(
                    onClick = { onStartWork() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text(
                        when(lang) { "tr" -> "▶  Çalışmaya Başla"; "en" -> "▶  Start Work"; else -> "▶  ابدأ العمل" },
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
                OutlinedButton(
                    onClick = { onDone() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        when(lang) { "tr" -> "Çıkış"; "en" -> "Exit"; else -> "خروج" },
                        fontSize = 16.sp, color = Color(0xFF1E88E5)
                    )
                }
            }
        }
        return
    }

    // ─── شاشة المعايرة ──────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(routeName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (driverCity.isNotBlank()) {
                Text("📍 $driverCity", fontSize = 11.sp, color = Color(0xFF90CAF9))
            }
            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, fontSize = 12.sp, color = Color(0xFFBBDEFB))
            }
            currentGeoPoint?.let {
                Text(
                    "${String.format("%.5f", it.latitude)}, ${String.format("%.5f", it.longitude)}",
                    fontSize = 10.sp, color = Color(0xFF90CAF9)
                )
            }
            when (phase) {
                "going"     -> Text("🔵 ${goingPoints.size} ${when(lang) { "tr" -> "nokta"; "en" -> "pts"; else -> "نقطة" }}", color = Color(0xFF90CAF9), fontSize = 12.sp)
                "returning" -> Text("🔴 ${returningPoints.size} ${when(lang) { "tr" -> "nokta"; "en" -> "pts"; else -> "نقطة" }}", color = Color(0xFFEF9A9A), fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xEEFFFFFF))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (phase) {

                // ─── idle ────────────────────────────────────────
                "idle" -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startLabel,
                        onValueChange = { startLabel = it },
                        label = { Text(when(lang) { "tr" -> "Başlangıç Noktası"; "en" -> "Start Point"; else -> "نقطة البداية" }) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor    = Color.Black,
                            unfocusedTextColor  = Color.Black,
                            focusedLabelColor   = Color(0xFF1E88E5),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    OutlinedTextField(
                        value = endLabel,
                        onValueChange = { endLabel = it },
                        label = { Text(when(lang) { "tr" -> "Bitiş Noktası"; "en" -> "End Point"; else -> "نقطة النهاية" }) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor    = Color.Black,
                            unfocusedTextColor  = Color.Black,
                            focusedLabelColor   = Color(0xFF1E88E5),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Button(
                        onClick = {
                            CalibrationSession.saveHeader(context, routeName, startLabel, endLabel)
                            LocationForegroundService.calibrationPoints.clear()
                            val i = Intent(context, LocationForegroundService::class.java).apply {
                                action = LocationForegroundService.ACTION_START_CALIBRATION
                            }
                            context.startForegroundService(i)
                            phase = "going"
                            statusMessage = when(lang) { "tr" -> "Gidiş kaydediliyor..."; "en" -> "Recording going..."; else -> "جاري تسجيل خط الذهاب..." }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Text(
                            when(lang) { "tr" -> "Gidişi Başlat"; "en" -> "Start Going"; else -> "ابدأ خط الذهاب" },
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                }

                // ─── going ───────────────────────────────────────
                "going" -> Button(
                    onClick = {
                        val rawPts = LocationForegroundService.calibrationPoints.toList()
                        val rawGeo = rawPts.map { GeoPoint(it.latitude, it.longitude) }

                        val cleanGeo = filterOutliers(rawGeo)
                        filteredGoingCount = rawGeo.size - cleanGeo.size

                        val pointData = cleanGeo.mapIndexed { i, geo ->
                            PointData(geo.latitude, geo.longitude,
                                if (i == 0) "بداية الذهاب"
                                else if (i == cleanGeo.lastIndex) "نهاية الذهاب"
                                else ""
                            )
                        }
                        saveRouteLocally(context, routeName, "going", pointData)
                        goingPoints = cleanGeo
                        goingPolyline.setPoints(cleanGeo)

                        // ─── رمز بداية الذهاب (🏠) ───────────────
                        cleanGeo.firstOrNull()?.let { first ->
                            Marker(mapView).also { m ->
                                m.position = first
                                m.title = if (startLabel.isNotBlank()) "🟢 $startLabel" else "🚌 ${when(lang) { "tr" -> "Başlangıç"; "en" -> "Start"; else -> "بداية الذهاب" }}"
                                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mapView.overlays.add(m)
                                m.showInfoWindow()
                            }
                        }
                        // ─── رمز نهاية الذهاب (🚩) ───────────────
                        cleanGeo.lastOrNull()?.let { last ->
                            Marker(mapView).also { m ->
                                m.position = last
                                m.title = if (endLabel.isNotBlank()) "🔴 $endLabel" else "🔵 ${when(lang) { "tr" -> "Gidiş Sonu"; "en" -> "Going End"; else -> "نهاية الذهاب" }}"
                                m.snippet = routeName
                                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mapView.overlays.add(m)
                                m.showInfoWindow()
                            }
                        }
                        mapView.invalidate()

                        val pauseIntent = Intent(context, LocationForegroundService::class.java).apply {
                            action = LocationForegroundService.ACTION_PAUSE_SAVING
                        }
                        context.startService(pauseIntent)

                        CalibrationSession.saveHeader(context, routeName, startLabel, endLabel)
                        CalibrationSession.saveGoingPoints(
                            context,
                            cleanGeo.map { CalibrationSession.SavedPoint(it.latitude, it.longitude) }
                        )

                        phase = "going_done"
                        statusMessage = when(lang) { "tr" -> "Gidiş tamamlandı."; "en" -> "Going done."; else -> "اكتمل خط الذهاب. ابدأ خط العودة." }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text(
                        when(lang) { "tr" -> "Gidiş Bitti"; "en" -> "End Going"; else -> "انتهى خط الذهاب" },
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }

                // ─── going_done ──────────────────────────────────
                "going_done" -> Button(
                    onClick = {
                        LocationForegroundService.calibrationPoints.clear()
                        val resumeIntent = Intent(context, LocationForegroundService::class.java).apply {
                            action = LocationForegroundService.ACTION_RESUME_SAVING
                        }
                        context.startService(resumeIntent)
                        phase = "returning"
                        statusMessage = when(lang) { "tr" -> "Dönüş kaydediliyor..."; "en" -> "Recording return..."; else -> "جاري تسجيل خط العودة..." }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) {
                    Text(
                        when(lang) { "tr" -> "Dönüşü Başlat"; "en" -> "Start Return"; else -> "ابدأ خط العودة" },
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }

                // ─── returning ───────────────────────────────────
                "returning" -> Button(
                    onClick = {
                        val rawPts = LocationForegroundService.calibrationPoints.toList()
                        val rawGeo = rawPts.map { GeoPoint(it.latitude, it.longitude) }

                        val cleanGeo = filterOutliers(rawGeo)
                        filteredReturningCount = rawGeo.size - cleanGeo.size

                        val pointData = cleanGeo.mapIndexed { i, geo ->
                            PointData(geo.latitude, geo.longitude,
                                if (i == 0) "بداية العودة"
                                else if (i == cleanGeo.lastIndex) "نهاية العودة"
                                else ""
                            )
                        }
                        saveRouteLocally(context, routeName, "returning", pointData)
                        returningPoints = cleanGeo
                        returningPolyline.setPoints(cleanGeo)

                        // ─── رموز العودة محذوفة بالكامل ──────────

                        mapView.invalidate()

                        val pauseIntent = Intent(context, LocationForegroundService::class.java).apply {
                            action = LocationForegroundService.ACTION_PAUSE_SAVING
                        }
                        context.startService(pauseIntent)
                        phase = "returning_done"
                        statusMessage = when(lang) { "tr" -> "Hazır. Yükleyin."; "en" -> "Ready. Upload."; else -> "جاهز للرفع." }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text(
                        when(lang) { "tr" -> "Dönüş Bitti"; "en" -> "End Return"; else -> "انتهى خط العودة" },
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }

                // ─── returning_done ──────────────────────────────
                "returning_done" -> Button(
                    onClick = {
                        phase = "uploading"
                        isUploading = true
                        scope.launch {
                            try {
                                val goingData = RouteData(
                                    route_name = routeName,
                                    direction  = "going",
                                    city       = driverCity,
                                    points     = goingPoints.mapIndexed { i, p ->
                                        PointData(p.latitude, p.longitude,
                                            if (i == 0) "بداية الذهاب"
                                            else if (i == goingPoints.lastIndex) "نهاية الذهاب"
                                            else ""
                                        )
                                    }
                                )
                                val returningData = RouteData(
                                    route_name = routeName,
                                    direction  = "returning",
                                    city       = driverCity,
                                    points     = returningPoints.mapIndexed { i, p ->
                                        PointData(p.latitude, p.longitude,
                                            if (i == 0) "بداية العودة"
                                            else if (i == returningPoints.lastIndex) "نهاية العودة"
                                            else ""
                                        )
                                    }
                                )
                                supabase.postgrest["routes"].insert(listOf(goingData, returningData))
                                CalibrationSession.clear(context)
                                routeDistanceKm = calculateRouteDistance(goingPoints) + calculateRouteDistance(returningPoints)
                                phase = "done"
                                statusMessage = ""
                            } catch (e: Exception) {
                                statusMessage = e.message ?: "Error"
                                phase = "returning_done"
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    enabled = !isUploading
                ) {
                    if (isUploading)
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                    else
                        Text(
                            when(lang) { "tr" -> "Kaydet ve Gönder"; "en" -> "Save & Upload"; else -> "حفظ وإرسال" },
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                }

                // ─── uploading ───────────────────────────────────
                "uploading" -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF1E88E5))
                        Text(
                            when(lang) { "tr" -> "Yükleniyor..."; "en" -> "Uploading..."; else -> "جاري الرفع..." },
                            color = Color(0xFF1E88E5)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = Color(0xFF1E88E5)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = valueColor)
    }
}