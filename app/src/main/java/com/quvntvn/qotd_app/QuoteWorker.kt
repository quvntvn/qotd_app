package com.quvntvn.qotd_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quvntvn.qotd_app.SharedPrefManager
import java.util.Calendar
// import java.util.TimeZone // Décommentez si vous voulez un fuseau horaire spécifique comme "Europe/Paris"
import java.util.concurrent.TimeUnit
import android.util.Log

// 6. QuoteWorker.kt (Tâche de fond)
class QuoteWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
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
            val api = QuoteApi.create()
            val response = api.getDailyQuote()

            if (response.isSuccessful) {
                response.body()?.let {
                    NotificationHelper(appContext).showNotification(it)
                }
                val (enabled, hour, minute) = SharedPrefManager.getNotificationSettings(appContext)
                if (enabled) {
                    scheduleDailyQuote(appContext, hour, minute)
                }
                Result.success()
            } else {
                Log.e(TAG, "Erreur API: ${response.code()} - ${response.message()}")
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
         * Planifie une tâche quotidienne pour afficher une citation.
         * @param context Le contexte de l'application.
         * @param hour L'heure de la journée (0-23) pour la notification.
         * @param minute La minute de l'heure (0-59) pour la notification.
         */
        fun scheduleDailyQuote(context: Context, hour: Int, minute: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Tentative de planification sans permission POST_NOTIFICATIONS.")
                // Vous pourriez vouloir informer l'utilisateur ici si cette fonction est appelée directement
                // après une interaction utilisateur où la permission aurait dû être demandée.
            }

            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            Log.d(TAG, "Planification de la citation quotidienne pour ${String.format("%02d", hour)}h${String.format("%02d", minute)}.")
            val calculatedDelay = calculateDelay(hour, minute)
            Log.d(TAG, "Délai calculé: $calculatedDelay ms (environ ${TimeUnit.MILLISECONDS.toMinutes(calculatedDelay)} minutes)")

            val dailyRequest = OneTimeWorkRequestBuilder<QuoteWorker>()
                .setInitialDelay(calculatedDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                "daily_quote",
                ExistingWorkPolicy.REPLACE,
                dailyRequest
            )
            Log.d(TAG, "Travail 'daily_quote' planifié.")
        }

        /**
         * Calcule le délai en millisecondes jusqu'à la prochaine occurrence de l'heure et minute cibles.
         * @param targetHour L'heure cible (0-23).
         * @param targetMinute La minute cible (0-59).
         * @return Le délai en millisecondes.
         */
        private fun calculateDelay(targetHour: Int, targetMinute: Int): Long {
            // Pour utiliser le fuseau horaire de l'appareil :
            val current = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Pour utiliser un fuseau horaire spécifique (ex: Paris) :
            // val tz = TimeZone.getTimeZone("Europe/Paris")
            // val current = Calendar.getInstance(tz)
            // val dueDate = Calendar.getInstance(tz).apply {
            //     set(Calendar.HOUR_OF_DAY, targetHour)
            //     set(Calendar.MINUTE, targetMinute)
            //     set(Calendar.SECOND, 0)
            //     set(Calendar.MILLISECOND, 0)
            // }

            if (current.after(dueDate)) {
                dueDate.add(Calendar.DATE, 1) // Planifier pour le lendemain si l'heure est déjà passée
            }

            val delay = dueDate.timeInMillis - current.timeInMillis
            // Log.d(TAG, "Current time: ${current.time}, Due date: ${dueDate.time}, Calculated delay: $delay ms")
            return delay
        }
    }
}