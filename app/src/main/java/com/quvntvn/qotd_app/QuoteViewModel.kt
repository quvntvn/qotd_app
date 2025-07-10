package com.quvntvn.qotd_app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// 10. QuoteViewModel.kt (ViewModel)
class QuoteViewModel : ViewModel() {
    private val repository = QuoteRepository()
    val quote = MutableLiveData<Quote?>()
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<Int?>()

    private val defaultQuote = Quote(
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

class QuoteRepository {
    private val api = QuoteApi.create()

    suspend fun getDailyQuote(): Quote? {
        return try {
            api.getDailyQuote().body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRandomQuote(): Quote? {
        return try {
            api.getRandomQuote().body()
        } catch (e: Exception) {
            null
        }
    }
}