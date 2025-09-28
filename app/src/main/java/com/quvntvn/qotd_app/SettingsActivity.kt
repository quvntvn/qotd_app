package com.quvntvn.qotd_app

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eightbitlab.com.blurview.BlurView
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

        setContentView(R.layout.activity_settings)

        setupBlurViews()

        val (notificationsPreviouslyEnabled, savedHour, savedMinute) = SharedPrefManager.getNotificationSettings(this)
        val savedLanguage = SharedPrefManager.getLanguage(this)

        val switchNotifications = findViewById<SwitchCompat>(R.id.switch_notifications)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val languageSpinner = findViewById<Spinner>(R.id.spinner_language)

        timePicker.setIs24HourView(true)
        timePicker.hour = savedHour
        timePicker.minute = savedMinute
        timePicker.isEnabled = notificationsPreviouslyEnabled

        switchNotifications.isChecked = notificationsPreviouslyEnabled
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            timePicker.isEnabled = isChecked
            if (isChecked) {
                checkExactAlarmPermission()
            }
        }

        val languagesArray = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_layout,
            languagesArray
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_layout)
        languageSpinner.adapter = adapter

        val languageIndex = when (savedLanguage.lowercase(Locale.ROOT)) {
            "en" -> 1
            "fr" -> 0
            else -> 0
        }
        languageSpinner.setSelection(languageIndex)

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

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
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(localizedContext, R.string.exact_alarm_permission_toast, Toast.LENGTH_LONG).show()
                } else {
                    QuoteAlarmReceiver.scheduleDailyQuote(this.applicationContext, newHour, newMinute)
                    val message = localizedContext.getString(R.string.notifications_scheduled, newHour, newMinute)
                    Toast.makeText(localizedContext, message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(localizedContext, localizedContext.getString(R.string.notifications_disabled), Toast.LENGTH_SHORT).show()
            }

            val resultIntent = Intent().putExtra("languageChanged", languageHasChanged)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                showPermissionDialog()
            }
        }
    }

    private fun showPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.exact_alarm_permission_title)
            .setMessage(R.string.exact_alarm_permission_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
