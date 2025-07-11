package com.quvntvn.qotd_app

import android.content.Context
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.state.PreferencesGlanceStateDefinition
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.Alignment
import androidx.glance.text.Text
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simple app widget showing the quote of the day.
 */
class QuoteWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun onUpdate(context: Context, ids: IntArray) {
        super.onUpdate(context, ids)
        updateQuote(context, ids)
    }

    override suspend fun Content() {
        val prefs = currentState<Preferences>()
        val quote = prefs[KEY_QUOTE] ?: ""
        val author = prefs[KEY_AUTHOR] ?: ""
        val year = prefs[KEY_YEAR] ?: ""

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\u00AB $quote \u00BB",
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(android.graphics.Color.WHITE)
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = author,
                style = TextStyle(
                    textAlign = TextAlign.Center,
                    color = ColorProvider(android.graphics.Color.WHITE)
                )
            )
            if (year.isNotEmpty()) {
                Text(
                    text = year,
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                        color = ColorProvider(android.graphics.Color.WHITE)
                    )
                )
            }
        }
    }

    private suspend fun updateQuote(context: Context, ids: IntArray) {
        val repo = QuoteRepository()
        val quote = withContext(Dispatchers.IO) { repo.getDailyQuote() }
        ids.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                val mutable = prefs.toMutablePreferences()
                if (quote != null) {
                    mutable[KEY_QUOTE] = quote.citation
                    mutable[KEY_AUTHOR] = quote.auteur
                    mutable[KEY_YEAR] = quote.dateCreation?.take(4) ?: ""
                }
                mutable
            }
        }
        // Refresh widget UI
        updateAll(context)
    }

    companion object {
        private val KEY_QUOTE = stringPreferencesKey("quote_text")
        private val KEY_AUTHOR = stringPreferencesKey("quote_author")
        private val KEY_YEAR = stringPreferencesKey("quote_year")
    }
}

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuoteWidget()
}
