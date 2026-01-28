package com.celdy.groufr.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleDailyAlarms() {
        scheduleAlarm(ALARM_REQUEST_MORNING, 9)
        scheduleAlarm(ALARM_REQUEST_EVENING, 18)
    }

    private fun scheduleAlarm(requestCode: Int, hourOfDay: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAtMillis = nextTriggerAtMillis(hourOfDay)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, NotificationAlarmReceiver::class.java)
                .setAction(ACTION_ALARM)
                .putExtra(EXTRA_ALARM_HOUR, hourOfDay),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun nextTriggerAtMillis(hourOfDay: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    companion object {
        const val ACTION_ALARM = "com.celdy.groufr.action.NOTIFICATION_ALARM"
        const val EXTRA_ALARM_HOUR = "alarm_hour"
        private const val ALARM_REQUEST_MORNING = 9100
        private const val ALARM_REQUEST_EVENING = 1800
    }
}
