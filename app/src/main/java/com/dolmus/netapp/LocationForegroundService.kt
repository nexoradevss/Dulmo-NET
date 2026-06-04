package com.dolmus.netapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class LocationForegroundService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastSavedLocation: Location? = null
    private var lastUploadedLocation: Location? = null

    companion object {
        const val CHANNEL_ID = "dolmus_location_channel"
        const val NOTIF_ID = 1001

        const val ACTION_START_CALIBRATION = "ACTION_START_CALIBRATION"
        const val ACTION_START_WORK        = "ACTION_START_WORK"
        const val ACTION_STOP              = "ACTION_STOP"
        const val ACTION_PAUSE_SAVING      = "ACTION_PAUSE_SAVING"
        const val ACTION_RESUME_SAVING     = "ACTION_RESUME_SAVING"

        var isRunning: Boolean = false
        var currentMode: String = "idle"

        val calibrationPoints = mutableListOf<android.location.Location>()
        var lastKnownLocation: Location? = null
        var onNewLocation: ((Location) -> Unit)? = null

        // بيانات السائق — تُعيَّن من HomeScreen عند بدء العمل
        var currentDriverName: String = ""
        var currentRouteName: String  = ""
    }

    // ─── Data class للرفع إلى Supabase ──────────────────────────
    @Serializable
    data class LiveLocationUpload(
        val driver_name: String,
        val route_name: String,
        val lat: Double,
        val lng: Double
    )

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DolmusNET::LocationWakeLock"
        )
    }

    @SuppressWarnings("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        when (intent?.action) {

            ACTION_START_CALIBRATION -> {
                currentMode = "calibration"
                calibrationPoints.clear()
                lastSavedLocation = null
                updateNotification("🔴 جارٍ تسجيل الخط...")
                startForeground(NOTIF_ID, buildNotification("🔴 جارٍ تسجيل الخط..."))
                acquireWakeLock()
                startLocationUpdates(intervalMs = 2000L, minDistanceM = 5f)
                isRunning = true
                Log.d("DOLMUS_SERVICE", "Started CALIBRATION mode")
            }

            ACTION_START_WORK -> {
                currentMode = "work"
                lastSavedLocation = null
                lastUploadedLocation = null
                updateNotification("🟢 البث الحي جارٍ...")
                startForeground(NOTIF_ID, buildNotification("🟢 البث الحي جارٍ..."))
                acquireWakeLock()
                startLocationUpdates(intervalMs = 3000L, minDistanceM = 5f)
                isRunning = true
                Log.d("DOLMUS_SERVICE", "Started WORK mode")
            }

            ACTION_PAUSE_SAVING -> {
                currentMode = "calibration_paused"
                Log.d("DOLMUS_SERVICE", "Calibration PAUSED")
            }

            ACTION_RESUME_SAVING -> {
                currentMode = "calibration"
                lastSavedLocation = null
                Log.d("DOLMUS_SERVICE", "Calibration RESUMED")
            }

            ACTION_STOP -> {
                // احذف موقع السائق من Supabase عند الإيقاف
                if (currentDriverName.isNotBlank()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            supabase.postgrest["live_locations"].delete {
                                filter { eq("driver_name", currentDriverName) }
                            }
                            Log.d("DOLMUS_SERVICE", "Live location removed on stop")
                        } catch (e: Exception) {
                            Log.e("DOLMUS_SERVICE", "Remove location error: ${e.message}")
                        }
                    }
                }
                stopSelf()
            }

            else -> {
                if (!isRunning) {
                    currentMode = "work"
                    startForeground(NOTIF_ID, buildNotification("🟢 البث الحي جارٍ..."))
                    acquireWakeLock()
                    startLocationUpdates(3000L, 5f)
                    isRunning = true
                }
            }
        }

        return START_STICKY
    }

    @SuppressWarnings("MissingPermission")
    private fun startLocationUpdates(intervalMs: Long, minDistanceM: Float) {
        if (::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                lastKnownLocation = loc
                onNewLocation?.invoke(loc)

                when (currentMode) {
                    "calibration" -> handleCalibrationPoint(loc)
                    "work"        -> handleWorkPoint(loc)
                    else          -> { }
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(minDistanceM)
            .build()

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun handleCalibrationPoint(loc: Location) {
        val shouldSave = lastSavedLocation?.let { it.distanceTo(loc) >= 50f } ?: true
        if (shouldSave) {
            lastSavedLocation = loc
            calibrationPoints.add(loc)
            Log.d("DOLMUS_SERVICE", "Calibration point: ${calibrationPoints.size}")
            updateNotification("🔴 تسجيل الخط... ${calibrationPoints.size} نقطة")
        }
    }

    private fun handleWorkPoint(loc: Location) {
        // أرسل كل 50 متر فقط لتوفير البطارية والإنترنت
        val shouldUpload = lastUploadedLocation?.let { it.distanceTo(loc) >= 50f } ?: true
        if (!shouldUpload) return
        if (currentDriverName.isBlank() || currentRouteName.isBlank()) return

        lastUploadedLocation = loc

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = LiveLocationUpload(
                    driver_name = currentDriverName,
                    route_name  = currentRouteName,
                    lat         = loc.latitude,
                    lng         = loc.longitude
                )

                // upsert — إذا كان الاسم موجوداً يحدّث، وإلا يضيف
                supabase.postgrest["live_locations"].upsert(data) {
                    onConflict = "driver_name"
                }

                Log.d("DOLMUS_SERVICE", "📍 Uploaded: ${loc.latitude}, ${loc.longitude}")
            } catch (e: Exception) {
                Log.e("DOLMUS_SERVICE", "Upload error: ${e.message}")
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(10 * 60 * 60 * 1000L)
        }
    }

    override fun onDestroy() {
        isRunning = false
        currentMode = "idle"
        if (wakeLock?.isHeld == true) wakeLock?.release()
        if (::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
        onNewLocation = null
        Log.d("DOLMUS_SERVICE", "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Dolmuş Live GPS",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "تتبع موقع الحافلة"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dolmuş Live")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}