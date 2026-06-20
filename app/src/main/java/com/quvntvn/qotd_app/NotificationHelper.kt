package com.quvntvn.qotd_app

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Construit et affiche la notification de la citation du jour.
 *
 * Selon la version Android et le réglage utilisateur, deux rendus :
 *  - Pastille "Live Update" Android 16 (API 36) : promue dans la barre de statut / Dynamic
 *    Island. Repliée elle ne montre que le logo (pas de shortCriticalText) ; dépliée elle
 *    montre citation + auteur + date. Un bouton "Fermer" permet de la retirer, et au bout de
 *    [DEMOTE_DELAY_MS] elle redevient une notif simple (via [QuoteNotificationReceiver]).
 *  - Notif simple (pré-API 36, ou pastille désactivée dans les réglages) : classique, balayable.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID = "quote_channel"
        const val NOTIFICATION_ID = 101
        private const val PENDING_INTENT_REQUEST_CODE = 0
        private const val REQ_CLOSE = 1
        private const val REQ_DEMOTE = 2
        // Au bout de ce délai, la pastille redevient une notif simple (non promue, balayable).
        const val DEMOTE_DELAY_MS = 10L * 60 * 1000
        // Filet de sécurité : si le "demote" n'a jamais lieu, la notif promue ne reste pas
        // collée et s'auto-efface (la notif du lendemain la régénère de toute façon).
        private const val FAILSAFE_TIMEOUT_MS = 30L * 60 * 1000
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
     * Affiche la notification pour [quote] (citation/auteur déjà dans la bonne langue).
     * Promue en pastille si Android 16 ET réglage activé, sinon notif simple.
     */
    fun showNotification(quote: Quote) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "Permission POST_NOTIFICATIONS non accordée — notification ignorée.")
            return
        }
        val promote = Build.VERSION.SDK_INT >= 36 && SharedPrefManager.isLiveUpdateEnabled(context)
        post(quote, promote)
        if (promote) scheduleDemote()
    }

    /** Re-poste la citation en notif simple (non promue). Appelé après [DEMOTE_DELAY_MS]. */
    fun showPlainNotification(quote: Quote) {
        if (!hasNotificationPermission()) return
        post(quote, promote = false)
    }

    private fun post(quote: Quote, promote: Boolean) {
        val builder = baseBuilder(quote)
        if (promote) applyPromotion(builder)
        try {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Échec de l'affichage de la notification.", e)
        }
    }

    private fun baseBuilder(quote: Quote): Notification.Builder {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, PENDING_INTENT_REQUEST_CODE, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val accent = ContextCompat.getColor(context, R.color.colorPrimary)

        // Titre = auteur, suivi de l'année de la citation si dispo (ex. "Jean Rostand - 1940").
        val year = quote.dateCreation?.take(4)?.takeIf { it.isNotBlank() }
        val title = if (year != null) "${quote.auteur} - $year" else quote.auteur

        // NB : pas de setColorized(true) — une notif colorisée est refusée à la promotion.
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qotd_notif)
            .setContentTitle(title)
            .setContentText(quote.citation)
            .setStyle(Notification.BigTextStyle().bigText(quote.citation))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setColor(accent)

        runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_new_round)
                ?.let { builder.setLargeIcon(it) }
        }.onFailure { Log.w(TAG, "Grande icône non chargée: ${it.message}") }

        return builder
    }

    /**
     * Android 16 (API 36) : demande la promotion en "Live Update" (pastille).
     * Conditions vérifiées sur appareil : ongoing, contentTitle non vide, style supporté
     * (BigText), canal ≥ IMPORTANCE_LOW, NON colorisée, extra `android.requestPromotedOngoing`.
     * shortCriticalText vide → la pastille repliée n'affiche que le logo (sinon HyperOS y
     * recopie le contentTitle/auteur). On ajoute un bouton "Fermer" (l'ongoing n'est pas balayable).
     */
    private fun applyPromotion(builder: Notification.Builder) {
        try {
            builder.setOngoing(true)
            builder.setTimeoutAfter(FAILSAFE_TIMEOUT_MS)
            builder.setShortCriticalText("📅")
            builder.addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true) })
            builder.addAction(closeAction())
        } catch (t: Throwable) {
            Log.w(TAG, "API Live Update Android 16 indisponible: ${t.message}")
        }
    }

    private fun closeAction(): Notification.Action {
        val intent = Intent(context, QuoteNotificationReceiver::class.java)
            .setAction(QuoteNotificationReceiver.ACTION_CLOSE)
        val pi = PendingIntent.getBroadcast(
            context, REQ_CLOSE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel)
        return Notification.Action.Builder(icon, context.getString(R.string.close), pi).build()
    }

    /** Programme la rétrogradation de la pastille en notif simple après [DEMOTE_DELAY_MS]. */
    private fun scheduleDemote() {
        val intent = Intent(context, QuoteNotificationReceiver::class.java)
            .setAction(QuoteNotificationReceiver.ACTION_DEMOTE)
        val pi = PendingIntent.getBroadcast(
            context, REQ_DEMOTE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + DEMOTE_DELAY_MS, pi
        )
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
