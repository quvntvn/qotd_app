package com.quvntvn.qotd_app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log // Import pour le logging si besoin
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // Correction de l'import pour viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// 9. MainActivity.kt (Activité principale)
class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val context = LocaleHelper.wrapContext(newBase)
        super.attachBaseContext(context)
    }
    private val viewModel: QuoteViewModel by viewModels() // Utilisation correcte de viewModels
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnRandom = findViewById<Button>(R.id.btnRandom)
        val btnDaily = findViewById<Button>(R.id.btnDaily)
        val btnSettings = findViewById<ImageButton>(R.id.btn_settings)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvQuote = findViewById<TextView>(R.id.tvQuote)
        val tvAuthor = findViewById<TextView>(R.id.tvAuthor)
        val tvYear = findViewById<TextView>(R.id.tvYear)

        // Initialisation WorkManager
        // Récupérer enabled, hour, ET minute depuis SharedPrefManager
        val (enabled, hour, minute) = SharedPrefManager.getNotificationSettings(this)
        if (enabled) {
            Log.d("MainActivity", "Planification de la notification quotidienne depuis onCreate pour ${hour}h${String.format("%02d", minute)}.")
            // Passer l'heure ET la minute à scheduleDailyQuote
            QuoteWorker.scheduleDailyQuote(this, hour, minute)
        } else {
            Log.d("MainActivity", "Notifications désactivées, aucune planification depuis onCreate.")
        }

        btnRandom.setOnClickListener {
            viewModel.loadRandomQuote()
            btnDaily.visibility = View.VISIBLE
        }

        btnDaily.setOnClickListener {
            viewModel.loadDailyQuote()
            btnDaily.visibility = View.GONE
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        val translator = TranslationManager(this)
        viewModel.quote.observe(this) { quote ->
            quote?.let { q ->
                lifecycleScope.launch {
                    val lang = SharedPrefManager.getLanguage(this@MainActivity)
                    val text = translator.translate(q.citation, lang)
                    tvQuote.text = "\"$text\""
                    tvAuthor.text = q.auteur
                    val date = q.dateCreation
                    tvYear.text = if (date != null && date.length >= 4) {
                        date.substring(0, 4)
                    } else {
                        "N/A"
                    }
                }
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.loadDailyQuote() // Charger la citation quotidienne au démarrage
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        // Uniquement pour Android 13 (TIRAMISU) et versions ultérieures
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // La permission est déjà accordée
                    Log.d("MainActivity", "Permission POST_NOTIFICATIONS déjà accordée.")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Expliquer à l'utilisateur pourquoi la permission est nécessaire
                    // Vous pourriez afficher une boîte de dialogue ici avant de redemander
                    Log.d("MainActivity", "Affichage de la justification pour POST_NOTIFICATIONS.")
                    Toast.makeText(this, R.string.notification_permission_rationale, Toast.LENGTH_LONG).show()
                    // Puis demander la permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    // Demander directement la permission
                    Log.d("MainActivity", "Demande de la permission POST_NOTIFICATIONS.")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Pas besoin de demander la permission pour les versions antérieures à Android 13
            Log.d("MainActivity", "Pas besoin de demander la permission POST_NOTIFICATIONS pour API ${Build.VERSION.SDK_INT}.")
        }
    }
}