package com.celdy.groufr

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.celdy.groufr.data.notifications.NotificationAlarmScheduler
import com.celdy.groufr.data.notifications.NotificationSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GroufrApplication : Application() {
    @Inject lateinit var notificationSyncManager: NotificationSyncManager
    @Inject lateinit var notificationAlarmScheduler: NotificationAlarmScheduler

    override fun onCreate() {
        super.onCreate()
        notificationSyncManager.schedulePeriodicWork()
        notificationAlarmScheduler.scheduleDailyAlarms()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                notificationSyncManager.onAppForeground()
            }

            override fun onActivityStopped(activity: Activity) {
                notificationSyncManager.onAppBackground()
            }

            override fun onActivityResumed(activity: Activity) {
                notificationSyncManager.maybeSyncOnResume()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
