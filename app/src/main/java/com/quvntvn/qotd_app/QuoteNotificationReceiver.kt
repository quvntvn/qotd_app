package com.quvntvn.qotd_app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gère les actions de la notification "Live Update" :
 *  - [ACTION_CLOSE]  : le bouton "Fermer" → retire la notification.
 *  - [ACTION_DEMOTE] : programmé ~10 min après l'affichage → si la notif est toujours là,
 *    la re-poste en version simple (non promue, balayable) pour qu'elle ne reste pas
 *    en permanence dans la pastille. Si l'utilisateur l'a déjà fermée/ouverte, on ne fait rien.
 */
class QuoteNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        when (intent?.action) {
            ACTION_CLOSE -> nm.cancel(NotificationHelper.NOTIFICATION_ID)

            ACTION_DEMOTE -> {
                val stillShown = nm.activeNotifications.any {
                    it.id == NotificationHelper.NOTIFICATION_ID
                }
                if (!stillShown) return

                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val quote = QuoteRepository(context).getDailyQuote()
                        if (quote != null) {
                            val lang = SharedPrefManager.getLanguage(context)
                            val display = if (lang == "en") {
                                quote.copy(citation = quote.citationEn, auteur = quote.auteurEn)
                            } else {
                                quote
                            }
                            NotificationHelper(context).showPlainNotification(display)
                        }
                    } catch (_: Exception) {
                        // ignore
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_CLOSE = "com.quvntvn.qotd_app.action.CLOSE_QUOTE"
        const val ACTION_DEMOTE = "com.quvntvn.qotd_app.action.DEMOTE_QUOTE"
    }
}
