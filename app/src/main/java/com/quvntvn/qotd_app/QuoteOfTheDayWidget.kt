// QuoteOfTheDayWidget.kt
package com.quvntvn.qotd_app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color // Assurez-vous d'avoir cet import
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.*
import androidx.glance.layout.*
import androidx.glance.text.*
// Supprimez l'import de androidx.glance.state.PreferencesGlanceStateDefinition si vous ne l'utilisez pas directement
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.unit.ColorProvider // Cet import est correct
import com.quvntvn.qotd_app.R
import com.quvntvn.qotd_app.QuoteRefreshWorker

class QuoteOfTheDayWidget : GlanceAppWidget() {

    companion object {
        val quoteTextKey = stringPreferencesKey("quote_text")
        val quoteAuthorKey = stringPreferencesKey("quote_author")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()

            val quote  = prefs[quoteTextKey]    ?: context.getString(R.string.loading)
            val author = prefs[quoteAuthorKey]  ?: ""

            // Couleur de texte : Assurez-vous que R.color.white est bien défini dans vos colors.xml
            // Si vous avez l'erreur "ColorProviderKt.ColorProvider can only be called from within the same library group"
            // cela signifie que l'IDE a peut-être mal résolu l'import pour ColorProvider.
            // L'import androidx.glance.unit.ColorProvider est le bon.
            val commonTextStyle = TextStyle(
                color = ColorProvider(R.color.white), // Devrait fonctionner si R.color.white existe
                // et que l'import est correct.
                // Alternative : ColorProvider(Color.White) si R.color.white pose souci.
                textAlign = TextAlign.Center,
                fontFamily = FontFamily("uicksandedium")
            )

            // Définir la couleur de fond semi-transparente
            // Par exemple, un noir avec ~50% de transparence (alpha = 0x80)
            val semiTransparentBackground = Color(0x33000000)
            // Ou un blanc avec ~50% de transparence:
            // val semiTransparentBackground = Color(0x80FFFFFF)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    // Appliquer le fond semi-transparent
                    .background(ColorProvider(semiTransparentBackground)) // <--- MODIFIÉ ICI
                    .padding(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment   = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxSize()
                ) {

                    Text(
                        text      = "« $quote »",
                        style     = commonTextStyle.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines  = 5,
                        modifier  = GlanceModifier.padding(bottom = 4.dp)
                    )

                    if (author.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                    }

                    Text(
                        text  = author,
                        style = commonTextStyle.copy(
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic
                        ),
                        maxLines = 2,
                        modifier = GlanceModifier.takeUnless { author.isEmpty() } ?: GlanceModifier
                    )
                }
            }
        }
    }
}

class QuoteOfTheDayReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuoteOfTheDayWidget()

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        context?.let {
            QuoteRefreshWorker.schedule(it.applicationContext)
            QuoteRefreshWorker.refreshOnce(it.applicationContext)
        }
    }
}
