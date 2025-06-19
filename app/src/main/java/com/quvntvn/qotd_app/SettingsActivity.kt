package com.quvntvn.qotd_app

import android.os.Bundle
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.work.WorkManager
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
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
            val newMinute = timePicker.minute // <--- RÉCUPÉRER LES MINUTES
            val notificationsAreEnabled = switchNotifications.isChecked

            // Adapter SharedPrefManager pour sauvegarder aussi les minutes
            SharedPrefManager.saveSettings(this, notificationsAreEnabled, newHour, newMinute) // <--- PASSER LES MINUTES ICI

            val workManager = WorkManager.getInstance(this)
            workManager.cancelUniqueWork("daily_quote")

            if (notificationsAreEnabled) {
                // Passer l'heure ET les minutes
                QuoteWorker.scheduleDailyQuote(this, newHour, newMinute) // <--- PASSER LES MINUTES
                val message = String.format(
                    Locale.getDefault(),
                    "Notifications programmées pour %02dh%02d",
                    newHour,
                    newMinute
                )
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications désactivées", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}