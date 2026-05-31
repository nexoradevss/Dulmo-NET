package com.dolmus.netapp

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val context = LocalContext.current

    val passengerUrl = "https://mohamadyuonsos-beep.github.io/Dulmo-NET/"
    val driverApkUrl = "https://mohamadyuonsos-beep.github.io/Dulmo-NET/driver.apk"

    val qrBitmap = remember { generateQrBitmap(passengerUrl) }

    val title = when(lang) { "tr" -> "QR Kodu"; "en" -> "QR Code"; else -> "رمز QR" }
    val passengerLabel = when(lang) { "tr" -> "Yolcu Sayfası"; "en" -> "Passenger Page"; else -> "صفحة الراكب" }
    val driverLabel = when(lang) { "tr" -> "Sürücü Uygulaması"; "en" -> "Driver App"; else -> "تطبيق السائق" }
    val downloadLabel = when(lang) { "tr" -> "İndir"; "en" -> "Download"; else -> "تحميل" }
    val openLabel = when(lang) { "tr" -> "Aç"; "en" -> "Open"; else -> "فتح" }
    val backLabel = when(lang) { "tr" -> "Geri"; "en" -> "Back"; else -> "رجوع" }
    val scanLabel = when(lang) { "tr" -> "QR ile yolcu sayfasını aç"; "en" -> "Scan to open passenger page"; else -> "امسح للوصول لصفحة الراكب" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFF5F7FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {

            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(0xFF1E88E5)
            )

            // QR Card
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                    Text(
                        scanLabel,
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // زر صفحة الراكب
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(passengerUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF1E88E5)
                )
            ) {
                Text(
                    "🧑‍💼 $passengerLabel — $openLabel",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // زر تحميل تطبيق السائق
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(driverApkUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                )
            ) {
                Text(
                    "🚌 $driverLabel — $downloadLabel",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // زر رجوع
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    backLabel,
                    color = androidx.compose.ui.graphics.Color(0xFF1E88E5)
                )
            }
        }
    }
}