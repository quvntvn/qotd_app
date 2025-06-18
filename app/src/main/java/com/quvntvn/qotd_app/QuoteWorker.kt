package com.quvntvn.qotd_app

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// 6. QuoteWorker.kt (Tâche de fond)
class QuoteWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val api = QuoteApi.create()
            val response = api.getDailyQuote()

            if (response.isSuccessful) {
                response.body()?.let {
                    NotificationHelper(applicationContext).showNotification(it)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun scheduleDailyQuote(context: Context, hour: Int = 10) {
            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val dailyRequest = PeriodicWorkRequestBuilder<QuoteWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateDelay(hour), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "daily_quote",
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyRequest
            )
        }

        private fun calculateDelay(targetHour: Int): Long {
            val tz = TimeZone.getTimeZone("Europe/Paris")
            val current = Calendar.getInstance(tz)
            val dueDate = Calendar.getInstance(tz).apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            if (current.after(dueDate)) {
                dueDate.add(Calendar.DATE, 1)
            }

            return dueDate.timeInMillis - current.timeInMillis
        }
    }
}