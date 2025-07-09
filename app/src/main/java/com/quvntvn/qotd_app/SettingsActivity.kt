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
import androidx.work.WorkManager
import eightbitlab.com.blurview.BlurView // Import pour BlurView
import eightbitlab.com.blurview.RenderScriptBlur // Import pour l'algorithme de flou
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

            val workManager = WorkManager.getInstance(this.applicationContext)
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

    // Fonction mise à jour pour configurer toutes les BlurViews (portrait et paysage)
    private fun setupBlurViews() {
        val decorView = window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)
        val blurRadius = 15f // Ajustez le rayon de flou selon vos préférences

        // IDs du layout Portrait
        findViewById<BlurView>(R.id.blurSettingsTitle)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurNotificationsSection)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurTimePickerSection)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurLanguageSection)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurBtnBack)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurBtnSave)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        // IDs du layout Paysage (layout-land)
        // Ces appels ne feront rien si les ID ne sont pas dans le layout actuellement chargé (par ex. en mode portrait)
        findViewById<BlurView>(R.id.blurSettingsTitleLand)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurNotificationsSectionLand)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurTimePickerSectionLand)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurLanguageSectionLand)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurBtnBackLand)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)

        findViewById<BlurView>(R.id.blurBtnSaveLand)?.setupWith(rootView, RenderScriptBlur(this))
            ?.setBlurRadius(blurRadius)
            ?.setBlurAutoUpdate(true)
    }
}
