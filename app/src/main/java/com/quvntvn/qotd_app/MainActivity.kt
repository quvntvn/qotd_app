package com.quvntvn.qotd_app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View // Changement : Importation de android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
// androidx.constraintlayout.widget.ConstraintLayout // Plus nécessaire pour le glassPanel directement ici
import androidx.core.content.ContextCompat
import androidx.glance.visibility
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private val viewModel: QuoteViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    R.string.notification_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK &&
                (result.data?.getBooleanExtra("languageChanged", false) ?: false)
            ) {
                recreate()
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

        val glassBackground = findViewById<View>(R.id.glass_background_view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRadiusX = 60f
            val blurRadiusY = 60f
            val blur = RenderEffect.createBlurEffect(
                blurRadiusX,
                blurRadiusY,
                Shader.TileMode.MIRROR
            )
            glassBackground.setRenderEffect(blur)
        }

        val (enabled, hour, minute) = SharedPrefManager.getNotificationSettings(this)
        if (enabled) {
            Log.d("MainActivity", "Planif. notif quotidienne : ${hour}h${"%02d".format(minute)}")
            QuoteWorker.scheduleDailyQuote(this, hour, minute)
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
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }

        val translator = TranslationManager(this)

        viewModel.quote.observe(this) { quote ->
            quote ?: return@observe
            lifecycleScope.launch {
                val lang = SharedPrefManager.getLanguage(this@MainActivity)
                val translated = translator.translate(quote.citation, lang)
                tvQuote.text = "« $translated »"
                tvAuthor.text = quote.auteur
                tvYear.text = quote.dateCreation?.take(4) ?: "N/A"
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.loadDailyQuote()
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
            }

            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                Toast.makeText(
                    this,
                    R.string.notification_permission_rationale,
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
