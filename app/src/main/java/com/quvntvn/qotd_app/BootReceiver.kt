package com.quvntvn.qotd_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Ré-arme l'alarme de la citation quotidienne après les évènements qui effacent
 * les alarmes programmées :
 *  - redémarrage de l'appareil (BOOT_COMPLETED / LOCKED_BOOT_COMPLETED),
 *  - mise à jour de l'application (MY_PACKAGE_REPLACED).
 *
 * Sans ce receiver, la notification cessait de s'envoyer tant que l'utilisateur
 * ne rouvrait pas l'app manuellement.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",   // variantes constructeurs (HTC/Xiaomi)
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val (enabled, hour, minute) = SharedPrefManager.getNotificationSettings(context)
                if (enabled) {
                    QuoteAlarmReceiver.scheduleDailyQuote(context.applicationContext, hour, minute)
                }
            }
        }
    }
}
