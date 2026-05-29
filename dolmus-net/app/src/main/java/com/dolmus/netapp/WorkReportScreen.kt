package com.dolmus.netapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkReportScreen(lang: String, routeName: String, onClose: () -> Unit) {
    val date = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val time = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✅", fontSize = 52.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            when(lang) { "tr" -> "Günlük Rapor"; "en" -> "Work Report"; else -> "تقرير يوم العمل" },
            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ReportRow(
                    when(lang) { "tr" -> "Hat"; "en" -> "Route"; else -> "الخط" },
                    routeName
                )
                HorizontalDivider()
                ReportRow(
                    when(lang) { "tr" -> "Tarih"; "en" -> "Date"; else -> "التاريخ" },
                    date
                )
                HorizontalDivider()
                ReportRow(
                    when(lang) { "tr" -> "Bitiş Saati"; "en" -> "End Time"; else -> "وقت الانتهاء" },
                    time
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
        ) {
            Text(
                when(lang) { "tr" -> "Kapat"; "en" -> "Close"; else -> "إغلاق" },
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}