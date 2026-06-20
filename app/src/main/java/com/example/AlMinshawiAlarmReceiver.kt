package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.util.PrayerTimesCalculator
import java.util.*
import kotlinx.coroutines.launch

class AlMinshawiAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "الصلاة"

        // Fire the Athan service
        val serviceIntent = Intent(context, AthanService::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Reschedule alarms for the day to keep cycle alive
        scheduleAlarms(context)
    }

    companion object {
        const val RC_PRAYER_BASE = 5000

        fun scheduleAlarms(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Check precision alarm permission on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Fail gracefully, schedule standard alarms or alert user in application
                    return
                }
            }

            // Using Room in thread-safe flow to retrieve settings
            val db = AppDatabase.getDatabase(context)
            
            // Run on a background thread
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val config = db.configDao().getConfig() ?: com.example.data.AppConfig()
                
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMin = calendar.get(Calendar.MINUTE)

                // Calculate prayer times
                val prayerTimes = PrayerTimesCalculator.calculate(
                    latitude = config.customLatitude,
                    longitude = config.customLongitude,
                    method = config.calculationMethod,
                    calendar = calendar
                )

                val prayersList = listOf(
                    Triple("الفجر", prayerTimes.fajr, config.isFajrEnabled),
                    Triple("الظهر", prayerTimes.dhuhr, config.isDhuhrEnabled),
                    Triple("العصر", prayerTimes.asr, config.isAsrEnabled),
                    Triple("المغرب", prayerTimes.maghrib, config.isMaghribEnabled),
                    Triple("العشاء", prayerTimes.isha, config.isIshaEnabled)
                )

                // Clear previous registered alarms before scheduling new ones
                for (i in 0 until 5) {
                    val cancelIntent = Intent(context, AlMinshawiAlarmReceiver::class.java).apply {
                        setClass(context, AlMinshawiAlarmReceiver::class.java)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        RC_PRAYER_BASE + i,
                        cancelIntent,
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )
                    pendingIntent?.let {
                        alarmManager.cancel(it)
                    }
                }

                // Register active prayer alarms
                prayersList.forEachIndexed { i, triple ->
                    val name = triple.first
                    val timeStr = triple.second
                    val isEnabled = triple.third
                    if (!isEnabled) return@forEachIndexed

                    // Parse HH:mm
                    val parts = timeStr.split(":")
                    if (parts.size != 2) return@forEachIndexed
                    val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
                    val min = parts[1].toIntOrNull() ?: return@forEachIndexed

                    val targetCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, min)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // If times have passed for today, schedule them for tomorrow
                    if (targetCal.timeInMillis <= System.currentTimeMillis()) {
                        targetCal.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    val alarmIntent = Intent(context, AlMinshawiAlarmReceiver::class.java).apply {
                        putExtra("PRAYER_NAME", name)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        RC_PRAYER_BASE + i,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                targetCal.timeInMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                targetCal.timeInMillis,
                                pendingIntent
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
