package com.dolmus.netapp

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}

@Composable
fun QrScreen(lang: String, onBack: () -> Unit) {
    val url = "https://mohamadyuonsos-beep.github.io/Dulmo-NET/"
    val qrBitmap = remember { generateQrBitmap(url) }

    Box(
        modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF5F7FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                when(lang) { "tr" -> "QR Kodu"; "en" -> "QR Code"; else -> "رمز QR" },
                fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF1E88E5)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(220.dp)
                    )
                    Text(
                        when(lang) { "tr" -> "Tarayıcı veya sürücü uygulaması"; "en" -> "Passenger or driver app"; else -> "واجهة الراكب أو تطبيق السائق" },
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    when(lang) { "tr" -> "Geri"; "en" -> "Back"; else -> "رجوع" },
                    color = androidx.compose.ui.graphics.Color(0xFF1E88E5)
                )
            }
        }
    }
}