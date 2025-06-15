package com.quvntvn.qotd_app

import android.content.Context

// 7. SharedPrefManager.kt (Gestion des préférences)
object SharedPrefManager {
    private const val PREFS_NAME = "quote_prefs"
    private const val NOTIFICATION_ENABLED = "notif_enabled"
    private const val NOTIFICATION_HOUR = "notif_hour"

    fun saveSettings(context: Context, enabled: Boolean, hour: Int) {
        // Persist settings synchronously to guarantee they are immediately
        // available when returning to the settings screen
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                putBoolean(NOTIFICATION_ENABLED, enabled)
                putInt(NOTIFICATION_HOUR, hour)
                commit() // ensure value is written before we finish the activity
            }
    }

    fun getNotificationSettings(context: Context): Pair<Boolean, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Pair(
            prefs.getBoolean(NOTIFICATION_ENABLED, true),
            prefs.getInt(NOTIFICATION_HOUR, 10)
        )
    }
}