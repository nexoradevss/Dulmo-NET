package com.dolmus.netapp

import android.annotation.SuppressLint
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
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@SuppressLint("MissingPermission")
@Composable
fun LiveMapScreen(lang: String, routeName: String, onEndWork: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapView = remember { MapView(context) }
    val marker = remember { Marker(mapView) }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }

    val goingPolyline = remember {
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

    // ─── جلب المسار من Supabase ────────────────────────────────
    LaunchedEffect(routeName) {
        try {
            val goingResult = supabase.postgrest["routes"]
                .select {
                    filter {
                        eq("route_name", routeName)
                        eq("direction", "going")
                    }
                }
                .decodeSingle<RouteData>()

            val returningResult = supabase.postgrest["routes"]
                .select {
                    filter {
                        eq("route_name", routeName)
                        eq("direction", "returning")
                    }
                }
                .decodeSingle<RouteData>()

            val goingPoints = goingResult.points.map { GeoPoint(it.lat, it.lng) }
            val returningPoints = returningResult.points.map { GeoPoint(it.lat, it.lng) }

            goingPolyline.setPoints(goingPoints)
            returningPolyline.setPoints(returningPoints)

            if (!mapView.overlays.contains(goingPolyline)) mapView.overlays.add(0, goingPolyline)
            if (!mapView.overlays.contains(returningPolyline)) mapView.overlays.add(1, returningPolyline)
            mapView.invalidate()

        } catch (_: Exception) { }
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val point = GeoPoint(loc.latitude, loc.longitude)
                currentLocation = point
                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = when(lang) { "tr" -> "Buradasınız"; "en" -> "You are here"; else -> "موقعك الحالي" }
                mapView.controller.setCenter(point)
                mapView.invalidate()
            }
        }
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.0)
        mapView.overlays.add(marker)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(5f).build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

        onDispose { fusedClient.removeLocationUpdates(locationCallback) }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // شريط علوي
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

        // زر إنهاء العمل
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(
                onClick = { onEndWork() },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xDDFFFFFF)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    when(lang) { "tr" -> "⏹ Günü Bitir"; "en" -> "⏹ End Day"; else -> "⏹ إنهاء يوم العمل" },
                    fontSize = 14.sp,
                    color = Color(0xFFE53935)
                )
            }
        }
    }
}