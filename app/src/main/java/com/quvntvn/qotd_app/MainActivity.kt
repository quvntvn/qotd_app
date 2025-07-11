package com.quvntvn.qotd_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
// Supprimez les imports non utilisés si Android Studio les signale (comme text, glance.visibility)
// import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.glance.visibility
// import androidx.glance.visibility
import androidx.lifecycle.lifecycleScope
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.launch
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    /* ------------------------------------------------------------------ */
    /*  Helpers de langue, ViewModel, permissions                         */
    /* ------------------------------------------------------------------ */

    override fun attachBaseContext(newBase: Context) =
        super.attachBaseContext(LocaleHelper.wrapContext(newBase)) // Assurez-vous que LocaleHelper existe et fonctionne

    private val viewModel: QuoteViewModel by viewModels() // Assurez-vous que QuoteViewModel existe et est configuré

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted)
                Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show()
        }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK &&
                (result.data?.getBooleanExtra("languageChanged", false) ?: false)
            ) {
                recreate() // Recrée l'activité si la langue a changé
            }
        }

    /* ------------------------------------------------------------------ */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make system bars transparent so the app background extends behind them
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContentView(R.layout.activity_main)

        // ---------- Vues ----------
        // Assurez-vous que tous ces IDs existent dans R.layout.activity_main
        val blurCard        = findViewById<BlurView>(R.id.blurCard)
        val btnRandom       = findViewById<Button>(R.id.btnRandom)
        val btnDaily        = findViewById<Button>(R.id.btnDaily) // Toujours nécessaire pour le listener
        val imageBtnSettings = findViewById<ImageButton>(R.id.btn_settings) // ImageButton pour le listener (ID du bouton lui-même)
        val tvQuote         = findViewById<TextView>(R.id.tvQuote)
        val tvAuthor        = findViewById<TextView>(R.id.tvAuthor)
        val tvYear          = findViewById<TextView>(R.id.tvYear)
        val progressBar     = findViewById<ProgressBar>(R.id.progressBar)
        val divider         = findViewById<View>(R.id.quote_author_divider)
        // val tvAppName       = findViewById<TextView>(R.id.tvAppName) // Le TextView lui-même n'est plus directement manipulé pour le flou

        // BlurViews
        val blurBtnDaily    = findViewById<BlurView>(R.id.blurBtnDaily)
        val blurBtnRandom   = findViewById<BlurView>(R.id.blurBtnRandom)
        val blurBtnSettings = findViewById<BlurView>(R.id.blurBtnSettings) // ID du BlurView pour le bouton settings
        val blurAppName     = findViewById<BlurView>(R.id.blurAppName)     // **NOUVEAU** ID du BlurView pour le titre


        // ---------- BlurView Setup ----------
        val decorView = window.decorView
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

        blurCard.setupWith(rootView, RenderScriptBlur(this))
            .setBlurRadius(20f)
            .setBlurAutoUpdate(true)

        blurBtnDaily.setupWith(rootView, RenderScriptBlur(this))
            .setBlurRadius(15f)
            .setBlurAutoUpdate(true)

        blurBtnRandom.setupWith(rootView, RenderScriptBlur(this))
            .setBlurRadius(15f)
            .setBlurAutoUpdate(true)

        blurBtnSettings.setupWith(rootView, RenderScriptBlur(this))
            .setBlurRadius(10f) // Ajustez selon vos préférences
            .setBlurAutoUpdate(true)

        blurAppName.setupWith(rootView, RenderScriptBlur(this)) // **NOUVEAU**
            .setBlurRadius(10f) // Ajustez selon vos préférences
            .setBlurAutoUpdate(true)


        // ---------- Notifications planifiées (si activées au démarrage) ----------
        SharedPrefManager.getNotificationSettings(this).apply {
            if (this.first) {
                QuoteWorker.scheduleDailyQuote(this@MainActivity, this.second, this.third)
            }
        }

        // ---------- Listeners ----------
        btnRandom.setOnClickListener {
            viewModel.loadRandomQuote()
            blurBtnDaily.visibility = View.VISIBLE
        }

        btnDaily.setOnClickListener {
            viewModel.loadDailyQuote()
            blurBtnDaily.visibility = View.GONE
        }

        // Le listener est sur ImageButton, pas sur le BlurView qui l'enveloppe
        imageBtnSettings.setOnClickListener {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
        }

        // ---------- Observers ----------
        val translator = TranslationManager(this)

        viewModel.quote.observe(this) { q ->
            q ?: return@observe
            lifecycleScope.launch {
                val lang = SharedPrefManager.getLanguage(this@MainActivity)
                tvQuote.text  = "« ${translator.translate(q.citation, lang)} »"
                tvAuthor.text = q.auteur

                val dateText = q.dateCreation?.take(4)
                if (dateText.isNullOrBlank()) {
                    tvYear.visibility = View.GONE
                } else {
                    tvYear.text = dateText
                    tvYear.visibility = View.VISIBLE
                }
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            divider.visibility = if (loading) View.GONE else View.VISIBLE
            val contentVisibility = if (loading) View.INVISIBLE else View.VISIBLE
            tvQuote.visibility = contentVisibility
            tvAuthor.visibility = contentVisibility
            if (loading) {
                tvYear.visibility = View.INVISIBLE
            }
        }

        viewModel.errorMessage.observe(this) { msgRes ->
            msgRes ?: return@observe
            Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show()
        }

        // Charge la citation du jour au démarrage de l'activité
        viewModel.loadDailyQuote()
        blurBtnDaily.visibility = View.GONE

        // Demande la permission de notification si nécessaire (Android 13+)
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED -> {
                // La permission est déjà accordée
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                Toast.makeText(this, R.string.notification_permission_rationale, Toast.LENGTH_LONG).show()
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
