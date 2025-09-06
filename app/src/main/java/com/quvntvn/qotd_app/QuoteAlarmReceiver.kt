package com.quvntvn.qotd_app

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class QuoteAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = (context.applicationContext as MyApp).quoteRepository
                val quote = repository.getRandomQuote()
                if (quote != null) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        NotificationHelper(context).showNotification(quote)
                    }
                }
            } catch (e: Exception) {
                // Ignore errors
            } finally {
                // Re-planifier pour le lendemain si toujours activé
                val settings = SharedPrefManager.getNotificationSettings(context)
                if (settings.first) {
                    scheduleDailyQuote(context, settings.second, settings.third)
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 1001

        fun scheduleDailyQuote(context: Context, hour: Int, minute: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = getPendingIntent(context)
            val triggerAt = calculateTriggerTime(hour, minute)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }

        fun cancelDailyQuote(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(getPendingIntent(context))
        }

        private fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, QuoteAlarmReceiver::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        }

        private fun calculateTriggerTime(hour: Int, minute: Int): Long {
            val now = Calendar.getInstance()
            val dueTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (dueTime.before(now)) {
                dueTime.add(Calendar.DAY_OF_YEAR, 1)
            }
            return dueTime.timeInMillis
        }
    }
}
