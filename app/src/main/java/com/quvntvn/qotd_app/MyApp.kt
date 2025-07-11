package com.quvntvn.qotd_app

import android.app.Application

/**
 * Application class exposing a shared [QuoteRepository] instance.
 */
class MyApp : Application() {
    val quoteRepository: QuoteRepository by lazy { QuoteRepository() }
}
