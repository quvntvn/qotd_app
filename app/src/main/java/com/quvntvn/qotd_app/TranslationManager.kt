package com.quvntvn.qotd_app

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.Translation
import kotlinx.coroutines.tasks.await

class TranslationManager(private val context: Context) {
    private val frToEnTranslator: Translator by lazy {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.FRENCH)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        Translation.getClient(options)
    }

    fun close() {
        try {
            frToEnTranslator.close()
        } catch (_: Exception) {
        }
    }

    suspend fun translate(text: String, target: String): String {
        if (target == "fr") return text
        val translator = when (target) {
            "en" -> frToEnTranslator
            else -> return text
        }
        translator.downloadModelIfNeeded().await()
        return translator.translate(text).await()
    }
}
