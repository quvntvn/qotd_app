package com.quvntvn.qotd_app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup // Nécessaire pour rootView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
// import androidx.core.view.WindowInsetsControllerCompat // Non explicitement utilisé dans ce code modifié
import com.quvntvn.qotd_app.QuoteAlarmReceiver
import eightbitlab.com.blurview.BlurView // Import pour BlurView
import eightbitlab.com.blurview.BlurTarget
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    // Appliquer le contexte wrappé pour la gestion de la langue
    override fun attachBaseContext(newBase: Context) {
        val context = LocaleHelper.wrapContext(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Rendre les barres système transparentes
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        // Optionnel : ajuster l'apparence des icônes de la barre d'état si nécessaire
        // WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_settings) // Ceci charge activity_settings.xml ou res/layout-land/activity_settings.xml

        // Initialisation des BlurViews (maintenant pour portrait ET paysage)
        setupBlurViews()

        // Récupérer les paramètres sauvegardés
        val (notificationsPreviouslyEnabled, savedHour, savedMinute) = SharedPrefManager.getNotificationSettings(this)
        val savedLanguage = SharedPrefManager.getLanguage(this)

        // Initialisation des vues (ces ID doivent être les mêmes dans les deux layouts)
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
        val languagesArray = resources.getStringArray(R.array.languages) // Assurez-vous que c'est le bon nom d'array
        // Utiliser les layouts personnalisés pour le Spinner pour le texte blanc
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_layout, // Layout pour l'élément sélectionné
            languagesArray
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout) // Layout pour les éléments du dropdown
        languageSpinner.adapter = adapter

        // Sélectionner la langue sauvegardée dans le Spinner
        val languageIndex = when (savedLanguage.lowercase(Locale.ROOT)) {
            "en" -> 1
            "fr" -> 0
            else -> 0
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
            val selectedLanguageName = languagesArray.getOrElse(selectedPosition) { languagesArray[0] }

            val selectedLanguageCode = if (selectedLanguageName.equals("English", ignoreCase = true)) {
                "en"
            } else {
                "fr"
            }

            val languageHasChanged = selectedLanguageCode != savedLanguage

            SharedPrefManager.saveLanguage(this, selectedLanguageCode)
            val localizedContext = LocaleHelper.wrapContext(this)

            SharedPrefManager.saveSettings(this, notificationsAreNowEnabled, newHour, newMinute)

            QuoteAlarmReceiver.cancelDailyQuote(this.applicationContext)

            if (notificationsAreNowEnabled) {
                QuoteAlarmReceiver.scheduleDailyQuote(this.applicationContext, newHour, newMinute)
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

    // Fonction mise à jour pour configurer toutes les BlurViews (portrait et paysage)
    private fun setupBlurViews() {
        val blurTarget = findViewById<BlurTarget>(R.id.settings_blur_target)
        val blurRadius = 15f // Ajustez le rayon de flou selon vos préférences

        // IDs du layout Portrait
        findViewById<BlurView>(R.id.blurSettingsTitle)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurNotificationsSection)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurTimePickerSection)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurLanguageSection)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurBtnBack)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurBtnSave)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        // IDs du layout Paysage (layout-land)
        // Ces appels ne feront rien si les ID ne sont pas dans le layout actuellement chargé (par ex. en mode portrait)
        findViewById<BlurView>(R.id.blurSettingsTitleLand)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurNotificationsSectionLand)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurTimePickerSectionLand)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurLanguageSectionLand)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurBtnBackLand)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurBtnSaveLand)?.setupWith(blurTarget)
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)
    }
}
