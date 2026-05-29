package com.dolmus.netapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

@Composable
fun RegisterScreen(lang: String, onRegisterSuccess: () -> Unit, onBack: () -> Unit) {
    var currentLang  by remember { mutableStateOf(lang) }
    var username     by remember { mutableStateOf("") }
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var city         by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val title       = when(currentLang) { "tr" -> "Yeni Hesap"; "en" -> "New Account"; else -> "حساب جديد" }
    val nameLabel   = when(currentLang) { "tr" -> "Ad Soyad"; "en" -> "Full Name"; else -> "الاسم الكامل" }
    val emailLabel  = when(currentLang) { "tr" -> "E-posta"; "en" -> "Email"; else -> "البريد الإلكتروني" }
    val passLabel   = when(currentLang) { "tr" -> "Sifre"; "en" -> "Password"; else -> "كلمة المرور" }
    val cityLabel   = when(currentLang) { "tr" -> "Sehir"; "en" -> "City"; else -> "المدينة" }
    val registerBtn = when(currentLang) { "tr" -> "Kayit Ol"; "en" -> "Register"; else -> "إنشاء الحساب" }
    val backBtn     = when(currentLang) { "tr" -> "Geri Don"; "en" -> "Back"; else -> "رجوع" }
    val errorText   = when(currentLang) { "tr" -> "Kayit hatasi"; "en" -> "Registration error"; else -> "خطأ في إنشاء الحساب" }
    val showPass    = when(currentLang) { "tr" -> "Göster"; "en" -> "Show"; else -> "إظهار" }
    val hidePass    = when(currentLang) { "tr" -> "Gizle"; "en" -> "Hide"; else -> "إخفاء" }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor    = Color(0xFF0D0D0D),
        unfocusedTextColor  = Color(0xFF0D0D0D),
        focusedLabelColor   = Color(0xFF1E88E5),
        unfocusedLabelColor = Color(0xFF444444),
        focusedBorderColor  = Color(0xFF1E88E5),
        unfocusedBorderColor = Color(0xFF888888),
        cursorColor         = Color(0xFF1E88E5)
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dolmuş NET", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 16.sp, color = Color(0xFF444444))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("tr" to "Türkçe", "en" to "English", "ar" to "العربية").forEach { (code, label) ->
                val selected = currentLang == code
                OutlinedButton(
                    onClick = { currentLang = code },
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

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(nameLabel) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(emailLabel) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = fieldColors
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(passLabel) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = fieldColors,
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) hidePass else showPass, fontSize = 12.sp, color = Color(0xFF1E88E5))
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text(cityLabel) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
                        // ─── التسجيل مع حفظ الاسم في user_metadata ──
                        supabase.auth.signUpWith(Email) {
                            this.email = email
                            this.password = password
                            this.data = buildJsonObject {
                                put("full_name", username.trim())
                                put("city", city.trim())
                            }
                        }
                        onRegisterSuccess()
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
            else Text(registerBtn, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(backBtn, fontSize = 16.sp, color = Color(0xFF1E88E5))
        }
    }
}