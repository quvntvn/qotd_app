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

    fun loadDailyQuote() {
        viewModelScope.launch {
            isLoading.value = true
            quote.value = repository.getDailyQuote()
            isLoading.value = false
        }
    }

    fun loadRandomQuote() {
        viewModelScope.launch {
            isLoading.value = true
            quote.value = repository.getRandomQuote()
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