package com.quvntvn.qotd_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy // <- AJOUTÉ
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder // <- MODIFIÉ (au lieu de OneTime)
import androidx.work.WorkManager
import androidx.work.WorkerParameters
// Supposons que SharedPrefManager, TranslationManager, QuoteApi, Quote, NotificationHelper existent et fonctionnent
// import com.quvntvn.qotd_app.SharedPrefManager
// import com.quvntvn.qotd_app.TranslationManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.util.Log

class QuoteWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork: Exécution du travail pour la citation.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Permission POST_NOTIFICATIONS non accordée. Impossible d'afficher la notification.")
                // Si la permission n'est pas accordée, il est préférable d'arrêter ici et de ne pas réessayer indéfiniment.
                // Le travail devrait être replanifié uniquement lorsque la permission est accordée.
                return Result.failure()
            }
        }

        return try {
            val api = QuoteApi.create() // Assurez-vous que cette méthode existe et est correcte
            val response = api.getDailyQuote() // Assurez-vous que cette méthode existe et est correcte

            if (response.isSuccessful) {
                response.body()?.let { quote ->
                    val lang = SharedPrefManager.getLanguage(appContext)
                    val citationText = if (lang == "en") { // Vous pourriez vouloir aussi traduire si la langue est "fr" et que la citation est dans une autre langue
                        try {
                            // Assurez-vous que TranslationManager gère bien les exceptions réseau etc.
                            TranslationManager(appContext).translate(quote.citation, lang)
                        } catch (e: Exception) {
                            Log.e(TAG, "Erreur de traduction: ${e.localizedMessage}", e)
                            quote.citation // Retour à la citation originale en cas d'erreur
                        }
                    } else {
                        quote.citation
                    }
                    val translatedQuote = Quote(citationText, quote.auteur, quote.dateCreation) // Assurez-vous que la classe Quote est définie
                    NotificationHelper(appContext).showNotification(translatedQuote) // Assurez-vous que NotificationHelper est défini
                    Log.d(TAG, "Notification de citation affichée avec succès.")
                } ?: Log.w(TAG, "Réponse API réussie mais corps vide.")
                Result.success()
            } else {
                Log.e(TAG, "Erreur API: ${response.code()} - ${response.message()}")
                // En cas d'erreur API, un retry est raisonnable. WorkManager gère les backoffs.
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception dans doWork: ${e.localizedMessage}", e)
            Result.retry() // Retry en cas d'autres exceptions (ex: réseau)
        }
    }

    companion object {
        private const val TAG = "QuoteWorker"
        // C'EST LA CONSTANTE IMPORTANTE POUR IDENTIFIER LE TRAVAIL DE MANIÈRE UNIQUE
        const val UNIQUE_WORK_NAME = "daily_quote_notification_worker"

        /**
         * Planifie une tâche périodique quotidienne pour afficher une citation.
         * @param context Le contexte de l'application.
         * @param hour L'heure de la journée (0-23) pour la première notification.
         * @param minute La minute de l'heure (0-59) pour la première notification.
         */
        fun scheduleDailyQuote(context: Context, hour: Int, minute: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Tentative de planification sans permission POST_NOTIFICATIONS. Le travail ne sera pas planifié.")
                // Il est important de ne pas planifier si la permission n'est pas là,
                // car le worker échouera de toute façon à afficher la notification.
                return
            }

            val workManager = WorkManager.getInstance(context.applicationContext) // Utiliser applicationContext

            // Calculer le délai initial pour que la première notification soit à l'heure spécifiée
            val initialDelay = calculateInitialDelay(hour, minute)
            Log.d(TAG, "Planification de la citation quotidienne. Prochaine dans: ${TimeUnit.MILLISECONDS.toMinutes(initialDelay)} minutes.")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Exemple de contrainte
                .build()

            val dailyQuoteRequest =
                PeriodicWorkRequestBuilder<QuoteWorker>(1, TimeUnit.DAYS) // Répéter tous les jours
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints) // Appliquer les contraintes
                    .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME, // Utilisation du nom unique
                ExistingPeriodicWorkPolicy.REPLACE, // Remplace le travail existant s'il y en a un avec le même nom
                dailyQuoteRequest
            )
            Log.d(TAG, "Travail périodique '${UNIQUE_WORK_NAME}' planifié pour se répéter tous les jours.")
        }

        /**
         * Calcule le délai en millisecondes jusqu'à la prochaine occurrence de l'heure et minute cibles.
         * @param targetHour L'heure cible (0-23).
         * @param targetMinute La minute cible (0-59).
         * @return Le délai en millisecondes.
         */
        private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
            val currentTime = Calendar.getInstance()
            val dueTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Si l'heure spécifiée est déjà passée pour aujourd'hui, programmer pour le lendemain à la même heure
            if (dueTime.before(currentTime)) {
                dueTime.add(Calendar.DAY_OF_YEAR, 1)
            }

            return dueTime.timeInMillis - currentTime.timeInMillis
        }
    }
}
