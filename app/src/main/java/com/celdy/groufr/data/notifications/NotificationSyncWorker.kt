package com.celdy.groufr.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class NotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncEntryPoint::class.java
        )
        entryPoint.notificationSyncManager().forceSync()
        return Result.success()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncEntryPoint {
        fun notificationSyncManager(): NotificationSyncManager
    }
}
