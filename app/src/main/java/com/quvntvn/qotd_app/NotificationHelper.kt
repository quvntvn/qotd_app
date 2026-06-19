package com.quvntvn.qotd_app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Construit et affiche la notification de la citation du jour.
 *
 * Deux niveaux de rendu :
 *  1. Notification heads-up soignée (haute importance, couleur, grande icône, BigText) — partout.
 *  2. Pastille "Live Update" Android 16 (API 36) — promotion en pastille de la barre de statut
 *     + AOD/écran verrouillé (fonctionne aussi sur HyperOS qui honore l'API AOSP standard).
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID = "quote_channel"
        private const val NOTIFICATION_ID = 101
        private const val PENDING_INTENT_REQUEST_CODE = 0
        // Sécurité : une notif "ongoing" (promue) ne doit jamais rester collée indéfiniment.
        // Elle s'auto-efface après 12 h (et de toute façon celle du lendemain la remplace).
        private const val LIVE_UPDATE_TIMEOUT_MS = 12L * 60 * 60 * 1000
        // Notification.EXTRA_REQUEST_PROMOTED_ONGOING (constante @hide en API 36).
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
    }

    init {
        createNotificationChannel()
    }

    /** minSdk = 28 → le canal de notification est toujours requis et disponible. */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.createNotificationChannel(channel) ?: Log.e(TAG, "NotificationManager indisponible.")
    }

    /**
     * Affiche la notification pour [quote]. La citation/l'auteur sont supposés déjà
     * dans la bonne langue (l'appelant a fait le choix FR/EN au préalable).
     */
    fun showNotification(quote: Quote) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Permission POST_NOTIFICATIONS non accordée — notification ignorée.")
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = quote.auteur
        val content = quote.citation
        val accent = ContextCompat.getColor(context, R.color.colorPrimary)

        // NB : pas de setColorized(true) — une notif colorisée est refusée à la promotion "Live Update".
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qotd_notif)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(Notification.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setColor(accent)

        runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_new_round)
                ?.let { builder.setLargeIcon(it) }
        }.onFailure { Log.w(TAG, "Grande icône non chargée: ${it.message}") }

        applyAndroid16LiveUpdate(builder, quote)

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Échec de l'affichage de la notification.", e)
        }
    }

    /**
     * Android 16 (API 36) : demande la promotion de la notif en "Live Update" → pastille dans
     * la barre de statut + AOD/écran verrouillé.
     *
     * Conditions de promotion (vérifiées sur appareil) : ongoing, contentTitle non vide,
     * style supporté (BigText convient), canal ≥ IMPORTANCE_LOW, NON colorisée, et l'extra
     * `android.requestPromotedOngoing`. Le `Builder` n'expose pas de setter public en API 36,
     * d'où l'extra posé à la main. La permission POST_PROMOTED_NOTIFICATIONS est déclarée au
     * manifeste ; l'utilisateur garde la main via "Mises à jour en direct" dans les réglages.
     *
     * Appels protégés : si l'API n'existe pas sur la ROM, on ignore silencieusement.
     */
    private fun applyAndroid16LiveUpdate(builder: Notification.Builder, quote: Quote) {
        if (Build.VERSION.SDK_INT < 36) return
        try {
            builder.setOngoing(true)
            builder.setShortCriticalText(shortChipText(quote))
            // Filet de sécurité : la notif "ongoing" ne reste pas collée — auto-effacée après 12 h.
            builder.setTimeoutAfter(LIVE_UPDATE_TIMEOUT_MS)
            builder.addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true) })
        } catch (t: Throwable) {
            Log.w(TAG, "API Live Update Android 16 indisponible: ${t.message}")
        }
    }

    /** Texte ultra-court affiché dans la pastille de la barre de statut (Android 16). */
    private fun shortChipText(quote: Quote): String {
        val author = quote.auteur.trim()
        val label = author.ifBlank { context.getString(R.string.app_name) }
        return if (label.length > 20) label.take(19) + "…" else label
    }
}
