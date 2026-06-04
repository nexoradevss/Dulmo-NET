package com.dolmus.netapp

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class RouteNameOnly(val route_name: String)

fun loadRoutes(context: Context): List<String> {
    val prefs = context.getSharedPreferences("dolmus_prefs", Context.MODE_PRIVATE)
    val raw   = prefs.getString("saved_routes_list", "") ?: ""
    return if (raw.isBlank()) emptyList() else raw.split("||").filter { it.isNotBlank() }
}

fun saveRoutes(context: Context, routes: List<String>) {
    val prefs = context.getSharedPreferences("dolmus_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("saved_routes_list", routes.joinToString("||")).apply()
}

fun getDriverName(context: Context): String {
    return context.getSharedPreferences("dolmus_prefs", Context.MODE_PRIVATE)
        .getString("driver_name", "") ?: ""
}

fun getDriverCity(context: Context): String {
    return context.getSharedPreferences("dolmus_prefs", Context.MODE_PRIVATE)
        .getString("driver_city", "") ?: ""
}

@Composable
fun HomeScreen(lang: String = "tr", onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var screen          by remember { mutableStateOf("input") }
    var routeName       by remember { mutableStateOf("") }
    var savedRoutes     by remember { mutableStateOf(loadRoutes(context)) }
    var selectedFromSuggestion by remember { mutableStateOf(false) }

    var cityRoutes          by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingCityRoutes by remember { mutableStateOf(false) }

    val driverCity = remember { getDriverCity(context) }

    LaunchedEffect(Unit) {
        if (CalibrationSession.hasPendingGoing(context)) {
            routeName = CalibrationSession.getRouteName(context)
            screen = "calibration"
        }

        isLoadingCityRoutes = true
        try {
            val result = supabase.postgrest["routes"]
                .select()
                .decodeList<RouteNameOnly>()
            cityRoutes = result.map { it.route_name }.distinct()
        } catch (e: Exception) {
            android.util.Log.e("DOLMUS", "Routes error: ${e.message}")
        } finally {
            isLoadingCityRoutes = false
        }
    }

    val routeLabel       = when(lang) { "tr" -> "Hat adı veya numarası"; "en" -> "Route name or number"; else -> "اسم أو رقم الخط" }
    val startCalibBtn    = when(lang) { "tr" -> "Yeni Hat Kaydet"; "en" -> "Calibrate New Route"; else -> "معايرة خط جديد" }
    val startWorkBtn     = when(lang) { "tr" -> "Çalış"; "en" -> "Start Work"; else -> "ابدأ العمل" }
    val deleteBtn        = when(lang) { "tr" -> "Sil"; "en" -> "Delete"; else -> "حذف" }
    val logoutBtn        = when(lang) { "tr" -> "Çıkış Yap"; "en" -> "Logout"; else -> "تسجيل الخروج" }
    val savedRoutesLabel = when(lang) { "tr" -> "Kayıtlı Hatlarım"; "en" -> "My Saved Routes"; else -> "خطوطي المحفوظة" }
    val noRoutesLabel    = when(lang) { "tr" -> "Henüz hat kaydedilmedi"; "en" -> "No routes saved yet"; else -> "لا يوجد خطوط محفوظة بعد" }
    val selectRouteLabel = when(lang) { "tr" -> "Hat seçin veya yeni girin"; "en" -> "Select or enter new route"; else -> "اختر خطاً أو أدخل جديداً" }

    val suggestions = remember(routeName, cityRoutes, savedRoutes) {
        if (routeName.isBlank()) emptyList()
        else cityRoutes
            .filter { it.contains(routeName, ignoreCase = true) }
            .filter { !savedRoutes.contains(it) }
            .take(5)
    }

    when (screen) {

        "input" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Dolmus NET", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))

                if (driverCity.isNotBlank()) {
                    Text(
                        "📍 $driverCity",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    if (savedRoutes.isNotEmpty()) {
                        item {
                            Text(
                                savedRoutesLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E88E5)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        items(savedRoutes) { route ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        route,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E88E5),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { routeName = route; screen = "live" },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) { Text(startWorkBtn, fontSize = 13.sp, color = Color.White) }

                                        OutlinedButton(
                                            onClick = {
                                                val updated = savedRoutes.filter { it != route }
                                                savedRoutes = updated
                                                saveRoutes(context, updated)
                                                MainScope().launch {
                                                    try {
                                                        supabase.postgrest["routes"].delete {
                                                            filter { eq("route_name", route) }
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("DOLMUS", "Delete error: ${e.message}")
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) { Text(deleteBtn, fontSize = 13.sp, color = Color(0xFFE53935)) }
                                    }
                                }
                            }
                        }
                    }

                    if (savedRoutes.isEmpty() && !isLoadingCityRoutes) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(noRoutesLabel, fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }

                    if (isLoadingCityRoutes) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF1E88E5))
                            }
                        }
                    }

                    if (suggestions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column {
                                    suggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    routeName = suggestion
                                                    selectedFromSuggestion = true
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text("🚌", fontSize = 16.sp)
                                                Text(
                                                    suggestion,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF1E88E5)
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    routeName = suggestion
                                                    if (!savedRoutes.contains(suggestion)) {
                                                        val updated = listOf(suggestion) + savedRoutes
                                                        savedRoutes = updated
                                                        saveRoutes(context, updated)
                                                    }
                                                    screen = "live"
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1E88E5)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    "▶ $startWorkBtn",
                                                    fontSize = 12.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        if (suggestion != suggestions.last()) {
                                            HorizontalDivider(color = Color(0xFFE0E0E0))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(selectRouteLabel, fontSize = 12.sp, color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = routeName,
                    onValueChange = {
                        routeName = it
                        selectedFromSuggestion = false
                    },
                    label = { Text(routeLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor    = Color.Black,
                        unfocusedTextColor  = Color.Black,
                        focusedLabelColor   = Color(0xFF1E88E5),
                        unfocusedLabelColor = Color.Black,
                        cursorColor         = Color(0xFF1E88E5)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { if (routeName.isNotBlank()) screen = "route_select" },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor   = Color.White
                    ),
                    enabled = routeName.isNotBlank()
                ) { Text(startCalibBtn, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

                Spacer(modifier = Modifier.height(10.dp))

                // ─── زر QR ────────────────────────────────────────
                OutlinedButton(
                    onClick = { screen = "qr" },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        when(lang) { "tr" -> "📱 QR Kodu"; "en" -> "📱 QR Code"; else -> "📱 رمز QR" },
                        fontSize = 16.sp, color = Color(0xFF1E88E5)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(logoutBtn, fontSize = 16.sp, color = Color(0xFF1E88E5)) }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        "route_select" -> {
            RouteSelectScreen(
                lang       = lang,
                routeName  = routeName,
                cityRoutes = cityRoutes,
                onSelectExisting = { selected ->
                    routeName = selected
                    val updated = if (savedRoutes.contains(selected)) savedRoutes
                                  else listOf(selected) + savedRoutes
                    savedRoutes = updated
                    saveRoutes(context, updated)
                    screen = "live"
                },
                onCalibrateNew = { screen = "calibration" },
                onBack         = { screen = "input" }
            )
        }

        "calibration" -> {
            CalibrationScreen(
                lang      = lang,
                routeName = routeName,
                onDone    = { screen = "input" },
                onStartWork = {
                    val updated = if (savedRoutes.contains(routeName)) savedRoutes
                    else listOf(routeName) + savedRoutes
                    savedRoutes = updated
                    saveRoutes(context, updated)
                    screen = "live"
                }
            )
        }

        "live" -> {
            DisposableEffect(routeName) {
                LocationForegroundService.currentDriverName = getDriverName(context)
                LocationForegroundService.currentRouteName  = routeName
                val intent = Intent(context, LocationForegroundService::class.java).apply {
                    action = LocationForegroundService.ACTION_START_WORK
                }
                context.startForegroundService(intent)
                onDispose { }
            }
            LiveMapScreen(
                lang      = lang,
                routeName = routeName,
                onEndWork = {
                    val stopIntent = Intent(context, LocationForegroundService::class.java).apply {
                        action = LocationForegroundService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                    screen = "input"
                }
            )
        }

        // ─── شاشة QR ──────────────────────────────────────────────
        "qr" -> {
            QrScreen(lang = lang, onBack = { screen = "input" })
        }
    }
}