import android.content.Context

// SharedPrefManager.kt
object SharedPrefManager {
    private const val PREFS_NAME = "quote_prefs"
    private const val NOTIFICATION_ENABLED = "notif_enabled"
    private const val NOTIFICATION_HOUR = "notif_hour"
    private const val NOTIFICATION_MINUTE = "notif_minute" // Nouvelle clé

    // Adapter pour sauvegarder les minutes
    fun saveSettings(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                putBoolean(NOTIFICATION_ENABLED, enabled)
                putInt(NOTIFICATION_HOUR, hour)
                putInt(NOTIFICATION_MINUTE, minute) // Sauvegarder les minutes
                commit()
            }
    }

    // Adapter pour retourner un Triplet ou un objet personnalisé
    // Ici, un Triplet (Boolean, Int, Int) pour enabled, hour, minute
    fun getNotificationSettings(context: Context): Triple<Boolean, Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Triple(
            prefs.getBoolean(NOTIFICATION_ENABLED, true),
            prefs.getInt(NOTIFICATION_HOUR, 10),
            prefs.getInt(NOTIFICATION_MINUTE, 0) // Valeur par défaut pour les minutes
        )
    }
}

// Puis dans SettingsActivity, lors du chargement :
// val (enabled, savedHour, savedMinute) = SharedPrefManager.getNotificationSettings(this)
// Et lors de la sauvegarde :
// SharedPrefManager.saveSettings(this, notificationsAreEnabled, newHour, newMinute)