package com.dolmus.netapp

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CalibrationSession {

    private const val PREF_NAME       = "dolmus_calibration_session"
    private const val KEY_ROUTE_NAME  = "route_name"
    private const val KEY_START_LABEL = "start_label"
    private const val KEY_END_LABEL   = "end_label"
    private const val KEY_GOING_PTS   = "going_points"
    private const val KEY_PHASE       = "phase"

    @Serializable
    data class SavedPoint(val lat: Double, val lng: Double)

    fun saveHeader(context: Context, routeName: String, startLabel: String, endLabel: String) {
        prefs(context).edit()
            .putString(KEY_ROUTE_NAME,  routeName)
            .putString(KEY_START_LABEL, startLabel)
            .putString(KEY_END_LABEL,   endLabel)
            .putString(KEY_PHASE,       "header_saved")
            .apply()
    }

    fun saveGoingPoints(context: Context, points: List<SavedPoint>) {
        val json = Json.encodeToString(points)
        prefs(context).edit()
            .putString(KEY_GOING_PTS, json)
            .putString(KEY_PHASE, "going_done")
            .apply()
    }

    fun getPhase(context: Context): String =
        prefs(context).getString(KEY_PHASE, "idle") ?: "idle"

    fun getRouteName(context: Context): String =
        prefs(context).getString(KEY_ROUTE_NAME, "") ?: ""

    fun getStartLabel(context: Context): String =
        prefs(context).getString(KEY_START_LABEL, "") ?: ""

    fun getEndLabel(context: Context): String =
        prefs(context).getString(KEY_END_LABEL, "") ?: ""

    fun getGoingPoints(context: Context): List<SavedPoint> {
        val json = prefs(context).getString(KEY_GOING_PTS, null) ?: return emptyList()
        return try { Json.decodeFromString(json) } catch (e: Exception) { emptyList() }
    }

    fun hasPendingGoing(context: Context): Boolean =
        getPhase(context) == "going_done" && getRouteName(context).isNotBlank()

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
