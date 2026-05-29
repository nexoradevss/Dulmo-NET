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

@Composable
fun LanguageScreen(onLanguageSelected: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("Dolmus Live", fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("اختر اللغة / Dil Secin / Select Language", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            LanguageButton("العربية", "ar", onLanguageSelected)
            LanguageButton("Turkce", "tr", onLanguageSelected)
            LanguageButton("English", "en", onLanguageSelected)
        }
    }
}

@Composable
fun LanguageButton(label: String, code: String, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(code) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}