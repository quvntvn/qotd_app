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
import org.json.JSONObject

/**
 * Construit et affiche la notification de la citation du jour.
 *
 * Trois niveaux de rendu, du plus universel au plus spécifique :
 *  1. Notification heads-up soignée (haute importance, couleur, grande icône, BigText) — partout.
 *  2. Pastille / Live Update Android 16 (API 36) — `setShortCriticalText` + promotion "ongoing".
 *  3. Island HyperOS (Xiaomi/Poco) — extras MIUI `miui.focus.*` (best-effort, non documenté).
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID = "quote_channel"
        private const val NOTIFICATION_ID = 101
        private const val PENDING_INTENT_REQUEST_CODE = 0
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

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qotd_notif)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(Notification.BigTextStyle().bigText(content))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setColor(accent)
            .setColorized(true)

        runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_new_round)
                ?.let { builder.setLargeIcon(it) }
        }.onFailure { Log.w(TAG, "Grande icône non chargée: ${it.message}") }

        applyAndroid16LiveUpdate(builder, quote)
        applyMiuiFocus(builder, quote)

        val notification = builder.build()
        // Android 16 : exprime l'intention de promotion en pastille. Le système ne la
        // conservera que si la notif a des "promotable characteristics" et que l'utilisateur
        // a autorisé les Live Updates pour l'app.
        if (Build.VERSION.SDK_INT >= 36) {
            runCatching { notification.flags = notification.flags or Notification.FLAG_PROMOTED_ONGOING }
        }

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de l'affichage de la notification.", e)
        }
    }

    /**
     * Android 16 (API 36) : demande la promotion en "Live Update" → pastille dans la barre
     * de statut + affichage sur l'écran de verrouillage/AOD. La promotion exige une
     * notification "ongoing" (le tap l'ouvre et la referme via autoCancel ; elle est de
     * toute façon remplacée le lendemain car même NOTIFICATION_ID).
     *
     * Appels protégés : si l'API n'existe pas sur la ROM, on ignore silencieusement.
     */
    private fun applyAndroid16LiveUpdate(builder: Notification.Builder, quote: Quote) {
        if (Build.VERSION.SDK_INT < 36) return
        try {
            builder.setOngoing(true)
            builder.setShortCriticalText(shortChipText(quote))
        } catch (t: Throwable) {
            Log.w(TAG, "API Live Update Android 16 indisponible: ${t.message}")
        }
    }

    /**
     * HyperOS (Xiaomi / Redmi / POCO) : tente d'afficher la citation dans l'« Island »
     * (notification focus) via les extras MIUI non documentés. Best-effort : selon la
     * version HyperOS, l'utilisateur peut devoir activer « notifications focus » pour l'app,
     * et le schéma JSON peut varier. Encapsulé dans un try/catch pour ne jamais crasher.
     */
    private fun applyMiuiFocus(builder: Notification.Builder, quote: Quote) {
        val isXiaomi = listOf(Build.MANUFACTURER, Build.BRAND).any {
            it.equals("Xiaomi", true) || it.equals("Redmi", true) || it.equals("POCO", true)
        }
        if (!isXiaomi) return

        try {
            val param = JSONObject().apply {
                put("protocol", 1)
                put("enableFloat", true)
                put("ticker", quote.auteur)
                put("title", quote.auteur)
                put("content", quote.citation)
                put("updatable", false)
            }
            val extras = Bundle().apply {
                putBoolean("miui.enableFocus", true)
                putString("miui.focus.ticker", quote.auteur)
                putString("miui.focus.param.v2", JSONObject().put("param_v2", param).toString())
            }
            builder.addExtras(extras)
        } catch (t: Throwable) {
            Log.w(TAG, "Notification focus HyperOS indisponible: ${t.message}")
        }
    }

    /** Texte ultra-court affiché dans la pastille de la barre de statut (Android 16). */
    private fun shortChipText(quote: Quote): String {
        val author = quote.auteur.trim()
        val label = author.ifBlank { context.getString(R.string.app_name) }
        return if (label.length > 20) label.take(19) + "…" else label
    }
}
