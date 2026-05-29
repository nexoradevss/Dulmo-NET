package com.dolmus.netapp

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
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
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class RouteData(
    val route_name: String,
    val direction: String,
    val points: List<PointData>
)

@Serializable
data class PointData(
    val lat: Double,
    val lng: Double
)

fun calculateRouteDistance(points: List<GeoPoint>): Double {
    var total = 0.0
    for (i in 1 until points.size) {
        val results = FloatArray(1)
        Location.distanceBetween(
            points[i-1].latitude, points[i-1].longitude,
            points[i].latitude, points[i].longitude,
            results
        )
        total += results[0]
    }
    return total / 1000.0
}

@SuppressLint("MissingPermission")
@Composable
fun CalibrationScreen(lang: String, routeName: String, onDone: () -> Unit, onStartWork: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf("idle") }
    var goingPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var returningPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var lastSavedLocation by remember { mutableStateOf<Location?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var routeDistanceKm by remember { mutableStateOf(0.0) }

    val mapView = remember { MapView(context) }
    val goingPolyline = remember {
        Polyline().apply {
            outlinePaint.color = android.graphics.Color.BLUE
            outlinePaint.strokeWidth = 8f
        }
    }
    val returningPolyline = remember {
        Polyline().apply {
            outlinePaint.color = android.graphics.Color.RED
            outlinePaint.strokeWidth = 8f
        }
    }
    val marker = remember { Marker(mapView) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val newPoint = GeoPoint(loc.latitude, loc.longitude)
                marker.position = newPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.controller.setCenter(newPoint)

                val shouldSave = lastSavedLocation?.let { it.distanceTo(loc) >= 50f } ?: true
                if (shouldSave && (phase == "going" || phase == "returning")) {
                    lastSavedLocation = loc
                    if (phase == "going") {
                        goingPoints = goingPoints + newPoint
                        goingPolyline.setPoints(goingPoints)
                    } else {
                        returningPoints = returningPoints + newPoint
                        returningPolyline.setPoints(returningPoints)
                    }
                    mapView.invalidate()
                }
            }
        }
    }

    fun startTracking() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(10f).build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.0)
        mapView.overlays.add(goingPolyline)
        mapView.overlays.add(returningPolyline)
        mapView.overlays.add(marker)
        startTracking()
        onDispose { stopTracking() }
    }

    // شاشة النتيجة بعد الرفع
    if (phase == "done") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("✅", fontSize = 56.sp)
                Text(
                    when(lang) { "tr" -> "Hat Kaydedildi!"; "en" -> "Route Saved!"; else -> "تم حفظ الخط!" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E88E5)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when(lang) { "tr" -> "Hat Adı"; "en" -> "Route Name"; else -> "اسم الخط" },
                                color = Color.Gray, fontSize = 14.sp
                            )
                            Text(routeName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when(lang) { "tr" -> "Toplam Mesafe"; "en" -> "Total Distance"; else -> "المسافة الكلية" },
                                color = Color.Gray, fontSize = 14.sp
                            )
                            Text(
                                "${(routeDistanceKm * 10).roundToInt() / 10.0} كم",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when(lang) { "tr" -> "Gidiş Noktaları"; "en" -> "Going Points"; else -> "نقاط الذهاب" },
                                color = Color.Gray, fontSize = 14.sp
                            )
                            Text("${goingPoints.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E88E5))
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when(lang) { "tr" -> "Dönüş Noktaları"; "en" -> "Return Points"; else -> "نقاط العودة" },
                                color = Color.Gray, fontSize = 14.sp
                            )
                            Text("${returningPoints.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE53935))
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when(lang) { "tr" -> "Tahmini Süre"; "en" -> "Est. Duration"; else -> "المدة التقديرية" },
                                color = Color.Gray, fontSize = 14.sp
                            )
                            Text(
                                "${(routeDistanceKm / 30 * 60).roundToInt()} ${when(lang) { "tr" -> "dk"; "en" -> "min"; else -> "دقيقة" }}",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onStartWork() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Text(
                        when(lang) { "tr" -> "▶  Çalışmaya Başla"; "en" -> "▶  Start Work"; else -> "▶  بدء العمل" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = { onDone() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        when(lang) { "tr" -> "Çıkış"; "en" -> "Exit"; else -> "خروج" },
                        fontSize = 16.sp,
                        color = Color(0xFF1E88E5)
                    )
                }
            }
        }
        return
    }

    // شاشة المعايرة العادية
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
            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, fontSize = 12.sp, color = Color(0xFFBBDEFB))
            }
            when (phase) {
                "going" -> Text("● ${goingPoints.size} ${when(lang) { "tr" -> "nokta"; "en" -> "pts"; else -> "نقطة" }}", color = Color(0xFF90CAF9), fontSize = 12.sp)
                "returning" -> Text("● ${returningPoints.size} ${when(lang) { "tr" -> "nokta"; "en" -> "pts"; else -> "نقطة" }}", color = Color(0xFFEF9A9A), fontSize = 12.sp)
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
                "idle" -> Button(
                    onClick = {
                        phase = "going"
                        goingPoints = emptyList()
                        statusMessage = when(lang) { "tr" -> "Gidiş kaydediliyor..."; "en" -> "Recording going..."; else -> "جاري تسجيل خط الذهاب..." }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) { Text(when(lang) { "tr" -> "Gidişi Başlat"; "en" -> "Start Going"; else -> "بدء خط الذهاب" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                "going" -> Button(
                    onClick = {
                        phase = "going_done"
                        statusMessage = when(lang) { "tr" -> "Gidiş tamamlandı."; "en" -> "Going done."; else -> "اكتمل خط الذهاب. ابدأ خط العودة." }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text(when(lang) { "tr" -> "Gidiş Bitti"; "en" -> "End Going"; else -> "انتهى خط الذهاب" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                "going_done" -> Button(
                    onClick = {
                        phase = "returning"
                        returningPoints = emptyList()
                        statusMessage = when(lang) { "tr" -> "Dönüş kaydediliyor..."; "en" -> "Recording return..."; else -> "جاري تسجيل خط العودة..." }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) { Text(when(lang) { "tr" -> "Dönüşü Başlat"; "en" -> "Start Return"; else -> "بدء خط العودة" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                "returning" -> Button(
                    onClick = {
                        phase = "returning_done"
                        statusMessage = when(lang) { "tr" -> "Hazır. Yükleyin."; "en" -> "Ready. Upload."; else -> "جاهز للرفع." }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text(when(lang) { "tr" -> "Dönüş Bitti"; "en" -> "End Return"; else -> "انتهى خط العودة" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                "returning_done" -> Button(
                    onClick = {
                        phase = "uploading"
                        isUploading = true
                        scope.launch {
                            try {
                                val goingData = RouteData(
                                    route_name = routeName,
                                    direction = "going",
                                    points = goingPoints.map { PointData(it.latitude, it.longitude) }
                                )
                                val returningData = RouteData(
                                    route_name = routeName,
                                    direction = "returning",
                                    points = returningPoints.map { PointData(it.latitude, it.longitude) }
                                )
                                supabase.postgrest["routes"].insert(listOf(goingData, returningData))
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
                    if (isUploading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                    else Text(when(lang) { "tr" -> "Kaydet ve Gönder"; "en" -> "Save & Upload"; else -> "حفظ وإرسال" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}