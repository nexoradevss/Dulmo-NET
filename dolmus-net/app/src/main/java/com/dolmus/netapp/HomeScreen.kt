package com.dolmus.netapp

import android.content.Context
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

fun loadRoutes(context: Context): List<String> {
    val prefs = context.getSharedPreferences("dolmus_prefs", Context.MODE_PRIVATE)
    val raw = prefs.getString("saved_routes_list", "") ?: ""
    return if (raw.isBlank()) emptyList() else raw.split("||").filter { it.isNotBlank() }
}

fun saveRoutes(context: Context, routes: List<String>) {
    val prefs = context.getSharedPreferences("dolmus_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("saved_routes_list", routes.joinToString("||")).apply()
}

@Composable
fun HomeScreen(lang: String = "tr", onLogout: () -> Unit = {}) {
    val context = LocalContext.current

    var screen by remember { mutableStateOf("input") }
    var routeName by remember { mutableStateOf("") }
    var savedRoutes by remember { mutableStateOf(loadRoutes(context)) }
    var showReport by remember { mutableStateOf(false) }

    val routeLabel = when(lang) { "tr" -> "Hat adı veya numarası"; "en" -> "Route name or number"; else -> "اسم أو رقم الخط" }
    val startCalibBtn = when(lang) { "tr" -> "+ Yeni Hat"; "en" -> "+ New Route"; else -> "+ معايرة خط جديد" }
    val startWorkBtn = when(lang) { "tr" -> "Çalışmaya Başla"; "en" -> "Start Work"; else -> "بدء العمل" }
    val deleteBtn = when(lang) { "tr" -> "Sil"; "en" -> "Del"; else -> "حذف" }
    val logoutBtn = when(lang) { "tr" -> "Çıkış"; "en" -> "Logout"; else -> "خروج" }
    val savedRoutesLabel = when(lang) { "tr" -> "Kayıtlı Hatlar"; "en" -> "Saved Routes"; else -> "الخطوط المحفوظة" }
    val noRoutesLabel = when(lang) { "tr" -> "Henüz hat kaydedilmedi"; "en" -> "No routes saved yet"; else -> "لا يوجد خطوط محفوظة بعد" }

    when (screen) {
        "input" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "Dolmus NET",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E88E5)
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (savedRoutes.isNotEmpty()) {
                    Text(
                        savedRoutesLabel,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedRoutes) { route ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        route,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E88E5)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // زر بدء العمل — رئيسي وكبير
                                        Button(
                                            onClick = {
                                                routeName = route
                                                screen = "live"
                                            },
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                                        ) {
                                            Text(startWorkBtn, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // زر الحذف — صغير وثانوي
                                        TextButton(
                                            onClick = {
                                                val updated = savedRoutes.filter { it != route }
                                                savedRoutes = updated
                                                saveRoutes(context, updated)
                                            }
                                        ) {
                                            Text(deleteBtn, fontSize = 12.sp, color = Color(0xFFE53935))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(noRoutesLabel, fontSize = 14.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text(routeLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // زر المعايرة — صغير وثانوي
                OutlinedButton(
                    onClick = { if (routeName.isNotBlank()) screen = "calibration" },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = routeName.isNotBlank()
                ) {
                    Text(startCalibBtn, fontSize = 14.sp, color = Color(0xFF43A047))
                }
                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onLogout) {
                    Text(logoutBtn, fontSize = 13.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        "calibration" -> {
            CalibrationScreen(
                lang = lang,
                routeName = routeName,
                onDone = { screen = "input" },
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
            LiveMapScreen(
                lang = lang,
                routeName = routeName,
                onEndWork = { screen = "report" }
            )
        }

        "report" -> {
            WorkReportScreen(
                lang = lang,
                routeName = routeName,
                onClose = { screen = "input" }
            )
        }
    }
}