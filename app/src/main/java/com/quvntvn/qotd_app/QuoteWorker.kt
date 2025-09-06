package com.quvntvn.qotd_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
// Supposons que SharedPrefManager, TranslationManager, QuoteApi, Quote, NotificationHelper existent et fonctionnent
// import com.quvntvn.qotd_app.SharedPrefManager
// import com.quvntvn.qotd_app.TranslationManager
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
                return Result.failure()
            }
        }

        return try {
            val repository = (appContext.applicationContext as MyApp).quoteRepository
            val quote = repository.getRandomQuote()

            if (quote != null) {
                NotificationHelper(appContext).showNotification(quote)
                Log.d(TAG, "Notification de citation affichée avec succès.")
                Result.success()
            } else {
                Log.w(TAG, "Impossible de récupérer une citation.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception dans doWork: ${e.localizedMessage}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "QuoteWorker"

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

            QuoteAlarmReceiver.scheduleDailyQuote(context.applicationContext, hour, minute)
            Log.d(TAG, "Alarm scheduled for daily quote at $hour:$minute")
        }

    }
}
