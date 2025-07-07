package com.quvntvn.qotd_app

import android.os.Bundle
import android.widget.Button
import android.content.Context
import android.widget.TimePicker
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.work.WorkManager
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val context = LocaleHelper.wrapContext(newBase)
        super.attachBaseContext(context)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Récupérer l'heure ET les minutes depuis SharedPreferences
        // Assurez-vous que SharedPrefManager.getNotificationSettings est adapté
        // pour retourner également les minutes.
        val (enabled, savedHour, savedMinute) = SharedPrefManager.getNotificationSettings(this)

        val switchNotifications = findViewById<SwitchCompat>(R.id.switch_notifications)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        timePicker.setIs24HourView(true)
        val spinnerLanguage = findViewById<Spinner>(R.id.spinner_language)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.languages,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        val savedLanguage = SharedPrefManager.getLanguage(this)
        spinnerLanguage.setSelection(if (savedLanguage == "en") 1 else 0)

        switchNotifications.isChecked = enabled
        timePicker.hour = savedHour
        timePicker.minute = savedMinute // Utiliser la minute sauvegardée
        timePicker.isEnabled = enabled

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            timePicker.isEnabled = isChecked
        }

        findViewById<Button>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val newHour = timePicker.hour
            val newMinute = timePicker.minute
            val notificationsAreEnabled = switchNotifications.isChecked
            val selectedLanguage = if (spinnerLanguage.selectedItemPosition == 1) "en" else "fr"
            val languageChanged = selectedLanguage != savedLanguage
            SharedPrefManager.saveLanguage(this, selectedLanguage)

            val ctx = LocaleHelper.wrapContext(this)

            SharedPrefManager.saveSettings(this, notificationsAreEnabled, newHour, newMinute)

            val workManager = WorkManager.getInstance(this)
            workManager.cancelUniqueWork("daily_quote")

            if (notificationsAreEnabled) {
                QuoteWorker.scheduleDailyQuote(this, newHour, newMinute)
                val message = ctx.getString(R.string.notifications_scheduled, newHour, newMinute)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, ctx.getString(R.string.notifications_disabled), Toast.LENGTH_SHORT).show()
            }

            val resultIntent = Intent().putExtra("languageChanged", languageChanged)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}