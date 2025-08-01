package com.quvntvn.qotd_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log // Import pour Log.e
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.checkSelfPermission

// Assurez-vous que R est correctement importé. Normalement, il l'est automatiquement
// si le package est correct et que le fichier est dans le bon module.
// import com.quvntvn.qotd_app.R

// Supposons que ces classes sont définies dans votre projet et correctement importées
// Si elles sont dans le même package, l'import n'est pas nécessaire.
// import com.quvntvn.qotd_app.MainActivity
// import com.quvntvn.qotd_app.Quote

/**
 * Classe utilitaire pour créer et afficher des notifications de citation.
 *
 * @param context Le contexte de l'application.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper" // Tag pour les logs
        private const val CHANNEL_ID = "quote_channel" // ID unique pour le canal de notification
        private const val NOTIFICATION_ID = 101 // ID unique pour cette notification spécifique
        private const val PENDING_INTENT_REQUEST_CODE = 0 // Code de requête pour le PendingIntent
    }

    init {
        createNotificationChannel()
    }

    /**
     * Crée le canal de notification. Nécessaire pour Android Oreo (API 26) et versions ultérieures.
     * Pour les versions antérieures, cette méthode ne fait rien.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Nom du canal visible par l'utilisateur dans les paramètres de l'application
            val channelName = context.getString(R.string.notification_channel_name)
            // Description du canal visible par l'utilisateur
            val channelDescription = context.getString(R.string.notification_channel_description)
            // Importance du canal. IMPORTANCE_HIGH fait apparaître la notification en mode prioritaire (heads-up)
            // et émet un son (si non désactivé par l'utilisateur).
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                importance
            ).apply {
                description = channelDescription
                // Options supplémentaires du canal (facultatif) :
                // setShowBadge(false) // Pour ne pas afficher de badge pour ce canal
                // enableLights(true) // Activer la LED de notification (si l'appareil en a une)
                // lightColor = Color.RED // Couleur de la LED
                // enableVibration(true) // Activer la vibration
                // vibrationPattern = longArrayOf(100, 200, 300) // Modèle de vibration personnalisé
            }

            // Enregistrer le canal auprès du système de notification
            val notificationManager: NotificationManager? =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            notificationManager?.createNotificationChannel(channel)
                ?: Log.e(TAG, "NotificationManager non disponible.")
        }
    }

    /**
     * Affiche une notification avec la citation fournie.
     *
     * Nécessite la permission Manifest.permission.POST_NOTIFICATIONS pour Android 13 (API 33) et plus.
     * La vérification de cette permission à l'exécution doit être gérée avant d'appeler cette méthode.
     *
     * @param quote L'objet Quote contenant les informations à afficher.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(quote: Quote) {
        // Intent à lancer lorsque l'utilisateur clique sur la notification.
        // Ouvre MainActivity.
        val intent = Intent(context, MainActivity::class.java).apply {
            // Ces flags assurent que si MainActivity est déjà ouverte, elle est ramenée au premier plan,
            // et une nouvelle instance n'est pas créée au-dessus d'une existante dans la même tâche.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Optionnel : Passer des données supplémentaires à MainActivity.
            // Assurez-vous que `quote.id` est Parcelable/Serializable ou un type primitif.
            // Exemple : intent.putExtra("QUOTE_ID_EXTRA", quote.id)
        }

        // Crée un PendingIntent pour l'action de la notification.
        // FLAG_UPDATE_CURRENT: si le PendingIntent existe déjà, il est conservé, mais son extra data est remplacé.
        // FLAG_IMMUTABLE: requis pour Android 12 (API 31)+ si le PendingIntent est passé à une autre application (comme le système ici).
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

        // Récupération des chaînes de caractères avec gestion des valeurs nulles.
        val title = quote.auteur ?: context.getString(R.string.unknown_author)
        val content = quote.citation ?: context.getString(R.string.quote_not_available)

        // Construction de la notification.
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            // Petite icône (obligatoire). Doit être une icône blanche avec des zones transparentes.
            .setSmallIcon(R.drawable.ic_qotd_notif)
            // Titre de la notification.
            .setContentTitle(title)
            // Texte principal de la notification.
            .setContentText(content)
            // Style pour afficher un texte plus long lorsque la notification est étendue.
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            // Action à exécuter lors du clic sur la notification.
            .setContentIntent(pendingIntent)
            // La notification est automatiquement annulée (supprimée) lorsque l'utilisateur clique dessus.
            .setAutoCancel(true)
            // Pour ne pas afficher de nombre sur le badge de l'application pour cette notification.
            .setNumber(0)
            // Priorité de la notification (pour les versions antérieures à Android Oreo).
            // Pour Oreo et plus, l'importance est définie sur le canal.
            // PRIORITY_MAX est utilisé ici pour maximiser la visibilité sur les anciennes versions.
            .setPriority(NotificationCompat.PRIORITY_MAX) // Valeur par défaut pour les canaux IMPORTANCE_HIGH sur les anciennes versions
        try {
            val largeIconBitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_new_round)
            if (largeIconBitmap != null) {
                notificationBuilder.setLargeIcon(largeIconBitmap)
            } else {
                Log.w(TAG, "La grande icône (R.mipmap.ic_launcher_new_round) n'a pas pu être décodée ou est nulle.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du chargement de la grande icône pour la notification.", e)
        }

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "La permission POST_NOTIFICATIONS n'a pas été accordée. La notification ne sera pas affichée.")
                    return // Ne pas afficher la notification si la permission est manquante
                }
            }
            try {
                notify(NOTIFICATION_ID, notificationBuilder.build())
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException lors de l'affichage de la notification. Vérifiez les permissions et les restrictions en arrière-plan.", e)
            } catch (e: Exception) {
                Log.e(TAG, "Exception inattendue lors de l'affichage de la notification.", e)
            }
        }
    }
}