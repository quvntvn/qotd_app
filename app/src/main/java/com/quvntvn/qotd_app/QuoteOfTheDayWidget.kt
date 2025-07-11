// QuoteOfTheDayWidget.kt
package com.quvntvn.qotd_app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.provideContent
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.quvntvn.qotd_app.R
import kotlinx.coroutines.runBlocking

class QuoteOfTheDayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = currentState(key = PreferencesGlanceStateDefinition)

        val quote  = prefs["quote_text"]    ?: context.getString(R.string.loading)
        val author = prefs["quote_author"]  ?: ""

        provideContent {
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

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Bouton “Refresh”
                    Button(
                        text     = "Nouvelle citation",
                        onClick  = actionRunCallback<RefreshAction>()
                    )
                }
            }
        }
    }

    /** Action exécutée quand l’utilisateur appuie sur le bouton */
    class RefreshAction : ActionCallback {
        override suspend fun onRun(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
            // Ici tu récupères une citation (API, base locale, etc.)
            val repo = (context.applicationContext as MyApp).quoteRepository
            val quote = repo.randomQuote()

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs["quote_text"]   = quote.citation
                prefs["quote_author"] = quote.auteur
            }

            QuoteOfTheDayWidget().update(context, glanceId) // force le re-compose
        }
    }
}

/** Receiver déclaré dans le manifeste */
class QuoteOfTheDayReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuoteOfTheDayWidget()
}
