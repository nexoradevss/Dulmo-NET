package com.dolmus.netapp

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "dolmus_session"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun save(email: String, password: String) {
        prefs?.edit()?.putString(KEY_EMAIL, email)?.putString(KEY_PASSWORD, password)?.apply()
    }

    fun getEmail(): String? = prefs?.getString(KEY_EMAIL, null)
    fun getPassword(): String? = prefs?.getString(KEY_PASSWORD, null)

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }
}
