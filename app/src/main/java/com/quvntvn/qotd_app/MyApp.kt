package com.quvntvn.qotd_app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager

/**
 * Application class responsible for WorkManager initialization.
 */
class MyApp : Application(), Configuration.Provider {

    val quoteRepository: QuoteRepository by lazy { QuoteRepository() }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Manual initialization because WorkManagerInitializer is removed
        WorkManager.initialize(this, workManagerConfiguration)

        QuoteRefreshWorker.schedule(applicationContext)
        QuoteRefreshWorker.refreshOnce(applicationContext)
    }
}


