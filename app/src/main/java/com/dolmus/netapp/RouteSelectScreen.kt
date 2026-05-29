package com.dolmus.netapp

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RouteSelectScreen(
    lang: String,
    routeName: String,
    cityRoutes: List<String>,
    onSelectExisting: (String) -> Unit,
    onCalibrateNew: () -> Unit,
    onBack: () -> Unit
) {
    val title         = when(lang) { "tr" -> "Hat Seç veya Yeni Ekle"; "en" -> "Select or Add Route"; else -> "اختر خطاً أو أضف جديداً" }
    val existingLabel = when(lang) { "tr" -> "Veritabanındaki Mevcut Hatlar"; "en" -> "Existing Routes (Database)"; else -> "الخطوط الموجودة في قاعدة البيانات" }
    val calibrateBtn  = when(lang) { "tr" -> "Yeni Hat Olarak Kaydet (Kalibrasyon)"; "en" -> "Calibrate as New Route"; else -> "معايرة كخط جديد" }
    val backBtn       = when(lang) { "tr" -> "Geri"; "en" -> "Back"; else -> "رجوع" }
    val selectBtn     = when(lang) { "tr" -> "Seç ve Çalış"; "en" -> "Select & Work"; else -> "اختر وابدأ" }
    val noMatchLabel  = when(lang) {
        "tr" -> "\"$routeName\" veritabanında bulunamadı"
        "en" -> "\"$routeName\" not found in database"
        else -> "\"$routeName\" غير موجود في قاعدة البيانات"
    }
    val newRouteHint  = when(lang) {
        "tr" -> "\"$routeName\" yeni hat olarak eklemek için kalibrasyon başlat"
        "en" -> "Start calibration to add \"$routeName\" as a new route"
        else -> "ابدأ المعايرة لإضافة \"$routeName\" كخط جديد"
    }

    // فلترة الخطوط التي تحتوي على النص المكتوب
    val filteredRoutes = remember(routeName, cityRoutes) {
        if (routeName.isBlank()) cityRoutes
        else cityRoutes.filter { it.contains(routeName, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ─── العنوان ──────────────────────────────────────────────
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E88E5)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ─── النص المُدخل ─────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔍", fontSize = 18.sp)
                Text(
                    routeName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E88E5)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ─── الخطوط الموجودة في قاعدة البيانات ───────────────
            if (filteredRoutes.isNotEmpty()) {
                item {
                    Text(
                        existingLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(filteredRoutes) { route ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectExisting(route) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🚌", fontSize = 20.sp)
                                Column {
                                    Text(
                                        route,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        when(lang) {
                                            "tr" -> "Veritabanında mevcut"
                                            "en" -> "Available in database"
                                            else -> "موجود في قاعدة البيانات"
                                        },
                                        fontSize = 11.sp,
                                        color = Color(0xFF66BB6A)
                                    )
                                }
                            }
                            Button(
                                onClick = { onSelectExisting(route) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(selectBtn, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }

            } else {
                // ─── لم يُوجد الخط في قاعدة البيانات ─────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 20.sp)
                            Text(
                                noMatchLabel,
                                fontSize = 14.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ─── فاصل + زر المعايرة ───────────────────────────────
            item {
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    newRouteHint,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onCalibrateNew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1565C0)
                    )
                ) {
                    Text(
                        "📡  $calibrateBtn",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // ─── زر الرجوع ────────────────────────────────────────────
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(backBtn, fontSize = 16.sp, color = Color(0xFF1E88E5))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}