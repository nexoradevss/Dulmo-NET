package com.dolmus.netapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.dolmus.netapp.ui.theme.DulmusNETTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkGpsEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            checkGpsEnabled()
        }

        setContent {
            DulmusNETTheme {
                val prefs = getSharedPreferences("dolmus_prefs", android.content.Context.MODE_PRIVATE)
                var lang by remember { mutableStateOf(prefs.getString("lang", "tr") ?: "tr") }
                var screen by remember { mutableStateOf("loading") }

                LaunchedEffect(Unit) {
                    try {
                        supabase.auth.awaitInitialization()
                        val session = supabase.auth.currentSessionOrNull()
                        screen = if (session != null) "home" else "login"
                    } catch (e: Exception) {
                        screen = "login"
                    }
                }

                when (screen) {
                    "loading" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    "login" -> LoginScreen(
                        lang = lang,
                        onLangChange = {
                        lang = it
                        prefs.edit().putString("lang", it).apply()
                    },
                        onLoginSuccess = { screen = "home" },
                        onRegister = { screen = "register" }
                    )
                    "register" -> RegisterScreen(
                        lang = lang,
                        onRegisterSuccess = { screen = "home" },
                        onBack = { screen = "login" }
                    )
                    "home" -> HomeScreen(
                        lang = lang,
                        onLogout = {
                            MainScope().launch {
                                supabase.auth.signOut()
                            }
                            screen = "login"
                        }
                    )
                }
            }
        }
    }

    private fun checkGpsEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
}
