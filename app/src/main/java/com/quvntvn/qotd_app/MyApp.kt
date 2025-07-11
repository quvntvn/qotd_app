package com.quvntvn.qotd_app

import android.app.Application

class MyApp : Application() {
    val quoteRepository: QuoteRepository by lazy { QuoteRepository() }
}

