package com.quvntvn.qotd_app // Déclaration du package ajoutée

import android.content.Context

// SharedPrefManager.kt
object SharedPrefManager {
    private const val PREFS_NAME = "quote_prefs"
    private const val NOTIFICATION_ENABLED = "notif_enabled"
    private const val NOTIFICATION_HOUR = "notif_hour"
    private const val NOTIFICATION_MINUTE = "notif_minute" // Nouvelle clé

    // Sauvegarde les paramètres de notification
    fun saveSettings(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply { // Utilisation de apply() pour une écriture asynchrone
                putBoolean(NOTIFICATION_ENABLED, enabled)
                putInt(NOTIFICATION_HOUR, hour)
                putInt(NOTIFICATION_MINUTE, minute) // Sauvegarder les minutes
                apply() // Appliquer les changements
            }
    }

    // Récupère les paramètres de notification
    // Retourne un Triplet (Boolean, Int, Int) pour enabled, hour, minute
    fun getNotificationSettings(context: Context): Triple<Boolean, Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Triple(
            prefs.getBoolean(NOTIFICATION_ENABLED, true), // Valeur par défaut : activé
            prefs.getInt(NOTIFICATION_HOUR, 10),       // Valeur par défaut : 10h
            prefs.getInt(NOTIFICATION_MINUTE, 0)        // Valeur par défaut : 00 minutes
        )
    }
}

// Utilisation typique dans une activité (par exemple, SettingsActivity) :

// Lors du chargement des paramètres :
// val (notificationsAreEnabled, savedHour, savedMinute) = SharedPrefManager.getNotificationSettings(this)
// timePicker.hour = savedHour
// timePicker.minute = savedMinute
// switchNotifications.isChecked = notificationsAreEnabled

// Lors de la sauvegarde des paramètres (par exemple, dans un OnClickListener) :
// val newHour = timePicker.hour
// val newMinute = timePicker.minute
// val notificationsAreEnabled = switchNotifications.isChecked
// SharedPrefManager.saveSettings(this, notificationsAreEnabled, newHour, newMinute)