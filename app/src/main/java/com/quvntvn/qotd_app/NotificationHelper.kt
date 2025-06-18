package com.quvntvn.qotd_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresPermission
import com.quvntvn.qotd_app.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationHelper(private val context: Context) {
    private val channelId = "quote_channel"
    private val notificationId = 101 // ID unique pour cette notification
    private val PENDING_INTENT_REQUEST_CODE = 0

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.notification_channel_name) // Extrait de strings.xml
            val channelDescription = context.getString(R.string.notification_channel_description) // Extrait de strings.xml
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Pour utiliser le son par défaut, la priorité DEFAULT est bien.
            // Si vous vouliez absolument pas de son, IMPORTANCE_LOW pourrait être une option,
            // mais cela affecte aussi d'autres aspects de la notification.

            // Plus besoin de définir soundUri ou audioAttributes ici si on utilise le son par défaut du système.

            val channel = NotificationChannel(
                channelId,
                channelName,
                importance
            ).apply {
                description = channelDescription
                // On ne fait plus appel à setSound() pour utiliser le son par défaut du canal/système
                setShowBadge(false) // Désactiver les badges numériques/points pour ce canal

                // Optionnel: Configurer la vibration, la lumière, etc.
                // Si vous ne spécifiez rien pour la vibration, elle suivra aussi les paramètres système/canal par défaut.
                // enableLights(true)
                // lightColor = Color.RED
                // enableVibration(true) // Si vous voulez forcer la vibration même si le son est désactivé
                // vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(quote: Quote) {
        // La logique de parsing de la date peut rester ici ou être déplacée dans le modèle Quote si besoin
        val year = quote.dateCreation?.let { dateString ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let {
                    val outputFormat = SimpleDateFormat("yyyy", Locale.getDefault())
                    outputFormat.format(it)
                } ?: "N/A"
            } catch (e: Exception) {
                // Log.e("NotificationHelper", "Erreur de parsing de date", e)
                "N/A"
            }
        } ?: "N/A"

        // Intent pour lancer MainActivity lorsque la notification est cliquée
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Optionnel: Passer des données à l'Activity
            // putExtra("QUOTE_ID", quote.id)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST_CODE,
            intent,
            pendingIntentFlags
        )

        // Plus besoin de soundUriForPreOreo si on utilise le son par défaut

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            // Petite icône (blanche et transparente)
            // Remplacez R.drawable.ic_stat_quote par votre petite icône
            .setSmallIcon(R.drawable.ic_qotd_notif)
            .setContentTitle(quote.auteur)
            .setContentText(quote.citation)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote.citation))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // La notification disparaît après le clic
            // Grande icône (en couleur)
            // Remplacez R.drawable.ic_quote_large par votre grande icône
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round))
            // Spécifier qu'il ne faut pas incrémenter le compteur de badge
            .setNumber(0)
        // Optionnel: Définir une couleur d'accentuation
        // .setColor(ContextCompat.getColor(context, R.color.your_notification_accent_color))

        // On ne définit plus de son sur le builder pour les versions pré-Oreo non plus,
        // afin qu'elles utilisent également le son de notification par défaut du système.
        // Si vous ne faites rien, NotificationCompat.Builder utilisera les valeurs par défaut
        // pour le son, la vibration, etc., basées sur la priorité de la notification.
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        // notificationBuilder.priority = NotificationCompat.PRIORITY_DEFAULT // Déjà la valeur par défaut
        // }

        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
    }
}

// Assurez-vous que votre modèle de données Quote est défini quelque part, par exemple :
// data class Quote(
//    val id: String? = null, // Ou Int, si vous l'utilisez pour le putExtra
//    val citation: String,
//    val auteur: String,
//    val dateCreation: String? // Format "yyyy-MM-dd"
// )

// N'oubliez pas d'ajouter les chaînes de caractères pour le nom et la description du canal
// dans votre fichier res/values/strings.xml :
// <string name="notification_channel_name">Citations</string>
// <string name="notification_channel_description">Notifications pour les citations quotidiennes</string>