// QuoteOfTheDayWidget.kt
package com.quvntvn.qotd_app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.provideContent
import androidx.glance.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quvntvn.qotd_app.R
import com.quvntvn.qotd_app.MyApp
import com.quvntvn.qotd_app.QuoteRefreshWorker

class QuoteOfTheDayWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    companion object {
        val quoteTextKey = stringPreferencesKey("quote_text")
        val quoteAuthorKey = stringPreferencesKey("quote_author")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()

            val quote  = prefs[quoteTextKey]    ?: context.getString(R.string.loading)
            val author = prefs[quoteAuthorKey]  ?: ""
            // UI declarative style (Compose-like)
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment   = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxSize()
                ) {

                    Text(
                        text      = "« $quote »",
                        style     = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        maxLines  = 5,
                        modifier  = GlanceModifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text  = author,
                        style = TextStyle(fontSize = 12.sp, fontStyle = FontStyle.Italic)
                    )

                    // Extra spacing for readability
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

/** Receiver déclaré dans le manifeste */
class QuoteOfTheDayReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuoteOfTheDayWidget()

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        context?.let { QuoteRefreshWorker.schedule(it.applicationContext) }
    }
}
