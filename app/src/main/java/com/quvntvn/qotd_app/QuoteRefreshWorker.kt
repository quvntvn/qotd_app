import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quvntvn.qotd_app.widget.QuoteOfTheDayWidget

class QuoteRefreshWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val glanceIds = GlanceAppWidgetManager(ctx).getGlanceIds(QuoteOfTheDayWidget::class.java)
        val repo = (ctx as MyApp).quoteRepository
        val quote = repo.dailyQuote()

        glanceIds.forEach { id ->
            updateAppWidgetState(ctx, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs["quote_text"]   = quote.citation
                prefs["quote_author"] = quote.auteur
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
    }
}
