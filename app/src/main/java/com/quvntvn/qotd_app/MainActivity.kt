package com.quvntvn.qotd_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

// 9. MainActivity.kt (Activité principale)
class MainActivity : AppCompatActivity() {
    private val viewModel: QuoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRandom = findViewById<Button>(R.id.btnRandom)
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvQuote = findViewById<TextView>(R.id.tvQuote)
        val tvAuthor = findViewById<TextView>(R.id.tvAuthor)
        val tvYear = findViewById<TextView>(R.id.tvYear)

        // Initialisation WorkManager
        val (enabled, hour) = SharedPrefManager.getNotificationSettings(this)
        if (enabled) QuoteWorker.scheduleDailyQuote(this, hour)

        btnRandom.setOnClickListener { viewModel.loadRandomQuote() }
        btnSettings.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        viewModel.quote.observe(this) { quote ->
            quote?.let {
                tvQuote.text = it.citation
                tvAuthor.text = it.auteur
                tvYear.text = it.dateCreation.substring(0, 4)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.loadDailyQuote()
    }
}