package com.quvntvn.qotd_app

import android.app.Application
import com.quvntvn.qotd_app.QuoteRefreshWorker

class MyApp : Application() {
    val quoteRepository: QuoteRepository by lazy { QuoteRepository() }

    override fun onCreate() {
        super.onCreate()
        QuoteRefreshWorker.schedule(applicationContext)
    }
}

