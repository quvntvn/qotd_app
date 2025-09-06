package com.quvntvn.qotd_app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import androidx.lifecycle.ViewModelProvider
import com.quvntvn.qotd_app.R

// 10. QuoteViewModel.kt (ViewModel)
class QuoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QuoteRepository(application.applicationContext)
    val quote = MutableLiveData<Quote?>()
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<Int?>()

    private val defaultQuote = Quote(
        0,
        "Le courage n'est pas l'absence de peur, mais la capacité de vaincre ce qui fait peur.",
        "Nelson Mandela",
        "1996"
    )

    fun loadDailyQuote() {
        viewModelScope.launch {
            isLoading.value = true
            val result = repository.getDailyQuote()
            if (result == null) {
                errorMessage.value = R.string.quote_error
                quote.value = defaultQuote
            } else {
                quote.value = result
                errorMessage.value = null
            }
            isLoading.value = false
        }
    }

    fun loadRandomQuote() {
        viewModelScope.launch {
            isLoading.value = true
            val result = repository.getRandomQuote()
            if (result == null) {
                errorMessage.value = R.string.quote_error
                quote.value = defaultQuote
            } else {
                quote.value = result
                errorMessage.value = null
            }
            isLoading.value = false
        }
    }
}

class QuoteViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuoteViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class QuoteRepository(private val context: Context) {
    private var quotes: List<Quote> = emptyList()

    init {
        loadQuotes()
    }

    private fun loadQuotes() {
        try {
            val inputStream = context.resources.openRawResource(R.raw.quotes)
            val reader = InputStreamReader(inputStream)
            val quoteListType = object : TypeToken<List<Quote>>() {}.type
            quotes = Gson().fromJson(reader, quoteListType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getDailyQuote(): Quote? {
        delay(300)
        return if (quotes.isNotEmpty()) {
            quotes.random()
        } else {
            null
        }
    }

    suspend fun getRandomQuote(): Quote? {
        delay(300)
        return if (quotes.isNotEmpty()) {
            quotes.random()
        } else {
            null
        }
    }
}