package com.quvntvn.qotd_app

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quvntvn.qotd_app.widget.QuoteOfTheDayWidget
import com.quvntvn.qotd_app.MyApp
import java.util.concurrent.TimeUnit
import java.util.Calendar

class QuoteRefreshWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val quoteTextKey = stringPreferencesKey("quote_text")
    private val quoteAuthorKey = stringPreferencesKey("quote_author")

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val glanceIds = GlanceAppWidgetManager(ctx).getGlanceIds(QuoteOfTheDayWidget::class.java)
        val repo = (ctx as MyApp).quoteRepository
        val quote = repo.getDailyQuote() ?: return Result.retry()

        glanceIds.forEach { id ->
            updateAppWidgetState(ctx, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs[quoteTextKey]   = quote.citation
                prefs[quoteAuthorKey] = quote.auteur
            }
            QuoteOfTheDayWidget().update(ctx, id)
        }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<QuoteRefreshWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(computeDelayUntilTomorrow(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "widget_refresh", ExistingPeriodicWorkPolicy.UPDATE, request
            )
        }

        private fun computeDelayUntilTomorrow(): Long {
            val now = Calendar.getInstance()
            val tomorrow = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }
            return tomorrow.timeInMillis - now.timeInMillis
        }
    }
}
