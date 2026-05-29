package com.dolmus.netapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

@Composable
fun LoginScreen(lang: String, onLangChange: (String) -> Unit, onLoginSuccess: () -> Unit, onRegister: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val title       = when(lang) { "tr" -> "Surucü Girisi"; "en" -> "Driver Login"; else -> "تسجيل دخول السائق" }
    val emailLabel  = when(lang) { "tr" -> "E-posta"; "en" -> "Email"; else -> "البريد الإلكتروني" }
    val passLabel   = when(lang) { "tr" -> "Sifre"; "en" -> "Password"; else -> "كلمة المرور" }
    val loginBtn    = when(lang) { "tr" -> "Giris Yap"; "en" -> "Login"; else -> "تسجيل الدخول" }
    val registerBtn = when(lang) { "tr" -> "Hesap Olustur"; "en" -> "Create Account"; else -> "إنشاء حساب جديد" }
    val errorText   = when(lang) { "tr" -> "Giris hatasi"; "en" -> "Login error"; else -> "خطأ في تسجيل الدخول" }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor     = Color(0xFF0D0D0D),
        unfocusedTextColor   = Color(0xFF0D0D0D),
        focusedLabelColor    = Color(0xFF1E88E5),
        unfocusedLabelColor  = Color(0xFF444444),
        focusedBorderColor   = Color(0xFF1E88E5),
        unfocusedBorderColor = Color(0xFF888888),
        cursorColor          = Color(0xFF1E88E5)
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dolmus NET", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontSize = 16.sp, color = Color(0xFF444444), fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("tr" to "Turkce", "en" to "English", "ar" to "العربية").forEach { (code, label) ->
                val selected = lang == code
                OutlinedButton(
                    onClick = { onLangChange(code) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) Color(0xFF1E88E5) else Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(label, fontSize = 13.sp, color = if (selected) Color.White else Color(0xFF1E88E5))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(emailLabel) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(passLabel) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = ""
                    try {
                        supabase.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }

                        // ─── جلب بيانات السائق من user_metadata ──
                        val user = supabase.auth.currentUserOrNull()

                        val fullName = user?.userMetadata
                            ?.get("full_name")
                            ?.toString()
                            ?.trim('"')
                            ?.takeIf { it.isNotBlank() }
                            ?: email.substringBefore("@")

                        val city = user?.userMetadata
                            ?.get("city")
                            ?.toString()
                            ?.trim('"')
                            ?.takeIf { it.isNotBlank() }
                            ?: ""

                        // ─── حفظ الاسم والمدينة والإيميل محلياً ──
                        context.getSharedPreferences("dolmus_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString("driver_name", fullName)
                            .putString("driver_email", email)
                            .putString("driver_city", city)
                            .apply()

                        SessionManager.save(email, password)
                        onLoginSuccess()
                    } catch (e: Exception) {
                        errorMessage = e.message ?: errorText
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
            else Text(loginBtn, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(registerBtn, fontSize = 16.sp, color = Color(0xFF1E88E5), fontWeight = FontWeight.Medium)
        }
    }
}