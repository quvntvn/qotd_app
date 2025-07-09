package com.quvntvn.qotd_app

// Imports nécessaires pour cette activité
import android.content.Context
import android.content.Intent
import android.os.Bundle
// android.view.View // Peut être retiré si non explicitement utilisé par un listener
// android.widget.AdapterView // Peut être retiré si onItemSelectedListener n'est pas utilisé directement
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.work.WorkManager
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    // Appliquer le contexte wrappé pour la gestion de la langue
    override fun attachBaseContext(newBase: Context) {
        val context = LocaleHelper.wrapContext(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make system bars transparent so the app background extends behind them
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContentView(R.layout.activity_settings)

        // Récupérer les paramètres sauvegardés
        val (notificationsPreviouslyEnabled, savedHour, savedMinute) = SharedPrefManager.getNotificationSettings(this)
        val savedLanguage = SharedPrefManager.getLanguage(this)

        // Initialisation des vues
        val switchNotifications = findViewById<SwitchCompat>(R.id.switch_notifications)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val languageSpinner = findViewById<Spinner>(R.id.spinner_language)

        // Configuration du TimePicker
        timePicker.setIs24HourView(true)
        timePicker.hour = savedHour
        timePicker.minute = savedMinute
        timePicker.isEnabled = notificationsPreviouslyEnabled

        // Configuration du Switch pour les notifications
        switchNotifications.isChecked = notificationsPreviouslyEnabled
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            timePicker.isEnabled = isChecked
        }

        // Configuration du Spinner pour la langue
        val languagesArray = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            languagesArray
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        // Sélectionner la langue sauvegardée dans le Spinner
        // Le premier élément du tableau correspond au français dans toutes les langues
        // et le second à l'anglais. On sélectionne donc directement l'index selon le code enregistré.
        val languageIndex = when (savedLanguage.lowercase(Locale.ROOT)) {
            "en" -> 1
            "fr" -> 0
            else -> 0 // Sécurité : défaut sur le français
        }
        languageSpinner.setSelection(languageIndex)

        // Bouton Retour
        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Bouton Sauvegarder
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val newHour = timePicker.hour
            val newMinute = timePicker.minute
            val notificationsAreNowEnabled = switchNotifications.isChecked

            val selectedPosition = languageSpinner.selectedItemPosition
            val selectedLanguageName = languagesArray.getOrElse(selectedPosition) { languagesArray[0] } // Sécurité

            val selectedLanguageCode = if (selectedLanguageName.equals("English", ignoreCase = true)) {
                "en"
            } else { // Par défaut à "fr" (si "Français" est l'autre option)
                "fr"
            }

            val languageHasChanged = selectedLanguageCode != savedLanguage

            SharedPrefManager.saveLanguage(this, selectedLanguageCode)
            val localizedContext = LocaleHelper.wrapContext(this) // Obtenir le contexte mis à jour APRES avoir sauvegardé la langue

            SharedPrefManager.saveSettings(this, notificationsAreNowEnabled, newHour, newMinute)

            val workManager = WorkManager.getInstance(this.applicationContext)
            // Assurez-vous que QuoteWorker.UNIQUE_WORK_NAME est défini dans QuoteWorker.kt
            workManager.cancelUniqueWork(QuoteWorker.UNIQUE_WORK_NAME)

            if (notificationsAreNowEnabled) {
                QuoteWorker.scheduleDailyQuote(this.applicationContext, newHour, newMinute)
                val message = localizedContext.getString(R.string.notifications_scheduled, newHour, newMinute)
                Toast.makeText(localizedContext, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(localizedContext, localizedContext.getString(R.string.notifications_disabled), Toast.LENGTH_SHORT).show()
            }

            val resultIntent = Intent().putExtra("languageChanged", languageHasChanged)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
