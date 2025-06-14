package com.quvntvn.qotd_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.quvntvn.qotd_app.ui.theme.Qotd_appTheme

// 9. MainActivity.kt (Activité principale)
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: QuoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialisation WorkManager
        val (enabled, hour) = SharedPrefManager.getNotificationSettings(this)
        if (enabled) QuoteWorker.scheduleDailyQuote(this, hour)

        setupUI()
        setupObservers()
        viewModel.loadDailyQuote()
    }

    private fun setupUI() {
        binding.apply {
            btnRandom.setOnClickListener { viewModel.loadRandomQuote() }
            btnSettings.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }
    }

    private fun setupObservers() {
        viewModel.quote.observe(this) { quote ->
            quote?.let {
                binding.tvQuote.text = it.citation
                binding.tvAuthor.text = it.auteur
                binding.tvYear.text = it.dateCreation.substring(0, 4)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }
}