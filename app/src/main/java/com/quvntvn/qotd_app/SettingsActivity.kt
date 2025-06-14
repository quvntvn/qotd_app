package com.quvntvn.qotd_app

import android.os.Bundle
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.work.WorkManager

// 8. SettingsActivity.kt (Configuration)
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val (enabled, hour) = SharedPrefManager.getNotificationSettings(this)
        val switch = findViewById<SwitchCompat>(R.id.switch_notifications)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)

        switch.isChecked = enabled
        timePicker.hour = hour
        timePicker.minute = 0
        timePicker.isEnabled = enabled

        switch.setOnCheckedChangeListener { _, isChecked ->
            timePicker.isEnabled = isChecked
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val newHour = timePicker.hour
            SharedPrefManager.saveSettings(this, switch.isChecked, newHour)

            if (switch.isChecked) {
                QuoteWorker.scheduleDailyQuote(this, newHour)
            } else {
                WorkManager.getInstance(this).cancelUniqueWork("daily_quote")
            }

            Toast.makeText(this, "Paramètres sauvegardés", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}