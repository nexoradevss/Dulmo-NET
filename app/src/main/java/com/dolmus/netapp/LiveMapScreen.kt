package com.dolmus.netapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
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
import com.google.android.gms.location.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

data class TripRecord(
    val tripNumber: Int,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Int,
    val passengerCount: Int
)

data class WorkReport(
    val date: String,
    val startTime: String,
    val endTime: String,
    val totalMinutes: Int,
    val trips: List<TripRecord>,
    val routeName: String
)

@Serializable
data class LiveLocation(
    val id: String = "",
    @SerialName("driver_name") val driverName: String = "",
    @SerialName("route_name")  val routeName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    @SerialName("updated_at")  val updatedAt: String = ""
)

@Serializable
data class PassengerLocation(
    val id: String = "",
    @SerialName("route_name") val routeName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

fun loadRouteLocally(context: Context, routeName: String, direction: String): List<PointData> {
    return try {
        val prefs = context.getSharedPreferences("dolmus_routes", Context.MODE_PRIVATE)
        val key   = "route_${routeName}_${direction}"
        val json  = prefs.getString(key, "") ?: ""
        if (json.isBlank()) emptyList()
        else Json.decodeFromString(ListSerializer(PointData.serializer()), json)
    } catch (e: Exception) {
        Log.e("DOLMUS", "Error loading route: ${e.message}")
        emptyList()
    }
}

// ─── أيقونة حافلتك ───────────────────────────────────────────────
fun createBusIcon(
    context: Context,
    driverName: String = "",
    routeName: String = ""
): android.graphics.drawable.BitmapDrawable {
    val width  = 220
    val height = 160
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val cx     = width / 2f

    if (driverName.isNotBlank() || routeName.isNotBlank()) {
        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#CC1565C0")
        }
        canvas.drawRoundRect(android.graphics.RectF(cx - 100f, 0f, cx + 100f, 62f), 10f, 10f, bgPaint)

        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color     = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface  = android.graphics.Typeface.DEFAULT_BOLD
        }

        if (driverName.isNotBlank() && routeName.isNotBlank()) {
            textPaint.textSize = 22f
            canvas.drawText(if (driverName.length > 13) driverName.take(13) + "…" else driverName, cx, 24f, textPaint)
            textPaint.textSize = 19f
            textPaint.color    = android.graphics.Color.parseColor("#90CAF9")
            canvas.drawText(if (routeName.length > 13) routeName.take(13) + "…" else routeName, cx, 52f, textPaint)
        } else {
            textPaint.textSize = 22f
            canvas.drawText(if (driverName.isNotBlank()) driverName else routeName, cx, 38f, textPaint)
        }
    }

    val busPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 80f
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText("🚌", cx, 148f, busPaint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// ─── أيقونة حافلة سائق آخر ───────────────────────────────────────
fun createOtherBusIcon(
    context: Context,
    driverName: String,
    routeName: String = ""
): android.graphics.drawable.BitmapDrawable {
    val width  = 220
    val height = 160
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val cx     = width / 2f

    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#CCB71C1C")
    }
    canvas.drawRoundRect(android.graphics.RectF(cx - 100f, 0f, cx + 100f, 62f), 10f, 10f, bgPaint)

    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color     = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        typeface  = android.graphics.Typeface.DEFAULT_BOLD
        textSize  = 22f
    }
    canvas.drawText(if (driverName.length > 13) driverName.take(13) + "…" else driverName, cx, 24f, textPaint)

    if (routeName.isNotBlank()) {
        textPaint.textSize = 19f
        textPaint.color    = android.graphics.Color.parseColor("#FFCDD2")
        canvas.drawText(if (routeName.length > 13) routeName.take(13) + "…" else routeName, cx, 52f, textPaint)
    }

    val busPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 80f
        textAlign = android.graphics.Paint.Align.CENTER
    }
    canvas.drawText("🚌", cx, 148f, busPaint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

// ─── أيقونة الراكب ───────────────────────────────────────────────
fun createPassengerIcon(context: Context): android.graphics.drawable.BitmapDrawable {
    val size   = 80
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint  = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 60f
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText("🧍", size / 2f, y, paint)
    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}

fun drawRouteOnMap(
    mapView: MapView,
    points: List<PointData>,
    polyline: Polyline,
    startTitle: String,
    endTitle: String
) {
    if (points.isEmpty()) return
    val geoPoints = points.map { GeoPoint(it.lat, it.lng) }
    polyline.setPoints(geoPoints)
    Marker(mapView).also { m ->
        m.position = geoPoints.first()
        m.title    = startTitle
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(m)
    }
    Marker(mapView).also { m ->
        m.position = geoPoints.last()
        m.title    = endTitle
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(m)
    }
    mapView.invalidate()
}

@SuppressLint("MissingPermission")
@Composable
fun LiveMapScreen(lang: String, routeName: String, onEndWork: (WorkReport) -> Unit) {
    val context     = LocalContext.current
    val mapView     = remember { MapView(context) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentLocation   by remember { mutableStateOf<GeoPoint?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var routeLoadStatus   by remember { mutableStateOf("") }
    var otherBusCount     by remember { mutableStateOf(0) }
    var passengerCount    by remember { mutableStateOf(0) }

    val otherBusMarkers  = remember { mutableMapOf<String, Marker>() }
    val passengerMarkers = remember { mutableMapOf<String, Marker>() }
    val myDriverName     = remember { LocationForegroundService.currentDriverName }

    val workStartTime    = remember { System.currentTimeMillis() }
    val workStartTimeStr = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    val dateStr          = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }

    val goingPolyline = remember {
        Polyline().apply {
            outlinePaint.color = android.graphics.Color.parseColor("#1E88E5")
            outlinePaint.strokeWidth = 10f
            outlinePaint.alpha = 200
        }
    }

    val returningPolyline = remember {
        Polyline().apply {
            outlinePaint.color = android.graphics.Color.parseColor("#E53935")
            outlinePaint.strokeWidth = 10f
            outlinePaint.alpha = 200
        }
    }

    val driverMarker = remember { Marker(mapView) }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc   = result.lastLocation ?: return
                val point = GeoPoint(loc.latitude, loc.longitude)
                currentLocation       = point
                driverMarker.position = point
                mapView.controller.setCenter(point)
                mapView.invalidate()
            }
        }
    }

    // ─── جلب المسار من Supabase إذا لم يوجد محلياً ───────────────
    LaunchedEffect(routeName) {
        val localGoing     = loadRouteLocally(context, routeName, "going")
        val localReturning = loadRouteLocally(context, routeName, "returning")

        if (localGoing.isEmpty() || localReturning.isEmpty()) {
            try {
                val results = supabase.postgrest["routes"]
                    .select { filter { eq("route_name", routeName) } }
                    .decodeList<RouteData>()

                val going     = results.firstOrNull { it.direction == "going" }
                val returning = results.firstOrNull { it.direction == "returning" }

                going?.let {
                    if (it.points.isNotEmpty()) {
                        drawRouteOnMap(mapView, it.points, goingPolyline,
                            when(lang) { "tr" -> "Gidiş Başlangıç"; "en" -> "Going Start"; else -> "بداية الذهاب" },
                            when(lang) { "tr" -> "Gidiş Sonu"; "en" -> "Going End"; else -> "نهاية الذهاب" }
                        )
                        saveRouteLocally(context, routeName, "going", it.points)
                    }
                }
                returning?.let {
                    if (it.points.isNotEmpty()) {
                        drawRouteOnMap(mapView, it.points, returningPolyline,
                            when(lang) { "tr" -> "Dönüş Başlangıç"; "en" -> "Return Start"; else -> "بداية العودة" },
                            when(lang) { "tr" -> "Dönüş Sonu"; "en" -> "Return End"; else -> "نهاية العودة" }
                        )
                        saveRouteLocally(context, routeName, "returning", it.points)
                    }
                }
                routeLoadStatus = "G:${going?.points?.size ?: 0} R:${returning?.points?.size ?: 0} ☁️"
            } catch (e: Exception) {
                Log.e("DOLMUS", "Supabase route fetch error: ${e.message}")
                routeLoadStatus = "❌"
            }
        }
    }

    // ─── جلب الحافلات الأخرى كل 5 ثوانٍ ────────────────────────
    LaunchedEffect(routeName) {
        while (true) {
            try {
                val currentMyName = LocationForegroundService.currentDriverName
                val result = supabase.postgrest["live_locations"]
                    .select { filter { eq("route_name", routeName) } }
                    .decodeList<LiveLocation>()

                val currentNames = result.map { it.driverName }.toSet()
                otherBusMarkers.keys.filter { it !in currentNames }.forEach { name ->
                    mapView.overlays.remove(otherBusMarkers[name])
                    otherBusMarkers.remove(name)
                }

                result.forEach { loc ->
                    if (currentMyName.isBlank() || loc.driverName == currentMyName) return@forEach
                    val point    = GeoPoint(loc.lat, loc.lng)
                    val existing = otherBusMarkers[loc.driverName]
                    if (existing != null) {
                        existing.position = point
                        existing.icon     = createOtherBusIcon(context, loc.driverName, loc.routeName)
                    } else {
                        val m = Marker(mapView).apply {
                            position = point
                            icon     = createOtherBusIcon(context, loc.driverName, loc.routeName)
                            title    = "${loc.driverName} — ${loc.routeName}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(m)
                        otherBusMarkers[loc.driverName] = m
                    }
                }

                otherBusCount = result.count { it.driverName != currentMyName && currentMyName.isNotBlank() }
                mapView.invalidate()

            } catch (e: Exception) {
                Log.e("DOLMUS", "Other buses fetch error: ${e.message}")
            }
            delay(5000L)
        }
    }

    // ─── جلب الركاب كل 10 ثوانٍ ─────────────────────────────────
    LaunchedEffect(routeName) {
        while (true) {
            try {
                val result = supabase.postgrest["passenger_locations"]
                    .select { filter { eq("route_name", routeName) } }
                    .decodeList<PassengerLocation>()

                val currentIds = result.map { it.id }.toSet()
                passengerMarkers.keys.filter { it !in currentIds }.forEach { id ->
                    mapView.overlays.remove(passengerMarkers[id])
                    passengerMarkers.remove(id)
                }

                result.forEach { p ->
                    val point    = GeoPoint(p.lat, p.lng)
                    val existing = passengerMarkers[p.id]
                    if (existing != null) {
                        existing.position = point
                    } else {
                        val m = Marker(mapView).apply {
                            position = point
                            icon     = createPassengerIcon(context)
                            title    = when(lang) { "tr" -> "Yolcu"; "en" -> "Passenger"; else -> "راكب" }
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(m)
                        passengerMarkers[p.id] = m
                    }
                }

                passengerCount = result.size
                mapView.invalidate()

            } catch (e: Exception) {
                Log.e("DOLMUS", "Passengers fetch error: ${e.message}")
            }
            delay(10000L)
        }
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.0)
        mapView.overlays.clear()

        val goingPoints     = loadRouteLocally(context, routeName, "going")
        val returningPoints = loadRouteLocally(context, routeName, "returning")

        if (goingPoints.isNotEmpty()) {
            drawRouteOnMap(mapView, goingPoints, goingPolyline,
                when(lang) { "tr" -> "Gidiş Başlangıç"; "en" -> "Going Start"; else -> "بداية الذهاب" },
                when(lang) { "tr" -> "Gidiş Sonu"; "en" -> "Going End"; else -> "نهاية الذهاب" }
            )
        }

        if (returningPoints.isNotEmpty()) {
            drawRouteOnMap(mapView, returningPoints, returningPolyline,
                when(lang) { "tr" -> "Dönüş Başlangıç"; "en" -> "Return Start"; else -> "بداية العودة" },
                when(lang) { "tr" -> "Dönüş Sonu"; "en" -> "Return End"; else -> "نهاية العودة" }
            )
        }

        if (goingPoints.isNotEmpty() || returningPoints.isNotEmpty()) {
            routeLoadStatus = "G:${goingPoints.size} R:${returningPoints.size}"
        }

        mapView.overlays.add(goingPolyline)
        mapView.overlays.add(returningPolyline)

        driverMarker.icon  = createBusIcon(context, myDriverName, routeName)
        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        driverMarker.title = myDriverName.ifBlank {
            when(lang) { "tr" -> "Siz"; "en" -> "You"; else -> "أنت" }
        }
        mapView.overlays.add(driverMarker)
        mapView.invalidate()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(5f).build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

        onDispose { fusedClient.removeLocationUpdates(locationCallback) }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    when(lang) { "tr" -> "Günü Bitir?"; "en" -> "End Work Day?"; else -> "إنهاء يوم العمل؟" },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(when(lang) { "tr" -> "Emin misiniz?"; "en" -> "Are you sure?"; else -> "هل أنت متأكد؟" })
            },
            confirmButton = {
                Button(
                    onClick = {
                        val now          = System.currentTimeMillis()
                        val nowStr       = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val totalMinutes = ((now - workStartTime) / 60000).toInt()
                        val report       = WorkReport(dateStr, workStartTimeStr, nowStr, totalMinutes, emptyList(), routeName)
                        showConfirmDialog = false
                        onEndWork(report)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text(when(lang) { "tr" -> "Evet"; "en" -> "Yes"; else -> "نعم" }, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text(when(lang) { "tr" -> "İptal"; "en" -> "Cancel"; else -> "إلغاء" })
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xCC000000))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(routeName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    when(lang) { "tr" -> "● Canlı Yayın"; "en" -> "● Live"; else -> "● بث حي" },
                    fontSize = 12.sp, color = Color(0xFF69F0AE)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔵 ${when(lang) { "tr" -> "Gidiş"; "en" -> "Going"; else -> "ذهاب" }}", fontSize = 10.sp, color = Color(0xFF90CAF9))
                    Text("🔴 ${when(lang) { "tr" -> "Dönüş"; "en" -> "Return"; else -> "عودة" }}", fontSize = 10.sp, color = Color(0xFFEF9A9A))
                    Text("🚌 $myDriverName", fontSize = 10.sp, color = Color(0xFF69F0AE))
                    if (otherBusCount > 0) {
                        Text("🚌 $otherBusCount ${when(lang) { "tr" -> "diğer"; "en" -> "others"; else -> "أخرى" }}", fontSize = 10.sp, color = Color(0xFFEF9A9A))
                    }
                    if (passengerCount > 0) {
                        Text("🧍 $passengerCount", fontSize = 10.sp, color = Color(0xFFFFD54F))
                    }
                    if (routeLoadStatus.isNotEmpty()) {
                        Text("[$routeLoadStatus]", fontSize = 9.sp, color = Color(0xFFFFD54F))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .background(Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    currentLocation?.let {
                        "${String.format("%.4f", it.latitude)}, ${String.format("%.4f", it.longitude)}"
                    } ?: when(lang) { "tr" -> "Konum alınıyor..."; "en" -> "Getting location..."; else -> "جاري تحديد الموقع..." },
                    fontSize = 10.sp, color = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(
                onClick = { showConfirmDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xDDFFFFFF)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    when(lang) { "tr" -> "⏹ Günü Bitir"; "en" -> "⏹ End Day"; else -> "⏹ إنهاء يوم العمل" },
                    fontSize = 13.sp, color = Color(0xFFE53935)
                )
            }
        }
    }
}