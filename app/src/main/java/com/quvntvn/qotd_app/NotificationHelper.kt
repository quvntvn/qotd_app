package com.quvntvn.qotd_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Locale

// 5. NotificationHelper.kt (Gestion des notifications)
class NotificationHelper(private val context: Context) {
    private val channelId = "quote_channel"
    private val notificationId = 101

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Citations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Citations quotidiennes"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(quote: Quote) {
        val year = quote.dateCreation?.let {
            try {
                SimpleDateFormat("yyyy", Locale.getDefault()).format(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)!!
                )
            } catch (e: Exception) {
                "N/A"
            }
        } ?: "N/A"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Citation du jour")
            .setContentText(quote.citation)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quote.citation))
            .setSubText("${quote.auteur} ($year)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}